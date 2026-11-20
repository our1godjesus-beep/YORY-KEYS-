import SwiftUI
import UIKit
import UniformTypeIdentifiers

private let yoryGroup = UserDefaults(suiteName: "group.app.yory.keys")

struct YoryThemeStudioView: View {
    @State private var background = Color(red: 0.10, green: 0.11, blue: 0.16)
    @State private var key = Color(red: 0.92, green: 0.94, blue: 0.98)
    @State private var text = Color(red: 0.09, green: 0.10, blue: 0.14)
    @State private var accent = Color(red: 0.47, green: 0.72, blue: 0.97)
    @State private var pressedKey = Color(red: 0.47, green: 0.72, blue: 0.97)
    @State private var opacity = 0.92
    @State private var cornerRadius = 13.0
    @State private var keySpacing = 4.0
    @State private var keyHeight = 44.0
    @State private var fontSize = 18.0
    @State private var fontFamily = "system"
    @State private var keyShape = "rounded"
    @State private var soundEnabled = true
    @State private var hapticsEnabled = true
    @State private var personalLearningEnabled = false
    @State private var selectedLanguage = "English"
    @State private var themeName = "Tinted Glass"
    @State private var fileMessage = "Themes stay on this device unless you export them."
    @State private var showExporter = false
    @State private var showImporter = false
    @State private var themeDocument = YoryThemeDocument(data: Data())

