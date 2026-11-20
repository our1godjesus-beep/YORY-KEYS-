package app.yory.keys.ime

import android.content.Intent
import android.media.AudioManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
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
    private var sensitiveField = false
    private var keyboardRoot: LinearLayout? = null
    private var suggestionRow: LinearLayout? = null
    private var emojiRow: LinearLayout? = null
    private var keysContainer: LinearLayout? = null
    private var wordSuggestions: LocalWordSuggestions? = null

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
        addToolbarButton(toolbar, "⋯", 1f) { openStudio() }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(38)))

        suggestionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(2))
        }
        root.addView(suggestionRow, LinearLayout.LayoutParams(-1, dp(34)))

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
        return root
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        theme = YoryThemeStore.load(applicationContext)
        sensitiveField = isSensitive(info)
        refreshKeyboard()
        updateSuggestions()
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
        container.removeAllViews()

        emojiRow?.visibility = if (emojiMode) View.VISIBLE else View.GONE
        suggestionRow?.visibility = if (sensitiveField || emojiMode) View.GONE else View.VISIBLE

        if (symbolMode) {
            addKeyRow(container, "1234567890")
            addKeyRow(container, "@#$%&-*+=")
            addKeyRow(container, "()[]{}⌫")
        } else {
            addKeyRow(container, if (languageTag.startsWith("ar")) "ضصثقفغعهخحجد" else "qwertyuiop")
            addKeyRow(container, if (languageTag.startsWith("ar")) "شسيبلاتنمك" else "asdfghjkl")
            addSpecialLetterRow(container)
        }
        addBottomRow(container)
    }

    private fun addKeyRow(parent: LinearLayout, letters: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        letters.forEach { char ->
            val label = char.toString()
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
        val letters = if (languageTag.startsWith("ar")) "ئءؤرىةوزظذ" else "zxcvbnm"
        letters.forEach { char ->
            val label = char.toString()
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
        addKey(row, ",", 0.8f) { currentInputConnection?.commitText(",", 1); updateSuggestions() }
        addKey(row, "space", 3.8f) { currentInputConnection?.commitText(" ", 1); shifted = true; updateSuggestions() }
        addKey(row, ".", 0.8f) { currentInputConnection?.commitText(".", 1); updateSuggestions() }
        addKey(row, "↵", 1.15f) {
            val handled = sendDefaultEditorAction(true)
            if (!handled) currentInputConnection?.commitText("\n", 1)
            updateSuggestions()
        }
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(theme.keyHeightDp)))
    }

    private fun addKey(parent: LinearLayout, label: String, weight: Float, action: () -> Unit) {
        val view = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = if (label == "space") 12f else 18f
            setTextColor(theme.textArgb)
            background = keyBackground(label)
            isClickable = true
            isFocusable = true
            setOnClickListener { performKeyFeedback(this); action() }
            contentDescription = when (label) {
                "⌫" -> "Delete"
                "↵" -> "Return"
                "space" -> "Space"
                "⇧" -> "Shift"
                else -> label
            }
        }
        val params = LinearLayout.LayoutParams(0, dp(theme.keyHeightDp), weight).apply {
            setMargins(dp(2), dp(3), dp(2), dp(3))
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
                updateSuggestions()
            }
            "⇧" -> {
                shifted = !shifted
                refreshKeyboard()
            }
            else -> {
                val output = if (shifted && label.length == 1 && label[0].isLetter()) label.uppercase() else label
                connection.commitText(output, 1)
                if (shifted) shifted = false
                refreshKeyboard()
                updateSuggestions()
            }
        }
    }

    private fun updateSuggestions() {
        val row = suggestionRow ?: return
        row.removeAllViews()
        if (sensitiveField || emojiMode) {
            row.visibility = View.GONE
            return
        }
        row.visibility = View.VISIBLE
        val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val prefix = Regex("[\\p{L}\\p{M}]+$").find(before)?.value.orEmpty()
        val suggestions = wordSuggestions?.suggest(prefix, languageTag).orEmpty()
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

    private fun panelBackground(): GradientDrawable {
        val raw = theme.backgroundArgb
        val alpha = (Color.alpha(raw) * theme.opacity).toInt().coerceIn(30, 255)
        return GradientDrawable().apply {
            setColor(Color.argb(alpha, Color.red(raw), Color.green(raw), Color.blue(raw)))
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), Color.argb(65, 255, 255, 255))
        }
    }

    private fun keyBackground(label: String): GradientDrawable {
        val special = label in setOf("⇧", "⌫", "↵", "!#1", "ABC", "🌐", "123", "⚙", "⋯")
        val raw = if (special) theme.accentArgb else theme.keyArgb
        val fill = if (special) Color.argb(48, Color.red(raw), Color.green(raw), Color.blue(raw)) else raw
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(theme.cornerRadiusDp).toFloat()
            setStroke(dp(1), Color.argb(42, 255, 255, 255))
        }
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
