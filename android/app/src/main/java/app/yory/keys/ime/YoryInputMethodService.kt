package app.yory.keys.ime

import android.content.Intent
import android.content.ClipboardManager
import android.media.AudioManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import app.yory.keys.MainActivity
import app.yory.keys.theme.YoryTheme
import app.yory.keys.theme.YoryThemeStore

/**
 * First functional Android IME slice:
 * - commits text through InputConnection
 * - supports English/Arabic layouts, shift, delete, enter, symbols and emoji
 * - reads the active local theme from the host app
 * - uses a small on-device completion dictionary for non-sensitive fields
 *
 * This is a functional foundation, not yet a full neural prediction engine.
 */
class YoryInputMethodService : InputMethodService() {
    private var theme: YoryTheme = YoryTheme.default
    private var languageTag: String = "en-US"
    private var shifted = false
    private var symbolMode = false
    private var emojiMode = false
    private var clipboardMode = false
    private var sensitiveField = false
    private var numericField = false
    private var emailField = false
    private var urlField = false
    private var keyboardRoot: LinearLayout? = null
    private var suggestionRow: LinearLayout? = null
    private var clipboardRow: LinearLayout? = null
    private var emojiRow: LinearLayout? = null
    private var keysContainer: LinearLayout? = null
    private var wordSuggestions: LocalWordSuggestions? = null
    private val learningExecutor: ExecutorService = Executors.newSingleThreadExecutor { task ->
        Thread(task, "YORY-PersonalLearning").apply { isDaemon = true }
    }
    private var clipboardListenerRegistered = false
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener { captureClipboard() }