    var body: some View {
        ZStack {
            LinearGradient(colors: [Color(red: 0.10, green: 0.14, blue: 0.22), Color(red: 0.025, green: 0.035, blue: 0.065), Color(red: 0.10, green: 0.09, blue: 0.17)], startPoint: .topLeading, endPoint: .bottomTrailing).ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    VStack(alignment: .leading, spacing: 5) {
                        Text("YORY KEYS").font(.caption.weight(.bold)).tracking(2.4).foregroundStyle(accent)
                        Text("YORY Studio").font(.system(size: 34, weight: .bold, design: .rounded)).foregroundStyle(.white)
                        Text("Your words. Your flow.").foregroundStyle(.white.opacity(0.62))
                    }

                    keyboardPreview

                    sectionTitle("MAKE IT YOURS")
                    HStack(spacing: 9) {
                        preset("Tinted", bg: "#1A1D2B", key: "#E8F1F3", accent: "#78B8F8")
                        preset("Midnight", bg: "#10121B", key: "#292D3B", accent: "#9B8CFF")
                        preset("Arctic", bg: "#DDE7F5", key: "#FFFFFF", accent: "#5598DB")
                        preset("Rose", bg: "#F3DDE5", key: "#FFFFFF", accent: "#DE7297")
                    }

                    sectionTitle("MY THEMES")
                    TextField("Theme name", text: $themeName)
                        .textInputAutocapitalization(.words)
                        .padding(12)
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14))
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(.white.opacity(0.18), lineWidth: 0.7))
                    Button(action: saveThemeToLibrary) {
                        Text("Save to My Themes")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 13)
                            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14))
                            .overlay(RoundedRectangle(cornerRadius: 14).stroke(.white.opacity(0.28), lineWidth: 0.8))
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(Color(red: 0.66, green: 0.78, blue: 1.0))

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
                        .foregroundStyle(.white.opacity(0.65))
                        .padding(12)
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14))
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(.white.opacity(0.16), lineWidth: 0.7))

                    sectionTitle("COLORS")
                    colorPickerRow("Background", selection: $background)
                    colorPickerRow("Key surface", selection: $key)
                    colorPickerRow("Key labels", selection: $text)
                    colorPickerRow("Accent", selection: $accent)
                    colorPickerRow("Pressed key", selection: $pressedKey)

                    sectionTitle("GLASS & GEOMETRY")
                    valueSlider("Glass opacity", value: $opacity, range: 0.35...1.0, suffix: "%", multiplier: 100)
                    valueSlider("Key corner radius", value: $cornerRadius, range: 4...28, suffix: " pt")
                    valueSlider("Key spacing", value: $keySpacing, range: 0...12, suffix: " pt")
                    valueSlider("Keyboard height", value: $keyHeight, range: 32...60, suffix: " pt")
                    valueSlider("Letter size", value: $fontSize, range: 12...26, suffix: " pt")
                    Picker("Letter font", selection: $fontFamily) {
                        Text("System").tag("system")
                        Text("Rounded").tag("rounded")
                        Text("Serif").tag("serif")
                        Text("Mono").tag("mono")
                    }
                    .pickerStyle(.segmented)
                    .padding(10)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(.white.opacity(0.18), lineWidth: 0.7))
                    Picker("Key shape", selection: $keyShape) {
                        Text("Rounded").tag("rounded")
                        Text("Squircle").tag("squircle")
                        Text("Pill").tag("pill")
                        Text("Rect").tag("rect")
                        Text("Circle").tag("circle")
                    }
                    .pickerStyle(.segmented)
                    .padding(10)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(.white.opacity(0.18), lineWidth: 0.7))

                    sectionTitle("SENSORY EXPERIENCE")
                    Toggle("Keyboard sounds", isOn: $soundEnabled).tint(accent).foregroundStyle(.white)
                    Toggle("Haptic feedback", isOn: $hapticsEnabled).tint(accent).foregroundStyle(.white)

                    sectionTitle("PERSONAL DICTIONARY")
                    Toggle("Learn words on this device", isOn: $personalLearningEnabled)
                        .tint(accent).foregroundStyle(.white)
                    Text("Optional local learning. Words are learned only when you finish typing them in ordinary text fields. Nothing is uploaded. Use the ••• key to clear learned words from the keyboard.")
                        .font(.footnote).foregroundStyle(.white.opacity(0.55))

                    sectionTitle("LANGUAGE")
                    Picker("Primary layout", selection: $selectedLanguage) {
                        Text("English (US)").tag("English")
                        Text("Deutsch (DE)").tag("Deutsch")
                        Text("العربية (مصر)").tag("العربية")
                    }
                    .pickerStyle(.segmented)

                    Button(action: persistTheme) {
                        Text("Save my keyboard")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 15)
                            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
                            .background(accent.opacity(0.55), in: RoundedRectangle(cornerRadius: 16))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(.white.opacity(0.35), lineWidth: 0.8))
                            .foregroundStyle(.black)
                    }

                    VStack(alignment: .leading, spacing: 6) {
                        Text("ACTIVATE YORY").font(.caption.weight(.bold)).tracking(1.5).foregroundStyle(accent)
                        Text("Settings → General → Keyboard → Keyboards → Add New Keyboard → YORY KEYS")
                            .font(.footnote).foregroundStyle(.white.opacity(0.7))
                        Text("Basic typing works without Full Access. Optional iOS keyboard click sounds may require Allow Full Access in Settings. YORY never sends keystrokes, passwords, or clipboard contents to a server.")
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
        .onChange(of: pressedKey) { _ in persistTheme() }
        .onChange(of: opacity) { _ in persistTheme() }
        .onChange(of: cornerRadius) { _ in persistTheme() }
        .onChange(of: keySpacing) { _ in persistTheme() }
        .onChange(of: keyHeight) { _ in persistTheme() }
        .onChange(of: fontSize) { _ in persistTheme() }
        .onChange(of: fontFamily) { _ in persistTheme() }
        .onChange(of: keyShape) { _ in persistTheme() }
        .onChange(of: soundEnabled) { _ in persistTheme() }
        .onChange(of: hapticsEnabled) { _ in persistTheme() }
        .onChange(of: personalLearningEnabled) { _ in persistTheme() }
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
        VStack(spacing: keySpacing) {
            HStack {
                ForEach(["☺", "✦", "文", "⚙", "•••"], id: \.self) {
                    Text($0).foregroundStyle(accent).frame(maxWidth: .infinity)
                }
            }
            ForEach(["qwertyuiop", "asdfghjkl", "⇧zxcvbnm⌫"], id: \.self) { row in
                HStack(spacing: keySpacing) {
                    ForEach(Array(row), id: \.self) { character in
                        Text(String(character))
                            .font(keyboardFont(size: fontSize, family: fontFamily))
                            .foregroundStyle(text)
                            .frame(maxWidth: .infinity)
                            .frame(height: 35)
                            .background(.ultraThinMaterial, in: keyShapeView)
                            .background(key.opacity(0.28), in: keyShapeView)
                            .overlay(keyShapeView.stroke(.white.opacity(0.25), lineWidth: 0.7))
                    }
                }
            }
            HStack(spacing: keySpacing) {
                previewKey("!#1")
                previewKey("◎")
                previewKey("English")
                previewKey(".")
                previewKey("↵")
            }
        }
        .padding(12)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 23))
        .background(background.opacity(opacity * 0.30), in: RoundedRectangle(cornerRadius: 23))
        .overlay(RoundedRectangle(cornerRadius: 23).stroke(.white.opacity(0.30), lineWidth: 1))
        .shadow(color: accent.opacity(0.12), radius: 18, y: 8)
    }

    private func previewKey(_ label: String) -> some View {
        Text(label)
            .font(.system(size: 10, weight: .medium))
            .foregroundStyle(text)
            .frame(maxWidth: label == "English" ? .infinity : 42)
            .frame(height: 34)
            .background(.ultraThinMaterial, in: keyShapeView)
            .background((label == "↵" ? pressedKey : key).opacity(0.32), in: keyShapeView)
            .overlay(keyShapeView.stroke(.white.opacity(0.25), lineWidth: 0.7))
    }

    private func keyboardFont(size: Double, family: String) -> Font {
        switch family {
        case "rounded": return .system(size: size, weight: .medium, design: .rounded)
        case "serif": return .system(size: size, weight: .medium, design: .serif)
        case "mono": return .system(size: size, weight: .medium, design: .monospaced)
        default: return .system(size: size, weight: .medium, design: .default)
        }
    }

    private var keyShapeView: RoundedRectangle {
        let radius = keyShape == "pill" || keyShape == "circle"
            ? 18
            : keyShape == "squircle" ? cornerRadius * 1.3
            : keyShape == "rect" ? 3
            : cornerRadius
        return RoundedRectangle(cornerRadius: radius)
    }

    private func sectionTitle(_ title: String) -> some View {
        Text(title).font(.caption.weight(.bold)).tracking(1.5).foregroundStyle(.white.opacity(0.62))
    }

    private func preset(_ name: String, bg: String, key: String, accent: String) -> some View {
        Button {
            themeName = name
            background = Color(hex: bg)
            self.key = Color(hex: key)
            self.accent = Color(hex: accent)
            self.pressedKey = Color(hex: accent)
            persistTheme()
        } label: {
            Text(name).font(.caption.weight(.semibold)).frame(maxWidth: .infinity).padding(.vertical, 12)
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
                .background(Color(hex: key).opacity(0.30), in: RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(.white.opacity(0.25), lineWidth: 0.7))
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
        themeName = defaults.string(forKey: "themeName") ?? "Tinted Glass"
        background = Color(hex: defaults.string(forKey: "backgroundHex") ?? "#1A1D2B")
        key = Color(hex: defaults.string(forKey: "keyHex") ?? "#F1F3FA")
        text = Color(hex: defaults.string(forKey: "textHex") ?? "#171923")
        accent = Color(hex: defaults.string(forKey: "accentHex") ?? "#78B8F8")
        pressedKey = Color(hex: defaults.string(forKey: "keyPressedHex") ?? "#78B8F8")
        opacity = defaults.object(forKey: "opacity") == nil ? 0.92 : defaults.double(forKey: "opacity")
        cornerRadius = defaults.object(forKey: "cornerRadius") == nil ? 13 : defaults.double(forKey: "cornerRadius")
        keySpacing = defaults.object(forKey: "keySpacing") == nil ? 4 : defaults.double(forKey: "keySpacing")
        keyHeight = defaults.object(forKey: "keyHeight") == nil ? 44 : defaults.double(forKey: "keyHeight")
        fontSize = defaults.object(forKey: "fontSize") == nil ? 18 : defaults.double(forKey: "fontSize")
        let savedFont = defaults.string(forKey: "fontFamily") ?? "system"
        fontFamily = ["system", "rounded", "serif", "mono"].contains(savedFont) ? savedFont : "system"
        keyShape = defaults.string(forKey: "keyShape") ?? "rounded"
        soundEnabled = defaults.object(forKey: "soundEnabled") as? Bool ?? true
        hapticsEnabled = defaults.object(forKey: "hapticsEnabled") as? Bool ?? true
        personalLearningEnabled = defaults.object(forKey: "personalLearningEnabled") as? Bool ?? false
        switch defaults.string(forKey: "yory.active.language") ?? "en-US" {
        case "ar": selectedLanguage = "العربية"
        case "de": selectedLanguage = "Deutsch"
        default: selectedLanguage = "English"
        }
    }

    private func encodeTheme() -> Data {
        let document: [String: Any] = [
            "schemaVersion": 1,
            "id": UUID().uuidString,
            "name": themeName,
            "appearance": [
                "background": background.hexString,
                "key": key.hexString,
                "keyPressed": pressedKey.hexString,
                "text": text.hexString,
                "accent": accent.hexString
            ],
            "glass": ["enabled": true, "opacity": opacity],
            "geometry": [
                "keyShape": keyShape,
                "cornerRadius": cornerRadius,
                "keySpacing": keySpacing,
                "keyHeight": keyHeight
            ],
            "typography": ["fontSize": fontSize, "fontFamily": fontFamily],
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
                  hex.range(of: "^#[A-Fa-f0-9]{6}([A-Fa-f0-9]{2})?$", options: .regularExpression) != nil else {
                if appearance[key] == nil { return fallback }
                throw CocoaError(.fileReadCorruptFile)
            }
            return Color(hex: hex)
        }
        let importedBackground = try color("background", fallback: background)
        let importedKey = try color("key", fallback: key)
        let importedText = try color("text", fallback: text)
        let importedAccent = try color("accent", fallback: accent)
        let importedPressed = try color("keyPressed", fallback: importedAccent)
        let importedGlass = document["glass"] as? [String: Any] ?? [:]
        let importedGeometry = document["geometry"] as? [String: Any] ?? [:]
        let importedTypography = document["typography"] as? [String: Any] ?? [:]
        let importedSound = document["sound"] as? [String: Any] ?? [:]
        let importedHaptics = document["haptics"] as? [String: Any] ?? [:]

        background = importedBackground
        key = importedKey
        text = importedText
        accent = importedAccent
        pressedKey = importedPressed
        opacity = min(1.0, max(0.35, (importedGlass["opacity"] as? NSNumber)?.doubleValue ?? opacity))
        cornerRadius = min(28.0, max(4.0, (importedGeometry["cornerRadius"] as? NSNumber)?.doubleValue ?? cornerRadius))
        keySpacing = min(12.0, max(0.0, (importedGeometry["keySpacing"] as? NSNumber)?.doubleValue ?? keySpacing))
        keyHeight = min(60.0, max(32.0, (importedGeometry["keyHeight"] as? NSNumber)?.doubleValue ?? keyHeight))
        fontSize = min(26.0, max(12.0, (importedTypography["fontSize"] as? NSNumber)?.doubleValue ?? fontSize))
        let importedFont = importedTypography["fontFamily"] as? String ?? fontFamily
        fontFamily = ["system", "rounded", "serif", "mono"].contains(importedFont) ? importedFont : "system"
        let importedShape = importedGeometry["keyShape"] as? String ?? keyShape
        keyShape = ["rounded", "squircle", "pill", "rect", "circle"].contains(importedShape) ? importedShape : "rounded"
        soundEnabled = importedSound["enabled"] as? Bool ?? soundEnabled
        hapticsEnabled = importedHaptics["enabled"] as? Bool ?? hapticsEnabled
        themeName = String((document["name"] as? String ?? "Imported Theme").prefix(48))
    }

    private func saveThemeToLibrary() {
        let cleanName = String(themeName.trimmingCharacters(in: .whitespacesAndNewlines).prefix(48))
        guard !cleanName.isEmpty, let defaults = yoryGroup else {
            fileMessage = "Enter a theme name before saving."
            return
        }
        themeName = cleanName
        persistTheme()
        let document = encodeTheme()
        var entries = defaults.array(forKey: "yory.saved.themes") as? [[String: String]] ?? []
        entries.removeAll { $0["name"]?.caseInsensitiveCompare(cleanName) == .orderedSame }
        entries.append(["name": cleanName, "document": document.base64EncodedString()])
        defaults.set(entries, forKey: "yory.saved.themes")
        fileMessage = "\(cleanName) was saved to My Themes."
    }

    private func persistTheme() {
        guard let defaults = yoryGroup else { return }
        defaults.set(themeName, forKey: "themeName")
        defaults.set(background.hexString, forKey: "backgroundHex")
        defaults.set(key.hexString, forKey: "keyHex")
        defaults.set(text.hexString, forKey: "textHex")
        defaults.set(accent.hexString, forKey: "accentHex")
        defaults.set(pressedKey.hexString, forKey: "keyPressedHex")
        defaults.set(opacity, forKey: "opacity")
        defaults.set(cornerRadius, forKey: "cornerRadius")
        defaults.set(keySpacing, forKey: "keySpacing")
        defaults.set(keyHeight, forKey: "keyHeight")
        defaults.set(fontSize, forKey: "fontSize")
        defaults.set(fontFamily, forKey: "fontFamily")
        defaults.set(keyShape, forKey: "keyShape")
        defaults.set(soundEnabled, forKey: "soundEnabled")
        defaults.set(hapticsEnabled, forKey: "hapticsEnabled")
        defaults.set(personalLearningEnabled, forKey: "personalLearningEnabled")
        defaults.set(selectedLanguage, forKey: "language")
        let languageTag = selectedLanguage == "العربية" ? "ar" : (selectedLanguage == "Deutsch" ? "de" : "en-US")
        defaults.set(languageTag, forKey: "yory.active.language")
    }
}


