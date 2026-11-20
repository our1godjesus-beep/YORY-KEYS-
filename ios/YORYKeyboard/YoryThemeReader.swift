import UIKit

enum YoryThemeReader {
    private static let groupID = "group.app.yory.keys"
    private static var settings: UserDefaults? { UserDefaults(suiteName: groupID) }

    static var backgroundColor: UIColor {
        color(for: "backgroundHex", fallback: "#D91A1D")
    }
    static var keyColor: UIColor {
        color(for: "keyHex", fallback: "#F1F3FA")
    }
    static var textColor: UIColor {
        color(for: "textHex", fallback: "#171923")
    }
    static var accentColor: UIColor {
        color(for: "accentHex", fallback: "#78B8F8")
    }
    static var cornerRadius: CGFloat {
        CGFloat(settings?.double(forKey: "cornerRadius") ?? 13)
    }
    static var opacity: CGFloat {
        CGFloat(settings?.double(forKey: "opacity") ?? 0.92)
    }

    private static func color(for key: String, fallback: String) -> UIColor {
        let value = settings?.string(forKey: key) ?? fallback
        let hex = value.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        guard let number = UInt64(hex, radix: 16) else { return .darkGray }
        if hex.count == 8 {
            return UIColor(
                red: CGFloat((number >> 16) & 0xFF) / 255,
                green: CGFloat((number >> 8) & 0xFF) / 255,
                blue: CGFloat(number & 0xFF) / 255,
                alpha: CGFloat((number >> 24) & 0xFF) / 255
            )
        }
        return UIColor(
            red: CGFloat((number >> 16) & 0xFF) / 255,
            green: CGFloat((number >> 8) & 0xFF) / 255,
            blue: CGFloat(number & 0xFF) / 255,
            alpha: 1
        )
    }
}
