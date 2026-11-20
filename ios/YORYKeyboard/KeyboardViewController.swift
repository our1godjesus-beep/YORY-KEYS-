import UIKit

private final class YoryKeyboardRootView: UIView, UIInputViewAudioFeedback {
    var enableInputClicksWhenVisible: Bool { true }
}

final class KeyboardViewController: UIInputViewController {
    private var isArabic = false
    private var isShifted = false
    private var emojiMode = false
    private var symbolMode = false
    private var numericInput = false
    private var currentKeyboardType: UIKeyboardType = .default
    private let root = UIStackView()
    private var toolbarRow: UIStackView?
    private let emojiRow = UIStackView()
    private let suggestionRow = UIStackView()
    private let keysStack = UIStackView()

    override func loadView() {
        view = YoryKeyboardRootView()
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = YoryThemeReader.backgroundColor.withAlphaComponent(YoryThemeReader.opacity)
        isArabic = UserDefaults.standard.object(forKey: "yory.keyboard.arabic") as? Bool ?? YoryThemeReader.isArabic
        primaryLanguage = isArabic ? "ar" : "en-US"
        applyCurrentTheme()
        buildInterface()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        updateInputTraits()
        applyCurrentTheme()
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        updateInputTraits()
        updateSuggestions()
    }

    private func updateInputTraits() {
        let nextType = textDocumentProxy.keyboardType
        guard nextType != currentKeyboardType else { return }
        currentKeyboardType = nextType
        numericInput = nextType == .numberPad || nextType == .decimalPad || nextType == .phonePad
        rebuildKeys()
        updateSuggestions()
    }

    private var returnKeyTitle: String {
        switch textDocumentProxy.returnKeyType {
        case .go: return "Go"
        case .search: return "Search"
        case .send: return "Send"
        case .next: return "Next"
        case .done: return "Done"
        default: return "↵"
        }
    }

    private func applyCurrentTheme() {
        view.backgroundColor = YoryThemeReader.backgroundColor.withAlphaComponent(YoryThemeReader.opacity)
        if isViewLoaded {
            rebuildKeys()
        }
    }

