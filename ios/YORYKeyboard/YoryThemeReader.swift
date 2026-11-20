import UIKit

enum YoryThemeReader {
    private static let groupID = "group.app.yory.keys"
    private static var settings: UserDefaults? { UserDefaults(suiteName: groupID) }

    static var backgroundColor: UIColor {
        color(for: "backgroundHex", fallback: "#1A1D2B")
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
    static var keyPressedColor: UIColor {
        color(for: "keyPressedHex", fallback: "#78B8F8")
    }
    static var keySpacing: CGFloat {
        CGFloat(settings?.double(forKey: "keySpacing") ?? 4).clamped(to: 0...12)
    }
    static var fontSize: CGFloat {
        CGFloat(settings?.double(forKey: "fontSize") ?? 18).clamped(to: 12...26)
    }
    static var keyShape: String {
        let value = settings?.string(forKey: "keyShape") ?? "rounded"
        return ["rounded", "squircle", "pill", "circle", "rect"].contains(value) ? value : "rounded"
    }
    static var cornerRadius: CGFloat {
        CGFloat(settings?.double(forKey: "cornerRadius") ?? 13).clamped(to: 4...28)
    }
    static var opacity: CGFloat {
        CGFloat(settings?.double(forKey: "opacity") ?? 0.92).clamped(to: 0.35...1)
    }

    static var isArabic: Bool {
        settings?.string(forKey: "language") == "العربية"
    }

    static var soundEnabled: Bool {
        settings?.object(forKey: "soundEnabled") as? Bool ?? true
    }

    static var hapticsEnabled: Bool {
        settings?.object(forKey: "hapticsEnabled") as? Bool ?? true
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

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
