import UIKit

final class KeyboardViewController: UIInputViewController {
    private var isArabic = false
    private var isShifted = false
    private var emojiMode = false
    private var symbolMode = false
    private let root = UIStackView()
    private let emojiRow = UIStackView()
    private let keysStack = UIStackView()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = YoryThemeReader.backgroundColor.withAlphaComponent(YoryThemeReader.opacity)
        buildInterface()
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
        root.addArrangedSubview(toolbar)

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
    }

    private func rebuildKeys() {
        keysStack.arrangedSubviews.forEach { item in
            keysStack.removeArrangedSubview(item)
            item.removeFromSuperview()
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
        bottom.axis = .horizontal
        bottom.spacing = 4
        bottom.distribution = .fill
        bottom.addArrangedSubview(keyButton(symbolMode ? "ABC" : "!#1", action: #selector(toggleSymbols)))
        bottom.addArrangedSubview(keyButton("🌐", action: #selector(toggleLanguage)))
        bottom.addArrangedSubview(keyButton(","))
        let space = keyButton(isArabic ? "العربية" : "English", action: #selector(insertSpace))
        bottom.addArrangedSubview(space)
        bottom.addArrangedSubview(keyButton("."))
        bottom.addArrangedSubview(keyButton("↵", action: #selector(insertReturn)))
        space.widthAnchor.constraint(equalTo: bottom.widthAnchor, multiplier: 0.38).isActive = true
        keysStack.addArrangedSubview(bottom)
    }

    private func makeRow(_ labels: [String]) -> UIStackView {
        let row = UIStackView()
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
        textDocumentProxy.insertText(isShifted ? title.uppercased() : title)
        if isShifted {
            isShifted = false
            rebuildKeys()
        }
    }

    @objc private func deleteBackward() { textDocumentProxy.deleteBackward() }
    @objc private func insertSpace() { textDocumentProxy.insertText(" ") }
    @objc private func insertReturn() { textDocumentProxy.insertText("\n") }

    @objc private func toggleShift() {
        isShifted.toggle()
        rebuildKeys()
    }

    @objc private func toggleLanguage() {
        isArabic.toggle()
        symbolMode = false
        rebuildKeys()
    }

    @objc private func toggleSymbols() {
        symbolMode.toggle()
        rebuildKeys()
    }

    @objc private func toggleEmoji() {
        emojiMode.toggle()
        emojiRow.isHidden = !emojiMode
    }

    @objc private func switchKeyboard() { advanceToNextInputMode() }
}