private struct YoryGalleryPreset: Identifiable {
    let name: String
    let category: String
    let background: String
    let key: String
    let text: String
    let accent: String
    let pressed: String
    let opacity: Double
    let radius: Double
    let keyHeight: Double
    let keySpacing: Double
    let fontSize: Double
    let fontFamily: String
    let keyShape: String
    let soundEnabled: Bool
    let hapticsEnabled: Bool
    let isCustom: Bool
    var id: String { name }

    static let all: [YoryGalleryPreset] = [
        .init(name: "Tinted Glass", category: "Dark", background: "#1A1D2B", key: "#E8F1F3", text: "#171923", accent: "#78B8F8", pressed: "#78B8F8", opacity: 0.92, radius: 14, keyHeight: 44, keySpacing: 4, fontSize: 18, fontFamily: "system", keyShape: "rounded", soundEnabled: true, hapticsEnabled: true, isCustom: false),
        .init(name: "Midnight", category: "Dark", background: "#10121B", key: "#292D3B", text: "#F6F7FC", accent: "#9B8CFF", pressed: "#9B8CFF", opacity: 0.98, radius: 12, keyHeight: 46, keySpacing: 4, fontSize: 18, fontFamily: "system", keyShape: "rounded", soundEnabled: true, hapticsEnabled: true, isCustom: false),
        .init(name: "Arctic", category: "Light", background: "#DDE7F5", key: "#FFFFFF", text: "#202432", accent: "#5598DB", pressed: "#5598DB", opacity: 0.88, radius: 16, keyHeight: 46, keySpacing: 4, fontSize: 18, fontFamily: "system", keyShape: "rounded", soundEnabled: true, hapticsEnabled: true, isCustom: false),
        .init(name: "Neon", category: "Colorful", background: "#171928", key: "#252B3B", text: "#F7F8FF", accent: "#51E5DC", pressed: "#51E5DC", opacity: 0.95, radius: 11, keyHeight: 46, keySpacing: 4, fontSize: 18, fontFamily: "system", keyShape: "rounded", soundEnabled: true, hapticsEnabled: true, isCustom: false),
        .init(name: "Rose", category: "Colorful", background: "#F3DDE5", key: "#FFFFFF", text: "#32212B", accent: "#DE7297", pressed: "#DE7297", opacity: 0.90, radius: 18, keyHeight: 46, keySpacing: 4, fontSize: 18, fontFamily: "system", keyShape: "rounded", soundEnabled: true, hapticsEnabled: true, isCustom: false)
    ]
}

