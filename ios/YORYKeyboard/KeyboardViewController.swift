private struct YoryKeyboardLayout {
    let tag: String
    let name: String
    let spaceLabel: String
    let rows: [String]
    let isRightToLeft: Bool
    let hasBundledDictionary: Bool

    static let supported: [YoryKeyboardLayout] = [
        .init(tag: "en-US", name: "English (US)", spaceLabel: "English", rows: ["qwertyuiop", "asdfghjkl", "zxcvbnm"], isRightToLeft: false, hasBundledDictionary: true),
        .init(tag: "ar", name: "العربية (مصر)", spaceLabel: "العربية", rows: ["ضصثقفغعهخحجد", "شسيبلاتنمكذ", "طظئءؤرىةوز"], isRightToLeft: true, hasBundledDictionary: true),
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
    private let keysStack = UIStackView()
    private var keyboardHeightConstraint: NSLayoutConstraint?
    private var suggestionRowHeightConstraint: NSLayoutConstraint?

    override func loadView() {
        view = YoryKeyboardRootView()
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.black.withAlphaComponent(0.94)
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
        view.backgroundColor = UIColor.black.withAlphaComponent(0.94)
        if isViewLoaded {
            updateKeyboardHeight()
            rebuildKeys()
        }
    }

    private func buildInterface() {
        root.axis = .vertical
        root.spacing = YoryThemeReader.keySpacing
        root.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(root)
        let heightConstraint = view.heightAnchor.constraint(equalToConstant: 270)
        keyboardHeightConstraint = heightConstraint
        NSLayoutConstraint.activate([
            root.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 5),
            root.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -5),
            root.topAnchor.constraint(equalTo: view.topAnchor, constant: 7),
            root.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -7),
            heightConstraint
        ])
        updateKeyboardHeight()

        let toolbar = UIStackView()
        toolbar.axis = .horizontal
        toolbar.distribution = .fillEqually
        toolbar.spacing = 5
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
        toolbar.semanticContentAttribute = isArabic ? .forceRightToLeft : .forceLeftToRight
        toolbarRow = toolbar
        root.addArrangedSubview(toolbar)

        suggestionRow.axis = .horizontal
        suggestionRow.distribution = .fillEqually
        suggestionRow.spacing = 6
        root.addArrangedSubview(suggestionRow)
        let suggestionHeight = suggestionRow.heightAnchor.constraint(equalToConstant: 28)
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
        keysStack.spacing = 6
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
                labels.insert("⇧", at: 0)
                labels.append("⌫")
            }
            let hints = (isArabicRow && index == 0)
                ? Dictionary(uniqueKeysWithValues: zip(baseLabels, ["1","2","3","4","5","6","7","8","9","0","-","="]))
                : [:]
            keysStack.addArrangedSubview(makeRow(labels, compact: isArabicRow, secondaryHints: hints))
        }

        let bottom = UIStackView()
        bottom.semanticContentAttribute = isArabic ? .forceRightToLeft : .forceLeftToRight
        bottom.axis = .horizontal
        bottom.spacing = 4
        bottom.distribution = .fill
        bottom.addArrangedSubview(keyButton(symbolMode ? "ABC" : "!#1", action: symbolMode ? #selector(exitSymbols) : #selector(toggleSymbols)))
        if symbolMode {
            bottom.addArrangedSubview(keyButton(symbolPage == 0 ? "2/2" : "1/2", action: #selector(toggleSymbolPage)))
        }
        bottom.addArrangedSubview(keyButton("🌐", action: #selector(toggleLanguage)))
        let fieldSymbol = currentKeyboardType == .emailAddress ? "@" : (currentKeyboardType == .URL ? "/" : ",")
        bottom.addArrangedSubview(keyButton(fieldSymbol))
        let space = keyButton("‹  ${activeLayout.spaceLabel}  ›", action: #selector(insertSpace))
        space.accessibilityLabel = "Space, " + activeLayout.spaceLabel
        space.accessibilityHint = "Swipe left for the next added language or right for the previous added language"
        let swipeLeft = UISwipeGestureRecognizer(target: self, action: #selector(handleSpacebarSwipe(_:)))
        swipeLeft.direction = .left
        let swipeRight = UISwipeGestureRecognizer(target: self, action: #selector(handleSpacebarSwipe(_:)))
        swipeRight.direction = .right
        space.addGestureRecognizer(swipeLeft)
        space.addGestureRecognizer(swipeRight)
        bottom.addArrangedSubview(space)
        bottom.addArrangedSubview(keyButton("."))
        bottom.addArrangedSubview(keyButton(returnKeyTitle, action: #selector(insertReturn)))
        space.widthAnchor.constraint(equalTo: bottom.widthAnchor, multiplier: symbolMode ? 0.33 : 0.38).isActive = true
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
        secondaryHints: [String: String] = [:]
    ) -> UIStackView {
        let row = UIStackView()
        row.semanticContentAttribute = isArabic && !numericInput ? .forceRightToLeft : .forceLeftToRight
        row.axis = .horizontal
        row.spacing = compact ? max(1, YoryThemeReader.keySpacing * 0.35) : YoryThemeReader.keySpacing
        row.distribution = .fillEqually
        labels.forEach { label in
            if label == "⇧" {
                row.addArrangedSubview(keyButton(label, action: #selector(toggleShift), compact: compact))
            } else if label == "⌫" {
                row.addArrangedSubview(keyButton(label, action: #selector(deleteBackward), compact: compact))
            } else {
                let display = isShifted ? label.uppercased() : label
                row.addArrangedSubview(keyButton(display, secondaryHint: secondaryHints[label], compact: compact))
            }
        }
        return row
    }

    private func updateKeyboardHeight() {
        if toolsMode {
            keyboardHeightConstraint?.constant = 270
            return
        }
        let rowCount: CGFloat
        if numericInput {
            rowCount = 5
        } else {
            rowCount = CGFloat(4 + (YoryThemeReader.numberRowEnabled ? 1 : 0) + (YoryThemeReader.arrowKeysEnabled ? 1 : 0))
        }
        let innerGaps = max(0, rowCount - 1) * 5
        let emojiPanelHeight: CGFloat = emojiMode ? (emojiCategory == "People" ? 126 : 98) : 0
        let required = 78 + rowCount * YoryThemeReader.keyHeight + innerGaps + emojiPanelHeight
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
        button.setTitle(title, for: .normal)
        let baseFontSize = compact
            ? min(15.5, YoryThemeReader.fontSize)
            : (title.count > 2 ? max(10, YoryThemeReader.fontSize - 6) : YoryThemeReader.fontSize)
        let displayedFontSize = YoryThemeReader.largeKeyTextEnabled && !compact ? baseFontSize * 1.08 : baseFontSize
        button.titleLabel?.font = keyboardFont(size: displayedFontSize, weight: .medium)
        button.setTitleColor(YoryThemeReader.textColor, for: .normal)
        button.backgroundColor = UIColor.white.withAlphaComponent(0.055)
        button.layer.cornerRadius = {
            switch YoryThemeReader.keyShape {
            case "pill", "circle": return 18.0
            case "squircle": return YoryThemeReader.cornerRadius * 1.3
            case "rect": return 3.0
            default: return YoryThemeReader.cornerRadius
            }
        }()
        button.clipsToBounds = true
        let glass = UIVisualEffectView(effect: UIBlurEffect(style: .systemThinMaterialDark))
        glass.isUserInteractionEnabled = false
        glass.frame = button.bounds
        glass.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        glass.layer.cornerRadius = button.layer.cornerRadius
        glass.clipsToBounds = true
        button.insertSubview(glass, at: 0)
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
        button.layer.borderColor = UIColor.white.withAlphaComponent(0.28).cgColor
        button.layer.shadowColor = UIColor.white.cgColor
        button.layer.shadowOpacity = 0.10
        button.layer.shadowRadius = 5
        button.layer.shadowOffset = CGSize(width: 0, height: 2)
        button.accessibilityLabel = title
        button.addTarget(self, action: action ?? #selector(insertKey(_:)), for: .touchUpInside)
        button.addTarget(self, action: #selector(keyTouchDown(_:)), for: .touchDown)
        button.addTarget(self, action: #selector(keyTouchUp(_:)), for: [.touchUpInside, .touchUpOutside, .touchCancel])
        button.heightAnchor.constraint(equalToConstant: YoryThemeReader.keyHeight).isActive = true
        if let menu = longPressMenu(for: title) {
            button.menu = menu
            button.showsMenuAsPrimaryAction = false
        }
        return button
    }

    @objc private func keyTouchDown(_ sender: UIButton) {
        sender.backgroundColor = YoryThemeReader.keyPressedColor.withAlphaComponent(0.58)
        if YoryThemeReader.keyPopupsEnabled {
            UIView.animate(withDuration: 0.08) {
                sender.transform = CGAffineTransform(scaleX: 0.96, y: 0.94)
            }
        }
    }

    @objc private func keyTouchUp(_ sender: UIButton) {
        sender.backgroundColor = UIColor.white.withAlphaComponent(0.055)
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
            "◎": "eye"
        ]
        if let iconName = iconNames[title], let image = UIImage(systemName: iconName) {
            button.setImage(image, for: .normal)
            button.tintColor = YoryThemeReader.accentColor
        } else {
            button.setTitle(title, for: .normal)
            button.setTitleColor(YoryThemeReader.accentColor, for: .normal)
            button.titleLabel?.font = .systemFont(ofSize: 18, weight: .semibold)
        }
        button.accessibilityLabel = accessibilityLabel ?? title
        button.backgroundColor = UIColor.white.withAlphaComponent(0.045)
        button.layer.cornerRadius = 10
        button.layer.borderWidth = 0.5
        button.layer.borderColor = UIColor.white.withAlphaComponent(0.18).cgColor
        button.addTarget(self, action: action, for: .touchUpInside)
        button.heightAnchor.constraint(equalToConstant: 30).isActive = true
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
        if toolsMode { emojiMode = false }
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
        if incognitoMode { toolsMode = false }
        keysStack.isHidden = toolsMode
        refreshToolbarItems()
        rebuildKeys()
        updateSuggestions()
    }

    @objc private func toggleEmoji() {
        playKeyClick()
        toolsMode = false
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
        if toolsMode {
            keysStack.isHidden = true
            emojiRow.isHidden = true
            suggestionRow.isHidden = false
            suggestionRow.axis = .vertical
            suggestionRow.distribution = .fillEqually
            suggestionRow.spacing = 7
            suggestionRowHeightConstraint?.constant = 168
            var items: [(String, String, () -> Void)] = [
                ("Emoji", "face.smiling", { [weak self] in self?.toggleEmoji() }),
                ("Language", "globe", { [weak self] in self?.cycleLanguage(direction: 1) }),
                ("Symbols", "character", { [weak self] in self?.toggleSymbols() }),
                ("Next keyboard", "keyboard", { [weak self] in self?.switchKeyboard() }),
                ("Incognito", "eye.slash", { [weak self] in self?.toggleIncognito() })
            ]
            // App Group writes are available only when the user explicitly enables Full Access.
            if hasFullAccess {
                items.append(("Clear words", "text.badge.minus", { [weak self] in self?.clearLearnedWords() }))
            }
            let grid = UIStackView()
            grid.axis = .vertical
            grid.spacing = 7
            for start in stride(from: 0, to: items.count, by: 4) {
                let row = UIStackView()
                row.axis = .horizontal
                row.distribution = .fillEqually
                row.spacing = 7
                for item in items[start..<min(start + 4, items.count)] {
                    row.addArrangedSubview(makeToolGridButton(item.0, symbol: item.1, action: item.2))
                }
                while row.arrangedSubviews.count < 4 {
                    row.addArrangedSubview(UIView())
                }
                grid.addArrangedSubview(row)
            }
            suggestionRow.addArrangedSubview(grid)
            view.setNeedsLayout()
            updateKeyboardHeight()
            return
        }

        keysStack.isHidden = false
        suggestionRow.axis = .horizontal
        suggestionRow.distribution = .fillEqually
        suggestionRow.spacing = 6
        let compactMode = UserDefaults(suiteName: "group.app.yory.keys")?.bool(forKey: "yory.input.compactMode") ?? false
        suggestionRowHeightConstraint?.constant = suggestionsCollapsed ? 18 : (compactMode ? 22 : 29)
        view.setNeedsLayout()
        suggestionRow.isHidden = incognitoMode || emojiMode || numericInput || !YoryThemeReader.wordSuggestionsEnabled
        guard !incognitoMode && !emojiMode && !numericInput && !toolsMode && YoryThemeReader.wordSuggestionsEnabled else {
            updateKeyboardHeight()
            return
        }
        suggestionRow.isHidden = false
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
        if let range = context.range(of: "[\\p{L}\\p{M}]+$", options: .regularExpression) {
            let prefix = String(context[range])
            let suggestions = YoryLocalWordSuggestions.suggestions(prefix: prefix, languageTag: languageTag)
            for suggestion in suggestions.prefix(compactMode ? 2 : 3) {
                let button = UIButton(type: .system)
                button.setTitle(suggestion, for: .normal)
                button.setTitleColor(YoryThemeReader.accentColor, for: .normal)
                button.titleLabel?.font = .systemFont(ofSize: 13, weight: .medium)
                button.accessibilityLabel = "Use suggestion " + suggestion
                button.addAction(UIAction { [weak self] _ in
                    self?.replaceCurrentWord(with: suggestion)
                }, for: .touchUpInside)
                suggestionRow.addArrangedSubview(button)
            }
        }
        updateKeyboardHeight()
    }

    private func makeSuggestionUtilityButton(
        _ symbol: String,
        accessibility: String,
        action: @escaping () -> Void
    ) -> UIButton {
        let button = UIButton(type: .system)
        button.setImage(UIImage(systemName: symbol), for: .normal)
        button.tintColor = YoryThemeReader.accentColor
        button.accessibilityLabel = accessibility
        button.addAction(UIAction { _ in action() }, for: .touchUpInside)
        return button
    }

    private func makeToolGridButton(
        _ title: String,
        symbol: String,
        action: @escaping () -> Void
    ) -> UIButton {
        var configuration = UIButton.Configuration.plain()
        configuration.image = UIImage(systemName: symbol)
        configuration.title = title
        configuration.imagePlacement = .top
        configuration.imagePadding = 5
        configuration.baseForegroundColor = YoryThemeReader.accentColor
        let button = UIButton(configuration: configuration)
        button.backgroundColor = UIColor.white.withAlphaComponent(0.055)
        button.layer.cornerRadius = 13
        button.layer.borderWidth = 0.6
        button.layer.borderColor = UIColor.white.withAlphaComponent(0.18).cgColor
        button.titleLabel?.font = .systemFont(ofSize: 11, weight: .medium)
        button.heightAnchor.constraint(equalToConstant: 70).isActive = true
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