    private func buildInterface() {
        root.axis = .vertical
        root.spacing = 7
        root.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(root)
        NSLayoutConstraint.activate([
            root.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 5),
            root.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -5),
            root.topAnchor.constraint(equalTo: view.topAnchor, constant: 7),
            root.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -7),
            view.heightAnchor.constraint(equalToConstant: 270)
        ])

        let toolbar = UIStackView()
        toolbar.axis = .horizontal
        toolbar.distribution = .fillEqually
        toolbar.spacing = 5
        toolbar.addArrangedSubview(toolButton("☺", action: #selector(toggleEmoji)))
        toolbar.addArrangedSubview(toolButton("⇄", action: #selector(toggleLanguage)))
        toolbar.addArrangedSubview(toolButton("Aa", action: #selector(toggleSymbols)))
        toolbar.addArrangedSubview(toolButton("⌨", action: #selector(switchKeyboard)))
        toolbar.addArrangedSubview(toolButton("•••", action: #selector(toggleEmoji)))
        toolbar.semanticContentAttribute = isArabic ? .forceRightToLeft : .forceLeftToRight
        toolbarRow = toolbar
        root.addArrangedSubview(toolbar)

        suggestionRow.axis = .horizontal
        suggestionRow.distribution = .fillEqually
        suggestionRow.spacing = 8
        root.addArrangedSubview(suggestionRow)
        suggestionRow.heightAnchor.constraint(equalToConstant: 28).isActive = true

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
        } else {
            rows = ["qwertyuiop", "asdfghjkl", "zxcvbnm"]
        }

        keysStack.addArrangedSubview(makeRow(Array("1234567890").map(String.init)))
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
        let space = keyButton(isArabic ? "العربية" : "English", action: #selector(insertSpace))
        bottom.addArrangedSubview(space)
        bottom.addArrangedSubview(keyButton("."))
        bottom.addArrangedSubview(keyButton(returnKeyTitle, action: #selector(insertReturn)))
        space.widthAnchor.constraint(equalTo: bottom.widthAnchor, multiplier: 0.38).isActive = true
        keysStack.addArrangedSubview(bottom)
    }

    private func makeRow(_ labels: [String]) -> UIStackView {
        let row = UIStackView()
        row.semanticContentAttribute = isArabic && !numericInput ? .forceRightToLeft : .forceLeftToRight
        row.axis = .horizontal
        row.spacing = 4
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

    private func keyButton(_ title: String, action: Selector? = #selector(insertKey(_:))) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(title, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: title.count > 2 ? 12 : 18, weight: .medium)
        button.setTitleColor(YoryThemeReader.textColor, for: .normal)
        button.backgroundColor = YoryThemeReader.keyColor
        button.layer.cornerRadius = YoryThemeReader.cornerRadius
        button.layer.borderWidth = 0.5
        button.layer.borderColor = UIColor.white.withAlphaComponent(0.24).cgColor
        button.accessibilityLabel = title
        button.addTarget(self, action: action ?? #selector(insertKey(_:)), for: .touchUpInside)
        button.heightAnchor.constraint(equalToConstant: 36).isActive = true
        return button
    }

    private func toolButton(_ title: String, action: Selector) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(title, for: .normal)
        button.setTitleColor(YoryThemeReader.accentColor, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 18, weight: .semibold)
        button.addTarget(self, action: action, for: .touchUpInside)
        button.heightAnchor.constraint(equalToConstant: 30).isActive = true
        return button
    }

    @objc private func insertKey(_ sender: UIButton) {
        guard let title = sender.title(for: .normal) else { return }
        playKeyClick()
        textDocumentProxy.insertText(isShifted ? title.uppercased() : title)
        if isShifted {
            isShifted = false
            rebuildKeys()
        }
        updateSuggestions()
    }

    private func playKeyClick() {
        if YoryThemeReader.soundEnabled {
            UIDevice.current.playInputClick()
        }
        if YoryThemeReader.hapticsEnabled {
            let feedback = UIImpactFeedbackGenerator(style: .light)
            feedback.prepare()
            feedback.impactOccurred()
        }
    }

    @objc private func deleteBackward() {
        playKeyClick()
        textDocumentProxy.deleteBackward()
        updateSuggestions()
    }

    @objc private func insertSpace() {
        playKeyClick()
        textDocumentProxy.insertText(" ")
        updateSuggestions()
    }

    @objc private func insertReturn() {
        playKeyClick()
        textDocumentProxy.insertText("\n")
        updateSuggestions()
    }

    @objc private func toggleShift() {
        playKeyClick()
        isShifted.toggle()
        rebuildKeys()
    }

    @objc private func toggleLanguage() {
        playKeyClick()
        isArabic.toggle()
        UserDefaults.standard.set(isArabic, forKey: "yory.keyboard.arabic")
        primaryLanguage = isArabic ? "ar" : "en-US"
        symbolMode = false
        rebuildKeys()
        updateSuggestions()
    }

    @objc private func toggleSymbols() {
        playKeyClick()
        symbolMode.toggle()
        rebuildKeys()
    }

    @objc private func toggleEmoji() {
        playKeyClick()
        emojiMode.toggle()
        emojiRow.isHidden = !emojiMode
        updateSuggestions()
    }

    private func updateSuggestions() {
        suggestionRow.arrangedSubviews.forEach { item in
            suggestionRow.removeArrangedSubview(item)
            item.removeFromSuperview()
        }
        suggestionRow.isHidden = emojiMode || numericInput
        guard !emojiMode && !numericInput else { return }

        let context = textDocumentProxy.documentContextBeforeInput ?? ""
        guard let range = context.range(of: "[\\p{L}\\p{M}]+$", options: .regularExpression) else {
            return
        }
        let prefix = String(context[range])
        let language = isArabic ? "ar" : "en"
        let suggestions = YoryLocalWordSuggestions.suggestions(prefix: prefix, languageTag: language)

        for suggestion in suggestions {
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