struct YoryThemeGalleryView: View {
    @State private var category = "All"
    @State private var appliedTheme = UserDefaults(suiteName: "group.app.yory.keys")?.string(forKey: "themeName") ?? "Tinted Glass"
    @State private var statusMessage = ""
    @State private var showEditor = false
    @State private var showDetail = false
    @State private var detailPreset: YoryGalleryPreset?
    @State private var favoritesRevision = 0
    private let categories = ["All", "Yours", "Favorites", "Dark", "Light", "Colorful"]

    private var favoriteNames: Set<String> {
        _ = favoritesRevision
        let defaults = UserDefaults(suiteName: "group.app.yory.keys")
        let names = defaults?.stringArray(forKey: "yory.favorite.themes") ?? []
        return Set(names.map { $0.lowercased() })
    }

    private var savedPresets: [YoryGalleryPreset] {
        let defaults = UserDefaults(suiteName: "group.app.yory.keys")
        let entries = defaults?.array(forKey: "yory.saved.themes") as? [[String: String]] ?? []
        var result: [YoryGalleryPreset] = []
        for entry in entries {
            if let preset = decodeSavedPreset(entry) {
                result.append(preset)
            }
        }
        return result
    }

    private func decodeSavedPreset(_ entry: [String: String]) -> YoryGalleryPreset? {
        guard let name = entry["name"],
              let encoded = entry["document"],
              let data = Data(base64Encoded: encoded) else { return nil }

        let jsonObject: Any
        do {
            jsonObject = try JSONSerialization.jsonObject(with: data)
        } catch {
            return nil
        }
        guard let root = jsonObject as? [String: Any],
              let appearance = root["appearance"] as? [String: Any] else { return nil }

        let glass = root["glass"] as? [String: Any] ?? [:]
        let geometry = root["geometry"] as? [String: Any] ?? [:]
        let typography = root["typography"] as? [String: Any] ?? [:]
        let sound = root["sound"] as? [String: Any] ?? [:]
        let haptics = root["haptics"] as? [String: Any] ?? [:]

        let background = appearance["background"] as? String ?? "#1A1D2B"
        let key = appearance["key"] as? String ?? "#E8F1F3"
        let text = appearance["text"] as? String ?? "#171923"
        let accent = appearance["accent"] as? String ?? "#78B8F8"
        let pressed = appearance["keyPressed"] as? String ?? accent
        let opacity = (glass["opacity"] as? NSNumber)?.doubleValue ?? 0.92
        let radius = (geometry["cornerRadius"] as? NSNumber)?.doubleValue ?? 14
        let keyHeight = (geometry["keyHeight"] as? NSNumber)?.doubleValue ?? 44
        let keySpacing = (geometry["keySpacing"] as? NSNumber)?.doubleValue ?? 4
        let fontSize = (typography["fontSize"] as? NSNumber)?.doubleValue ?? 18
        let fontFamily = typography["fontFamily"] as? String ?? "system"
        let keyShape = geometry["keyShape"] as? String ?? "rounded"
        let soundEnabled = sound["enabled"] as? Bool ?? true
        let hapticsEnabled = haptics["enabled"] as? Bool ?? true

        let preset = YoryGalleryPreset(
            name: name,
            category: "Yours",
            background: background,
            key: key,
            text: text,
            accent: accent,
            pressed: pressed,
            opacity: opacity,
            radius: radius,
            keyHeight: keyHeight,
            keySpacing: keySpacing,
            fontSize: fontSize,
            fontFamily: fontFamily,
            keyShape: keyShape,
            soundEnabled: soundEnabled,
            hapticsEnabled: hapticsEnabled,
            isCustom: true
        )
        return preset
    }

