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
    private var symbolPage = 0
    private var emojiMode = false
    private var clipboardMode = false
    private var incognitoMode = false
    private var toolsMode = false
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
    private var wordSuggestions: LocalWordSuggestions? = null
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
        toolbarRoot = toolbar
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(38)))
        refreshToolbar()

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

        moreToolsPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        root.addView(moreToolsPanel, LinearLayout.LayoutParams(-1, -2))

        emojiRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        refreshEmojiPicker()
        root.addView(emojiRow, LinearLayout.LayoutParams(-1, dp(104)))

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
        val defaultOrder = listOf("emoji", "language", "symbols", "settings", "clipboard", "privacy", "more")
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
                "symbols" -> addToolbarButton(toolbar, if (symbolMode) if (symbolPage == 0) "2/2" else "1/2" else "123", 1f) {
                    if (symbolMode) {
                        symbolPage = (symbolPage + 1) % 2
                    } else {
                        symbolMode = true
                        symbolPage = 0
                    }
                    refreshKeyboard()
                }
                "settings" -> addToolbarIconButton(toolbar, "settings", "Open YORY Studio", 1f) { openStudio() }
                "more" -> addToolbarIconButton(toolbar, if (toolsMode) "close" else "more", if (toolsMode) "Close tools" else "More tools", 1f) {
                    toolsMode = !toolsMode
                    if (toolsMode) {
                        emojiMode = false
                        clipboardMode = false
                    }
                    refreshToolbar()
                    refreshKeyboard()
                    updateSuggestions()
                }
                "clipboard" -> if (theme.clipboardEnabled && !incognitoMode &&
                    !sensitiveField && !numericField && !emailField && !urlField) {
                    addToolbarIconButton(toolbar, "clipboard", "Clipboard history", 1f) {
                        clipboardMode = !clipboardMode
                        refreshKeyboard()
                        refreshClipboardRow()
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
            addKeyRow(container, activeLayout.rows[0], compact = true, secondaryHints = digitHints)
            addKeyRow(container, activeLayout.rows[1], compact = true)
            addSpecialLetterRow(container, activeLayout.rows[2], compact = true)
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
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(theme.keyHeightDp * 0.72f)))
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
            cycleLanguage()
            refreshKeyboard()
        }
        addKey(actions, "↵", 1f) {
            val handled = sendDefaultEditorAction(true)
            if (!handled) currentInputConnection?.commitText("\n", 1)
        }
        parent.addView(actions, LinearLayout.LayoutParams(-1, dp(theme.keyHeightDp)))
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
            val label = if (shifted && char.isLetter()) char.uppercaseChar().toString() else char.toString()
            addKey(
                row, label, 1f, { handleKey(label) },
                compact = compact,
                secondaryLegend = secondaryHints.getOrNull(index)
            )
        }
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(theme.keyHeightDp)))
    }

    private fun addSpecialLetterRow(parent: LinearLayout, letters: String, compact: Boolean = false) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addKey(row, "⇧", 1.25f, { shifted = !shifted; refreshKeyboard() }, compact = compact)
        letters.forEach { char ->
            val label = if (shifted && char.isLetter()) char.uppercaseChar().toString() else char.toString()
            addKey(row, label, 1f, { handleKey(label) }, compact = compact)
        }
        addKey(row, "⌫", 1.25f, { handleKey("⌫") }, compact = compact)
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(theme.keyHeightDp)))
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
            parent.addView(row, LinearLayout.LayoutParams(-1, dp(theme.keyHeightDp)))
        }
    }

    private fun refreshMoreTools() {
        val panel = moreToolsPanel ?: return
        panel.removeAllViews()
        val heading = TextView(this).apply {
            text = "YORY TOOLS"
            textSize = 11f
            letterSpacing = 0.16f
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setTextColor(theme.textArgb)
            setPadding(dp(5), dp(2), dp(5), dp(7))
        }
        panel.addView(heading, LinearLayout.LayoutParams(-1, dp(25)))

        val tools = mutableListOf<Triple<String, String, () -> Unit>>()
        tools.add(Triple("Emoji", "emoji") {
            toolsMode = false
            emojiMode = true
            refreshToolbar()
            refreshKeyboard()
            refreshEmojiPicker()
            updateSuggestions()
        })
        tools.add(Triple("Languages", "language") {
            toolsMode = false
            cycleLanguage(1)
            refreshToolbar()
            refreshKeyboard()
            updateSuggestions()
        })
        tools.add(Triple("Symbols", "symbols") {
            toolsMode = false
            symbolMode = true
            symbolPage = 0
            refreshToolbar()
            refreshKeyboard()
        })
        if (theme.clipboardEnabled && !sensitiveField && !numericField && !emailField && !urlField && !incognitoMode) {
            tools.add(Triple("Clipboard", "clipboard") {
                toolsMode = false
                clipboardMode = true
                refreshToolbar()
                refreshKeyboard()
                refreshClipboardRow()
            })
        }
        tools.add(Triple("Incognito", "privacy") {
            toolsMode = false
            incognitoMode = !incognitoMode
            if (incognitoMode) clipboardMode = false
            refreshToolbar()
            refreshKeyboard()
            updateSuggestions()
            refreshClipboardListener()
        })
        tools.add(Triple("Next keyboard", "keyboard") {
            toolsMode = false
            switchToNextInputMethod(false)
            refreshToolbar()
            refreshKeyboard()
        })
        tools.add(Triple("Clear words", "text") {
            PersonalDictionaryStore.clear(applicationContext)
            toolsMode = false
            refreshKeyboard()
            updateSuggestions()
        })
        tools.add(Triple("Studio", "settings") {
            toolsMode = false
            openStudio()
            refreshToolbar()
            refreshKeyboard()
        })

        tools.chunked(4).forEach { group ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            group.forEach { (title, icon, action) ->
                val tile = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    isClickable = true
                    isFocusable = true
                    contentDescription = title
                    background = GradientDrawable().apply {
                        setColor(Color.argb(22, 255, 255, 255))
                        cornerRadius = dp(15).toFloat()
                        setStroke(dp(1), Color.argb(42, 255, 255, 255))
                    }
                    setPadding(dp(2), dp(5), dp(2), dp(5))
                    setOnClickListener {
                        performKeyFeedback(this)
                        action()
                    }
                }
                val glyph = YoryKeyboardIconView(this, icon, theme.textArgb)
                val caption = TextView(this).apply {
                    text = title
                    textSize = 10.5f
                    gravity = Gravity.CENTER
                    maxLines = 1
                    setTextColor(theme.textArgb)
                }
                tile.addView(glyph, LinearLayout.LayoutParams(-1, dp(31)))
                tile.addView(caption, LinearLayout.LayoutParams(-1, dp(18)))
                row.addView(tile, LinearLayout.LayoutParams(0, dp(64), 1f).apply {
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                })
            }
            repeat(4 - group.size) {
                row.addView(View(this), LinearLayout.LayoutParams(0, dp(64), 1f).apply {
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                })
            }
            panel.addView(row, LinearLayout.LayoutParams(-1, dp(70)))
        }
    }

    private fun addBottomRow(parent: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addKey(row, if (symbolMode) "ABC" else "!#1", 1.15f) {
            symbolMode = !symbolMode
            symbolPage = 0
            refreshKeyboard()
        }
        addKey(row, if (symbolMode) "${symbolPage + 1}/2" else "🌐", 1f) {
            if (symbolMode) {
                symbolPage = (symbolPage + 1) % 2
            } else {
                cycleLanguage(1)
                shifted = false
            }
            refreshKeyboard()
            updateSuggestions()
        }
        addKey(row, if (emailField) "@" else if (urlField) "/" else ",", 0.8f) {
            learnCurrentWord()
            currentInputConnection?.commitText(if (emailField) "@" else if (urlField) "/" else ",", 1)
            updateSuggestions()
        }
        addKey(
            row,
            "‹  ${YoryLanguageLayouts.forTag(languageTag).shortLabel}  ›",
            3.3f,
            {
                learnCurrentWord()
                currentInputConnection?.commitText(" ", 1)
                refreshAutomaticShift()
                updateSuggestions()
            },
            onSwipeDirection = { deltaX ->
                // Left = next language; right = previous. Swipes never insert a space.
                cycleLanguage(if (deltaX < 0f) 1 else -1)
                shifted = false
                refreshToolbar()
                refreshKeyboard()
                updateSuggestions()
            }
        )
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
            text = if (secondaryLegend.isNullOrBlank()) {
                label
            } else {
                val legendLine = secondaryLegend
                android.text.SpannableString("$legendLine\n$label").apply {
                    setSpan(
                        android.text.style.RelativeSizeSpan(0.48f),
                        0,
                        legendLine.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    setSpan(
                        android.text.style.ForegroundColorSpan(Color.argb(160, 255, 255, 255)),
                        0,
                        legendLine.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
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
        val gap = if (compact) dp(1.25f) else dp((theme.keySpacingDp / 2f).coerceIn(1.5f, 4f))
        val params = LinearLayout.LayoutParams(0, dp(theme.keyHeightDp), weight).apply {
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
        val surfaceLabel = when (icon) {
            "emoji" -> "☺"
            "language" -> "⇄"
            "settings" -> "⚙"
            "clipboard" -> "▣"
            "privacy", "privacy-off" -> "◎"
            "more", "close" -> "⋯"
            else -> icon
        }
        val button = FrameLayout(this).apply {
            background = keyBackground(surfaceLabel)
            isClickable = true
            isFocusable = true
            contentDescription = accessibilityLabel
            setOnClickListener {
                performKeyFeedback(this)
                action()
            }
        }
        button.addView(
            YoryKeyboardIconView(this, icon, theme.accentArgb),
            FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
        )
        parent.addView(button, LinearLayout.LayoutParams(0, dp(34), weight).apply {
            setMargins(dp(3), dp(1), dp(3), dp(1))
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

    private fun updateSuggestions() {
        val row = suggestionRow ?: return
        if (toolsMode) {
            row.visibility = View.GONE
            return
        }
        val prefs = getSharedPreferences("yory_user_settings", MODE_PRIVATE)
        val compactMode = prefs.getBoolean("compact_mode", false)
        row.layoutParams = row.layoutParams.apply {
            height = dp(if (suggestionsCollapsed) 19 else if (compactMode) 25 else 34)
        }
        row.requestLayout()
        row.removeAllViews()
        if (incognitoMode || sensitiveField || numericField || emailField || urlField || emojiMode || clipboardMode ||
            !prefs.getBoolean("word_suggestions", true)) {
            row.visibility = View.GONE
            return
        }
        row.visibility = View.VISIBLE
        addToolbarButton(row, if (suggestionsCollapsed) "⌄" else "⌃", 0.42f) {
            suggestionsCollapsed = !suggestionsCollapsed
            updateSuggestions()
        }
        if (suggestionsCollapsed) return

        val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val prefix = Regex("[\\p{L}\\p{M}]+$").find(before)?.value.orEmpty()
        val suggestionLimit = if (compactMode) 2 else 3
        val personal = PersonalDictionaryStore.suggestions(applicationContext, prefix, languageTag, 3)
        val bundled = wordSuggestions?.suggest(prefix, languageTag, 3).orEmpty()
        val suggestions = (personal + bundled).distinct().take(suggestionLimit)
        if (suggestions.isEmpty()) {
            addToolbarButton(row, YoryLanguageLayouts.forTag(languageTag).shortLabel, 1f) { }
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
    private fun panelBackground(): GradientDrawable {
        val raw = theme.backgroundArgb
        val base = Color.rgb(Color.red(raw), Color.green(raw), Color.blue(raw))
        val top = blendColors(base, Color.WHITE, 0.035f)
        val bottom = blendColors(base, Color.BLACK, 0.045f)
        return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom)).apply {
            cornerRadius = dp(23).toFloat()
            setStroke(dp(1), Color.rgb(39, 42, 48))
        }
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
        val keyHeight = dp(theme.keyHeightDp).toFloat()
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
                    color = tint
                    textAlign = Paint.Align.CENTER
                    textSize = 8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("Aa", 12f, 15f, textPaint)
            }
        }
        canvas.restore()
    }
}