    override fun onCreateInputView(): View {
        wordSuggestions = LocalWordSuggestions(applicationContext)
        theme = YoryThemeStore.load(applicationContext)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(7), dp(7), dp(7), dp(5))
            background = panelBackground()
        }
        keyboardRoot = root

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addToolbarButton(toolbar, "☺", 1f) {
            emojiMode = !emojiMode
            refreshKeyboard()
        }
        addToolbarButton(toolbar, "⇄", 1f) {
            languageTag = if (languageTag.startsWith("ar")) "en-US" else "ar"
            shifted = false
            refreshKeyboard()
            updateSuggestions()
        }
        addToolbarButton(toolbar, "123", 1f) {
            symbolMode = !symbolMode
            refreshKeyboard()
        }
        addToolbarButton(toolbar, "⚙", 1f) { openStudio() }
        addToolbarButton(toolbar, "▣", 1f) {
            clipboardMode = !clipboardMode
            refreshKeyboard()
            refreshClipboardRow()
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(38)))

        suggestionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(2))
        }
        root.addView(suggestionRow, LinearLayout.LayoutParams(-1, dp(34)))

        clipboardRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        root.addView(clipboardRow, LinearLayout.LayoutParams(-1, dp(42)))

        emojiRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        listOf("😀", "😂", "🥰", "❤️", "✨", "🙏", "👍", "🔥").forEach { emoji ->
            addToolbarButton(emojiRow!!, emoji, 1f) { currentInputConnection?.commitText(emoji, 1) }
        }
        root.addView(emojiRow, LinearLayout.LayoutParams(-1, dp(42)))

        keysContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(keysContainer, LinearLayout.LayoutParams(-1, -2))

        refreshKeyboard()
        updateSuggestions()
        refreshClipboardListener()
        return root
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        theme = YoryThemeStore.load(applicationContext)
        sensitiveField = isSensitive(info)
        val inputClass = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        numericField = inputClass == InputType.TYPE_CLASS_NUMBER ||
            inputClass == InputType.TYPE_CLASS_PHONE
        emailField = inputClass == InputType.TYPE_CLASS_TEXT &&
            (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
        urlField = inputClass == InputType.TYPE_CLASS_TEXT &&
            variation == InputType.TYPE_TEXT_VARIATION_URI
        clipboardMode = false
        refreshAutomaticShift()
        refreshKeyboard()
        updateSuggestions()
        refreshClipboardListener()
    }

    private fun isSensitive(info: EditorInfo?): Boolean {
        if (info == null) return true
        val inputClass = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        return (inputClass == InputType.TYPE_CLASS_TEXT &&
            (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
            (inputClass == InputType.TYPE_CLASS_NUMBER &&
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
    }

    private fun refreshKeyboard() {
        val container = keysContainer ?: return
        theme = YoryThemeStore.load(applicationContext)
        keyboardRoot?.background = panelBackground()
        val direction = if (languageTag.startsWith("ar") && !numericField && !emailField && !urlField) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        keyboardRoot?.layoutDirection = direction
        container.layoutDirection = direction
        container.removeAllViews()

        emojiRow?.visibility = if (emojiMode) View.VISIBLE else View.GONE
        suggestionRow?.visibility = if (sensitiveField || numericField || emojiMode || clipboardMode) View.GONE else View.VISIBLE
        clipboardRow?.visibility = if (theme.clipboardEnabled && clipboardMode && !sensitiveField && !numericField) View.VISIBLE else View.GONE

        if (numericField) {
            addNumericLayout(container)
            return
        }

        if (symbolMode) {
            addKeyRow(container, "1234567890")
            addKeyRow(container, "@#$%&-*+=")
            addKeyRow(container, "()[]{}⌫")
        } else {
            addKeyRow(container, if (languageTag.startsWith("ar")) "ضصثقفغعهخح" else "qwertyuiop")
            addKeyRow(container, if (languageTag.startsWith("ar")) "شسيبلاتنم" else "asdfghjkl")
            addSpecialLetterRow(container)
        }
        addBottomRow(container)
    }

    private fun addNumericLayout(parent: LinearLayout) {
        addKeyRow(parent, "123")
        addKeyRow(parent, "456")
        addKeyRow(parent, "789")
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addKey(bottom, "-", 1f) { currentInputConnection?.commitText("-", 1) }
        addKey(bottom, "0", 1.2f) { currentInputConnection?.commitText("0", 1) }
        addKey(bottom, if (emailField) "@" else if (urlField) "/" else ".", 1f) {
            currentInputConnection?.commitText(if (emailField) "@" else if (urlField) "/" else ".", 1)
        }
        addKey(bottom, "⌫", 1f) { handleKey("⌫") }
        parent.addView(bottom, LinearLayout.LayoutParams(-1, dp(theme.keyHeightDp)))
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addKey(actions, "🌐", 1f) {
            languageTag = if (languageTag.startsWith("ar")) "en-US" else "ar"
            refreshKeyboard()
        }
        addKey(actions, "↵", 1f) {
            val handled = sendDefaultEditorAction(true)
            if (!handled) currentInputConnection?.commitText("\n", 1)
        }
        parent.addView(actions, LinearLayout.LayoutParams(-1, dp(theme.keyHeightDp)))
    }

    private fun addKeyRow(parent: LinearLayout, letters: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        letters.forEach { char ->
            val label = if (shifted && char.isLetter()) char.uppercaseChar().toString() else char.toString()
            addKey(row, label, 1f) { handleKey(label) }
        }
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(theme.keyHeightDp)))
    }

    private fun addSpecialLetterRow(parent: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addKey(row, "⇧", 1.25f) { shifted = !shifted; refreshKeyboard() }
        val letters = if (languageTag.startsWith("ar")) "ئءؤرىةوز" else "zxcvbnm"
        letters.forEach { char ->
            val label = if (shifted && char.isLetter()) char.uppercaseChar().toString() else char.toString()
            addKey(row, label, 1f) { handleKey(label) }
        }
        addKey(row, "⌫", 1.25f) { handleKey("⌫") }
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(theme.keyHeightDp)))
    }

    private fun addBottomRow(parent: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addKey(row, if (symbolMode) "ABC" else "!#1", 1.15f) {
            symbolMode = !symbolMode
            refreshKeyboard()
        }
        addKey(row, "🌐", 1f) {
            languageTag = if (languageTag.startsWith("ar")) "en-US" else "ar"
            shifted = false
            refreshKeyboard()
            updateSuggestions()
        }
        addKey(row, if (emailField) "@" else if (urlField) "/" else ",", 0.8f) {
            learnCurrentWord()
            currentInputConnection?.commitText(if (emailField) "@" else if (urlField) "/" else ",", 1)
            updateSuggestions()
        }
        addKey(row, if (languageTag.startsWith("ar")) "العربية" else "English", 3.8f) {
            learnCurrentWord()
            currentInputConnection?.commitText(" ", 1)
            refreshAutomaticShift()
            updateSuggestions()
        }
        addKey(row, ".", 0.8f) {
            learnCurrentWord()
            currentInputConnection?.commitText(".", 1)
            updateSuggestions()
        }
        addKey(row, "↵", 1.15f) {
            learnCurrentWord()
            val handled = sendDefaultEditorAction(true)
            if (!handled) currentInputConnection?.commitText("\n", 1)
            refreshAutomaticShift()
            updateSuggestions()
        }
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(theme.keyHeightDp)))
    }

    private fun addKey(parent: LinearLayout, label: String, weight: Float, action: () -> Unit) {
        val view = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = if (label == "space" || label == "English" || label == "العربية") {
                (theme.fontSizeSp - 6f).coerceAtLeast(10f)
            } else theme.fontSizeSp
            setTextColor(theme.textArgb)
            background = keyBackground(label)
            isClickable = true
            isFocusable = true
            setOnTouchListener { touched, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        background = keyBackground(label, pressed = true)
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        background = keyBackground(label)
                    }
                }
                false
            }
            setOnClickListener { performKeyFeedback(this); action() }
            contentDescription = when (label) {
                "⌫" -> "Delete"
                "↵" -> "Return"
                "space", "English", "العربية" -> "Space"
                "⇧" -> "Shift"
                else -> label
            }
        }
        val gap = dp(theme.keySpacingDp / 2f)
        val params = LinearLayout.LayoutParams(0, dp(theme.keyHeightDp), weight).apply {
            setMargins(gap, dp(3), gap, dp(3))
        }
        parent.addView(view, params)
    }

    private fun addToolbarButton(parent: LinearLayout, label: String, weight: Float, action: () -> Unit) {
        val item = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = if (label.length > 2) 12f else 19f
            setTextColor(theme.accentArgb)
            background = keyBackground(label)
            setOnClickListener { performKeyFeedback(this); action() }
            contentDescription = label
        }
        parent.addView(item, LinearLayout.LayoutParams(0, dp(34), weight).apply {
            setMargins(dp(5), dp(1), dp(5), dp(1))
        })
    }

    private fun handleKey(label: String) {
        val connection: InputConnection = currentInputConnection ?: return
        when (label) {
            "⌫" -> {
                connection.deleteSurroundingText(1, 0)
                refreshAutomaticShift()
                refreshKeyboard()
                updateSuggestions()
            }
            "⇧" -> {
                shifted = !shifted
                refreshKeyboard()
            }
            else -> {
                val output = if (shifted && label.length == 1 && label[0].isLetter()) label.uppercase() else label
                connection.commitText(output, 1)
                refreshAutomaticShift()
                refreshKeyboard()
                updateSuggestions()
            }
        }
    }

    private fun refreshAutomaticShift() {
        val requestedModes = TextUtils.CAP_MODE_CHARACTERS or
            TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES
        shifted = (currentInputConnection?.getCursorCapsMode(requestedModes) ?: 0) != 0
    }

    private fun refreshClipboardListener() {
        val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val shouldListen = theme.clipboardEnabled && !sensitiveField && !numericField
        if (shouldListen && !clipboardListenerRegistered) {
            manager.addPrimaryClipChangedListener(clipboardListener)
            clipboardListenerRegistered = true
        } else if (!shouldListen && clipboardListenerRegistered) {
            manager.removePrimaryClipChangedListener(clipboardListener)
            clipboardListenerRegistered = false
        }
    }

    private fun captureClipboard() {
        if (sensitiveField || numericField || !theme.clipboardEnabled) return
        val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = manager.primaryClip ?: return
        if (clip.itemCount < 1) return
        val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return
        ClipboardHistoryStore.add(applicationContext, text)
        refreshClipboardRow()
    }

    private fun refreshClipboardRow() {
        val row = clipboardRow ?: return
        row.removeAllViews()
        if (!theme.clipboardEnabled || sensitiveField || !clipboardMode) {
            row.visibility = View.GONE
            return
        }
        row.visibility = View.VISIBLE
        val entries = ClipboardHistoryStore.entries(applicationContext)
        if (entries.isEmpty()) {
            addToolbarButton(row, "No saved clips", 1f) { }
        } else {
            entries.take(3).forEach { entry ->
                val label = entry.text.lineSequence().firstOrNull().orEmpty().take(18)
                addToolbarButton(row, label, 1f) {
                    currentInputConnection?.commitText(entry.text, 1)
                    clipboardMode = false
                    refreshKeyboard()
                }
            }
            addToolbarButton(row, "Clear", 0.7f) {
                ClipboardHistoryStore.clear(applicationContext)
                refreshClipboardRow()
            }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        refreshClipboardListenerOff()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        refreshClipboardListenerOff()
        learningExecutor.shutdown()
        super.onDestroy()
    }

    private fun refreshClipboardListenerOff() {
        if (clipboardListenerRegistered) {
            val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            manager.removePrimaryClipChangedListener(clipboardListener)
            clipboardListenerRegistered = false
        }
    }

    private fun learnCurrentWord() {
        if (sensitiveField || numericField || emailField || urlField ||
            !PersonalDictionaryStore.isEnabled(applicationContext)) return
        val before = currentInputConnection?.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        val word = Regex("[\\p{L}\\p{M}]{2,32}$").find(before)?.value ?: return
        val acceptedLanguage = languageTag
        learningExecutor.execute {
            runCatching { PersonalDictionaryStore.learn(applicationContext, word, acceptedLanguage) }
        }
    }

    private fun updateSuggestions() {
        val row = suggestionRow ?: return
        row.removeAllViews()
        if (sensitiveField || numericField || emailField || urlField || emojiMode || clipboardMode) {
            row.visibility = View.GONE
            return
        }
        row.visibility = View.VISIBLE
        val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val prefix = Regex("[\\p{L}\\p{M}]+$").find(before)?.value.orEmpty()
        val personal = PersonalDictionaryStore.suggestions(applicationContext, prefix, languageTag, 3)
        val bundled = wordSuggestions?.suggest(prefix, languageTag, 3).orEmpty()
        val suggestions = (personal + bundled).distinct().take(3)
        if (suggestions.isEmpty()) {
            addToolbarButton(row, if (languageTag.startsWith("ar")) "العربية" else "English", 1f) { }
        } else {
            suggestions.forEach { suggestion ->
                addToolbarButton(row, suggestion, 1f) { replaceCurrentWord(suggestion) }
            }
        }
    }

    private fun replaceCurrentWord(suggestion: String) {
        val connection = currentInputConnection ?: return
        val before = connection.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val prefix = Regex("[\\p{L}\\p{M}]+$").find(before)?.value ?: return
        connection.deleteSurroundingText(prefix.length, 0)
        connection.commitText(suggestion, 1)
        updateSuggestions()
    }

    private fun performKeyFeedback(target: View) {
        if (theme.hapticsEnabled) {
            target.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (theme.soundEnabled) {
            val audio = getSystemService(AUDIO_SERVICE) as AudioManager
            audio.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    private fun openStudio() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * Premium dark glass surface. Keep the keyboard geometry and key order untouched;
     * only the material treatment changes.
     */
    private fun panelBackground(): GradientDrawable {
        val raw = theme.backgroundArgb
        val alpha = (Color.alpha(raw) * theme.opacity * 0.78f).toInt().coerceIn(90, 220)
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.argb((alpha * 1.08f).toInt().coerceAtMost(235), Color.red(raw), Color.green(raw), Color.blue(raw)),
                Color.argb((alpha * 0.82f).toInt().coerceAtLeast(75), Color.red(raw), Color.green(raw), Color.blue(raw))
            )
        ).apply {
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), Color.argb(105, 255, 255, 255))
        }
    }

    private fun keyBackground(label: String, pressed: Boolean = false): GradientDrawable {
        val special = label in setOf("⇧", "⌫", "↵", "!#1", "ABC", "🌐", "123", "⚙", "⋯")
        val raw = when {
            pressed -> theme.keyPressedArgb
            special -> theme.accentArgb
            else -> theme.keyArgb
        }
        val baseAlpha = when {
            pressed -> 176
            special -> 82
            else -> 92
        }
        val fill = Color.argb(
            (Color.alpha(raw) * (baseAlpha / 255f) * theme.opacity).toInt().coerceIn(42, 190),
            Color.red(raw), Color.green(raw), Color.blue(raw)
        )
        val keyHeight = dp(theme.keyHeightDp).toFloat()
        val radius = when (theme.keyShape) {
            "pill", "circle" -> keyHeight / 2f
            "squircle" -> dp(theme.cornerRadiusDp * 1.35f).toFloat()
            "rect" -> dp(3).toFloat()
            else -> dp(theme.cornerRadiusDp).toFloat()
        }
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.argb((Color.alpha(fill) * 1.18f).toInt().coerceAtMost(205), Color.red(fill), Color.green(fill), Color.blue(fill)),
                fill
            )
        ).apply {
            cornerRadius = radius
            setStroke(dp(1), if (pressed) Color.argb(190, 255, 255, 255) else Color.argb(88, 255, 255, 255))
        }
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
