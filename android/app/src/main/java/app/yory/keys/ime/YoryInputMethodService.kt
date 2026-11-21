package app.yory.keys.ime

import android.content.Intent
import android.content.ClipboardManager
import android.media.AudioManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.widget.PopupWindow
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.view.KeyEvent
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
    private var symbolPage = 0
    private var emojiMode = false
    private var clipboardMode = false
    private var incognitoMode = false
    private var toolsMode = false
    private var resizeMode = false
    private var suggestionsCollapsed = false
    private var sensitiveField = false
    private var numericField = false
    private var emailField = false
    private var urlField = false
    private var keyboardRoot: LinearLayout? = null
    private var toolbarRoot: LinearLayout? = null
    private var suggestionRow: LinearLayout? = null
    private var clipboardRow: LinearLayout? = null
    private var moreToolsPanel: LinearLayout? = null
    private var emojiRow: LinearLayout? = null
    private var emojiCategory = "Recent"
    private var emojiSkinTone = ""
    private var keysContainer: LinearLayout? = null
    private var keyboardMainArea: FrameLayout? = null
    private var keyboardContent: LinearLayout? = null
    private var resizeOverlay: FrameLayout? = null
    private var wordSuggestions: LocalWordSuggestions? = null
    private val dynamicKeyLabels = mutableListOf<Triple<TextView, String, String?>>()
    private val suggestionLabels = mutableListOf<TextView>()
    private var suggestionToggle: FrameLayout? = null
    private var suggestionToggleGlyph: TextView? = null
    private var imeWindow: Window? = null
    private var resizeWidthPercent = 1f
    private var resizeOffsetXdp = 0f
    private var resizeOffsetYdp = 0f
    private val learningExecutor: ExecutorService = Executors.newSingleThreadExecutor { task ->
        Thread(task, "YORY-PersonalLearning").apply { isDaemon = true }
    }
    private var clipboardListenerRegistered = false
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener { captureClipboard() }

    override fun onCreateInputView(): View {
        wordSuggestions = LocalWordSuggestions(applicationContext)
        languageTag = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
            .getString("active_language", "en-US") ?: "en-US"
        theme = YoryThemeStore.load(applicationContext)
        loadResizeGeometry()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = panelBackground()
            clipChildren = false
        }
        keyboardRoot = root

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = ColorDrawable(Color.rgb(34, 34, 34))
        }
        toolbarRoot = toolbar
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(36)))
        refreshToolbar()

        val mainArea = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }
        keyboardMainArea = mainArea
        root.addView(mainArea, LinearLayout.LayoutParams(-1, -2))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        keyboardContent = content
        mainArea.addView(content, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))

        suggestionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(2))
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        buildSuggestionRow()
        content.addView(suggestionRow, LinearLayout.LayoutParams(-1, dp(32)))

        clipboardRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        content.addView(clipboardRow, LinearLayout.LayoutParams(-1, dp(42)))

        moreToolsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        content.addView(moreToolsPanel, LinearLayout.LayoutParams(-1, -2))

        emojiRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        refreshEmojiPicker()
        content.addView(emojiRow, LinearLayout.LayoutParams(-1, dp(104)))

        keysContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
        }
        content.addView(keysContainer, LinearLayout.LayoutParams(-1, -2))

        resizeOverlay = createResizeOverlay()
        mainArea.addView(resizeOverlay, FrameLayout.LayoutParams(-1, 0))
        content.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            syncResizeOverlayBounds()
        }

        refreshKeyboard()
        updateSuggestions()
        refreshClipboardListener()
        return root
    }

    override fun onConfigureWindow(win: Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        super.onConfigureWindow(win, isFullscreen, isCandidatesOnly)
        imeWindow = win
        if (!isFullscreen && !isCandidatesOnly) applyKeyboardWindowGeometry(win)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        loadResizeGeometry()
        applyKeyboardWindowGeometry()
        keyboardRoot?.post { applyKeyboardWindowGeometry() }
    }

    private fun loadResizeGeometry() {
        val prefs = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        resizeWidthPercent = prefs.getFloat("keyboard_width_percent", 1f).coerceIn(0.70f, 1f)
        resizeOffsetXdp = prefs.getFloat("keyboard_offset_x_dp", 0f).coerceIn(-120f, 120f)
        resizeOffsetYdp = prefs.getFloat("keyboard_offset_y_dp", 0f).coerceIn(0f, 300f)
    }

    private fun persistResizeGeometry() {
        getSharedPreferences("yory_user_settings", MODE_PRIVATE).edit()
            .putFloat("keyboard_width_percent", resizeWidthPercent.coerceIn(0.70f, 1f))
            .putFloat("keyboard_offset_x_dp", resizeOffsetXdp.coerceIn(-120f, 120f))
            .putFloat("keyboard_offset_y_dp", resizeOffsetYdp.coerceIn(0f, 300f))
            .apply()
        applyKeyboardWindowGeometry()
    }

    private fun applyKeyboardWindowGeometry(window: Window? = imeWindow) {
        val target = window ?: runCatching { getWindow().window }.getOrNull() ?: return
        val metrics = resources.displayMetrics
        val widthPercent = resizeWidthPercent.coerceIn(0.70f, 1f)
        val offsetX = resizeOffsetXdp.coerceIn(-120f, 120f)
        val offsetY = resizeOffsetYdp.coerceIn(0f, 300f)
        val widthPx = if (widthPercent >= 0.995f) WindowManager.LayoutParams.MATCH_PARENT
            else (metrics.widthPixels * widthPercent).toInt().coerceAtLeast(dp(260))
        val params = target.attributes
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.width = widthPx
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.x = dp(offsetX).toInt()
        params.y = dp(offsetY).toInt()
        target.attributes = params
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        theme = YoryThemeStore.load(applicationContext)
        loadResizeGeometry()
        languageTag = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
            .getString("active_language", languageTag) ?: languageTag
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
        emojiMode = false
        incognitoMode = false
        toolsMode = false
        resizeMode = false
        resizeOverlay?.visibility = View.GONE
        symbolPage = 0
        suggestionsCollapsed = false
        refreshAutomaticShift()
        refreshToolbar()
        refreshKeyboard()
        updateSuggestions()
        refreshClipboardListener()
    }

    private fun refreshToolbar() {
        val toolbar = toolbarRoot ?: return
        toolbar.removeAllViews()
        val prefs = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        val defaultOrder = listOf("emoji", "clipboard", "language", "symbols", "privacy", "settings", "more")
        val savedOrder = prefs.getString("toolbar_order", null)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it in defaultOrder }
            ?.distinct()
        val order = savedOrder?.plus(defaultOrder.filterNot(savedOrder::contains)) ?: defaultOrder
        val defaultVisible = defaultOrder.toSet()
        val visible = prefs.getStringSet("toolbar_visible", defaultVisible) ?: defaultVisible

        order.filter { it in visible }.forEach { tool ->
            when (tool) {
                "emoji" -> if (prefs.getBoolean("emoji_key", true)) {
                    addToolbarIconButton(toolbar, "emoji", "Emoji picker", 1f) {
                        emojiMode = !emojiMode
                        if (emojiMode) refreshEmojiPicker()
                        refreshKeyboard()
                    }
                }
                "language" -> addToolbarIconButton(toolbar, "language", "Switch language", 1f) {
                    cycleLanguage()
                    shifted = false
                    refreshKeyboard()
                    updateSuggestions()
                }
                "symbols" -> addToolbarIconButton(toolbar, "symbols", if (symbolMode) "Symbols page ${symbolPage + 1}" else "Symbols", 1f) {
                    if (symbolMode) {
                        symbolPage = (symbolPage + 1) % 2
                    } else {
                        symbolMode = true
                        symbolPage = 0
                    }
                    refreshKeyboard()
                }
                "settings" -> addToolbarIconButton(toolbar, "settings", "Open YORY Studio", 1f) { openStudio() }
                "more" -> addToolbarIconButton(
                    toolbar,
                    if (toolsMode || resizeMode) "close" else "more",
                    if (toolsMode || resizeMode) "Close tools" else "More tools",
                    1f
                ) {
                    if (resizeMode) {
                        finishResizeOverlay()
                    } else {
                        toolsMode = !toolsMode
                        if (toolsMode) {
                            emojiMode = false
                            clipboardMode = false
                        }
                        refreshToolbar()
                        refreshKeyboard()
                        updateSuggestions()
                    }
                }
                "clipboard" -> if (!incognitoMode && !sensitiveField &&
                    !numericField && !emailField && !urlField) {
                    addToolbarIconButton(toolbar, "clipboard", "Clipboard history", 1f) {
                        if (theme.clipboardEnabled) {
                            clipboardMode = !clipboardMode
                            refreshKeyboard()
                            refreshClipboardRow()
                        } else {
                            Toast.makeText(this, "Enable clipboard history in YORY Studio first.", Toast.LENGTH_SHORT).show()
                            openStudio()
                        }
                    }
                }
                "privacy" -> addToolbarIconButton(toolbar, if (incognitoMode) "privacy-off" else "privacy", if (incognitoMode) "Incognito mode on" else "Incognito mode off", 1f) {
                    incognitoMode = !incognitoMode
                    if (incognitoMode) clipboardMode = false
                    refreshToolbar()
                    refreshKeyboard()
                    updateSuggestions()
                    refreshClipboardListener()
                }
            }
        }
        if (toolbar.childCount == 0) {
            addToolbarIconButton(toolbar, "language", "Switch language", 1f) { cycleLanguage(); refreshKeyboard(); updateSuggestions() }
            addToolbarIconButton(toolbar, "settings", "Open YORY Studio", 1f) { openStudio() }
        }
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
        val activeLayout = YoryLanguageLayouts.forTag(languageTag)
        val direction = if (activeLayout.isRtl && !numericField && !emailField && !urlField) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        keyboardRoot?.layoutDirection = direction
        container.layoutDirection = direction
        suggestionRow?.layoutDirection = View.LAYOUT_DIRECTION_LTR
        dynamicKeyLabels.clear()
        container.removeAllViews()

        moreToolsPanel?.visibility = if (toolsMode) View.VISIBLE else View.GONE
        container.visibility = if (toolsMode) View.GONE else View.VISIBLE
        if (toolsMode) {
            emojiRow?.visibility = View.GONE
            suggestionRow?.visibility = View.GONE
            clipboardRow?.visibility = View.GONE
            refreshMoreTools()
            return
        }

        emojiRow?.visibility = if (emojiMode) View.VISIBLE else View.GONE
        suggestionRow?.visibility = if (sensitiveField || numericField || emojiMode || clipboardMode) View.GONE else View.VISIBLE
        clipboardRow?.visibility = if (
            theme.clipboardEnabled && clipboardMode && !sensitiveField &&
            !numericField && !emailField && !urlField && !incognitoMode
        ) View.VISIBLE else View.GONE

        if (numericField) {
            addNumericLayout(container)
            return
        }

        val layoutPrefs = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        val showNumberRow = !symbolMode && layoutPrefs.getBoolean("number_row", false)
        val numberPosition = layoutPrefs.getString("number_position", "top") ?: "top"
        if (showNumberRow && numberPosition == "top") addKeyRow(container, "1234567890")

        if (symbolMode) {
            addSymbolLayout(container)
        } else if (activeLayout.isRtl) {
            val digitHints = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=")
            val secondHints = listOf("@", "#", "&", "پ", "لا", "أ", "=", "(", ")", "گ", "*")
            val thirdHints = listOf("−", "~", "−", "+", "؛", ":", "/", "ژ", "”", "◌")
            addKeyRow(container, activeLayout.rows[0], compact = true, secondaryHints = digitHints)
            addKeyRow(container, activeLayout.rows[1], compact = true, secondaryHints = secondHints, horizontalInsetDp = 10)
            addArabicFinalRow(container, activeLayout.rows[2], thirdHints, horizontalInsetDp = 10)
        } else {
            addKeyRow(container, activeLayout.rows[0])
            addKeyRow(container, activeLayout.rows[1], horizontalInsetDp = 10)
            addSpecialLetterRow(container, activeLayout.rows[2])
        }
        addBottomRow(container)
        if (showNumberRow && numberPosition == "bottom") addKeyRow(container, "1234567890")
        if (layoutPrefs.getBoolean("arrow_keys", false)) addCursorArrowRow(container)
    }

    private fun refreshEmojiPicker() {
        val picker = emojiRow ?: return
        picker.removeAllViews()

        val categories = YoryEmojiCatalog.categories
        val categoryBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        categories.forEach { category ->
            addEmojiCategoryButton(categoryBar, category.icon, category.id) {
                emojiCategory = category.id
                refreshEmojiPicker()
            }
        }
        picker.addView(categoryBar, LinearLayout.LayoutParams(-1, dp(31)))

        if (emojiCategory == "People") {
            val toneBar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            val tones = listOf("" to "○", "🏻" to "Light", "🏼" to "Light medium", "🏽" to "Medium", "🏾" to "Dark medium", "🏿" to "Dark")
            tones.forEach { (tone, _) ->
                addToolbarButton(toneBar, if (tone.isEmpty()) "○" else tone, 1f) {
                    emojiSkinTone = tone
                    refreshEmojiPicker()
                }
            }
            picker.addView(toneBar, LinearLayout.LayoutParams(-1, dp(28)))
        }

        val emojiGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val prefs = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        val recent = YoryEmojiCatalog.parseStored(prefs.getString("emoji_recent_v1", ""))
        val favorites = YoryEmojiCatalog.parseStored(prefs.getString("emoji_favorites_v1", ""))
        val choices = YoryEmojiCatalog.values(emojiCategory, recent, favorites).take(14)

        if (choices.isEmpty()) {
            val emptyState = TextView(this).apply {
                text = if (emojiCategory == "Favorites") "No favorites yet. Long-press an emoji to save it."
                    else "No recent emoji yet."
                gravity = Gravity.CENTER
                textSize = 11f
                setTextColor(theme.textArgb)
                contentDescription = text
            }
            emojiGrid.addView(emptyState, LinearLayout.LayoutParams(-1, dp(68)))
        } else {
            choices.chunked(7).forEach { group ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }
                group.forEach { rawEmoji ->
                    val outputEmoji = if (emojiCategory == "People" && emojiSkinTone.isNotEmpty()) {
                        YoryEmojiCatalog.applySkinTone(rawEmoji, emojiSkinTone)
                    } else rawEmoji
                    addEmojiPickerButton(row, outputEmoji) { insertEmoji(outputEmoji) }
                }
                repeat(7 - group.size) { addToolbarButton(row, "", 1f) {} }
                emojiGrid.addView(row, LinearLayout.LayoutParams(-1, dp(34)))
            }
        }
        picker.addView(emojiGrid, LinearLayout.LayoutParams(-1, dp(68)))
        picker.layoutParams?.height = dp(31 + 68 + if (emojiCategory == "People") 28 else 0)
        picker.requestLayout()
    }

    private fun addEmojiCategoryButton(parent: LinearLayout, icon: String, category: String, action: () -> Unit) {
        val button = TextView(this).apply {
            text = icon
            gravity = Gravity.CENTER
            textSize = 18f
            setTextColor(theme.accentArgb)
            background = keyBackground(icon)
            contentDescription = "Show $category emoji"
            setOnClickListener { performKeyFeedback(this); action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(0, dp(30), 1f).apply {
            setMargins(dp(2), dp(1), dp(2), dp(1))
        })
    }

    private fun addEmojiPickerButton(parent: LinearLayout, emoji: String, action: () -> Unit) {
        val prefs = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        val favorites = prefs.getString("emoji_favorites_v1", "").orEmpty()
            .split("\n").filter { it.isNotBlank() }.toSet()
        val button = TextView(this).apply {
            text = emoji
            gravity = Gravity.CENTER
            textSize = 19f
            setTextColor(theme.textArgb)
            background = keyBackground(emoji)
            contentDescription = if (emoji in favorites) "$emoji, favorite. Long press to remove from favorites."
                else "$emoji. Long press to add to favorites."
            setOnClickListener { performKeyFeedback(this); action() }
            setOnLongClickListener {
                toggleFavoriteEmoji(emoji)
                performKeyFeedback(this)
                true
            }
        }
        parent.addView(button, LinearLayout.LayoutParams(0, dp(34), 1f).apply {
            setMargins(dp(2), dp(1), dp(2), dp(1))
        })
    }

    private fun toggleFavoriteEmoji(emoji: String) {
        if (emoji.isBlank()) return
        val prefs = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        val current = YoryEmojiCatalog.parseStored(prefs.getString("emoji_favorites_v1", ""))
        val favorites = YoryEmojiCatalog.toggleFavorite(current, emoji)
        prefs.edit().putString("emoji_favorites_v1", YoryEmojiCatalog.encodeStored(favorites, 80)).apply()
        refreshEmojiPicker()
    }

    private fun insertEmoji(emoji: String) {
        if (emoji.isBlank()) return
        currentInputConnection?.commitText(emoji, 1)
        val prefs = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        val old = YoryEmojiCatalog.parseStored(prefs.getString("emoji_recent_v1", ""))
        val recent = YoryEmojiCatalog.addRecent(old, emoji)
        prefs.edit().putString("emoji_recent_v1", YoryEmojiCatalog.encodeStored(recent, 24)).apply()
        if (emojiCategory == "Recent") refreshEmojiPicker()
    }

    private fun addCursorArrowRow(parent: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        listOf(
            "←" to KeyEvent.KEYCODE_DPAD_LEFT,
            "↑" to KeyEvent.KEYCODE_DPAD_UP,
            "↓" to KeyEvent.KEYCODE_DPAD_DOWN,
            "→" to KeyEvent.KEYCODE_DPAD_RIGHT
        ).forEach { (label, keyCode) ->
            addKey(row, label, 1f) {
                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
        }
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(effectiveKeyHeightDp() * 0.72f)))
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
        parent.addView(bottom, LinearLayout.LayoutParams(-1, dp(effectiveKeyHeightDp() + 4f)))
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addKey(actions, "🌐", 1f) {
            cycleLanguage()
            refreshKeyboard()
        }
        addKey(actions, "↵", 1f) {
            val handled = sendDefaultEditorAction(true)
            if (!handled) currentInputConnection?.commitText("\n", 1)
        }
        parent.addView(actions, LinearLayout.LayoutParams(-1, dp(effectiveKeyHeightDp() + 4f)))
    }

    private fun addKeyRow(
        parent: LinearLayout,
        letters: String,
        compact: Boolean = false,
        secondaryHints: List<String> = emptyList(),
        horizontalInsetDp: Int = 0
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            if (horizontalInsetDp > 0) setPadding(dp(horizontalInsetDp), 0, dp(horizontalInsetDp), 0)
        }
        letters.forEachIndexed { index, char ->
            val label = char.toString()
            addKey(
                row, label, 1f, { handleKey(label) },
                compact = compact,
                secondaryLegend = secondaryHints.getOrNull(index)
            )
        }
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(effectiveKeyHeightDp() + 4f)))
    }

    private fun addArabicFinalRow(
        parent: LinearLayout,
        letters: String,
        secondaryHints: List<String>,
        horizontalInsetDp: Int = 0
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            if (horizontalInsetDp > 0) setPadding(dp(horizontalInsetDp), 0, dp(horizontalInsetDp), 0)
        }
        letters.forEachIndexed { index, char ->
            val label = char.toString()
            addKey(row, label, 1f, { handleKey(label) }, compact = true, secondaryLegend = secondaryHints.getOrNull(index))
        }
        addKey(row, "⌫", 1.25f, { handleKey("⌫") }, compact = true)
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(effectiveKeyHeightDp() + 4f)))
    }

    private fun addSpecialLetterRow(parent: LinearLayout, letters: String, compact: Boolean = false) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addKey(row, "⇧", 1.25f, { shifted = !shifted; updateDynamicKeyLabels() }, compact = compact)
        letters.forEach { char ->
            val label = char.toString()
            addKey(row, label, 1f, { handleKey(label) }, compact = compact)
        }
        addKey(row, "⌫", 1.25f, { handleKey("⌫") }, compact = compact)
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(effectiveKeyHeightDp() + 4f)))
    }

    private fun addSymbolLayout(parent: LinearLayout) {
        val rows = if (symbolPage == 0) {
            listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("@", "#", "$", "%", "&", "_", "-", "(", ")", "="),
                listOf("{", "}", "[", "]", "\"", "*", "'", "/", "؟", "⌫")
            )
        } else {
            listOf(
                listOf("~", "^", "`", "€", "£", "¥", "¢", "°", "×", "÷"),
                listOf("«", "»", "“", "”", "‘", "’", "؛", "،", "|", "\\"),
                listOf("<", ">", "!", "?", ";", ":", "+", "=", "•", "⌫")
            )
        }
        rows.forEachIndexed { rowIndex, labels ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            labels.forEach { label ->
                val weight = if (label == "⌫") 1.35f else 1f
                addKey(row, label, weight, { handleKey(label) }, compact = true)
            }
            parent.addView(row, LinearLayout.LayoutParams(-1, dp(effectiveKeyHeightDp() + 4f)))
        }
    }

    private fun refreshMoreTools() {
        val panel = moreToolsPanel ?: return
        panel.removeAllViews()
        if (resizeMode) {
            panel.visibility = View.GONE
            return
        }

        data class ToolItem(
            val title: String,
            val icon: String,
            val enabled: Boolean = true,
            val action: () -> Unit
        )
        val tools = listOf(
            ToolItem("Stickers", "stickers", false) {},
            ToolItem("Settings", "settings") { openStudio() },
            ToolItem("Languages", "language") {
                cycleLanguage(1)
                toolsMode = false
                refreshToolbar()
                refreshKeyboard()
                updateSuggestions()
            },
            ToolItem("Layouts", "keyboard", false) {},
            ToolItem("Incognito", "privacy") {
                incognitoMode = !incognitoMode
                if (incognitoMode) clipboardMode = false
                toolsMode = false
                refreshToolbar()
                refreshKeyboard()
                updateSuggestions()
                refreshClipboardListener()
            },
            ToolItem("Resize", "resize") {
                startResizeOverlay()
            },
            ToolItem("Themes", "themes") { openStudio() },
            ToolItem("Modes", "modes") {
                symbolMode = !symbolMode
                symbolPage = 0
                toolsMode = false
                refreshToolbar()
                refreshKeyboard()
            },
            ToolItem("Emoji", "emoji") {
                toolsMode = false
                emojiMode = true
                refreshEmojiPicker()
                refreshToolbar()
                refreshKeyboard()
                updateSuggestions()
            },
            ToolItem("Share", "share") { shareYory() },
            ToolItem("Autocorrect", "autocorrect", false) {},
            ToolItem("Create", "create", false) {}
        )

        tools.chunked(4).forEachIndexed { rowIndex, group ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            group.forEachIndexed { columnIndex, item ->
                val tile = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    isClickable = item.enabled
                    isFocusable = item.enabled
                    contentDescription = if (item.enabled) item.title else "${item.title}, unavailable"
                    alpha = if (item.enabled) 1f else 0.42f
                    background = ColorDrawable(Color.TRANSPARENT)
                    setOnClickListener {
                        if (item.enabled) {
                            performKeyFeedback(this)
                            item.action()
                        }
                    }
                }
                tile.addView(
                    YoryKeyboardIconView(this, item.icon, theme.textArgb),
                    LinearLayout.LayoutParams(-1, dp(31))
                )
                tile.addView(TextView(this).apply {
                    text = item.title
                    textSize = 11f
                    gravity = Gravity.CENTER
                    maxLines = 1
                    setTextColor(theme.textArgb)
                }, LinearLayout.LayoutParams(-1, dp(20)))
                row.addView(tile, LinearLayout.LayoutParams(0, dp(67), 1f))
                if (columnIndex < group.lastIndex) {
                    row.addView(View(this).apply {
                        setBackgroundColor(Color.argb(38, 255, 255, 255))
                    }, LinearLayout.LayoutParams(dp(1), dp(62)))
                }
            }
            panel.addView(row, LinearLayout.LayoutParams(-1, dp(68)))
            if (rowIndex < 2) {
                panel.addView(View(this).apply {
                    setBackgroundColor(Color.argb(38, 255, 255, 255))
                }, LinearLayout.LayoutParams(-1, dp(1)))
            }
        }
    }

    private fun createResizeOverlay(): FrameLayout {
        val overlay = FrameLayout(this).apply {
            visibility = View.GONE
            background = ColorDrawable(Color.argb(164, 0, 0, 0))
            isClickable = true
            isFocusable = true
            clipChildren = false
            contentDescription = "Adjust keyboard size and position"
        }

        val bounds = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.argb(4, 65, 105, 255))
                cornerRadius = dp(2).toFloat()
                setStroke(dp(2), Color.rgb(65, 105, 255))
            }
            clipChildren = false
        }
        overlay.addView(bounds, FrameLayout.LayoutParams(-1, -1).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        })

        fun addHandle(width: Int, height: Int, gravity: Int, mode: String) {
            bounds.addView(View(this).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(4).toFloat()
                    setColor(Color.rgb(65, 105, 255))
                }
                contentDescription = when (mode) {
                    "top", "bottom" -> "Drag to resize keyboard height"
                    "left", "right" -> "Drag to resize keyboard width"
                    else -> "Drag to move keyboard"
                }
                setOnTouchListener(resizeHandleTouch(mode))
            }, FrameLayout.LayoutParams(dp(width), dp(height), gravity))
        }
        addHandle(38, 8, Gravity.TOP or Gravity.CENTER_HORIZONTAL, "top")
        addHandle(38, 8, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, "bottom")
        addHandle(8, 28, Gravity.CENTER_VERTICAL or Gravity.START, "left")
        addHandle(8, 28, Gravity.CENTER_VERTICAL or Gravity.END, "right")

        val moveHandle = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(65, 105, 255))
            }
            contentDescription = "Drag to move keyboard"
            setOnTouchListener(resizeHandleTouch("move"))
        }
        moveHandle.addView(
            YoryKeyboardIconView(this, "move", Color.WHITE),
            FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER)
        )
        bounds.addView(moveHandle, FrameLayout.LayoutParams(dp(46), dp(46), Gravity.CENTER))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        fun actionPill(label: String, action: () -> Unit) {
            val button = TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(Color.BLACK)
                background = GradientDrawable().apply {
                    cornerRadius = dp(22).toFloat()
                    setColor(Color.WHITE)
                }
                isClickable = true
                isFocusable = true
                minHeight = dp(44)
                setOnClickListener { performKeyFeedback(this); action() }
            }
            actions.addView(button, LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                setMargins(dp(7), 0, dp(7), 0)
            })
        }
        actionPill("Reset") { resetResizeGeometry() }
        actionPill("OK") { finishResizeOverlay() }
        overlay.addView(actions, FrameLayout.LayoutParams(dp(300), dp(48), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(18)
        })
        return overlay
    }

    private fun syncResizeOverlayBounds() {
        val content = keyboardContent ?: return
        val overlay = resizeOverlay ?: return
        val width = content.width
        val height = content.height
        if (width <= 0 || height <= 0) return
        val params = overlay.layoutParams as? FrameLayout.LayoutParams ?: return
        if (params.width != width || params.height != height) {
            params.width = width
            params.height = height
            overlay.layoutParams = params
        }
        overlay.bringToFront()
    }

    private fun startResizeOverlay() {
        toolsMode = false
        resizeMode = true
        emojiMode = false
        clipboardMode = false
        refreshToolbar()
        refreshKeyboard()
        updateSuggestions()
        resizeOverlay?.visibility = View.VISIBLE
        resizeOverlay?.post { syncResizeOverlayBounds() }
        applyKeyboardWindowGeometry()
    }

    private fun finishResizeOverlay() {
        resizeMode = false
        resizeOverlay?.visibility = View.GONE
        persistResizeGeometry()
        refreshToolbar()
        refreshKeyboard()
        updateSuggestions()
        applyKeyboardWindowGeometry()
    }

    private fun resetResizeGeometry() {
        resizeWidthPercent = 1f
        resizeOffsetXdp = 0f
        resizeOffsetYdp = 0f
        theme = theme.copy(keyHeightDp = 40f)
        YoryThemeStore.save(applicationContext, theme)
        updateLiveKeyHeights()
        persistResizeGeometry()
    }

    private fun updateLiveKeyHeights() {
        val container = keysContainer ?: return
        for (rowIndex in 0 until container.childCount) {
            val row = container.getChildAt(rowIndex) as? LinearLayout ?: continue
            row.layoutParams = row.layoutParams.apply { height = dp(effectiveKeyHeightDp() + 4f) }
            for (keyIndex in 0 until row.childCount) {
                val key = row.getChildAt(keyIndex)
                key.layoutParams = key.layoutParams.apply { height = dp(effectiveKeyHeightDp()) }
            }
        }
        container.requestLayout()
        keyboardContent?.requestLayout()
        keyboardMainArea?.requestLayout()
    }

    private fun resizeHandleTouch(mode: String): View.OnTouchListener {
        var previousX = 0f
        var previousY = 0f
        return View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    previousX = event.rawX
                    previousY = event.rawY
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val density = resources.displayMetrics.density
                    val dx = (event.rawX - previousX) / density
                    val dy = (event.rawY - previousY) / density
                    previousX = event.rawX
                    previousY = event.rawY
                    val screenWidthDp = resources.displayMetrics.widthPixels / density
                    when (mode) {
                        "left" -> {
                            resizeWidthPercent = (resizeWidthPercent - dx / screenWidthDp).coerceIn(0.70f, 1f)
                            resizeOffsetXdp = (resizeOffsetXdp + dx / 2f).coerceIn(-120f, 120f)
                            applyKeyboardWindowGeometry()
                        }
                        "right" -> {
                            resizeWidthPercent = (resizeWidthPercent + dx / screenWidthDp).coerceIn(0.70f, 1f)
                            resizeOffsetXdp = (resizeOffsetXdp + dx / 2f).coerceIn(-120f, 120f)
                            applyKeyboardWindowGeometry()
                        }
                        "top" -> {
                            theme = theme.copy(keyHeightDp = (theme.keyHeightDp - dy * 0.38f).coerceIn(36f, 60f))
                            updateLiveKeyHeights()
                        }
                        "bottom" -> {
                            theme = theme.copy(keyHeightDp = (theme.keyHeightDp + dy * 0.38f).coerceIn(36f, 60f))
                            updateLiveKeyHeights()
                        }
                        "move" -> {
                            resizeOffsetXdp = (resizeOffsetXdp + dx).coerceIn(-120f, 120f)
                            resizeOffsetYdp = (resizeOffsetYdp - dy).coerceIn(0f, 300f)
                            applyKeyboardWindowGeometry()
                        }
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    if (mode == "top" || mode == "bottom") {
                        YoryThemeStore.save(applicationContext, theme)
                    }
                    persistResizeGeometry()
                    true
                }
                else -> true
            }
        }
    }

    private fun shareYory() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "YORY KEYS — a customizable keyboard. https://github.com/our1godjesus-beep/YORY-KEYS-")
        }
        startActivity(Intent.createChooser(send, "Share YORY KEYS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun addBottomRow(parent: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addKey(row, if (symbolMode) "ABC" else "123", 1f) {
            symbolMode = !symbolMode
            symbolPage = 0
            refreshKeyboard()
        }
        addKey(row, "☺", 1f) {
            emojiMode = !emojiMode
            if (emojiMode) refreshEmojiPicker()
            refreshKeyboard()
            updateSuggestions()
        }
        addKey(row, if (emailField) "@" else if (urlField) "/" else ",", 0.9f) {
            commitWordBoundary(if (emailField) "@" else if (urlField) "/" else ",")
        }
        addKey(
            row,
            "‹  ${YoryLanguageLayouts.forTag(languageTag).shortLabel}  ›",
            4.2f,
            {
                commitWordBoundary(" ")
            },
            onSwipeDirection = { deltaX ->
                cycleLanguage(if (deltaX < 0f) 1 else -1)
                shifted = false
                refreshToolbar()
                refreshKeyboard()
                updateSuggestions()
            }
        )
        addKey(row, ".", 1f, {
            commitWordBoundary(".")
        }, secondaryLegend = "؟!")
        addKey(row, "↵", 1.6f) {
            val before = currentInputConnection?.getTextBeforeCursor(64, 0)?.toString().orEmpty()
            val token = Regex("[\\p{L}\\p{M}]{2,32}$").find(before)?.value
            val correction = if (!incognitoMode && !sensitiveField && !numericField && !emailField && !urlField &&
                getSharedPreferences("yory_user_settings", MODE_PRIVATE).getBoolean("auto_correct", true)) {
                token?.let { wordSuggestions?.correct(it, languageTag) }
            } else null
            if (!token.isNullOrEmpty() && !correction.isNullOrEmpty() && correction != token) {
                currentInputConnection?.deleteSurroundingText(token.length, 0)
                currentInputConnection?.commitText(correction, 1)
            }
            learnCurrentWord()
            val handled = sendDefaultEditorAction(true)
            if (!handled) currentInputConnection?.commitText("\n", 1)
            refreshAutomaticShift()
            updateDynamicKeyLabels()
            updateSuggestions()
        }
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(effectiveKeyHeightDp() + 4f)))
    }

    private fun addKey(parent: LinearLayout, label: String, weight: Float, action: () -> Unit) {
        addKey(
            parent = parent,
            label = label,
            weight = weight,
            action = action,
            compact = false,
            secondaryLegend = null,
            onSwipeDirection = null
        )
    }

    private fun addKey(
        parent: LinearLayout,
        label: String,
        weight: Float,
        action: () -> Unit,
        compact: Boolean = false,
        secondaryLegend: String? = null,
        onSwipeDirection: ((Float) -> Unit)? = null
    ) {
        val settings = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        val baseLetter = label.takeIf { it.length == 1 && it[0].isLetter() }?.lowercase()
        val handler = Handler(Looper.getMainLooper())
        var longPressConsumed = false
        var pendingLongPressOptions: List<String> = emptyList()
        var downX = 0f
        var downY = 0f
        var swipeDetected = false
        val longPressTask = Runnable {
            pendingLongPressOptions = longPressOptions(label)
            longPressConsumed = pendingLongPressOptions.isNotEmpty()
        }
        val view = TextView(this).apply {
            text = styledKeyLabel(if (baseLetter != null && shifted) baseLetter.uppercase() else (baseLetter ?: label), secondaryLegend)
            gravity = Gravity.CENTER
            includeFontPadding = false
            val baseSize = when {
                compact -> (theme.fontSizeSp - 2.5f).coerceIn(12f, 16f)
                label.startsWith("‹") || label == "space" || label == "English" || label == "العربية" || label == "Deutsch" ->
                    (theme.fontSizeSp - 6f).coerceAtLeast(10f)
                else -> theme.fontSizeSp
            }
            textSize = if (settings.getBoolean("large_key_text", true) && !compact) baseSize * 1.08f else baseSize
            setTextColor(theme.textArgb)
            typeface = when (theme.fontFamily) {
                "rounded" -> Typeface.create("sans-serif-rounded", Typeface.NORMAL)
                "serif" -> Typeface.SERIF
                "mono" -> Typeface.MONOSPACE
                else -> Typeface.DEFAULT
            }
            background = keyBackground(label)
            elevation = dp(1.5f).toFloat()
            isClickable = true
            isFocusable = true
            setOnTouchListener { touched, event ->
                if (onSwipeDirection != null) {
                    when (event.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            downX = event.x
                            downY = event.y
                            swipeDetected = false
                            background = keyBackground(label, pressed = true)
                            true
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.x - downX
                            val deltaY = event.y - downY
                            val slop = android.view.ViewConfiguration.get(this@YoryInputMethodService).scaledTouchSlop
                            if (kotlin.math.abs(deltaX) > slop && kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) * 1.25f) {
                                swipeDetected = true
                            }
                            true
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            background = keyBackground(label)
                            if (swipeDetected) {
                                onSwipeDirection(event.x - downX)
                            } else {
                                performKeyFeedback(this)
                                action()
                            }
                            true
                        }
                        android.view.MotionEvent.ACTION_CANCEL -> {
                            background = keyBackground(label)
                            true
                        }
                        else -> true
                    }
                } else {
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            longPressConsumed = false
                            pendingLongPressOptions = emptyList()
                            background = keyBackground(label, pressed = true)
                            if (label.length == 1 && label[0].isLetter()) {
                                val duration = settings.getInt("long_press_duration", 450).coerceIn(200, 900).toLong()
                                handler.removeCallbacks(longPressTask)
                                handler.postDelayed(longPressTask, duration)
                            }
                            if (settings.getBoolean("key_popups", true) && label.length == 1) {
                                showKeyPreviewPopup(touched, label, handler)
                            }
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            handler.removeCallbacks(longPressTask)
                            background = keyBackground(label)
                            if (longPressConsumed && pendingLongPressOptions.isNotEmpty()) {
                                showLongPressOptionsPopup(touched, pendingLongPressOptions)
                            }
                        }
                        android.view.MotionEvent.ACTION_CANCEL -> {
                            handler.removeCallbacks(longPressTask)
                            background = keyBackground(label)
                        }
                    }
                    false
                }
            }
            setOnClickListener {
                if (longPressConsumed) {
                    longPressConsumed = false
                } else {
                    performKeyFeedback(this)
                    action()
                }
            }
            contentDescription = if (onSwipeDirection != null) {
                "Space. Swipe left for next added language or right for previous added language."
            } else when (label) {
                "⌫" -> "Delete"
                "↵" -> "Return"
                "space", "English", "العربية", "Deutsch", "Español", "Français", "Português", "Italiano" -> "Space"
                "⇧" -> "Shift"
                else -> label
            }
        }
        if (baseLetter != null) dynamicKeyLabels.add(Triple(view, baseLetter, secondaryLegend))
        val gap = if (compact) dp(1.0f) else dp((theme.keySpacingDp / 2f).coerceIn(1.5f, 4f))
        val params = LinearLayout.LayoutParams(0, dp(effectiveKeyHeightDp()), weight).apply {
            setMargins(gap, dp(2), gap, dp(2))
        }
        parent.addView(view, params)
    }

    private fun longPressOptions(label: String): List<String> {
        if (label.length != 1 || !label[0].isLetter()) return emptyList()
        val key = label.lowercase()
        val prefs = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        val options = mutableListOf<String>()
        if (prefs.getBoolean("long_press_symbols", true) && !languageTag.startsWith("ar")) {
            val symbol = when (key) {
                "q" -> "1"; "w" -> "2"; "e" -> "3"; "r" -> "4"; "t" -> "5"
                "y" -> "6"; "u" -> "7"; "i" -> "8"; "o" -> "9"; "p" -> "0"
                "a" -> "@"; "s" -> "$"; "d" -> "&"; "f" -> "-"; "g" -> "+"
                "h" -> "("; "j" -> ")"; "k" -> "/"; "l" -> ":"
                "z" -> "!"; "x" -> "?"; "c" -> "'"; "v" -> "\""; "b" -> ";"
                "n" -> "_"; "m" -> "."
                else -> null
            }
            if (symbol != null) options.add(symbol)
        }
        if (prefs.getBoolean("accented_chars", true)) {
            val accents = when {
                languageTag.startsWith("ar") -> when (key) {
                    "ا" -> listOf("أ", "إ", "آ")
                    "و" -> listOf("ؤ")
                    "ي" -> listOf("ئ", "ى")
                    "ه" -> listOf("ة")
                    else -> emptyList()
                }
                languageTag.startsWith("de") -> when (key) {
                    "a" -> listOf("ä"); "o" -> listOf("ö"); "u" -> listOf("ü"); "s" -> listOf("ß")
                    else -> emptyList()
                }
                else -> when (key) {
                    "a" -> listOf("á", "à", "â", "ä", "ã", "å")
                    "e" -> listOf("é", "è", "ê", "ë")
                    "i" -> listOf("í", "ì", "î", "ï")
                    "o" -> listOf("ó", "ò", "ô", "ö", "õ")
                    "u" -> listOf("ú", "ù", "û", "ü")
                    "n" -> listOf("ñ")
                    "c" -> listOf("ç")
                    "y" -> listOf("ý", "ÿ")
                    else -> emptyList()
                }
            }
            options.addAll(accents)
        }
        return options.distinct().take(8)
    }

    private fun showKeyPreviewPopup(anchor: View, label: String, handler: Handler) {
        val preview = TextView(this).apply {
            text = label.uppercase()
            gravity = Gravity.CENTER
            textSize = theme.fontSizeSp + 8f
            setTextColor(theme.textArgb)
            background = keyBackground(label, pressed = true)
        }
        val popup = PopupWindow(preview, dp(48), dp(52), false).apply {
            elevation = dp(8).toFloat()
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        runCatching { popup.showAsDropDown(anchor, 0, -dp(62)) }
        handler.postDelayed({ if (popup.isShowing) popup.dismiss() }, 260)
    }

    private fun showLongPressOptionsPopup(anchor: View, options: List<String>) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(4), dp(6), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.argb(244, 29, 34, 48))
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.argb(125, 255, 255, 255))
            }
        }
        lateinit var popup: PopupWindow
        options.forEach { candidate ->
            val choice = TextView(this).apply {
                text = candidate
                gravity = Gravity.CENTER
                textSize = theme.fontSizeSp
                setTextColor(theme.textArgb)
                setPadding(dp(10), dp(8), dp(10), dp(8))
                setOnClickListener {
                    performKeyFeedback(this)
                    currentInputConnection?.commitText(candidate, 1)
                    refreshAutomaticShift()
                    updateSuggestions()
                    popup.dismiss()
                }
            }
            row.addView(choice, LinearLayout.LayoutParams(dp(42), dp(44)))
        }
        popup = PopupWindow(row, WindowManager.LayoutParams.WRAP_CONTENT, dp(54), true).apply {
            elevation = dp(12).toFloat()
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            runCatching { showAsDropDown(anchor, 0, -dp(68)) }
        }
    }

    private fun addToolbarIconButton(
        parent: LinearLayout,
        icon: String,
        accessibilityLabel: String,
        weight: Float,
        action: () -> Unit
    ) {
        val active = when (icon) {
            "emoji" -> emojiMode
            "clipboard" -> clipboardMode
            "privacy", "privacy-off" -> incognitoMode
            "more", "close" -> toolsMode
            "symbols" -> symbolMode
            else -> false
        }
        val button = FrameLayout(this).apply {
            background = if (active) GradientDrawable().apply {
                setColor(Color.argb(38, 255, 255, 255))
                cornerRadius = dp(12).toFloat()
            } else ColorDrawable(Color.TRANSPARENT)
            isClickable = true
            isFocusable = true
            contentDescription = accessibilityLabel
            setOnClickListener {
                performKeyFeedback(this)
                action()
            }
        }
        button.addView(
            YoryKeyboardIconView(this, icon, theme.textArgb),
            FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER)
        )
        parent.addView(button, LinearLayout.LayoutParams(0, dp(34), weight).apply {
            setMargins(dp(2), 0, dp(2), 0)
        })
    }

    private fun addToolbarButton(parent: LinearLayout, label: String, weight: Float, action: () -> Unit) {
        val item = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = if (label.length > 2) 12f else 19f
            setTextColor(theme.accentArgb)
            background = keyBackground(label)
            setOnClickListener { performKeyFeedback(this); action() }
            contentDescription = when (label) {
                "☺" -> "Emoji picker"
                "⇄" -> "Switch language"
                "123" -> "Show symbols"
                "1/2" -> "Show symbol page one"
                "2/2" -> "Show symbol page two"
                "⚙" -> "Open YORY Studio"
                "▣" -> "Clipboard history"
                "◎" -> "Privacy mode"
                "◉" -> "Incognito mode"
                "⋯" -> "More tools"
                "×" -> "Close tools"
                else -> label
            }
        }
        parent.addView(item, LinearLayout.LayoutParams(0, dp(34), weight).apply {
            setMargins(dp(5), dp(1), dp(5), dp(1))
        })
    }

    private fun handleKey(label: String) {
        val connection: InputConnection = currentInputConnection ?: return
        when (label) {
            "⌫" -> {
                connection.deleteSurroundingTextInCodePoints(1, 0)
                refreshAutomaticShift()
                updateDynamicKeyLabels()
                updateSuggestions()
            }
            "⇧" -> {
                shifted = !shifted
                updateDynamicKeyLabels()
            }
            " " -> commitWordBoundary(" ")
            else -> {
                if (label.length == 1 && !label[0].isLetterOrDigit() && label !in setOf("'", "’")) {
                    commitWordBoundary(label)
                } else {
                    val output = if (shifted && label.length == 1 && label[0].isLetter()) label.uppercase() else label
                    connection.commitText(output, 1)
                    refreshAutomaticShift()
                    updateDynamicKeyLabels()
                    updateSuggestions()
                }
            }
        }
    }

    private fun styledKeyLabel(primary: String, secondary: String?): CharSequence {
        if (secondary.isNullOrBlank()) return primary
        return android.text.SpannableString("$secondary\n$primary").apply {
            setSpan(android.text.style.RelativeSizeSpan(0.48f), 0, secondary.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(android.text.style.ForegroundColorSpan(Color.argb(160, 255, 255, 255)),
                0, secondary.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun updateDynamicKeyLabels() {
        dynamicKeyLabels.forEach { (view, base, secondary) ->
            view.text = styledKeyLabel(if (shifted) base.uppercase() else base, secondary)
        }
    }

    private fun commitWordBoundary(boundary: String) {
        val connection = currentInputConnection ?: return
        if (!incognitoMode && !sensitiveField && !numericField && !emailField && !urlField &&
            getSharedPreferences("yory_user_settings", MODE_PRIVATE).getBoolean("auto_correct", true)) {
            val before = connection.getTextBeforeCursor(64, 0)?.toString().orEmpty()
            val token = Regex("[\\p{L}\\p{M}]{2,32}$").find(before)?.value
            val correction = token?.let { wordSuggestions?.correct(it, languageTag) }
            if (!token.isNullOrEmpty() && !correction.isNullOrEmpty() && correction != token) {
                connection.deleteSurroundingText(token.length, 0)
                connection.commitText(correction, 1)
            }
        }
        learnCurrentWord()
        connection.commitText(boundary, 1)
        refreshAutomaticShift()
        updateDynamicKeyLabels()
        updateSuggestions()
    }

    private fun cycleLanguage(direction: Int = 1) {
        val prefs = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        val addedTags = prefs.getStringSet("added_languages", setOf("en-US", "ar"))
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: setOf("en-US", "ar")
        val next = YoryLanguageLayouts.nextTag(languageTag, addedTags, direction)
        languageTag = next
        prefs.edit().putString("active_language", next).apply()
        shifted = false
    }

    private fun refreshAutomaticShift() {
        if (!getSharedPreferences("yory_user_settings", MODE_PRIVATE).getBoolean("auto_caps", true)) {
            shifted = false
            return
        }
        val requestedModes = TextUtils.CAP_MODE_CHARACTERS or
            TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES
        shifted = (currentInputConnection?.getCursorCapsMode(requestedModes) ?: 0) != 0
    }

    private fun refreshClipboardListener() {
        val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val shouldListen = theme.clipboardEnabled && !sensitiveField && !numericField && !emailField && !urlField && !incognitoMode
        if (shouldListen && !clipboardListenerRegistered) {
            manager.addPrimaryClipChangedListener(clipboardListener)
            clipboardListenerRegistered = true
        } else if (!shouldListen && clipboardListenerRegistered) {
            manager.removePrimaryClipChangedListener(clipboardListener)
            clipboardListenerRegistered = false
        }
    }

    private fun captureClipboard() {
        if (sensitiveField || numericField || emailField || urlField || incognitoMode || !theme.clipboardEnabled) return
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
        if (!theme.clipboardEnabled || sensitiveField || numericField || emailField || urlField ||
            incognitoMode || !clipboardMode) {
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
        if (incognitoMode || sensitiveField || numericField || emailField || urlField ||
            !PersonalDictionaryStore.isEnabled(applicationContext)) return
        val before = currentInputConnection?.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        val word = Regex("[\\p{L}\\p{M}]{2,32}$").find(before)?.value ?: return
        val acceptedLanguage = languageTag
        learningExecutor.execute {
            runCatching { PersonalDictionaryStore.learn(applicationContext, word, acceptedLanguage) }
        }
    }

    private fun buildSuggestionRow() {
        val row = suggestionRow ?: return
        row.removeAllViews()
        row.layoutDirection = View.LAYOUT_DIRECTION_LTR
        val toggle = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), Color.argb(170, 190, 190, 190))
            }
            isClickable = true
            isFocusable = true
            contentDescription = "Collapse suggestions"
            setOnClickListener {
                suggestionsCollapsed = !suggestionsCollapsed
                updateSuggestions()
            }
        }
        val glyph = TextView(this).apply {
            text = "⌃"
            gravity = Gravity.CENTER
            textSize = 17f
            setTextColor(theme.textArgb)
        }
        toggle.addView(glyph, FrameLayout.LayoutParams(-1, -1))
        row.addView(toggle, LinearLayout.LayoutParams(dp(28), dp(28)).apply {
            gravity = Gravity.CENTER_VERTICAL
            setMargins(dp(2), 0, dp(7), 0)
        })
        suggestionToggle = toggle
        suggestionToggleGlyph = glyph

        repeat(3) { index ->
            val label = TextView(this).apply {
                gravity = Gravity.CENTER
                textSize = 16f
                maxLines = 1
                setTextColor(theme.textArgb)
                background = ColorDrawable(Color.TRANSPARENT)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val chosen = text?.toString().orEmpty()
                    if (chosen.isNotBlank()) replaceCurrentWord(chosen)
                }
            }
            row.addView(label, LinearLayout.LayoutParams(0, -1, 1f))
            suggestionLabels.add(label)
            if (index < 2) {
                row.addView(View(this).apply {
                    setBackgroundColor(Color.argb(40, 255, 255, 255))
                }, LinearLayout.LayoutParams(dp(1), dp(16)))
            }
        }
    }

    private fun updateSuggestions() {
        val row = suggestionRow ?: return
        if (toolsMode) {
            row.visibility = View.GONE
            return
        }
        val prefs = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        val compactMode = prefs.getBoolean("compact_mode", false)
        row.layoutDirection = View.LAYOUT_DIRECTION_LTR
        val desiredHeight = dp(if (suggestionsCollapsed) 19 else if (compactMode) 25 else 32)
        if (row.layoutParams.height != desiredHeight) {
            row.layoutParams = row.layoutParams.apply { height = desiredHeight }
        }
        val enabled = !incognitoMode && !sensitiveField && !numericField && !emailField && !urlField &&
            !emojiMode && !clipboardMode && prefs.getBoolean("word_suggestions", true)
        row.visibility = if (enabled) View.VISIBLE else View.GONE
        if (!enabled) return

        suggestionToggle?.contentDescription = if (suggestionsCollapsed) "Expand suggestions" else "Collapse suggestions"
        suggestionToggleGlyph?.text = if (suggestionsCollapsed) "⌄" else "⌃"
        suggestionLabels.forEach { it.isEnabled = !suggestionsCollapsed }
        if (suggestionsCollapsed) {
            suggestionLabels.forEach { it.text = "" }
            return
        }

        val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val prefix = Regex("[\\p{L}\\p{M}]+$").find(before)?.value.orEmpty()
        val limit = if (compactMode) 2 else 3
        val candidates = if (prefix.length >= 2) wordSuggestions?.suggest(prefix, languageTag, limit).orEmpty() else emptyList()
        suggestionLabels.forEachIndexed { index, label ->
            val suggestion = candidates.getOrNull(index).orEmpty()
            label.text = suggestion
            label.isEnabled = suggestion.isNotEmpty()
            label.alpha = if (suggestion.isNotEmpty()) 1f else 0.18f
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
        val settings = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        if (theme.hapticsEnabled) {
            if (settings.getBoolean("system_haptics", true)) {
                target.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } else {
                val duration = settings.getFloat("vibration_duration", 12f).toLong().coerceIn(0L, 80L)
                if (duration > 0L) {
                    val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(duration)
                    }
                }
            }
        }
        if (theme.soundEnabled) {
            val volume = settings.getFloat("keypress_volume", 0.35f).coerceIn(0f, 1f)
            if (volume > 0f) {
                val audio = getSystemService(AUDIO_SERVICE) as AudioManager
                val effect = when (settings.getString("sound_profile", "Modern")) {
                    "Classic" -> AudioManager.FX_KEYPRESS_STANDARD
                    "Soft" -> AudioManager.FX_KEYPRESS_SPACEBAR
                    else -> SoundEffectConstants.CLICK
                }
                audio.playSoundEffect(effect, volume)
            }
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
    private fun panelBackground(): ColorDrawable {
        val raw = theme.backgroundArgb
        val base = Color.rgb(Color.red(raw), Color.green(raw), Color.blue(raw))
        return ColorDrawable(blendColors(base, Color.WHITE, 0.025f))
    }

    private fun keyBackground(label: String, pressed: Boolean = false): GradientDrawable {
        val special = label in setOf("⇧", "⌫", "↵", "!#1", "ABC", "🌐", "123", "⚙", "⋯")
        val base = when {
            pressed -> blendColors(theme.keyArgb, theme.keyPressedArgb, 0.30f)
            special -> blendColors(theme.keyArgb, theme.accentArgb, 0.12f)
            else -> theme.keyArgb
        }
        val alpha = (Color.alpha(base) * theme.opacity).toInt().coerceIn(80, 255)
        val topBase = blendColors(Color.rgb(Color.red(base), Color.green(base), Color.blue(base)), Color.WHITE, 0.055f)
        val bottomBase = blendColors(Color.rgb(Color.red(base), Color.green(base), Color.blue(base)), Color.BLACK, 0.07f)
        val top = Color.argb(alpha, Color.red(topBase), Color.green(topBase), Color.blue(topBase))
        val bottom = Color.argb(alpha, Color.red(bottomBase), Color.green(bottomBase), Color.blue(bottomBase))
        val keyHeight = dp(effectiveKeyHeightDp()).toFloat()
        val radius = when (theme.keyShape) {
            "pill", "circle" -> keyHeight / 2f
            "squircle" -> dp(theme.cornerRadiusDp * 1.35f).toFloat()
            "rect" -> dp(3).toFloat()
            else -> dp(theme.cornerRadiusDp).toFloat()
        }
        return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom)).apply {
            cornerRadius = radius
            val outline = when {
                pressed -> Color.argb(150, Color.red(theme.accentArgb), Color.green(theme.accentArgb), Color.blue(theme.accentArgb))
                special -> Color.argb(95, Color.red(theme.accentArgb), Color.green(theme.accentArgb), Color.blue(theme.accentArgb))
                else -> Color.argb(34, 255, 255, 255)
            }
            setStroke(dp(1), outline)
        }
    }

    private fun blendColors(first: Int, second: Int, amount: Float): Int {
        val ratio = amount.coerceIn(0f, 1f)
        val inverse = 1f - ratio
        return Color.rgb(
            (Color.red(first) * inverse + Color.red(second) * ratio).toInt().coerceIn(0, 255),
            (Color.green(first) * inverse + Color.green(second) * ratio).toInt().coerceIn(0, 255),
            (Color.blue(first) * inverse + Color.blue(second) * ratio).toInt().coerceIn(0, 255)
        )
    }

    private fun effectiveKeyHeightDp(): Float {
        val prefs = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        val showNumberRow = !symbolMode && prefs.getBoolean("number_row", false)
        val showArrowRow = prefs.getBoolean("arrow_keys", false)
        val rowCount = when {
            numericField -> 5f
            else -> (4 + (if (showNumberRow) 1 else 0) + (if (showArrowRow) 1 else 0)).toFloat()
        }
        val suggestionsHeight = if (!sensitiveField && !numericField && !emailField && !urlField &&
            !emojiMode && !clipboardMode && !incognitoMode && prefs.getBoolean("word_suggestions", true)) 32f else 0f
        val clipboardHeight = if (clipboardMode && theme.clipboardEnabled && !incognitoMode) 42f else 0f
        val emojiHeight = if (emojiMode) 104f else 0f
        val chrome = 8f + 36f + suggestionsHeight + clipboardHeight + emojiHeight
        val screenHeightDp = resources.displayMetrics.heightPixels / resources.displayMetrics.density
        val maximumImeHeight = (screenHeightDp * 0.54f).coerceIn(250f, 480f)
        val rowHeightBudget = (maximumImeHeight - chrome) / rowCount - 4f
        return minOf(theme.keyHeightDp, rowHeightBudget.coerceIn(32f, 60f))
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}