    private var galleryPresets: [YoryGalleryPreset] {
        let savedNames = Set(savedPresets.map { $0.name.lowercased() })
        return savedPresets + YoryGalleryPreset.all.filter { !savedNames.contains($0.name.lowercased()) }
    }

    private var visiblePresets: [YoryGalleryPreset] {
        switch category {
        case "Yours": return savedPresets
        case "All": return galleryPresets
        case "Favorites": return galleryPresets.filter { favoriteNames.contains($0.name.lowercased()) }
        default: return galleryPresets.filter { $0.category == category }
        }
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.12, green: 0.16, blue: 0.25),
                    Color(red: 0.025, green: 0.035, blue: 0.065),
                    Color(red: 0.10, green: 0.09, blue: 0.17)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            ).ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: 5) {
                        Text("YORY KEYS").font(.caption.weight(.bold)).tracking(2.2).foregroundStyle(Color(hex: "#A7C7FF"))
                        Text("Themes").font(.system(size: 30, weight: .bold, design: .rounded)).foregroundStyle(.white)
                        Text("Choose a look for your keyboard. Your selection stays on this device.")
                            .font(.footnote).foregroundStyle(.white.opacity(0.64))
                    }

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(categories, id: \.self) { item in
                                categoryChip(item)
                            }
                        }
                        .padding(.vertical, 2)
                    }

                    if category == "All" {
                        Text("AVAILABLE THEMES").font(.caption.weight(.bold)).tracking(1.6).foregroundStyle(.white.opacity(0.62))
                    } else if category == "Yours" {
                        Text("YOUR SAVED THEMES").font(.caption.weight(.bold)).tracking(1.6).foregroundStyle(.white.opacity(0.62))
                    }

                    if visiblePresets.isEmpty {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(category == "Yours" ? "No saved themes yet" : "No themes in this category")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(.white)
                            Text("Customize a preset, name it and save it here for offline use.")
                                .font(.footnote).foregroundStyle(.white.opacity(0.62))
                            Button("Create your theme") { showEditor = true }
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(Color(hex: "#A7C7FF"))
                        }
                        .padding(16).frame(maxWidth: .infinity, alignment: .leading)
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 18))
                    } else {
                        LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 14) {
                            ForEach(visiblePresets) { preset in
                                YoryGalleryThemeCard(
                                    preset: preset,
                                    isApplied: appliedTheme == preset.name,
                                    onApply: { apply(preset) },
                                    onCustomize: {
                                        apply(preset)
                                        showEditor = true
                                    },
                                    onDetails: {
                                        detailPreset = preset
                                        showDetail = true
                                    },
                                    isFavorite: favoriteNames.contains(preset.name.lowercased())
                                )
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: 9) {
                        Text("Make it yours").font(.headline).foregroundStyle(.white)
                        Text("Adjust key colors, glass opacity, spacing, shape and lettering in the theme editor.")
                            .font(.footnote).foregroundStyle(.white.opacity(0.64))
                        Button {
                            showEditor = true
                        } label: {
                            Label("Customize current theme", systemImage: "slider.horizontal.3")
                                .font(.subheadline.weight(.semibold)).frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14))
                                .overlay(RoundedRectangle(cornerRadius: 14).stroke(.white.opacity(0.24), lineWidth: 0.8))
                        }
                        .buttonStyle(.plain).foregroundStyle(Color(hex: "#A7C7FF"))
                    }
                    .padding(16)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 19))
                    .overlay(RoundedRectangle(cornerRadius: 19).stroke(.white.opacity(0.16), lineWidth: 0.8))

                    if !statusMessage.isEmpty {
                        Text(statusMessage).font(.footnote).foregroundStyle(Color(hex: "#A7C7FF"))
                    }
                }
                .padding(.horizontal, 16).padding(.top, 18).padding(.bottom, 22)
            }
        }
        .navigationTitle("Themes")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showEditor = true
                } label: {
                    Text("Create").fontWeight(.semibold).foregroundStyle(Color(hex: "#A7C7FF"))
                }
            }
        }
        .onAppear {
            appliedTheme = UserDefaults(suiteName: "group.app.yory.keys")?.string(forKey: "themeName") ?? "Tinted Glass"
        }
        .navigationDestination(isPresented: $showEditor) {
            YoryThemeStudioView()
        }
        .navigationDestination(isPresented: $showDetail) {
            if let preset = detailPreset {
                YoryThemeDetailView(
                    preset: preset,
                    isApplied: appliedTheme == preset.name,
                    isFavorite: favoriteNames.contains(preset.name.lowercased()),
                    onApply: { apply(preset) },
                    onFavorite: { setFavorite(preset.name, $0) },
                    onRename: { renameSavedTheme(preset, newName: $0) },
                    onDuplicate: { duplicateTheme(preset, newName: $0) },
                    onDelete: { deleteSavedTheme(preset) },
                    onCustomize: {
                        apply(preset)
                        showEditor = true
                    }
                )
            }
        }
    }

    private func setFavorite(_ name: String, _ enabled: Bool) {
        guard let defaults = UserDefaults(suiteName: "group.app.yory.keys") else { return }
        var names = Set(defaults.stringArray(forKey: "yory.favorite.themes") ?? [])
        if enabled { names.insert(name) } else { names = Set(names.filter { $0.caseInsensitiveCompare(name) != .orderedSame }) }
        defaults.set(Array(names).sorted(), forKey: "yory.favorite.themes")
        favoritesRevision += 1
    }

    private func documentData(for preset: YoryGalleryPreset, name: String) -> Data? {
        let document: [String: Any] = [
            "schemaVersion": 1,
            "id": UUID().uuidString,
            "name": name,
            "author": "YORY KEYS",
            "appearance": [
                "background": preset.background,
                "key": preset.key,
                "keyPressed": preset.pressed,
                "text": preset.text,
                "accent": preset.accent
            ],
            "glass": ["enabled": true, "opacity": preset.opacity],
            "geometry": [
                "keyShape": preset.keyShape,
                "cornerRadius": preset.radius,
                "keySpacing": preset.keySpacing,
                "keyHeight": preset.keyHeight
            ],
            "typography": ["fontSize": preset.fontSize, "fontFamily": preset.fontFamily],
            "sound": ["enabled": preset.soundEnabled],
            "haptics": ["enabled": preset.hapticsEnabled],
            "emoji": ["displayScale": 1.0, "recentFirst": true]
        ]
        return try? JSONSerialization.data(withJSONObject: document, options: [.prettyPrinted, .sortedKeys])
    }

    private func duplicateTheme(_ preset: YoryGalleryPreset, newName: String) -> Bool {
        let name = String(newName.trimmingCharacters(in: .whitespacesAndNewlines).prefix(48))
        guard !name.isEmpty,
              !savedPresets.contains(where: { $0.name.caseInsensitiveCompare(name) == .orderedSame }),
              let defaults = UserDefaults(suiteName: "group.app.yory.keys"),
              let data = documentData(for: preset, name: name) else { return false }
        var entries = defaults.array(forKey: "yory.saved.themes") as? [[String: String]] ?? []
        entries.append(["name": name, "document": data.base64EncodedString()])
        defaults.set(entries, forKey: "yory.saved.themes")
        statusMessage = "\(name) was added to My Themes."
        return true
    }

    private func renameSavedTheme(_ preset: YoryGalleryPreset, newName: String) -> Bool {
        let name = String(newName.trimmingCharacters(in: .whitespacesAndNewlines).prefix(48))
        guard !name.isEmpty,
              preset.isCustom,
              !savedPresets.contains(where: { $0.name.caseInsensitiveCompare(name) == .orderedSame && $0.name.caseInsensitiveCompare(preset.name) != .orderedSame }),
              let defaults = UserDefaults(suiteName: "group.app.yory.keys") else { return false }

        var entries = defaults.array(forKey: "yory.saved.themes") as? [[String: String]] ?? []
        guard let index = entries.firstIndex(where: { $0["name"]?.caseInsensitiveCompare(preset.name) == .orderedSame }),
              let encoded = entries[index]["document"],
              let data = Data(base64Encoded: encoded),
              var object = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else { return false }
        object["name"] = name
        guard let updatedData = try? JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys]) else { return false }
        entries.remove(at: index)
        entries.append(["name": name, "document": updatedData.base64EncodedString()])
        defaults.set(entries, forKey: "yory.saved.themes")
        if appliedTheme.caseInsensitiveCompare(preset.name) == .orderedSame {
            defaults.set(name, forKey: "themeName")
            appliedTheme = name
        }
        setFavorite(preset.name, false)
        return true
    }

    private func deleteSavedTheme(_ preset: YoryGalleryPreset) {
        guard preset.isCustom,
              let defaults = UserDefaults(suiteName: "group.app.yory.keys") else { return }
        let entries = (defaults.array(forKey: "yory.saved.themes") as? [[String: String]] ?? [])
            .filter { $0["name"]?.caseInsensitiveCompare(preset.name) != .orderedSame }
        defaults.set(entries, forKey: "yory.saved.themes")
        setFavorite(preset.name, false)
        if appliedTheme.caseInsensitiveCompare(preset.name) == .orderedSame {
            apply(YoryGalleryPreset.all[0])
        }
        statusMessage = "\(preset.name) was deleted."
    }

    @ViewBuilder
    private func categoryChip(_ item: String) -> some View {
        let selected = category == item
        let backgroundColor = selected
            ? Color(hex: "#A7C7FF").opacity(0.22)
            : Color.white.opacity(0.055)
        let borderColor = selected
            ? Color(hex: "#A7C7FF").opacity(0.72)
            : Color.white.opacity(0.15)

        Button(action: { category = item }) {
            Text(item)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color.white)
                .padding(.horizontal, 15)
                .padding(.vertical, 9)
                .background(backgroundColor)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(borderColor, lineWidth: 0.8))
        }
        .buttonStyle(.plain)
    }

    private func apply(_ preset: YoryGalleryPreset) {
        guard let defaults = UserDefaults(suiteName: "group.app.yory.keys") else {
            statusMessage = "Could not access local keyboard settings."
            return
        }
        defaults.set(preset.name, forKey: "themeName")
        defaults.set(preset.background, forKey: "backgroundHex")
        defaults.set(preset.key, forKey: "keyHex")
        defaults.set(preset.text, forKey: "textHex")
        defaults.set(preset.accent, forKey: "accentHex")
        defaults.set(preset.pressed, forKey: "keyPressedHex")
        defaults.set(preset.opacity, forKey: "opacity")
        defaults.set(preset.radius, forKey: "cornerRadius")
        defaults.set(preset.keyHeight, forKey: "keyHeight")
        defaults.set(preset.keySpacing, forKey: "keySpacing")
        defaults.set(preset.fontSize, forKey: "fontSize")
        defaults.set(preset.fontFamily, forKey: "fontFamily")
        defaults.set(preset.keyShape, forKey: "keyShape")
        defaults.set(preset.soundEnabled, forKey: "soundEnabled")
        defaults.set(preset.hapticsEnabled, forKey: "hapticsEnabled")
        appliedTheme = preset.name
        statusMessage = "\(preset.name) is now active."
    }
}

