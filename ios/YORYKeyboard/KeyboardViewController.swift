private struct YoryKeyboardLayout {
    let tag: String
    let name: String
    let spaceLabel: String
    let rows: [String]
    let isRightToLeft: Bool
    let hasBundledDictionary: Bool

    static let supported: [YoryKeyboardLayout] = [
        .init(tag: "en-US", name: "English (US)", spaceLabel: "English", rows: ["qwertyuiop", "asdfghjkl", "zxcvbnm"], isRightToLeft: false, hasBundledDictionary: true),
        .init(tag: "ar", name: "العربية (مصر)", spaceLabel: "مصري", rows: ["ضصثقفغعهخحجد", "شسيبلاتنمكذ", "طظئءؤرىةوز"], isRightToLeft: true, hasBundledDictionary: true),
        .init(tag: "de", name: "Deutsch (DE)", spaceLabel: "Deutsch", rows: ["qwertzuiop", "asdfghjkl", "yxcvbnm"], isRightToLeft: false, hasBundledDictionary: false),
        .init(tag: "es", name: "Español", spaceLabel: "Español", rows: ["qwertyuiop", "asdfghjklñ", "zxcvbnm"], isRightToLeft: false, hasBundledDictionary: false),
        .init(tag: "fr", name: "Français", spaceLabel: "Français", rows: ["azertyuiop", "qsdfghjklm", "wxcvbn"], isRightToLeft: false, hasBundledDictionary: false),
        .init(tag: "pt", name: "Português", spaceLabel: "Português", rows: ["qwertyuiop", "asdfghjklç", "zxcvbnm"], isRightToLeft: false, hasBundledDictionary: false),
        .init(tag: "it", name: "Italiano", spaceLabel: "Italiano", rows: ["qwertyuiop", "asdfghjklò", "zxcvbnm"], isRightToLeft: false, hasBundledDictionary: false)
    ]

    static func resolve(_ tag: String) -> YoryKeyboardLayout {
        supported.first(where: { $0.tag.caseInsensitiveCompare(tag) == .orderedSame }) ?? supported[0]
    }

    static func nextTag(
        after current: String,
        availableTags: [String]? = nil,
        direction: Int = 1
    ) -> String {
        let enabled = supported.filter { layout in
            availableTags?.contains(where: { $0.caseInsensitiveCompare(layout.tag) == .orderedSame }) ?? true
        }
        guard !enabled.isEmpty else { return supported[0].tag }
        guard enabled.count > 1 else { return enabled[0].tag }
        guard let index = enabled.firstIndex(where: { $0.tag.caseInsensitiveCompare(current) == .orderedSame }) else {
            return enabled[0].tag
        }
        let step = direction < 0 ? -1 : 1
        return enabled[(index + step + enabled.count) % enabled.count].tag
    }
}

import UIKit

private final class YoryKeyboardRootView: UIView, UIInputViewAudioFeedback {
    var enableInputClicksWhenVisible: Bool { true }
}

final class KeyboardViewController: UIInputViewController {
    private var isArabic = false
    private var languageTag = "en-US"
    private var isShifted = false
    private var emojiMode = false
    private var incognitoMode = false
    private var symbolMode = false
    private var symbolPage = 0
    private var suggestionsCollapsed = false
    private var numericInput = false
    private var currentKeyboardType: UIKeyboardType = .default
    private let root = UIStackView()
    private var toolbarRow: UIStackView?
    private let emojiRow = UIStackView()
    private let emojiCategoryRow = UIStackView()
    private let emojiGrid = UIStackView()
    private var emojiCategory = "Recent"
    private var emojiSkinTone = ""
    private let suggestionRow = UIStackView()
    private var toolsMode = false
    private var resizeMode = false
    private var localKeyHeightOverride: CGFloat?
    private var resizeHeightSlider: UISlider?
    private var resizeHeightLabel: UILabel?
    private var activeKeyHeight: CGFloat { localKeyHeightOverride ?? YoryThemeReader.keyHeight }
    private let keysStack = UIStackView()
    private var keyboardHeightConstraint: NSLayoutConstraint?
    private var suggestionRowHeightConstraint: NSLayoutConstraint?

    override func loadView() {
        view = YoryKeyboardRootView()
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        YoryThemeReader.migrateLegacyDefaultIfNeeded()
        if let savedHeight = UserDefaults.standard.object(forKey: "yory.keyboard.keyHeight.local") as? Double {
            localKeyHeightOverride = min(max(CGFloat(savedHeight), 36), 60)
        }
        view.backgroundColor = YoryThemeReader.backgroundColor
        let savedLanguage = UserDefaults(suiteName: "group.app.yory.keys")?.string(forKey: "yory.active.language") ?? (YoryThemeReader.isArabic ? "ar" : "en-US")
        languageTag = YoryKeyboardLayout.resolve(savedLanguage).tag
        isArabic = YoryKeyboardLayout.resolve(languageTag).isRightToLeft
        primaryLanguage = languageTag
        applyCurrentTheme()
        buildInterface()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        updateInputTraits()
        refreshAutomaticShift()
        applyCurrentTheme()
        emojiMode = false
        emojiRow.isHidden = true
        incognitoMode = false
        toolsMode = false
        resizeMode = false
        suggestionsCollapsed = false
        symbolPage = 0
        keysStack.isHidden = false
        updateKeyboardHeight()
        refreshToolbarItems()
        updateSuggestions()
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        updateInputTraits()
        refreshAutomaticShift()
        updateSuggestions()
    }

