import UIKit

private final class YoryKeyboardRootView: UIView, UIInputViewAudioFeedback {
    var enableInputClicksWhenVisible: Bool { true }
}

final class KeyboardViewController: UIInputViewController {
    private var isArabic = false
    private var languageTag = "en-US"
    private var isShifted = false
    private var emojiMode = false
    private var symbolMode = false
    private var numericInput = false
    private var currentKeyboardType: UIKeyboardType = .default
    private let root = UIStackView()
    private var toolbarRow: UIStackView?
    private let emojiRow = UIStackView()
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
        languageTag = UserDefaults(suiteName: "group.app.yory.keys")?.string(forKey: "yory.active.language") ?? (YoryThemeReader.isArabic ? "ar" : "en-US")
        isArabic = languageTag == "ar"
        primaryLanguage = languageTag
        applyCurrentTheme()
        buildInterface()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        updateInputTraits()
        refreshAutomaticShift()
        applyCurrentTheme()
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        updateInputTraits()
        refreshAutomaticShift()
        updateSuggestions()
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
        switch textDocumentProxy.autocapitalizationType {
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

    private var returnKeyTitle: String {
        switch textDocumentProxy.returnKeyType ?? .default {
        case .go: return "Go"
        case .search: return "Search"
        case .send: return "Send"
        case .next: return "Next"
        case .done: return "Done"
        default: return "↵"
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
        let defaultOrder = ["emoji", "language", "symbols", "switch", "tools"]
        let storedOrder = toolbarDefaults?.stringArray(forKey: "yory.toolbar.order") ?? defaultOrder
        let order = storedOrder.filter { defaultOrder.contains($0) } + defaultOrder.filter { !storedOrder.contains($0) }
        let visible = Set(toolbarDefaults?.stringArray(forKey: "yory.toolbar.visible") ?? defaultOrder)
        for tool in order where visible.contains(tool) {
            switch tool {
            case "emoji":
                if YoryThemeReader.emojiKeyEnabled {
                    toolbar.addArrangedSubview(toolButton("☺", action: #selector(toggleEmoji)))
                }
            case "language":
                toolbar.addArrangedSubview(toolButton("⇄", action: #selector(toggleLanguage)))
            case "symbols":
                toolbar.addArrangedSubview(toolButton("Aa", action: #selector(toggleSymbols)))
            case "switch":
                toolbar.addArrangedSubview(toolButton("⌨", action: #selector(switchKeyboard)))
            case "tools":
                toolbar.addArrangedSubview(toolButton("•••", action: #selector(toggleTools)))
            default:
                break
            }
        }
        if toolbar.arrangedSubviews.isEmpty {
            toolbar.addArrangedSubview(toolButton("⇄", action: #selector(toggleLanguage)))
            toolbar.addArrangedSubview(toolButton("⌨", action: #selector(switchKeyboard)))
        }
        toolbar.semanticContentAttribute = isArabic ? .forceRightToLeft : .forceLeftToRight
        toolbarRow = toolbar
        root.addArrangedSubview(toolbar)

        suggestionRow.axis = .horizontal
        suggestionRow.distribution = .fillEqually
        suggestionRow.spacing = 8
        root.addArrangedSubview(suggestionRow)
        let suggestionHeight = suggestionRow.heightAnchor.constraint(equalToConstant: 28)
        suggestionRowHeightConstraint = suggestionHeight
        suggestionHeight.isActive = true

        emojiRow.axis = .horizontal
        emojiRow.distribution = .fillEqually
        emojiRow.spacing = 4
        for emoji in ["😀", "😂", "🥰", "❤️", "✨", "🙏", "👍", "🔥"] {
            emojiRow.addArrangedSubview(keyButton(emoji))
        }
        emojiRow.isHidden = true
        root.addArrangedSubview(emojiRow)

        keysStack.axis = .vertical
        keysStack.spacing = 6
        root.addArrangedSubview(keysStack)
        rebuildKeys()
        updateSuggestions()
    }

    private func rebuildKeys() {
        updateKeyboardHeight()
        let direction: UISemanticContentAttribute = isArabic && !numericInput ? .forceRightToLeft : .forceLeftToRight
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

        let rows: [String]
        if symbolMode {
            rows = ["@#$%&*()-", "_+=[]{}?!", "<>/\\|;:"]
        } else if isArabic {
            rows = ["ضصثقفغعهخح", "شسيبلاتنم", "ئءؤرىةوز"]
        } else if languageTag == "de" {
            rows = ["qwertzuiop", "asdfghjkl", "yxcvbnm"]
        } else {
            rows = ["qwertyuiop", "asdfghjkl", "zxcvbnm"]
        }

        if YoryThemeReader.numberRowEnabled && YoryThemeReader.numberPosition == "top" {
            keysStack.addArrangedSubview(makeRow(Array("1234567890").map(String.init)))
        }
        for (index, rowText) in rows.enumerated() {
            var labels = Array(rowText).map(String.init)
            if index == 2 {
                labels.insert("⇧", at: 0)
                labels.append("⌫")
            }
            keysStack.addArrangedSubview(makeRow(labels))
        }

        let bottom = UIStackView()
        bottom.semanticContentAttribute = isArabic ? .forceRightToLeft : .forceLeftToRight
        bottom.axis = .horizontal
        bottom.spacing = 4
        bottom.distribution = .fill
        bottom.addArrangedSubview(keyButton(symbolMode ? "ABC" : "!#1", action: #selector(toggleSymbols)))
        bottom.addArrangedSubview(keyButton("🌐", action: #selector(toggleLanguage)))
        let fieldSymbol = currentKeyboardType == .emailAddress ? "@" : (currentKeyboardType == .URL ? "/" : ",")
        bottom.addArrangedSubview(keyButton(fieldSymbol))
        let space = keyButton(isArabic ? "العربية" : (languageTag == "de" ? "Deutsch" : "English"), action: #selector(insertSpace))
        bottom.addArrangedSubview(space)
        bottom.addArrangedSubview(keyButton("."))
        bottom.addArrangedSubview(keyButton(returnKeyTitle, action: #selector(insertReturn)))
        space.widthAnchor.constraint(equalTo: bottom.widthAnchor, multiplier: 0.38).isActive = true
        keysStack.addArrangedSubview(bottom)
        if YoryThemeReader.numberRowEnabled && YoryThemeReader.numberPosition == "bottom" {
            keysStack.addArrangedSubview(makeRow(Array("1234567890").map(String.init)))
        }
        if YoryThemeReader.arrowKeysEnabled {
            keysStack.addArrangedSubview(makeArrowRow())
        }
        updateKeyboardHeight()
    }

    private func makeRow(_ labels: [String]) -> UIStackView {
        let row = UIStackView()
        row.semanticContentAttribute = isArabic && !numericInput ? .forceRightToLeft : .forceLeftToRight
        row.axis = .horizontal
        row.spacing = YoryThemeReader.keySpacing
        row.distribution = .fillEqually
        labels.forEach { label in
            if label == "⇧" {
                row.addArrangedSubview(keyButton(label, action: #selector(toggleShift)))
            } else if label == "⌫" {
                row.addArrangedSubview(keyButton(label, action: #selector(deleteBackward)))
            } else {
                let display = isShifted ? label.uppercased() : label
                row.addArrangedSubview(keyButton(display))
            }
        }
        return row
    }

    private func updateKeyboardHeight() {
        let rowCount: CGFloat
        if numericInput {
            rowCount = 5
        } else {
            rowCount = CGFloat(4 + (YoryThemeReader.numberRowEnabled ? 1 : 0) + (YoryThemeReader.arrowKeysEnabled ? 1 : 0))
        }
        let innerGaps = max(0, rowCount - 1) * 6
        let required = 84 + rowCount * YoryThemeReader.keyHeight + innerGaps
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

    private func keyButton(_ title: String, action: Selector? = #selector(insertKey(_:))) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(title, for: .normal)
        let baseFontSize = title.count > 2 ? max(10, YoryThemeReader.fontSize - 6) : YoryThemeReader.fontSize
        let displayedFontSize = YoryThemeReader.largeKeyTextEnabled ? baseFontSize * 1.08 : baseFontSize
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

    private func toolButton(_ title: String, action: Selector) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(title, for: .normal)
        button.setTitleColor(YoryThemeReader.accentColor, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 18, weight: .semibold)
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
        languageTag = languageTag == "en-US" ? "de" : (languageTag == "de" ? "ar" : "en-US")
        isArabic = languageTag == "ar"
        UserDefaults(suiteName: "group.app.yory.keys")?.set(languageTag, forKey: "yory.active.language")
        primaryLanguage = languageTag
        symbolMode = false
        rebuildKeys()
        updateSuggestions()
    }

    @objc private func toggleSymbols() {
        playKeyClick()
        symbolMode.toggle()
        rebuildKeys()
    }

    private func learnCurrentWord() {
        guard YoryThemeReader.personalLearningEnabled,
              !numericInput,
              currentKeyboardType != .emailAddress,
              currentKeyboardType != .URL else { return }
        let context = textDocumentProxy.documentContextBeforeInput ?? ""
        guard let range = context.range(of: "[\\p{L}\\p{M}]{2,32}$", options: .regularExpression) else {
            return
        }
        let word = String(context[range])
        YoryLocalWordSuggestions.learnAcceptedWord(word, languageTag: languageTag == "de" ? "de" : (isArabic ? "ar" : "en"))
    }

    @objc private func toggleTools() {
        playKeyClick()
        toolsMode.toggle()
        updateSuggestions()
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

    @objc private func toggleEmoji() {
        playKeyClick()
        emojiMode.toggle()
        emojiRow.isHidden = !emojiMode
        updateSuggestions()
    }

    private func updateSuggestions() {
        let compactMode = UserDefaults(suiteName: "group.app.yory.keys")?.bool(forKey: "yory.input.compactMode") ?? false
        suggestionRowHeightConstraint?.constant = compactMode ? 20 : 28
        let suggestionLimit = compactMode ? 2 : 3
        view.setNeedsLayout()
        suggestionRow.arrangedSubviews.forEach { item in
            suggestionRow.removeArrangedSubview(item)
            item.removeFromSuperview()
        }
        suggestionRow.isHidden = emojiMode || numericInput || !YoryThemeReader.wordSuggestionsEnabled
        guard !emojiMode && !numericInput && YoryThemeReader.wordSuggestionsEnabled else { return }
        if toolsMode {
            suggestionRow.addArrangedSubview(toolActionButton("Clear learned words", action: #selector(clearLearnedWords)))
            suggestionRow.addArrangedSubview(toolActionButton("Done", action: #selector(toggleTools)))
            return
        }

        let context = textDocumentProxy.documentContextBeforeInput ?? ""
        guard let range = context.range(of: "[\\p{L}\\p{M}]+$", options: .regularExpression) else {
            return
        }
        let prefix = String(context[range])
        guard languageTag != "de" else { return }
        let language = isArabic ? "ar" : "en"
        let suggestions = YoryLocalWordSuggestions.suggestions(prefix: prefix, languageTag: language)

        for suggestion in suggestions.prefix(suggestionLimit) {
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