private struct YoryGalleryThemeCard: View {
    let preset: YoryGalleryPreset
    let isApplied: Bool
    let isFavorite: Bool
    let onApply: () -> Void
    let onCustomize: () -> Void
    let onDetails: () -> Void
    private let rows = ["qwertyuiop", "asdfghjkl", "zxcvbnm"]

    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            VStack(spacing: 4) {
                HStack {
                    ForEach(["☺", "✦", "文", "⚙", "•••"], id: \.self) {
                        Text($0).font(.system(size: 10, weight: .medium))
                            .foregroundStyle(Color(hex: preset.accent)).frame(maxWidth: .infinity)
                    }
                }
                ForEach(rows, id: \.self) { row in
                    HStack(spacing: 2) {
                        ForEach(Array(row).indices, id: \.self) { index in
                            Text(String(Array(row)[index])).font(.system(size: 8, weight: .medium))
                                .foregroundStyle(Color(hex: preset.text)).frame(maxWidth: .infinity)
                                .frame(height: 22)
                                .background(Color(hex: preset.key).opacity(0.88), in: RoundedRectangle(cornerRadius: CGFloat(min(preset.radius, 7))))
                        }
                    }
                }
                HStack(spacing: 2) {
                    galleryKey("!#1", wide: false)
                    galleryKey("◎", wide: false)
                    galleryKey("English", wide: true)
                    galleryKey("↵", wide: false)
                }
            }
            .padding(9)
            .background(Color(hex: preset.background).opacity(0.92), in: RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(.white.opacity(0.18), lineWidth: 0.7))

            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text(preset.name).font(.subheadline.weight(.semibold)).foregroundStyle(.white).lineLimit(1)
                    Text(isApplied ? "Currently applied" : (preset.isCustom ? "Saved by you" : "Built-in preset"))
                        .font(.caption2).foregroundStyle(isApplied ? Color(hex: "#A7C7FF") : .white.opacity(0.58))
                }
                Spacer(minLength: 0)
                if isApplied {
                    Image(systemName: "checkmark.circle.fill").foregroundStyle(Color(hex: "#A7C7FF"))
                }
            }

            Button(action: onApply) {
                Text(isApplied ? "Applied" : "Apply theme")
                    .font(.caption.weight(.semibold)).frame(maxWidth: .infinity).padding(.vertical, 9)
                    .background(isApplied ? Color(hex: "#A7C7FF").opacity(0.18) : Color(hex: "#A7C7FF"), in: RoundedRectangle(cornerRadius: 11))
                    .foregroundStyle(isApplied ? .white : Color(hex: "#101725"))
            }
            .buttonStyle(.plain)

            HStack(spacing: 4) {
                Button(action: onDetails) {
                    Text("Details").font(.caption.weight(.semibold)).frame(maxWidth: .infinity)
                        .foregroundStyle(Color(hex: "#A7C7FF"))
                }
                .buttonStyle(.plain)
                Button(action: onCustomize) {
                    Text("Customize").font(.caption.weight(.semibold)).frame(maxWidth: .infinity)
                        .foregroundStyle(Color(hex: "#A7C7FF"))
                }
                .buttonStyle(.plain)
                if isFavorite {
                    Image(systemName: "star.fill").font(.caption2).foregroundStyle(Color(hex: "#A7C7FF"))
                }
            }
        }
        .padding(10)
        .background(Color.white.opacity(0.055), in: RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(isApplied ? Color(hex: "#A7C7FF") : Color.white.opacity(0.16), lineWidth: isApplied ? 1.2 : 0.7))
    }

    private func galleryKey(_ title: String, wide: Bool) -> some View {
        Text(title).font(.system(size: 7, weight: .medium)).foregroundStyle(Color(hex: preset.text))
            .frame(maxWidth: wide ? .infinity : 34).frame(height: 22)
            .background(Color(hex: preset.key).opacity(0.92), in: RoundedRectangle(cornerRadius: CGFloat(min(preset.radius, 7))))
    }
}