/**
 * Original monochrome YORY keyboard toolbar glyphs. Drawing in a normalized 24×24
 * viewport avoids font/emoji fallback differences between Android manufacturers.
 */
private class YoryKeyboardIconView(
    context: android.content.Context,
    private val icon: String,
    tint: Int
) : View(context) {
    private val iconTint = tint
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = iconTint
        style = Paint.Style.STROKE
        strokeWidth = 1.8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = iconTint
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val side = kotlin.math.min(width.toFloat(), height.toFloat()).coerceAtLeast(1f)
        canvas.save()
        canvas.translate((width - side) / 2f, (height - side) / 2f)
        canvas.scale(side / 24f, side / 24f)
        when (icon) {
            "emoji" -> {
                canvas.drawCircle(12f, 12f, 8.2f, stroke)
                canvas.drawCircle(9f, 9.5f, 0.9f, fill)
                canvas.drawCircle(15f, 9.5f, 0.9f, fill)
                val smile = Path().apply {
                    moveTo(7.8f, 14f)
                    quadTo(12f, 18.2f, 16.2f, 14f)
                }
                canvas.drawPath(smile, stroke)
            }
            "language" -> {
                canvas.drawCircle(12f, 12f, 9f, stroke)
                canvas.drawOval(RectF(8f, 3f, 16f, 21f), stroke)
                canvas.drawLine(3.5f, 12f, 20.5f, 12f, stroke)
                canvas.drawLine(5.2f, 7.5f, 18.8f, 7.5f, stroke)
                canvas.drawLine(5.2f, 16.5f, 18.8f, 16.5f, stroke)
            }
            "settings" -> {
                for (i in 0 until 8) {
                    canvas.save()
                    canvas.rotate(i * 45f, 12f, 12f)
                    canvas.drawLine(12f, 1.8f, 12f, 5f, stroke)
                    canvas.restore()
                }
                canvas.drawCircle(12f, 12f, 7.2f, stroke)
                canvas.drawCircle(12f, 12f, 2.8f, stroke)
            }
            "clipboard" -> {
                canvas.drawRoundRect(RectF(5f, 4.5f, 19f, 21f), 2f, 2f, stroke)
                canvas.drawRoundRect(RectF(9f, 2.5f, 15f, 6.5f), 1.5f, 1.5f, stroke)
                canvas.drawLine(8f, 10f, 16f, 10f, stroke)
                canvas.drawLine(8f, 14f, 16f, 14f, stroke)
                canvas.drawLine(8f, 18f, 13f, 18f, stroke)
            }
            "privacy", "privacy-off" -> {
                val eye = Path().apply {
                    moveTo(2.5f, 12f)
                    quadTo(7f, 5.3f, 12f, 5.3f)
                    quadTo(17f, 5.3f, 21.5f, 12f)
                    quadTo(17f, 18.7f, 12f, 18.7f)
                    quadTo(7f, 18.7f, 2.5f, 12f)
                }
                canvas.drawPath(eye, stroke)
                canvas.drawCircle(12f, 12f, 3f, stroke)
                if (icon == "privacy-off") canvas.drawLine(4f, 20f, 20f, 4f, stroke)
            }
            "more" -> {
                canvas.drawCircle(5f, 12f, 1.7f, fill)
                canvas.drawCircle(12f, 12f, 1.7f, fill)
                canvas.drawCircle(19f, 12f, 1.7f, fill)
            }
            "close" -> {
                canvas.drawLine(6f, 6f, 18f, 18f, stroke)
                canvas.drawLine(18f, 6f, 6f, 18f, stroke)
            }
            "keyboard" -> {
                canvas.drawRoundRect(RectF(2.5f, 5f, 21.5f, 19f), 2f, 2f, stroke)
                val dots = listOf(8f to 9f, 12f to 9f, 16f to 9f, 8f to 13f, 12f to 13f, 16f to 13f)
                dots.forEach { (x, y) -> canvas.drawCircle(x, y, 0.7f, fill) }
                canvas.drawLine(8f, 16.5f, 16f, 16.5f, stroke)
            }
            "symbols" -> {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = iconTint
                    textAlign = Paint.Align.CENTER
                    textSize = 7.2f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("123", 12f, 15f, textPaint)
            }
            "text" -> {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = iconTint
                    textAlign = Paint.Align.CENTER
                    textSize = 8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("Aa", 12f, 15f, textPaint)
            }
            "chevron-down", "chevron-up" -> {
                val path = Path().apply {
                    if (icon == "chevron-down") {
                        moveTo(5f, 9f); lineTo(12f, 15f); lineTo(19f, 9f)
                    } else {
                        moveTo(5f, 15f); lineTo(12f, 9f); lineTo(19f, 15f)
                    }
                }
                canvas.drawPath(path, stroke)
            }
            "stickers" -> {
                canvas.drawRoundRect(RectF(3f, 4f, 18f, 19f), 3f, 3f, stroke)
                canvas.drawCircle(8f, 9f, 1f, fill)
                canvas.drawCircle(13f, 9f, 1f, fill)
                canvas.drawArc(RectF(7f, 9f, 15f, 16f), 15f, 150f, false, stroke)
                canvas.drawLine(18f, 3f, 18f, 8f, stroke)
                canvas.drawLine(15.5f, 5.5f, 20.5f, 5.5f, stroke)
            }
            "resize" -> {
                canvas.drawRoundRect(RectF(4f, 4f, 20f, 20f), 2f, 2f, stroke)
                canvas.drawLine(8f, 8f, 4f, 4f, stroke)
                canvas.drawLine(4f, 4f, 9f, 4f, stroke)
                canvas.drawLine(4f, 4f, 4f, 9f, stroke)
                canvas.drawLine(16f, 16f, 20f, 20f, stroke)
                canvas.drawLine(20f, 20f, 15f, 20f, stroke)
                canvas.drawLine(20f, 20f, 20f, 15f, stroke)
            }
            "themes" -> {
                canvas.drawCircle(12f, 12f, 8f, stroke)
                canvas.drawCircle(9f, 8f, 1.1f, fill)
                canvas.drawCircle(15f, 9f, 1.1f, fill)
                canvas.drawCircle(9f, 15f, 1.1f, fill)
                canvas.drawCircle(15f, 15f, 1.1f, fill)
            }
            "modes" -> {
                canvas.drawRoundRect(RectF(3f, 5f, 21f, 19f), 3f, 3f, stroke)
                canvas.drawLine(7f, 9f, 17f, 9f, stroke)
                canvas.drawLine(7f, 12f, 17f, 12f, stroke)
                canvas.drawLine(7f, 15f, 14f, 15f, stroke)
            }
            "share" -> {
                canvas.drawLine(7f, 12f, 17f, 6f, stroke)
                canvas.drawLine(7f, 12f, 17f, 18f, stroke)
                canvas.drawCircle(6f, 12f, 2.5f, stroke)
                canvas.drawCircle(18f, 5f, 2.5f, stroke)
                canvas.drawCircle(18f, 19f, 2.5f, stroke)
            }
            "autocorrect" -> {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = iconTint
                    textAlign = Paint.Align.CENTER
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("A", 9f, 15f, textPaint)
                val tick = Path().apply { moveTo(13f, 13f); lineTo(16f, 16f); lineTo(21f, 8f) }
                canvas.drawPath(tick, stroke)
            }
            "create" -> {
                canvas.drawRoundRect(RectF(3f, 3f, 21f, 21f), 4f, 4f, stroke)
                canvas.drawLine(12f, 7f, 12f, 17f, stroke)
                canvas.drawLine(7f, 12f, 17f, 12f, stroke)
            }
            "move" -> {
                canvas.drawLine(12f, 3f, 12f, 21f, stroke)
                canvas.drawLine(3f, 12f, 21f, 12f, stroke)
                canvas.drawLine(12f, 3f, 9f, 6f, stroke)
                canvas.drawLine(12f, 3f, 15f, 6f, stroke)
                canvas.drawLine(12f, 21f, 9f, 18f, stroke)
                canvas.drawLine(12f, 21f, 15f, 18f, stroke)
                canvas.drawLine(3f, 12f, 6f, 9f, stroke)
                canvas.drawLine(3f, 12f, 6f, 15f, stroke)
                canvas.drawLine(21f, 12f, 18f, 9f, stroke)
                canvas.drawLine(21f, 12f, 18f, 15f, stroke)
            }
        }
        canvas.restore()
    }
}
