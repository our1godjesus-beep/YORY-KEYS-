import SwiftUI
import UIKit
import UniformTypeIdentifiers

private let yoryGroup = UserDefaults(suiteName: "group.app.yory.keys")

struct ContentView: View {
    @State private var background = Color(red: 0.10, green: 0.11, blue: 0.16)
    @State private var key = Color(red: 0.92, green: 0.94, blue: 0.98)
    @State private var text = Color(red: 0.09, green: 0.10, blue: 0.14)
    @State private var accent = Color(red: 0.47, green: 0.72, blue: 0.97)
    @State private var opacity = 0.92
    @State private var cornerRadius = 13.0
    @State private var soundEnabled = true
    @State private var hapticsEnabled = true
    @State private var selectedLanguage = "English"
    @State private var themeName = "Tinted Glass"
    @State private var fileMessage = "Themes stay on this device unless you export them."
    @State private var showExporter = false
    @State private var showImporter = false
    @State private var themeDocument = YoryThemeDocument(data: Data())

    var body: some View {
        ZStack {
            Color(red: 0.04, green: 0.05, blue: 0.09).ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    VStack(alignment: .leading, spacing: 5) {
                        Text("YORY KEYS").font(.caption.weight(.bold)).tracking(2.4).foregroundStyle(accent)
                        Text("YORY Studio").font(.system(size: 34, weight: .bold, design: .rounded)).foregroundStyle(.white)
                        Text("Your keyboard. Your universe.").foregroundStyle(.white.opacity(0.62))
                    }

                    keyboardPreview

                    sectionTitle("MAKE IT YOURS")
                    HStack(spacing: 9) {
                        preset("Tinted", bg: "#1A1D2B", key: "#E8F1F3", accent: "#78B8F8")
                        preset("Midnight", bg: "#10121B", key: "#292D3B", accent: "#9B8CFF")
                        preset("Arctic", bg: "#DDE7F5", key: "#FFFFFF", accent: "#5598DB")
                        preset("Rose", bg: "#F3DDE5", key: "#FFFFFF", accent: "#DE7297")
                    }

                    sectionTitle("THEME FILES")
                    HStack(spacing: 10) {
                        Button("Export .yorytheme") {
                            themeDocument = YoryThemeDocument(data: encodeTheme())
                            showExporter = true
                        }
                        .buttonStyle(.bordered)
                        Button("Import theme") { showImporter = true }
                            .buttonStyle(.bordered)
                    }
                    .font(.caption.weight(.semibold))
                    Text(fileMessage)
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(0.55))

                    sectionTitle("COLORS")
                    colorPickerRow("Background", selection: $background)
                    colorPickerRow("Key surface", selection: $key)
                    colorPickerRow("Key labels", selection: $text)
                    colorPickerRow("Accent", selection: $accent)

                    sectionTitle("GLASS & GEOMETRY")
                    valueSlider("Glass opacity", value: $opacity, range: 0.35...1.0, suffix: "%", multiplier: 100)
                    valueSlider("Key corner radius", value: $cornerRadius, range: 4...28, suffix: " pt")

                    sectionTitle("SENSORY EXPERIENCE")
                    Toggle("Keyboard sounds", isOn: $soundEnabled).tint(accent).foregroundStyle(.white)
                    Toggle("Haptic feedback", isOn: $hapticsEnabled).tint(accent).foregroundStyle(.white)

                    sectionTitle("LANGUAGE")
                    Picker("Primary layout", selection: $selectedLanguage) {
                        Text("English").tag("English")
                        Text("العربية").tag("العربية")
                    }
                    .pickerStyle(.segmented)

                    Button(action: persistTheme) {
                        Text("Save my keyboard")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 15)
                            .background(accent, in: RoundedRectangle(cornerRadius: 16))
                            .foregroundStyle(.black)
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text("ACTIVATE YORY").font(.caption.weight(.bold)).tracking(1.5).foregroundStyle(accent)
                        Text("Settings → General → Keyboard → Keyboards → Add New Keyboard → YORY KEYS")
                            .font(.footnote).foregroundStyle(.white.opacity(0.7))
                        Text("Typing works without Open Access. The extension does not send keystrokes or text to a server.")
                            .font(.footnote).foregroundStyle(.white.opacity(0.55))
                    }
                }
                .padding(20)
            }
        }
        .preferredColorScheme(.dark)
        .onAppear(perform: loadTheme)
        .onChange(of: background) { _ in persistTheme() }
        .onChange(of: key) { _ in persistTheme() }
        .onChange(of: text) { _ in persistTheme() }
        .onChange(of: accent) { _ in persistTheme() }
        .onChange(of: opacity) { _ in persistTheme() }
        .onChange(of: cornerRadius) { _ in persistTheme() }
        .onChange(of: soundEnabled) { _ in persistTheme() }
        .onChange(of: hapticsEnabled) { _ in persistTheme() }
        .onChange(of: selectedLanguage) { _ in persistTheme() }
        .fileExporter(
            isPresented: $showExporter,
            document: themeDocument,
            contentType: .json,
            defaultFilename: "YORY-" + themeName.replacingOccurrences(of: " ", with: "-") + ".yorytheme"
        ) { result in
            switch result {
            case .success:
                fileMessage = "Theme exported successfully."
            case .failure(let error):
                fileMessage = "Export failed: " + error.localizedDescription
            }
        }
        .fileImporter(
            isPresented: $showImporter,
            allowedContentTypes: [.json],
            allowsMultipleSelection: false
        ) { result in
            do {
                let urls = try result.get()
                guard let url = urls.first else { return }
                let scoped = url.startAccessingSecurityScopedResource()
                defer { if scoped { url.stopAccessingSecurityScopedResource() } }
                let data = try Data(contentsOf: url)
                try importTheme(data)
                persistTheme()
                fileMessage = "Theme imported and applied."
            } catch {
                fileMessage = "Import failed: " + error.localizedDescription
            }
        }
    }

    private var keyboardPreview: some View {
        VStack(spacing: 7) {
            HStack {
                ForEach(["☺", "✦", "文", "⚙", "•••"], id: \.self) {
                    Text($0).foregroundStyle(accent).frame(maxWidth: .infinity)
                }
            }
            ForEach(["qwertyuiop", "asdfghjkl", "⇧zxcvbnm⌫"], id: \.self) { row in
                HStack(spacing: 4) {
                    ForEach(Array(row), id: \.self) { character in
                        Text(String(character))
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(text)
                            .frame(maxWidth: .infinity)
                            .frame(height: 35)
                            .background(key, in: RoundedRectangle(cornerRadius: cornerRadius))
                    }
                }
            }
            HStack(spacing: 5) {
                previewKey("!#1")
                previewKey("◎")
                previewKey("English")
                previewKey(".")
                previewKey("↵")
            }
        }
        .padding(12)
        .background(background.opacity(opacity), in: RoundedRectangle(cornerRadius: 23))
        .overlay(RoundedRectangle(cornerRadius: 23).stroke(.white.opacity(0.18), lineWidth: 1))
    }

    private func previewKey(_ label: String) -> some View {
        Text(label)
            .font(.system(size: 10, weight: .medium))
            .foregroundStyle(text)
            .frame(maxWidth: label == "English" ? .infinity : 42)
            .frame(height: 34)
            .background(key, in: RoundedRectangle(cornerRadius: cornerRadius))
    }

    private func sectionTitle(_ title: String) -> some View {
        Text(title).font(.caption.weight(.bold)).tracking(1.5).foregroundStyle(.white.opacity(0.62))
    }

    private func preset(_ name: String, bg: String, key: String, accent: String) -> some View {
        Button {
            background = Color(hex: bg)
            self.key = Color(hex: key)
            self.accent = Color(hex: accent)
            persistTheme()
        } label: {
            Text(name).font(.caption.weight(.semibold)).frame(maxWidth: .infinity).padding(.vertical, 12)
                .background(Color(hex: key), in: RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(Color(hex: "#202432"))
        }
        .buttonStyle(.plain)
    }

    private func colorPickerRow(_ title: String, selection: Binding<Color>) -> some View {
        HStack {
            Text(title).foregroundStyle(.white)
            Spacer()
            ColorPicker("", selection: selection, supportsOpacity: false).labelsHidden()
        }
    }

    private func valueSlider(_ title: String, value: Binding<Double>, range: ClosedRange<Double>, suffix: String, multiplier: Double = 1) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            HStack {
                Text(title).foregroundStyle(.white)
                Spacer()
                Text("\(Int(value.wrappedValue * multiplier))\(suffix)").foregroundStyle(.white.opacity(0.6))
            }
            Slider(value: value, in: range).tint(accent)
        }
    }

    private func loadTheme() {
        guard let defaults = yoryGroup else { return }
        background = Color(hex: defaults.string(forKey: "backgroundHex") ?? "#1A1D2B")
        key = Color(hex: defaults.string(forKey: "keyHex") ?? "#F1F3FA")
        text = Color(hex: defaults.string(forKey: "textHex") ?? "#171923")
        accent = Color(hex: defaults.string(forKey: "accentHex") ?? "#78B8F8")
        opacity = defaults.object(forKey: "opacity") == nil ? 0.92 : defaults.double(forKey: "opacity")
        cornerRadius = defaults.object(forKey: "cornerRadius") == nil ? 13 : defaults.double(forKey: "cornerRadius")
        soundEnabled = defaults.object(forKey: "soundEnabled") as? Bool ?? true
        hapticsEnabled = defaults.object(forKey: "hapticsEnabled") as? Bool ?? true
    }

    private func encodeTheme() -> Data {
        let document: [String: Any] = [
            "schemaVersion": 1,
            "id": UUID().uuidString,
            "name": themeName,
            "appearance": [
                "background": background.hexString,
                "key": key.hexString,
                "keyPressed": accent.hexString,
                "text": text.hexString,
                "accent": accent.hexString
            ],
            "glass": ["enabled": true, "opacity": opacity],
            "geometry": [
                "keyShape": "rounded",
                "keyShape": "rounded",
                "cornerRadius": cornerRadius,
                "keySpacing": 4,
                "keyHeight": 46
            ],
            "sound": ["enabled": soundEnabled],
            "haptics": ["enabled": hapticsEnabled],
            "emoji": ["displayScale": 1.0, "recentFirst": true]
        ]
        return (try? JSONSerialization.data(withJSONObject: document, options: [.prettyPrinted, .sortedKeys])) ?? Data()
    }

    private func importTheme(_ data: Data) throws {
        guard data.count <= 128 * 1024 else {
            throw CocoaError(.fileReadTooLarge)
        }
        let raw = try JSONSerialization.jsonObject(with: data)
        guard let document = raw as? [String: Any],
              (document["schemaVersion"] as? NSNumber)?.intValue == 1,
              let appearance = document["appearance"] as? [String: Any] else {
            throw CocoaError(.fileReadCorruptFile)
        }
        func color(_ key: String, fallback: Color) throws -> Color {
            guard let hex = appearance[key] as? String,
                  hex.range(of: "^#[A-Fa-f0-9]{6}$", options: .regularExpression) != nil else {
                if appearance[key] == nil { return fallback }
                throw CocoaError(.fileReadCorruptFile)
            }
            return Color(hex: hex)
        }
        let importedBackground = try color("background", fallback: background)
        let importedKey = try color("key", fallback: key)
        let importedText = try color("text", fallback: text)
        let importedAccent = try color("accent", fallback: accent)
        let importedGlass = document["glass"] as? [String: Any] ?? [:]
        let importedGeometry = document["geometry"] as? [String: Any] ?? [:]
        let importedSound = document["sound"] as? [String: Any] ?? [:]
        let importedHaptics = document["haptics"] as? [String: Any] ?? [:]

        background = importedBackground
        key = importedKey
        text = importedText
        accent = importedAccent
        opacity = min(1.0, max(0.35, (importedGlass["opacity"] as? NSNumber)?.doubleValue ?? opacity))
        cornerRadius = min(28.0, max(4.0, (importedGeometry["cornerRadius"] as? NSNumber)?.doubleValue ?? cornerRadius))
        soundEnabled = importedSound["enabled"] as? Bool ?? soundEnabled
        hapticsEnabled = importedHaptics["enabled"] as? Bool ?? hapticsEnabled
        themeName = String((document["name"] as? String ?? "Imported Theme").prefix(48))
    }

    private func persistTheme() {
        guard let defaults = yoryGroup else { return }
        defaults.set(background.hexString, forKey: "backgroundHex")
        defaults.set(key.hexString, forKey: "keyHex")
        defaults.set(text.hexString, forKey: "textHex")
        defaults.set(accent.hexString, forKey: "accentHex")
        defaults.set(opacity, forKey: "opacity")
        defaults.set(cornerRadius, forKey: "cornerRadius")
        defaults.set(soundEnabled, forKey: "soundEnabled")
        defaults.set(hapticsEnabled, forKey: "hapticsEnabled")
        defaults.set(selectedLanguage, forKey: "language")
    }
}

private extension Color {
    init(hex: String) {
        let raw = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        let value = UInt64(raw, radix: 16) ?? 0x1A1D2B
        self.init(
            .sRGB,
            red: Double((value >> 16) & 0xFF) / 255,
            green: Double((value >> 8) & 0xFF) / 255,
            blue: Double(value & 0xFF) / 255,
            opacity: 1
        )
    }

    var hexString: String {
        let uiColor = UIColor(self)
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        uiColor.getRed(&red, green: &green, blue: &blue, alpha: nil)
        return String(format: "#%02X%02X%02X", Int(red * 255), Int(green * 255), Int(blue * 255))
    }
}