    private func refreshToolbarItems() {
        guard let toolbar = toolbarRow else { return }
        toolbar.arrangedSubviews.forEach { item in
            toolbar.removeArrangedSubview(item)
            item.removeFromSuperview()
        }

        let defaults = UserDefaults(suiteName: "group.app.yory.keys")
        let defaultOrder = ["emoji", "language", "symbols", "switch", "tools", "privacy"]
        let storedOrder = defaults?.stringArray(forKey: "yory.toolbar.order") ?? defaultOrder
        let order = storedOrder.filter { defaultOrder.contains($0) } + defaultOrder.filter { !storedOrder.contains($0) }
        let visible = Set(defaults?.stringArray(forKey: "yory.toolbar.visible") ?? defaultOrder)

        for tool in order where visible.contains(tool) {
            switch tool {
            case "emoji":
                if YoryThemeReader.emojiKeyEnabled {
                    toolbar.addArrangedSubview(toolButton("☺", accessibilityLabel: "Emoji picker", action: #selector(toggleEmoji)))
                }
            case "language":
                toolbar.addArrangedSubview(toolButton("⇄", accessibilityLabel: "Switch language", action: #selector(toggleLanguage)))
            case "symbols":
                toolbar.addArrangedSubview(toolButton(
                    symbolMode ? (symbolPage == 0 ? "2/2" : "1/2") : "123",
                    accessibilityLabel: symbolMode ? "Show next symbol page" : "Show symbols",
                    action: #selector(toggleSymbols)
                ))
            case "switch":
                toolbar.addArrangedSubview(toolButton("⌨", accessibilityLabel: "Next keyboard", action: #selector(switchKeyboard)))
            case "tools":
                toolbar.addArrangedSubview(toolButton(
                    toolsMode ? "×" : "•••",
                    accessibilityLabel: toolsMode ? "Close tools" : "More tools",
                    action: #selector(toggleTools)
                ))
            case "privacy":
                toolbar.addArrangedSubview(toolButton(
                    incognitoMode ? "◉" : "◎",
                    accessibilityLabel: incognitoMode ? "Turn off Incognito" : "Turn on Incognito",
                    action: #selector(toggleIncognito)
                ))
            default:
                break
            }
        }

        if toolbar.arrangedSubviews.isEmpty {
            toolbar.addArrangedSubview(toolButton("⇄", accessibilityLabel: "Switch language", action: #selector(toggleLanguage)))
            toolbar.addArrangedSubview(toolButton("⌨", accessibilityLabel: "Next keyboard", action: #selector(switchKeyboard)))
        }
        toolbar.semanticContentAttribute = isArabic ? .forceRightToLeft : .forceLeftToRight
        view.setNeedsLayout()
    }

    private func updateInputTraits() {
        let nextType = textDocumentProxy.keyboardType ?? .default
        guard nextType != currentKeyboardType else { return }
        currentKeyboardType = nextType
        numericInput = nextType == .numberPad || nextType == .decimalPad || nextType == .phonePad
        rebuildKeys()
        updateSuggestions()
    }

    private func refreshAutomaticShift() {
        guard YoryThemeReader.autoCapsEnabled else {
            isShifted = false
            return
        }
        guard let capitalizationType = textDocumentProxy.autocapitalizationType else {
            isShifted = false
            return
        }
        switch capitalizationType {
        case .none:
            isShifted = false
        case .allCharacters:
            isShifted = true
        case .words:
            let before = textDocumentProxy.documentContextBeforeInput ?? ""
            isShifted = before.isEmpty || before.last.map {
                $0.isWhitespace || $0.isPunctuation
            } == true
        case .sentences:
            let before = textDocumentProxy.documentContextBeforeInput ?? ""
            let trimmed = before.trimmingCharacters(in: .whitespacesAndNewlines)
            isShifted = trimmed.isEmpty || [".", "!", "?", "…"].contains(String(trimmed.suffix(1)))
        @unknown default:
            isShifted = false
        }
    }

    private func applyCurrentTheme() {
        view.backgroundColor = YoryThemeReader.backgroundColor
        if isViewLoaded {
            updateKeyboardHeight()
            rebuildKeys()
        }
    }

    private func buildInterface() {
        root.axis = .vertical
        root.spacing = 4
        root.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(root)
        let heightConstraint = view.heightAnchor.constraint(equalToConstant: 260)
        keyboardHeightConstraint = heightConstraint
        NSLayoutConstraint.activate([
            root.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 4),
            root.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -4),
            root.topAnchor.constraint(equalTo: view.topAnchor, constant: 4),
            root.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -4),
            heightConstraint
        ])
        updateKeyboardHeight()

        let toolbar = UIStackView()
        toolbar.axis = .horizontal
        toolbar.distribution = .fillEqually
        toolbar.spacing = 2
        let toolbarDefaults = UserDefaults(suiteName: "group.app.yory.keys")
        let defaultOrder = ["emoji", "language", "symbols", "switch", "tools", "privacy"]
        let storedOrder = toolbarDefaults?.stringArray(forKey: "yory.toolbar.order") ?? defaultOrder
        let order = storedOrder.filter { defaultOrder.contains($0) } + defaultOrder.filter { !storedOrder.contains($0) }
        let visible = Set(toolbarDefaults?.stringArray(forKey: "yory.toolbar.visible") ?? defaultOrder)
        for tool in order where visible.contains(tool) {
            switch tool {
            case "emoji":
                if YoryThemeReader.emojiKeyEnabled {
                    toolbar.addArrangedSubview(toolButton("☺", accessibilityLabel: "Emoji picker", action: #selector(toggleEmoji)))
                }
            case "language":
                toolbar.addArrangedSubview(toolButton("⇄", accessibilityLabel: "Switch language", action: #selector(toggleLanguage)))
            case "symbols":
                toolbar.addArrangedSubview(toolButton(
                    symbolMode ? (symbolPage == 0 ? "2/2" : "1/2") : "123",
                    accessibilityLabel: symbolMode ? "Show next symbol page" : "Show symbols",
                    action: #selector(toggleSymbols)
                ))
            case "switch":
                toolbar.addArrangedSubview(toolButton("⌨", accessibilityLabel: "Next keyboard", action: #selector(switchKeyboard)))
            case "tools":
                toolbar.addArrangedSubview(toolButton(
                    toolsMode ? "×" : "•••",
                    accessibilityLabel: toolsMode ? "Close tools" : "More tools",
                    action: #selector(toggleTools)
                ))
            default:
                break
            }
        }
        if toolbar.arrangedSubviews.isEmpty {
            toolbar.addArrangedSubview(toolButton("⇄", accessibilityLabel: "Switch language", action: #selector(toggleLanguage)))
            toolbar.addArrangedSubview(toolButton("⌨", accessibilityLabel: "Next keyboard", action: #selector(switchKeyboard)))
        }
        toolbar.semanticContentAttribute = .forceLeftToRight
        toolbar.backgroundColor = UIColor(red: 34/255, green: 34/255, blue: 34/255, alpha: 1)
        toolbarRow = toolbar
        root.addArrangedSubview(toolbar)
        toolbar.heightAnchor.constraint(equalToConstant: 36).isActive = true

        suggestionRow.axis = .horizontal
        suggestionRow.distribution = .fill
        suggestionRow.spacing = 4
        suggestionRow.semanticContentAttribute = .forceLeftToRight
        root.addArrangedSubview(suggestionRow)
        let suggestionHeight = suggestionRow.heightAnchor.constraint(equalToConstant: 32)
        suggestionRowHeightConstraint = suggestionHeight
        suggestionHeight.isActive = true

        emojiRow.axis = .vertical
        emojiRow.spacing = 3
        emojiCategoryRow.axis = .horizontal
        emojiCategoryRow.distribution = .fillEqually
        emojiCategoryRow.spacing = 3
        for (category, title) in [
            ("Recent", "◷"), ("Favorites", "★"), ("Smile", "☺"), ("People", "👥"),
            ("Nature", "🌿"), ("Food", "🍕"), ("Travel", "✈"), ("Symbols", "♡")
        ] {
            let button = UIButton(type: .system)
            button.setTitle(title, for: .normal)
            button.setTitleColor(YoryThemeReader.accentColor, for: .normal)
            button.titleLabel?.font = .systemFont(ofSize: 15, weight: .semibold)
            button.backgroundColor = UIColor.white.withAlphaComponent(0.055)
            button.layer.cornerRadius = 8
            button.accessibilityLabel = category
            button.accessibilityHint = "Show \(category.lowercased()) emoji"
            button.heightAnchor.constraint(equalToConstant: 29).isActive = true
            button.addAction(UIAction { [weak self] _ in
                self?.emojiCategory = category
                self?.refreshEmojiPicker()
            }, for: .touchUpInside)
            emojiCategoryRow.addArrangedSubview(button)
        }
        emojiRow.addArrangedSubview(emojiCategoryRow)
        emojiGrid.axis = .vertical
        emojiGrid.spacing = 3
        emojiRow.addArrangedSubview(emojiGrid)
        emojiRow.isHidden = true
        root.addArrangedSubview(emojiRow)
        refreshEmojiPicker()

        keysStack.axis = .vertical
        keysStack.spacing = 4
        root.addArrangedSubview(keysStack)
        rebuildKeys()
        updateSuggestions()
    }

    private func rebuildKeys() {
        updateKeyboardHeight()
        let direction: UISemanticContentAttribute = YoryKeyboardLayout.resolve(languageTag).isRightToLeft && !numericInput ? .forceRightToLeft : .forceLeftToRight
        root.semanticContentAttribute = direction
        toolbarRow?.semanticContentAttribute = direction
        suggestionRow.semanticContentAttribute = direction
        emojiRow.semanticContentAttribute = direction
        keysStack.semanticContentAttribute = direction
        keysStack.arrangedSubviews.forEach { item in
            keysStack.removeArrangedSubview(item)
            item.removeFromSuperview()
        }

        if numericInput {
            for row in ["123", "456", "789"] {
                keysStack.addArrangedSubview(makeRow(Array(row).map(String.init)))
            }
            let finalRow = UIStackView()
            finalRow.axis = .horizontal
            finalRow.spacing = 4
            finalRow.distribution = .fillEqually
            finalRow.addArrangedSubview(keyButton("-", action: #selector(insertKey(_:))))
            finalRow.addArrangedSubview(keyButton("0", action: #selector(insertKey(_:))))
            finalRow.addArrangedSubview(keyButton(currentKeyboardType == .phonePad ? "+" : ".", action: #selector(insertKey(_:))))
            keysStack.addArrangedSubview(finalRow)
            let actions = UIStackView()
            actions.axis = .horizontal
            actions.spacing = 4
            actions.distribution = .fillEqually
            actions.addArrangedSubview(keyButton("🌐", action: #selector(toggleLanguage)))
            actions.addArrangedSubview(keyButton("⌫", action: #selector(deleteBackward)))
            actions.addArrangedSubview(keyButton(returnKeyTitle, action: #selector(insertReturn)))
            keysStack.addArrangedSubview(actions)
            return
        }

        let activeLayout = YoryKeyboardLayout.resolve(languageTag)
        let rows: [[String]]
        if symbolMode {
            rows = symbolPage == 0 ? [
                ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"],
                ["@", "#", "$", "%", "&", "_", "-", "(", ")", "="],
                ["{", "}", "[", "]", "\"", "*", "'", "/", "؟", "⌫"]
            ] : [
                ["~", "^", "`", "€", "£", "¥", "¢", "°", "×", "÷"],
                ["«", "»", "“", "”", "‘", "’", "؛", "،", "|", "\\"],
                ["<", ">", "!", "?", ";", ":", "+", "=", "•", "⌫"]
            ]
        } else {
            rows = activeLayout.rows.map { Array($0).map(String.init) }
        }

        if YoryThemeReader.numberRowEnabled && YoryThemeReader.numberPosition == "top" && !symbolMode {
            keysStack.addArrangedSubview(makeRow(Array("1234567890").map(String.init)))
        }
        for (index, baseLabels) in rows.enumerated() {
            var labels = baseLabels
            let isArabicRow = activeLayout.isRightToLeft && !symbolMode
            if !symbolMode && index == 2 {
                if isArabicRow {
                    labels.append("⌫")
                } else {
                    labels.insert("⇧", at: 0)
                    labels.append("⌫")
                }
            }
            let hints: [String: String]
            if isArabicRow && index == 0 {
                hints = Dictionary(uniqueKeysWithValues: zip(baseLabels, ["1","2","3","4","5","6","7","8","9","0","-","="]))
            } else if isArabicRow && index == 1 {
                hints = Dictionary(uniqueKeysWithValues: zip(baseLabels, ["@","#","&","پ","لا","أ","=","(",")","گ","*"]))
            } else if isArabicRow && index == 2 {
                hints = Dictionary(uniqueKeysWithValues: zip(baseLabels, ["−","~","−","+","؛",":","/","ژ","”","◌"]))
            } else {
                hints = [:]
            }
            let homeRowInset: CGFloat = isArabicRow && index > 0 ? 10 : ((!symbolMode && !isArabicRow && index == 1) ? 10 : 0)
            keysStack.addArrangedSubview(makeRow(
                labels,
                compact: isArabicRow,
                secondaryHints: hints,
                horizontalInset: homeRowInset
            ))
        }

        let bottom = UIStackView()
        bottom.semanticContentAttribute = isArabic ? .forceRightToLeft : .forceLeftToRight
        bottom.axis = .horizontal
        bottom.spacing = 4
        bottom.distribution = .fill
        func appendBottomKey(_ button: UIButton, width: CGFloat) {
            bottom.addArrangedSubview(button)
            button.widthAnchor.constraint(equalTo: bottom.widthAnchor, multiplier: width).isActive = true
        }
        let fieldSymbol = currentKeyboardType == .emailAddress ? "@" : (currentKeyboardType == .URL ? "/" : ",")
        if symbolMode {
            appendBottomKey(keyButton("ABC", action: #selector(exitSymbols)), width: 0.09)
            appendBottomKey(keyButton(symbolPage == 0 ? "2/2" : "1/2", action: #selector(toggleSymbolPage)), width: 0.07)
            appendBottomKey(keyButton("☺", action: #selector(toggleEmoji)), width: 0.08)
            appendBottomKey(keyButton(fieldSymbol), width: 0.07)
        } else {
            appendBottomKey(keyButton("123", action: #selector(toggleSymbols)), width: 0.10)
            appendBottomKey(keyButton("☺", action: #selector(toggleEmoji)), width: 0.09)
            appendBottomKey(keyButton(fieldSymbol), width: 0.08)
        }
        let spaceTitle = "‹  " + activeLayout.spaceLabel + "  ›"
        let space = keyButton(spaceTitle, action: #selector(insertSpace))
        space.accessibilityLabel = "Space, " + activeLayout.spaceLabel
        space.accessibilityHint = "Swipe left for the next added language or right for the previous added language"
        let swipeLeft = UISwipeGestureRecognizer(target: self, action: #selector(handleSpacebarSwipe(_:)))
        swipeLeft.direction = .left
        let swipeRight = UISwipeGestureRecognizer(target: self, action: #selector(handleSpacebarSwipe(_:)))
        swipeRight.direction = .right
        space.addGestureRecognizer(swipeLeft)
        space.addGestureRecognizer(swipeRight)
        appendBottomKey(space, width: symbolMode ? 0.39 : 0.42)
        appendBottomKey(keyButton(".", secondaryHint: "؟!"), width: symbolMode ? 0.08 : 0.09)
        appendBottomKey(keyButton(returnKeyTitle, action: #selector(insertReturn)), width: 0.16)
        keysStack.addArrangedSubview(bottom)
        if YoryThemeReader.numberRowEnabled && YoryThemeReader.numberPosition == "bottom" {
            keysStack.addArrangedSubview(makeRow(Array("1234567890").map(String.init)))
        }
        if YoryThemeReader.arrowKeysEnabled {
            keysStack.addArrangedSubview(makeArrowRow())
        }
        updateKeyboardHeight()
    }

    private func makeRow(
        _ labels: [String],
        compact: Bool = false,
        secondaryHints: [String: String] = [:],
        horizontalInset: CGFloat = 0
    ) -> UIStackView {
        let row = UIStackView()
        row.semanticContentAttribute = isArabic && !numericInput ? .forceRightToLeft : .forceLeftToRight
        row.axis = .horizontal
        row.spacing = compact ? max(1, YoryThemeReader.keySpacing * 0.35) : YoryThemeReader.keySpacing
        let hasWideModifiers = labels.contains("⇧") || labels.contains("⌫")
        row.distribution = hasWideModifiers ? .fill : .fillEqually
        if horizontalInset > 0 {
            row.isLayoutMarginsRelativeArrangement = true
            row.layoutMargins = UIEdgeInsets(top: 0, left: horizontalInset, bottom: 0, right: horizontalInset)
        }
        let totalUnits = labels.reduce(CGFloat(0)) { total, label in
            total + ((label == "⇧" || label == "⌫") ? 1.25 : 1.0)
        }
        for label in labels {
            let key: UIButton
            if label == "⇧" {
                key = keyButton(label, action: #selector(toggleShift), compact: compact)
            } else if label == "⌫" {
                key = keyButton(label, action: #selector(deleteBackward), compact: compact)
            } else {
                let display = isShifted ? label.uppercased() : label
                key = keyButton(display, secondaryHint: secondaryHints[label], compact: compact)
            }
            if hasWideModifiers {
                let units: CGFloat = (label == "⇧" || label == "⌫") ? 1.25 : 1.0
                key.widthAnchor.constraint(equalTo: row.widthAnchor, multiplier: 0.94 * units / totalUnits).isActive = true
            }
            row.addArrangedSubview(key)
        }
        return row
    }

    private func updateKeyboardHeight() {
        if toolsMode {
            let panelHeight: CGFloat = resizeMode ? 164 : 210
            let chrome = 4 + 36 + panelHeight + 8
            keyboardHeightConstraint?.constant = min(430, max(240, chrome))
            return
        }
        let rowCount: CGFloat = numericInput ? 5 :
            CGFloat(4 + (YoryThemeReader.numberRowEnabled ? 1 : 0) + (YoryThemeReader.arrowKeysEnabled ? 1 : 0))
        let rowGap: CGFloat = 4
        let innerGaps = max(0, rowCount - 1) * rowGap
        let emojiPanelHeight: CGFloat = emojiMode ? (emojiCategory == "People" ? 126 : 98) : 0
        let suggestionHeight = suggestionRowHeightConstraint?.constant ?? 32
        let topChrome: CGFloat = 8 + 36 + suggestionHeight + rowGap * 2
        let required = topChrome + rowCount * activeKeyHeight + innerGaps + emojiPanelHeight
        keyboardHeightConstraint?.constant = min(430, max(240, required))
    }

    private func longPressMenu(for title: String) -> UIMenu? {
        guard title.count == 1, title.first?.isLetter == true else { return nil }
        var choices: [String] = []
        let key = title.lowercased()
        if YoryThemeReader.longPressSymbolsEnabled && !isArabic {
            let symbols: [String: String] = [
                "q":"1","w":"2","e":"3","r":"4","t":"5","y":"6","u":"7","i":"8","o":"9","p":"0",
                "a":"@","s":"$","d":"&","f":"-","g":"+","h":"(","j":")","k":"/","l":":",
                "z":"!","x":"?","c":"'","v":"\"","b":";","n":"_","m":"."
            ]
            if let symbol = symbols[key] { choices.append(symbol) }
        }
        if YoryThemeReader.accentedCharactersEnabled {
            let accents: [String: [String]]
            if isArabic {
                accents = ["ا":["أ","إ","آ"], "و":["ؤ"], "ي":["ئ","ى"], "ه":["ة"]]
            } else if languageTag == "de" {
                accents = ["a":["ä"], "o":["ö"], "u":["ü"], "s":["ß"]]
            } else {
                accents = [
                    "a":["á","à","â","ä","ã","å"], "e":["é","è","ê","ë"],
                    "i":["í","ì","î","ï"], "o":["ó","ò","ô","ö","õ"],
                    "u":["ú","ù","û","ü"], "n":["ñ"], "c":["ç"], "y":["ý","ÿ"]
                ]
            }
            choices.append(contentsOf: accents[key] ?? [])
        }
        let unique = Array(NSOrderedSet(array: choices)) as? [String] ?? choices
        guard !unique.isEmpty else { return nil }
        let actions = unique.map { candidate in
            UIAction(title: candidate) { [weak self] _ in
                self?.playKeyClick()
                self?.textDocumentProxy.insertText(candidate)
                self?.refreshAutomaticShift()
                self?.updateSuggestions()
            }
        }
        return UIMenu(title: "", children: actions)
    }

    private func makeArrowRow() -> UIStackView {
        let row = UIStackView()
        row.axis = .horizontal
        row.spacing = YoryThemeReader.keySpacing
        row.distribution = .fillEqually
        for (symbol, direction) in [("←", -1), ("↑", -2), ("↓", -3), ("→", 1)] {
            let button = keyButton(symbol, action: #selector(moveCursor(_:)))
            button.tag = direction
            row.addArrangedSubview(button)
        }
        return row
    }

    @objc private func moveCursor(_ sender: UIButton) {
        playKeyClick()
        let before = textDocumentProxy.documentContextBeforeInput ?? ""
        let after = textDocumentProxy.documentContextAfterInput ?? ""
        let beforeLine = before.components(separatedBy: "\n")
        let afterLine = after.components(separatedBy: "\n")
        let currentColumn = beforeLine.last?.count ?? 0
        switch sender.tag {
        case -1:
            textDocumentProxy.adjustTextPosition(byCharacterOffset: -1)
        case 1:
            textDocumentProxy.adjustTextPosition(byCharacterOffset: 1)
        case -2:
            guard beforeLine.count > 1 else { return }
            let previousLineLength = beforeLine[beforeLine.count - 2].count
            let offset = -(currentColumn + 1 + previousLineLength - min(currentColumn, previousLineLength))
            textDocumentProxy.adjustTextPosition(byCharacterOffset: offset)
        case -3:
            guard afterLine.count > 1 else { return }
            let nextLineLength = afterLine[1].count
            let currentLineTail = afterLine[0].count
            let offset = currentLineTail + 1 + min(currentColumn, nextLineLength)
            textDocumentProxy.adjustTextPosition(byCharacterOffset: offset)
        default:
            break
        }
        updateSuggestions()
    }

    private func keyButton(
        _ title: String,
        action: Selector? = #selector(insertKey(_:)),
        secondaryHint: String? = nil,
        compact: Bool = false
    ) -> UIButton {
        let button = UIButton(type: .system)
        let controlIcons: [String: String] = [
            "⇧": "shift",
            "⌫": "delete.left",
            "🌐": "globe",
            "☺": "face.smiling",
            "↵": "return"
        ]
        if let symbolName = controlIcons[title], let image = UIImage(systemName: symbolName) {
            button.setImage(image.withRenderingMode(.alwaysTemplate), for: .normal)
            button.tintColor = YoryThemeReader.textColor
            button.imageView?.contentMode = .scaleAspectFit
        } else {
            button.setTitle(title, for: .normal)
            let baseFontSize = compact
                ? min(15.5, YoryThemeReader.fontSize)
                : (title.count > 2 ? max(10, YoryThemeReader.fontSize - 6) : YoryThemeReader.fontSize)
            let displayedFontSize = YoryThemeReader.largeKeyTextEnabled && !compact ? baseFontSize * 1.08 : baseFontSize
            button.titleLabel?.font = keyboardFont(size: displayedFontSize, weight: .medium)
            button.setTitleColor(YoryThemeReader.textColor, for: .normal)
        }
        button.backgroundColor = YoryThemeReader.keyColor.withAlphaComponent(YoryThemeReader.opacity)
        button.layer.cornerRadius = {
            switch YoryThemeReader.keyShape {
            case "pill", "circle": return 18.0
            case "squircle": return YoryThemeReader.cornerRadius * 1.3
            case "rect": return 3.0
            default: return YoryThemeReader.cornerRadius
            }
        }()
        button.clipsToBounds = false
        if let secondaryHint, !secondaryHint.isEmpty {
            let legend = UILabel()
            legend.text = secondaryHint
            legend.font = .systemFont(ofSize: 8, weight: .medium)
            legend.textColor = YoryThemeReader.textColor.withAlphaComponent(0.60)
            legend.textAlignment = .right
            legend.translatesAutoresizingMaskIntoConstraints = false
            legend.isUserInteractionEnabled = false
            button.addSubview(legend)
            NSLayoutConstraint.activate([
                legend.topAnchor.constraint(equalTo: button.topAnchor, constant: 2),
                legend.trailingAnchor.constraint(equalTo: button.trailingAnchor, constant: -4),
                legend.leadingAnchor.constraint(greaterThanOrEqualTo: button.leadingAnchor, constant: 2)
            ])
            button.titleEdgeInsets = UIEdgeInsets(top: 7, left: 0, bottom: -7, right: 0)
        }
        button.layer.borderWidth = 0.7
        let utilityKey = ["⇧", "⌫", "↵", "!#1", "ABC", "🌐", "123"].contains(title)
        button.layer.borderColor = (utilityKey
            ? YoryThemeReader.accentColor.withAlphaComponent(0.27)
            : UIColor.white.withAlphaComponent(0.10)).cgColor
        button.layer.shadowColor = UIColor.black.cgColor
        button.layer.shadowOpacity = 0.42
        button.layer.shadowRadius = 2
        button.layer.shadowOffset = CGSize(width: 0, height: 2)
        button.accessibilityLabel = [
            "⇧": "Shift",
            "⌫": "Delete",
            "🌐": "Switch language",
            "↵": "Return"
        ][title] ?? title
        button.addTarget(self, action: action ?? #selector(insertKey(_:)), for: .touchUpInside)
        button.addTarget(self, action: #selector(keyTouchDown(_:)), for: .touchDown)
        button.addTarget(self, action: #selector(keyTouchUp(_:)), for: [.touchUpInside, .touchUpOutside, .touchCancel])
        button.heightAnchor.constraint(equalToConstant: activeKeyHeight).isActive = true
        if let menu = longPressMenu(for: title) {
            button.menu = menu
            button.showsMenuAsPrimaryAction = false
        }
        return button
    }

    @objc private func keyTouchDown(_ sender: UIButton) {
        sender.backgroundColor = YoryThemeReader.keyPressedColor.withAlphaComponent(0.42)
        sender.layer.borderColor = YoryThemeReader.accentColor.withAlphaComponent(0.62).cgColor
        if YoryThemeReader.keyPopupsEnabled {
            UIView.animate(withDuration: 0.08) {
                sender.transform = CGAffineTransform(scaleX: 0.96, y: 0.94)
            }
        }
    }

    @objc private func keyTouchUp(_ sender: UIButton) {
        sender.backgroundColor = YoryThemeReader.keyColor.withAlphaComponent(YoryThemeReader.opacity)
        let utilityKey = ["Shift", "Delete", "Return", "Switch language", "!#1", "ABC", "123"].contains(sender.accessibilityLabel ?? "")
        sender.layer.borderColor = (utilityKey
            ? YoryThemeReader.accentColor.withAlphaComponent(0.27)
            : UIColor.white.withAlphaComponent(0.10)).cgColor
        UIView.animate(withDuration: 0.10) {
            sender.transform = .identity
        }
    }

    private func keyboardFont(size: CGFloat, weight: UIFont.Weight) -> UIFont {
        switch YoryThemeReader.fontFamily {
        case "rounded":
            let base = UIFont.systemFont(ofSize: size, weight: weight)
            let descriptor = base.fontDescriptor.withDesign(.rounded) ?? base.fontDescriptor
            return UIFont(descriptor: descriptor, size: size)
        case "serif":
            let base = UIFont.systemFont(ofSize: size, weight: weight)
            let descriptor = base.fontDescriptor.withDesign(.serif) ?? base.fontDescriptor
            return UIFont(descriptor: descriptor, size: size)
        case "mono":
            return UIFont.monospacedSystemFont(ofSize: size, weight: weight)
        default:
            return .systemFont(ofSize: size, weight: weight)
        }
    }

    private func toolButton(
        _ title: String,
        accessibilityLabel: String? = nil,
        action: Selector
    ) -> UIButton {
        let button = UIButton(type: .system)
        let iconNames: [String: String] = [
            "☺": "face.smiling",
            "⇄": "globe",
            "Aa": "character",
            "⌨": "keyboard",
            "•••": "ellipsis",
            "×": "xmark",
            "◉": "eye.slash",
            "◎": "eye",
            "▣": "clipboard"
        ]
        if let iconName = iconNames[title], let image = UIImage(systemName: iconName) {
            button.setImage(image, for: .normal)
            button.tintColor = YoryThemeReader.textColor
        } else {
            button.setTitle(title, for: .normal)
            button.setTitleColor(YoryThemeReader.textColor, for: .normal)
            button.titleLabel?.font = .systemFont(ofSize: 18, weight: .semibold)
        }
        button.accessibilityLabel = accessibilityLabel ?? title
        button.backgroundColor = .clear
        button.layer.cornerRadius = 10
        button.layer.borderWidth = 0
        button.layer.shadowOpacity = 0
        button.addTarget(self, action: action, for: .touchUpInside)
        button.heightAnchor.constraint(equalToConstant: 32).isActive = true
        return button
    }

    @objc private func insertKey(_ sender: UIButton) {
        guard let title = sender.title(for: .normal) else { return }
        if title.range(of: "^[^\\p{L}\\p{M}]+$", options: .regularExpression) != nil {
            learnCurrentWord()
        }
        playKeyClick()
        textDocumentProxy.insertText(isShifted ? title.uppercased() : title)
        refreshAutomaticShift()
        rebuildKeys()
        updateSuggestions()
    }

    private func playKeyClick() {
        if YoryThemeReader.soundEnabled && hasFullAccess {
            UIDevice.current.playInputClick()
        }
        if YoryThemeReader.hapticsEnabled {
            let feedback = UIImpactFeedbackGenerator(style: .light)
            feedback.prepare()
            feedback.impactOccurred(intensity: YoryThemeReader.hapticIntensity)
        }
    }

    @objc private func deleteBackward() {
        playKeyClick()
        textDocumentProxy.deleteBackward()
        refreshAutomaticShift()
        rebuildKeys()
        updateSuggestions()
    }

    @objc private func insertSpace() {
        learnCurrentWord()
        playKeyClick()
        textDocumentProxy.insertText(" ")
        refreshAutomaticShift()
        rebuildKeys()
        updateSuggestions()
    }

    private var returnKeyTitle: String {
        let returnType = textDocumentProxy.returnKeyType ?? UIReturnKeyType.default
        switch returnType {
        case .go:
            return "Go"
        case .google:
            return "Google"
        case .join:
            return "Join"
        case .next:
            return "Next"
        case .route:
            return "Route"
        case .search:
            return "Search"
        case .send:
            return "Send"
        case .done:
            return "Done"
        case .emergencyCall:
            return "Call"
        case .continue:
            return "Continue"
        default:
            return "↵"
        }
    }

    @objc private func insertReturn() {
        learnCurrentWord()
        playKeyClick()
        textDocumentProxy.insertText("\n")
        refreshAutomaticShift()
        rebuildKeys()
        updateSuggestions()
    }

    @objc private func toggleShift() {
        playKeyClick()
        isShifted.toggle()
        rebuildKeys()
    }

    @objc private func toggleLanguage() {
        playKeyClick()
        cycleLanguage(direction: 1)
    }

    private func cycleLanguage(direction: Int) {
        toolsMode = false
        keysStack.isHidden = false
        let defaults = UserDefaults(suiteName: "group.app.yory.keys")
        let stored = defaults?.stringArray(forKey: "yory.added.languages.v1") ?? ["en-US", "ar"]
        let available = stored.filter { tag in
            YoryKeyboardLayout.supported.contains(where: { $0.tag.caseInsensitiveCompare(tag) == .orderedSame })
        }
        languageTag = YoryKeyboardLayout.nextTag(after: languageTag, availableTags: available, direction: direction)
        isArabic = YoryKeyboardLayout.resolve(languageTag).isRightToLeft
        if hasFullAccess {
            defaults?.set(languageTag, forKey: "yory.active.language")
        }
        primaryLanguage = languageTag
        symbolMode = false
        symbolPage = 0
        rebuildKeys()
        updateSuggestions()
        refreshToolbarItems()
    }

    @objc private func handleSpacebarSwipe(_ recognizer: UISwipeGestureRecognizer) {
        playKeyClick()
        cycleLanguage(direction: recognizer.direction == .left ? 1 : -1)
    }

    @objc private func toggleSymbols() {
        playKeyClick()
        toolsMode = false
        resizeMode = false
        keysStack.isHidden = false
        emojiMode = false
        emojiRow.isHidden = true
        if symbolMode {
            symbolPage = (symbolPage + 1) % 2
        } else {
            symbolMode = true
            symbolPage = 0
        }
        rebuildKeys()
        updateSuggestions()
        refreshToolbarItems()
    }

    @objc private func exitSymbols() {
        playKeyClick()
        symbolMode = false
        symbolPage = 0
        rebuildKeys()
    }

    @objc private func toggleSymbolPage() {
        playKeyClick()
        symbolPage = (symbolPage + 1) % 2
        rebuildKeys()
    }

    private func learnCurrentWord() {
        guard hasFullAccess,
              !incognitoMode,
              YoryThemeReader.personalLearningEnabled,
              !numericInput,
              currentKeyboardType != .emailAddress,
              currentKeyboardType != .URL else { return }
        let context = textDocumentProxy.documentContextBeforeInput ?? ""
        guard let range = context.range(of: "[\\p{L}\\p{M}]{2,32}$", options: .regularExpression) else {
            return
        }
        let word = String(context[range])
        YoryLocalWordSuggestions.learnAcceptedWord(word, languageTag: languageTag)
    }

    @objc private func toggleTools() {
        playKeyClick()
        toolsMode.toggle()
        if toolsMode {
            resizeMode = false
            emojiMode = false
        }
        keysStack.isHidden = toolsMode
        emojiRow.isHidden = !emojiMode || toolsMode
        rebuildKeys()
        updateSuggestions()
        refreshToolbarItems()
    }

    @objc private func clearLearnedWords() {
        YoryLocalWordSuggestions.clearPersonalWords()
        updateSuggestions()
    }

    private func toolActionButton(_ title: String, action: Selector) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(title, for: .normal)
        button.setTitleColor(YoryThemeReader.accentColor, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 12, weight: .medium)
        button.addTarget(self, action: action, for: .touchUpInside)
        return button
    }

    @objc private func toggleIncognito() {
        incognitoMode.toggle()
        if incognitoMode { toolsMode = false; resizeMode = false }
        keysStack.isHidden = toolsMode
        refreshToolbarItems()
        rebuildKeys()
        updateSuggestions()
    }

    @objc private func toggleEmoji() {
        playKeyClick()
        toolsMode = false
        resizeMode = false
        keysStack.isHidden = false
        emojiMode.toggle()
        if emojiMode { refreshEmojiPicker() }
        emojiRow.isHidden = !emojiMode
        updateKeyboardHeight()
        updateSuggestions()
        refreshToolbarItems()
    }

    private func refreshEmojiPicker() {
        emojiGrid.arrangedSubviews.forEach { item in
            emojiGrid.removeArrangedSubview(item)
            item.removeFromSuperview()
        }

        if emojiCategory == "People" {
            let toneRow = UIStackView()
            toneRow.axis = .horizontal
            toneRow.distribution = .fillEqually
            toneRow.spacing = 3
            let tones = [("", "○", "Default skin tone"), ("🏻", "🏻", "Light skin tone"), ("🏼", "🏼", "Medium-light skin tone"), ("🏽", "🏽", "Medium skin tone"), ("🏾", "🏾", "Medium-dark skin tone"), ("🏿", "🏿", "Dark skin tone")]
            for (tone, title, description) in tones {
                let button = makeEmojiButton(title, small: true) { [weak self] in
                    self?.emojiSkinTone = tone
                    self?.refreshEmojiPicker()
                }
                button.accessibilityLabel = description
                toneRow.addArrangedSubview(button)
            }
            emojiGrid.addArrangedSubview(toneRow)
        }

        let recent = UserDefaults.standard.stringArray(forKey: "yory.emoji.recent.v1") ?? []
        let choices: [String]
        switch emojiCategory {
        case "Recent":
            choices = recent.isEmpty ? ["😀", "😂", "🥰", "❤️", "✨", "🙏", "👍", "🔥"] : recent
        case "Favorites":
            choices = UserDefaults.standard.stringArray(forKey: "yory.emoji.favorites.v1") ?? []
        case "Smile":
            choices = ["😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃", "😉", "😊", "😍", "🥰"]
        case "People":
            choices = ["👋", "👌", "✌", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆", "👇", "☝", "👍", "👎"]
        case "Nature":
            choices = ["🐶", "🐱", "🐻", "🦊", "🐼", "🐸", "🦋", "🌸", "🌹", "🌻", "🌈", "🌙", "⭐", "🌍"]
        case "Food":
            choices = ["🍎", "🍓", "🍉", "🍕", "🍔", "🍟", "🍿", "🍩", "🍪", "🎂", "☕", "🧋", "🥗", "🍣"]
        case "Travel":
            choices = ["🚗", "🚕", "🚌", "🚆", "✈️", "🚀", "🚲", "⛵", "🏝️", "🏖️", "🏙️", "🗽", "🏰", "🌋"]
        default:
            choices = ["❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💔", "💕", "💞", "💯", "✅", "✨"]
        }

        let visible = Array(choices.prefix(14))
        if visible.isEmpty {
            let emptyState = UILabel()
            emptyState.text = emojiCategory == "Favorites"
                ? "No favorites yet. Long-press an emoji to save it."
                : "No recent emoji yet."
            emptyState.textColor = YoryThemeReader.textColor
            emptyState.textAlignment = .center
            emptyState.font = .systemFont(ofSize: 11, weight: .medium)
            emptyState.numberOfLines = 2
            emptyState.accessibilityLabel = emptyState.text
            emojiGrid.addArrangedSubview(emptyState)
        } else {
            for start in stride(from: 0, to: visible.count, by: 7) {
                let end = min(start + 7, visible.count)
                let row = UIStackView()
                row.axis = .horizontal
                row.distribution = .fillEqually
                row.spacing = 3
                for rawEmoji in visible[start..<end] {
                    let emoji = emojiCategory == "People" ? emojiWithSkinTone(rawEmoji) : rawEmoji
                    row.addArrangedSubview(makeEmojiButton(emoji) { [weak self] in
                        self?.insertEmoji(emoji)
                    })
                }
                while row.arrangedSubviews.count < 7 {
                    row.addArrangedSubview(UIView())
                }
                emojiGrid.addArrangedSubview(row)
            }
        }
        updateKeyboardHeight()
    }

    private func makeEmojiButton(_ title: String, small: Bool = false, action: @escaping () -> Void) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(title, for: .normal)
        button.setTitleColor(YoryThemeReader.accentColor, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: small ? 17 : 22, weight: .medium)
        button.backgroundColor = UIColor.white.withAlphaComponent(0.055)
        button.layer.cornerRadius = 8
        button.layer.borderWidth = 0.5
        button.layer.borderColor = UIColor.white.withAlphaComponent(0.15).cgColor
        button.heightAnchor.constraint(equalToConstant: small ? 25 : 31).isActive = true
        button.accessibilityLabel = title
        button.addAction(UIAction { _ in action() }, for: .touchUpInside)
        if !small {
            let favorites = UserDefaults.standard.stringArray(forKey: "yory.emoji.favorites.v1") ?? []
            let isFavorite = favorites.contains(title)
            button.menu = UIMenu(children: [
                UIAction(
                    title: isFavorite ? "Remove from Favorites" : "Add to Favorites",
                    image: UIImage(systemName: isFavorite ? "star.slash" : "star")
                ) { [weak self] _ in
                    self?.toggleFavoriteEmoji(title)
                }
            ])
            button.showsMenuAsPrimaryAction = false
            button.accessibilityHint = "Long press for favorite options"
        }
        return button
    }

    private func toggleFavoriteEmoji(_ emoji: String) {
        guard !emoji.isEmpty else { return }
        let key = "yory.emoji.favorites.v1"
        var favorites = UserDefaults.standard.stringArray(forKey: key) ?? []
        if let index = favorites.firstIndex(of: emoji) {
            favorites.remove(at: index)
        } else {
            favorites.insert(emoji, at: 0)
        }
        UserDefaults.standard.set(Array(favorites.prefix(80)), forKey: key)
        refreshEmojiPicker()
    }

    private func emojiWithSkinTone(_ emoji: String) -> String {
        guard !emojiSkinTone.isEmpty, let first = emoji.unicodeScalars.first else { return emoji }
        return String(first) + emojiSkinTone
    }

    private func insertEmoji(_ emoji: String) {
        playKeyClick()
        textDocumentProxy.insertText(emoji)
        var recent = UserDefaults.standard.stringArray(forKey: "yory.emoji.recent.v1") ?? []
        recent.removeAll { $0 == emoji }
        recent.insert(emoji, at: 0)
        UserDefaults.standard.set(Array(recent.prefix(24)), forKey: "yory.emoji.recent.v1")
        if emojiCategory == "Recent" { refreshEmojiPicker() }
    }

    private func updateSuggestions() {
        suggestionRow.arrangedSubviews.forEach { item in
            suggestionRow.removeArrangedSubview(item)
            item.removeFromSuperview()
        }
        suggestionRow.isHidden = false

        if toolsMode {
            keysStack.isHidden = true
            emojiRow.isHidden = true
            suggestionRow.axis = .vertical
            suggestionRow.distribution = .fill
            suggestionRow.spacing = 0
            suggestionRowHeightConstraint?.constant = resizeMode ? 164 : 220
            suggestionRow.addArrangedSubview(resizeMode ? makeResizePanel() : makeToolsGrid())
            view.setNeedsLayout()
            updateKeyboardHeight()
            return
        }

        keysStack.isHidden = false
        suggestionRow.axis = .horizontal
        suggestionRow.distribution = .fill
        suggestionRow.spacing = 4
        suggestionRow.semanticContentAttribute = .forceLeftToRight
        let compactMode = UserDefaults(suiteName: "group.app.yory.keys")?.bool(forKey: "yory.input.compactMode") ?? false
        suggestionRowHeightConstraint?.constant = suggestionsCollapsed ? 19 : (compactMode ? 24 : 32)
        suggestionRow.isHidden = incognitoMode || emojiMode || numericInput || !YoryThemeReader.wordSuggestionsEnabled
        guard !incognitoMode && !emojiMode && !numericInput && YoryThemeReader.wordSuggestionsEnabled else {
            updateKeyboardHeight()
            return
        }

        suggestionRow.addArrangedSubview(makeSuggestionUtilityButton(
            suggestionsCollapsed ? "chevron.down" : "chevron.up",
            accessibility: suggestionsCollapsed ? "Expand suggestions" : "Collapse suggestions"
        ) { [weak self] in
            self?.suggestionsCollapsed.toggle()
            self?.updateSuggestions()
        })
        if suggestionsCollapsed {
            updateKeyboardHeight()
            return
        }

        let context = textDocumentProxy.documentContextBeforeInput ?? ""
        let candidates: [String]
        if let range = context.range(of: "[\\p{L}\\p{M}]+$", options: .regularExpression) {
            let prefix = String(context[range])
            candidates = Array(YoryLocalWordSuggestions.suggestions(prefix: prefix, languageTag: languageTag).prefix(compactMode ? 2 : 3))
        } else {
            candidates = []
        }

        let slots = compactMode ? 2 : 3
        let suggestionsStack = UIStackView()
        suggestionsStack.axis = .horizontal
        suggestionsStack.distribution = .fillEqually
        suggestionsStack.spacing = 0
        for index in 0..<slots {
            let suggestion = candidates.indices.contains(index) ? candidates[index] : ""
            let cell = UIView()
            let button = UIButton(type: .system)
            button.setTitle(suggestion, for: .normal)
            button.setTitleColor(YoryThemeReader.textColor.withAlphaComponent(suggestion.isEmpty ? 0.18 : 1), for: .normal)
            button.titleLabel?.font = .systemFont(ofSize: 14, weight: .medium)
            button.titleLabel?.lineBreakMode = .byTruncatingTail
            button.accessibilityLabel = suggestion.isEmpty ? "No suggestion" : "Use suggestion " + suggestion
            button.isEnabled = !suggestion.isEmpty
            if !suggestion.isEmpty {
                button.addAction(UIAction { [weak self] _ in
                    self?.replaceCurrentWord(with: suggestion)
                }, for: .touchUpInside)
            }
            button.translatesAutoresizingMaskIntoConstraints = false
            cell.addSubview(button)
            NSLayoutConstraint.activate([
                button.leadingAnchor.constraint(equalTo: cell.leadingAnchor),
                button.trailingAnchor.constraint(equalTo: cell.trailingAnchor, constant: index < slots - 1 ? -1 : 0),
                button.topAnchor.constraint(equalTo: cell.topAnchor),
                button.bottomAnchor.constraint(equalTo: cell.bottomAnchor)
            ])
            if index < slots - 1 {
                let divider = UIView()
                divider.backgroundColor = UIColor.white.withAlphaComponent(0.16)
                divider.translatesAutoresizingMaskIntoConstraints = false
                cell.addSubview(divider)
                NSLayoutConstraint.activate([
                    divider.widthAnchor.constraint(equalToConstant: 1),
                    divider.topAnchor.constraint(equalTo: cell.topAnchor, constant: 8),
                    divider.bottomAnchor.constraint(equalTo: cell.bottomAnchor, constant: -8),
                    divider.trailingAnchor.constraint(equalTo: cell.trailingAnchor)
                ])
            }
            suggestionsStack.addArrangedSubview(cell)
        }
        suggestionRow.addArrangedSubview(suggestionsStack)
        updateKeyboardHeight()
    }

    private func makeToolsGrid() -> UIView {
        typealias Tool = (title: String, symbol: String, enabled: Bool, action: () -> Void)
        let items: [Tool] = [
            ("Stickers", "face.smiling", false, {}),
            ("Settings", "gearshape", false, {}),
            ("Languages", "globe", true, { [weak self] in self?.cycleLanguage(direction: 1) }),
            ("Layouts", "keyboard", false, {}),
            ("Incognito", "eye.slash", true, { [weak self] in self?.toggleIncognito() }),
            ("Resize", "arrow.up.left.and.arrow.down.right", true, { [weak self] in
                self?.resizeMode = true
                self?.updateSuggestions()
            }),
            ("Themes", "paintpalette", false, {}),
            ("Modes", "character", true, { [weak self] in self?.toggleSymbols() }),
            ("Emoji", "face.smiling", true, { [weak self] in self?.toggleEmoji() }),
            ("Share", "square.and.arrow.up", false, {}),
            ("Autocorrect", "text.badge.checkmark", false, {}),
            ("Create", "plus.rectangle.on.folder", false, {})
        ]

        let grid = UIStackView()
        grid.axis = .vertical
        grid.distribution = .fillEqually
        grid.spacing = 0
        for rowIndex in 0..<3 {
            let row = UIStackView()
            row.axis = .horizontal
            row.distribution = .fillEqually
            row.spacing = 0
            for column in 0..<4 {
                let item = items[rowIndex * 4 + column]
                let cell = UIView()
                let button = UIButton(type: .system)
                var configuration = UIButton.Configuration.plain()
                configuration.image = UIImage(systemName: item.symbol)
                configuration.title = item.title
                configuration.imagePlacement = .top
                configuration.imagePadding = 5
                configuration.baseForegroundColor = YoryThemeReader.textColor.withAlphaComponent(item.enabled ? 1 : 0.40)
                configuration.titleTextAttributesTransformer = UIConfigurationTextAttributesTransformer { incoming in
                    var outgoing = incoming
                    outgoing.font = .systemFont(ofSize: 11, weight: .regular)
                    return outgoing
                }
                button.configuration = configuration
                button.isEnabled = item.enabled
                button.accessibilityLabel = item.title
                if !item.enabled {
                    button.accessibilityHint = "This tool is not available in the current build"
                    button.alpha = 0.48
                } else {
                    button.addAction(UIAction { _ in item.action() }, for: .touchUpInside)
                }
                button.translatesAutoresizingMaskIntoConstraints = false
                cell.addSubview(button)
                NSLayoutConstraint.activate([
                    button.leadingAnchor.constraint(equalTo: cell.leadingAnchor),
                    button.trailingAnchor.constraint(equalTo: cell.trailingAnchor),
                    button.topAnchor.constraint(equalTo: cell.topAnchor),
                    button.bottomAnchor.constraint(equalTo: cell.bottomAnchor)
                ])
                if column < 3 {
                    let separator = UIView()
                    separator.backgroundColor = UIColor.white.withAlphaComponent(0.10)
                    separator.translatesAutoresizingMaskIntoConstraints = false
                    cell.addSubview(separator)
                    NSLayoutConstraint.activate([
                        separator.widthAnchor.constraint(equalToConstant: 1),
                        separator.topAnchor.constraint(equalTo: cell.topAnchor),
                        separator.bottomAnchor.constraint(equalTo: cell.bottomAnchor),
                        separator.trailingAnchor.constraint(equalTo: cell.trailingAnchor)
                    ])
                }
                row.addArrangedSubview(cell)
            }
            grid.addArrangedSubview(row)
            if rowIndex < 2 {
                let separator = UIView()
                separator.backgroundColor = UIColor.white.withAlphaComponent(0.10)
                separator.heightAnchor.constraint(equalToConstant: 1).isActive = true
                grid.addArrangedSubview(separator)
            }
        }
        return grid
    }

    private func makeResizePanel() -> UIView {
        let container = UIStackView()
        container.axis = .vertical
        container.alignment = .fill
        container.distribution = .fill
        container.spacing = 5
        container.isLayoutMarginsRelativeArrangement = true
        container.layoutMargins = UIEdgeInsets(top: 5, left: 7, bottom: 3, right: 7)

        let title = UILabel()
        title.text = "RESIZE KEYBOARD"
        title.font = .systemFont(ofSize: 10, weight: .semibold)
        title.textColor = YoryThemeReader.textColor.withAlphaComponent(0.78)
        title.textAlignment = .center
        container.addArrangedSubview(title)

        let value = UILabel()
        value.font = .systemFont(ofSize: 13, weight: .medium)
        value.textColor = YoryThemeReader.textColor
        value.textAlignment = .center
        value.text = "\(Int(activeKeyHeight)) pt"
        resizeHeightLabel = value
        container.addArrangedSubview(value)

        let slider = UISlider()
        slider.minimumValue = 36
        slider.maximumValue = 60
        slider.value = Float(activeKeyHeight)
        slider.minimumTrackTintColor = YoryThemeReader.accentColor
        slider.maximumTrackTintColor = UIColor.white.withAlphaComponent(0.20)
        slider.addTarget(self, action: #selector(resizeHeightChanged(_:)), for: .valueChanged)
        resizeHeightSlider = slider
        container.addArrangedSubview(slider)

        let actions = UIStackView()
        actions.axis = .horizontal
        actions.distribution = .fillEqually
        actions.spacing = 12

        let reset = UIButton(type: .system)
        reset.setTitle("Reset", for: .normal)
        reset.setTitleColor(YoryThemeReader.textColor, for: .normal)
        reset.backgroundColor = UIColor.white.withAlphaComponent(0.08)
        reset.layer.cornerRadius = 18
        reset.addAction(UIAction { [weak self] _ in self?.setKeyboardHeight(40) }, for: .touchUpInside)

        let done = UIButton(type: .system)
        done.setTitle("OK", for: .normal)
        done.setTitleColor(.black, for: .normal)
        done.backgroundColor = YoryThemeReader.textColor
        done.layer.cornerRadius = 18
        done.addAction(UIAction { [weak self] _ in
            guard let self else { return }
            self.toolsMode = false
            self.resizeMode = false
            self.keysStack.isHidden = false
            self.updateSuggestions()
            self.refreshToolbarItems()
            self.rebuildKeys()
        }, for: .touchUpInside)

        reset.heightAnchor.constraint(equalToConstant: 34).isActive = true
        done.heightAnchor.constraint(equalToConstant: 34).isActive = true
        actions.addArrangedSubview(reset)
        actions.addArrangedSubview(done)
        container.addArrangedSubview(actions)
        return container
    }

    @objc private func resizeHeightChanged(_ sender: UISlider) {
        setKeyboardHeight(CGFloat(sender.value))
    }

    private func setKeyboardHeight(_ value: CGFloat) {
        let safe = min(max(value, 36), 60)
        localKeyHeightOverride = safe
        UserDefaults.standard.set(Double(safe), forKey: "yory.keyboard.keyHeight.local")
        resizeHeightSlider?.value = Float(safe)
        resizeHeightLabel?.text = "\(Int(safe)) pt"
        rebuildKeys()
        updateKeyboardHeight()
    }

    private func makeSuggestionUtilityButton(
        _ symbol: String,
        accessibility: String,
        action: @escaping () -> Void
    ) -> UIButton {
        let button = UIButton(type: .system)
        button.setImage(UIImage(systemName: symbol), for: .normal)
        button.tintColor = YoryThemeReader.textColor
        button.backgroundColor = .clear
        button.layer.cornerRadius = 14
        button.layer.borderWidth = 1
        button.layer.borderColor = YoryThemeReader.textColor.withAlphaComponent(0.48).cgColor
        button.accessibilityLabel = accessibility
        button.widthAnchor.constraint(equalToConstant: 28).isActive = true
        button.heightAnchor.constraint(equalToConstant: 28).isActive = true
        button.addAction(UIAction { _ in action() }, for: .touchUpInside)
        return button
    }

    private func makeToolGridButton(
        _ title: String,
        symbol: String,
        action: @escaping () -> Void
    ) -> UIButton {
        let button = UIButton(type: .system)
        var configuration = UIButton.Configuration.plain()
        configuration.image = UIImage(systemName: symbol)
        configuration.title = title
        configuration.imagePlacement = .top
        configuration.imagePadding = 5
        configuration.baseForegroundColor = YoryThemeReader.textColor
        button.configuration = configuration
        button.backgroundColor = .clear
        button.titleLabel?.font = .systemFont(ofSize: 11, weight: .regular)
        button.heightAnchor.constraint(equalToConstant: 68).isActive = true
        button.accessibilityLabel = title
        button.addAction(UIAction { _ in action() }, for: .touchUpInside)
        return button
    }

    private func replaceCurrentWord(with suggestion: String) {
        let context = textDocumentProxy.documentContextBeforeInput ?? ""
        guard let range = context.range(of: "[\\p{L}\\p{M}]+$", options: .regularExpression) else {
            return
        }
        let prefix = String(context[range])
        for _ in 0..<prefix.count {
            textDocumentProxy.deleteBackward()
        }
        playKeyClick()
        textDocumentProxy.insertText(suggestion)
        updateSuggestions()
    }

    @objc private func switchKeyboard() { advanceToNextInputMode() }
}
