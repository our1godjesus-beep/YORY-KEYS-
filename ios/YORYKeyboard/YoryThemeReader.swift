import UIKit

enum YoryThemeReader {
    private static let groupID = "group.app.yory.keys"
    private static var settings: UserDefaults? { UserDefaults(suiteName: groupID) }

    static var backgroundColor: UIColor {
        color(for: "backgroundHex", fallback: "#090A0C")
    }
    static var keyColor: UIColor {
        color(for: "keyHex", fallback: "#202328")
    }
    static var textColor: UIColor {
        color(for: "textHex", fallback: "#F2EEE7")
    }
    static var accentColor: UIColor {
        color(for: "accentHex", fallback: "#C9AE80")
    }
    static var keyPressedColor: UIColor {
        color(for: "keyPressedHex", fallback: "#B8945D")
    }
    static func migrateLegacyDefaultIfNeeded() {
        guard let defaults = settings,
              defaults.string(forKey: "themeName") == "Tinted Glass",
              defaults.string(forKey: "backgroundHex")?.uppercased() == "#1A1D2B",
              ["#F1F3FA", "#E8F1F3"].contains(defaults.string(forKey: "keyHex")?.uppercased() ?? ""),
              defaults.string(forKey: "textHex")?.uppercased() == "#171923",
              defaults.string(forKey: "accentHex")?.uppercased() == "#78B8F8" else { return }
        defaults.set("Obsidian Luxe", forKey: "themeName")
        defaults.set("#090A0C", forKey: "backgroundHex")
        defaults.set("#202328", forKey: "keyHex")
        defaults.set("#F2EEE7", forKey: "textHex")
        defaults.set("#C9AE80", forKey: "accentHex")
        defaults.set("#B8945D", forKey: "keyPressedHex")
        defaults.set(1.0, forKey: "opacity")
        defaults.set(12.0, forKey: "cornerRadius")
        defaults.set(4.0, forKey: "keySpacing")
        defaults.set(40.0, forKey: "keyHeight")
        defaults.set("squircle", forKey: "keyShape")
    }
    static var keySpacing: CGFloat {
        CGFloat(settings?.double(forKey: "keySpacing") ?? 4).clamped(to: 0...12)
    }
    static var fontSize: CGFloat {
        CGFloat(settings?.double(forKey: "fontSize") ?? 18).clamped(to: 12...26)
    }
    static var fontFamily: String {
        let value = settings?.string(forKey: "fontFamily") ?? "system"
        return ["system", "rounded", "serif", "mono"].contains(value) ? value : "system"
    }
    static var keyHeight: CGFloat {
        CGFloat(settings?.double(forKey: "keyHeight") ?? 40).clamped(to: 32...60)
    }
    static var longPressDuration: TimeInterval {
        (settings?.double(forKey: "yory.layout.longPressDuration") ?? 0.45).clamped(to: 0.2...0.9)
    }
    static var longPressSymbolsEnabled: Bool {
        settings?.bool(forKey: "yory.layout.longPressSymbols") ?? true
    }
    static var accentedCharactersEnabled: Bool {
        settings?.bool(forKey: "yory.layout.accentedCharacters") ?? true
    }
    static var arrowKeysEnabled: Bool {
        settings?.bool(forKey: "yory.layout.arrowKeys") ?? false
    }
    static var keyPopupsEnabled: Bool {
        settings?.bool(forKey: "yory.layout.keyPopups") ?? true
    }
    static var largeKeyTextEnabled: Bool {
        settings?.bool(forKey: "yory.layout.largeText") ?? true
    }
    static var numberPosition: String {
        settings?.string(forKey: "yory.layout.numberPosition") == "bottom" ? "bottom" : "top"
    }
    static var hapticIntensity: CGFloat {
        CGFloat(settings?.double(forKey: "yory.haptics.intensity") ?? 0.45).clamped(to: 0...1)
    }
    static var keyShape: String {
        let value = settings?.string(forKey: "keyShape") ?? "squircle"
        return ["rounded", "squircle", "pill", "circle", "rect"].contains(value) ? value : "rounded"
    }
    static var cornerRadius: CGFloat {
        CGFloat(settings?.double(forKey: "cornerRadius") ?? 12).clamped(to: 4...28)
    }
    static var opacity: CGFloat {
        CGFloat(settings?.double(forKey: "opacity") ?? 1.0).clamped(to: 0.35...1)
    }

    static var languageTag: String {
        settings?.string(forKey: "yory.active.language") ??
            ((settings?.string(forKey: "language") == "العربية") ? "ar" : "en-US")
    }

    static var isArabic: Bool { languageTag == "ar" }

    static var numberRowEnabled: Bool {
        settings?.bool(forKey: "yory.layout.numberRow") ?? false
    }

    static var emojiKeyEnabled: Bool {
        settings?.bool(forKey: "yory.emoji.dedicatedKey") ?? true
    }

    static var wordSuggestionsEnabled: Bool {
        settings?.bool(forKey: "yory.typing.suggestions") ?? true
    }

    static var autoCapsEnabled: Bool {
        settings?.bool(forKey: "yory.typing.autoCaps") ?? true
    }

    static var soundEnabled: Bool {
        settings?.object(forKey: "soundEnabled") as? Bool ?? settings?.object(forKey: "yory.sound.enabled") as? Bool ?? true
    }

    static var hapticsEnabled: Bool {
        settings?.object(forKey: "hapticsEnabled") as? Bool ?? settings?.object(forKey: "yory.haptics.enabled") as? Bool ?? true
    }

    static var personalLearningEnabled: Bool {
        settings?.object(forKey: "personalLearningEnabled") as? Bool ?? false
    }

    private static func color(for key: String, fallback: String) -> UIColor {
        let value = settings?.string(forKey: key) ?? fallback
        let hex = value.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        guard let number = UInt64(hex, radix: 16) else { return .darkGray }
        if hex.count == 8 {
            // Eight-digit theme colors are encoded consistently as #RRGGBBAA.
            return UIColor(
                red: CGFloat((number >> 24) & 0xFF) / 255,
                green: CGFloat((number >> 16) & 0xFF) / 255,
                blue: CGFloat((number >> 8) & 0xFF) / 255,
                alpha: CGFloat(number & 0xFF) / 255
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
