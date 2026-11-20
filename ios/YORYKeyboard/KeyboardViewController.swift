import UIKit

final class KeyboardViewController: UIInputViewController {
    private let rows = ["qwertyuiop", "asdfghjkl", "⇧zxcvbnm⌫"]

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red: 0.10, green: 0.11, blue: 0.16, alpha: 1)
        buildKeyboard()
    }

    private func buildKeyboard() {
        let root = UIStackView()
        root.axis = .vertical
        root.spacing = 7
        root.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(root)

        NSLayoutConstraint.activate([
            root.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 5),
            root.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -5),
            root.topAnchor.constraint(equalTo: view.topAnchor, constant: 8),
            root.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -8)
        ])

        for row in rows {
            let line = UIStackView()
            line.axis = .horizontal
            line.spacing = 4
            line.distribution = .fillEqually
            for character in row {
                let button = makeKey(String(character))
                line.addArrangedSubview(button)
            }
            root.addArrangedSubview(line)
        }

        let bottom = UIStackView()
        bottom.axis = .horizontal
        bottom.spacing = 4
        bottom.distribution = .fill
        bottom.addArrangedSubview(makeKey("123"))
        bottom.addArrangedSubview(makeKey("☺"))
        let space = makeKey("space")
        bottom.addArrangedSubview(space)
        bottom.addArrangedSubview(makeKey("↵"))
        root.addArrangedSubview(bottom)
        space.widthAnchor.constraint(greaterThanOrEqualToConstant: 120).isActive = true
    }

    private func makeKey(_ title: String) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(title, for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 17, weight: .medium)
        button.backgroundColor = UIColor(red: 0.22, green: 0.24, blue: 0.31, alpha: 1)
        button.layer.cornerRadius = 8
        button.addTarget(self, action: #selector(keyPressed(_:)), for: .touchUpInside)
        button.accessibilityLabel = title
        return button
    }

    @objc private func keyPressed(_ sender: UIButton) {
        guard let title = sender.title(for: .normal) else { return }
        switch title {
        case "⌫":
            textDocumentProxy.deleteBackward()
        case "space":
            textDocumentProxy.insertText(" ")
        case "↵":
            textDocumentProxy.insertText("\n")
        case "⇧", "123", "☺":
            break // mode and picker controllers are added in later milestones
        default:
            textDocumentProxy.insertText(title)
        }
    }
}