private struct YoryThemeDetailView: View {
    @Environment(\.dismiss) private var dismiss
    let preset: YoryGalleryPreset
    let isApplied: Bool
    let isFavorite: Bool
    let onApply: () -> Void
    let onFavorite: (Bool) -> Void
    let onRename: (String) -> Bool
    let onDuplicate: (String) -> Bool
    let onDelete: () -> Void
    let onCustomize: () -> Void

    @State private var showRename = false
    @State private var showDuplicate = false
    @State private var confirmDelete = false
    @State private var nameInput = ""

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.12, green: 0.16, blue: 0.25),
                    Color(red: 0.025, green: 0.035, blue: 0.065),
                    Color(red: 0.10, green: 0.09, blue: 0.17)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            ).ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(preset.name)
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)

                    detailPreview

                    Text(isApplied ? "Currently applied" : "Ready to apply")
                        .font(.footnote.weight(.medium))
                        .foregroundStyle(isApplied ? Color(red: 0.65, green: 0.78, blue: 1.0) : .white.opacity(0.66))

                    Button(action: onApply) {
                        Text(isApplied ? "Applied" : "Apply theme")
                            .font(.headline.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 13)
                            .background(Color(red: 0.65, green: 0.78, blue: 1.0), in: RoundedRectangle(cornerRadius: 14))
                            .foregroundStyle(Color(red: 0.06, green: 0.09, blue: 0.14))
                    }
                    .buttonStyle(.plain)

                    Button(action: onCustomize) {
                        Label("Customize this theme", systemImage: "slider.horizontal.3")
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14))
                            .overlay(RoundedRectangle(cornerRadius: 14).stroke(.white.opacity(0.22), lineWidth: 0.8))
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(Color(red: 0.65, green: 0.78, blue: 1.0))

                    VStack(alignment: .leading, spacing: 8) {
                        Text("THEME DETAILS")
                            .font(.caption.weight(.bold)).tracking(1.5)
                            .foregroundStyle(.white.opacity(0.65))
                        detailRow("Key height", value: "\(Int(preset.keyHeight)) dp")
                        detailRow("Key spacing", value: "\(Int(preset.keySpacing)) dp")
                        detailRow("Corner radius", value: "\(Int(preset.radius)) dp")
                        detailRow("Letter size", value: "\(Int(preset.fontSize)) pt")
                        detailRow("Glass opacity", value: "\(Int(preset.opacity * 100))%")
                    }
                    .padding(15)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 18))
                    .overlay(RoundedRectangle(cornerRadius: 18).stroke(.white.opacity(0.16), lineWidth: 0.8))

                    VStack(alignment: .leading, spacing: 8) {
                        Text("MANAGE THEME")
                            .font(.caption.weight(.bold)).tracking(1.5)
                            .foregroundStyle(.white.opacity(0.65))
                        Button {
                            onFavorite(!isFavorite)
                        } label: {
                            Label(isFavorite ? "Remove from Favorites" : "Add to Favorites", systemImage: isFavorite ? "star.slash" : "star")
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .foregroundStyle(Color(red: 0.65, green: 0.78, blue: 1.0))
                        if preset.isCustom {
                            Button {
                                nameInput = preset.name
                                showRename = true
                            } label: {
                                Label("Rename", systemImage: "pencil").frame(maxWidth: .infinity, alignment: .leading)
                            }
                            .foregroundStyle(.white)
                        }
                        Button {
                            nameInput = "Copy of \(preset.name)".prefix(48).description
                            showDuplicate = true
                        } label: {
                            Label("Duplicate", systemImage: "plus.square.on.square").frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .foregroundStyle(.white)
                        if preset.isCustom {
                            Button(role: .destructive) {
                                confirmDelete = true
                            } label: {
                                Label("Delete saved theme", systemImage: "trash").frame(maxWidth: .infinity, alignment: .leading)
                            }
                        } else {
                            Text("Built-in presets cannot be renamed or deleted. Duplicate one to create your own version.")
                                .font(.footnote).foregroundStyle(.white.opacity(0.62))
                        }
                    }
                    .padding(15)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 18))
                    .overlay(RoundedRectangle(cornerRadius: 18).stroke(.white.opacity(0.16), lineWidth: 0.8))
                }
                .padding(.horizontal, 16)
                .padding(.top, 20)
                .padding(.bottom, 28)
            }
        }
        .navigationTitle("Theme details")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Rename theme", isPresented: $showRename) {
            TextField("Theme name", text: $nameInput)
            Button("Save") { _ = onRename(nameInput) }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Use a name up to 48 characters.")
        }
        .alert("Duplicate theme", isPresented: $showDuplicate) {
            TextField("New theme name", text: $nameInput)
            Button("Duplicate") { _ = onDuplicate(nameInput) }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("A separate local copy will be saved on this device.")
        }
        .confirmationDialog("Delete this saved theme?", isPresented: $confirmDelete, titleVisibility: .visible) {
            Button("Delete Theme", role: .destructive) {
                onDelete()
                dismiss()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("The selected theme will be removed from this device. Built-in themes are not affected.")
        }
    }

    private var detailPreview: some View {
        VStack(spacing: 5) {
            ForEach(["qwertyuiop", "asdfghjkl", "zxcvbnm"], id: \.self) { row in
                HStack(spacing: 3) {
                    ForEach(Array(row), id: \.self) { character in
                        Text(String(character))
                            .font(.system(size: CGFloat(preset.fontSize * 0.55), weight: .medium))
                            .foregroundStyle(Color(hex: preset.text))
                            .frame(maxWidth: .infinity)
                            .frame(height: CGFloat(preset.keyHeight * 0.8))
                            .background(Color(hex: preset.key), in: RoundedRectangle(cornerRadius: CGFloat(preset.radius)))
                    }
                }
            }
        }
        .padding(12)
        .background(Color(hex: preset.background), in: RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(.white.opacity(0.18), lineWidth: 0.8))
    }

    private func detailRow(_ title: String, value: String) -> some View {
        HStack {
            Text(title).foregroundStyle(.white.opacity(0.72))
            Spacer()
            Text(value).font(.footnote.weight(.medium)).foregroundStyle(.white)
        }
        .font(.footnote)
    }
}

private extension Color {
    init(hex: String) {
        let raw = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        let value = UInt64(raw, radix: 16) ?? 0x1A1D2B
        let hasAlpha = raw.count == 8
        // Eight-digit theme colors use CSS-style #RRGGBBAA on every platform.
        let red = (value >> (hasAlpha ? 24 : 16)) & 0xFF
        let green = (value >> (hasAlpha ? 16 : 8)) & 0xFF
        let blue = (value >> (hasAlpha ? 8 : 0)) & 0xFF
        let alpha = hasAlpha ? (value & 0xFF) : 0xFF
        self.init(
            .sRGB,
            red: Double(red) / 255,
            green: Double(green) / 255,
            blue: Double(blue) / 255,
            opacity: Double(alpha) / 255
        )
    }

    var hexString: String {
        let uiColor = UIColor(self)
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 1
        uiColor.getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        return String(
            format: "#%02X%02X%02X%02X",
            Int(red * 255), Int(green * 255), Int(blue * 255), Int(alpha * 255)
        )
    }
}
