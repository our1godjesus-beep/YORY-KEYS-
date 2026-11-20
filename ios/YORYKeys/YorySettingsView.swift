import SwiftUI

private let yorySettingsStore = UserDefaults(suiteName: "group.app.yory.keys")

private struct YorySettingsItem: Identifiable {
    let title: String
    let subtitle: String
    let symbol: String
    var id: String { title }
}

struct ContentView: View {
    let onSignOut: () -> Void
    private let items = [
        YorySettingsItem(title: "Account", subtitle: "Optional account for cloud sync and online features", symbol: "person.crop.circle"),
        YorySettingsItem(title: "YORY Rewards", subtitle: "Discover keyboard milestones", symbol: "medal"),
        YorySettingsItem(title: "Languages", subtitle: "English (US), العربية", symbol: "globe"),
        YorySettingsItem(title: "Themes", subtitle: "Glass themes and customization", symbol: "paintpalette"),
        YorySettingsItem(title: "Typing", subtitle: "Suggestions and personal dictionary", symbol: "textformat"),
        YorySettingsItem(title: "Emoji", subtitle: "Emoji predictions and keyboard key", symbol: "face.smiling"),
        YorySettingsItem(title: "Rich input", subtitle: "Clipboard and input tools", symbol: "mic"),
        YorySettingsItem(title: "Keyboard toolbar", subtitle: "Choose and arrange keyboard shortcuts", symbol: "slider.horizontal.3"),
        YorySettingsItem(title: "Layout & keys", subtitle: "Size, rows, symbols and key behavior", symbol: "keyboard"),
        YorySettingsItem(title: "Sound & vibration", subtitle: "Key click and haptic feedback", symbol: "speaker.wave.2"),
        YorySettingsItem(title: "Privacy", subtitle: "Your data and on-device controls", symbol: "hand.raised"),
        YorySettingsItem(title: "Achievements", subtitle: "Your YORY milestones", symbol: "star"),
        YorySettingsItem(title: "Help & Feedback", subtitle: "Support and feedback", symbol: "questionmark.circle"),
        YorySettingsItem(title: "About YORY KEYS", subtitle: "Version and product information", symbol: "info.circle")
    ]

    var body: some View {
        NavigationStack {
            ZStack {
                yoryGlassBackground()
                List(items) { item in
                    NavigationLink(value: item.title) {
                        HStack(spacing: 15) {
                            Image(systemName: item.symbol)
                                .font(.system(size: 21, weight: .regular))
                                .foregroundStyle(.white)
                                .frame(width: 34, height: 42)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(item.title)
                                    .font(.system(size: 17, weight: .medium))
                                    .foregroundStyle(.white)
                                Text(item.subtitle)
                                    .font(.system(size: 12))
                                    .foregroundStyle(.white.opacity(0.62))
                            }
                            Spacer(minLength: 4)
                        }
                        .padding(.vertical, 8)
                        .padding(.horizontal, 8)
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(.white.opacity(0.15), lineWidth: 0.7))
                    }
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .navigationTitle("YORY KEYS")
                .navigationBarTitleDisplayMode(.large)
                .navigationDestination(for: String.self) { title in
                    if title == "Themes" {
                        YoryThemeGalleryView()
                    } else if title == "Keyboard toolbar" {
                        YoryToolbarCustomizationView()
                    } else {
                        YorySettingsDetailView(title: title, onSignOut: onSignOut)
                    }
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                HStack {
                    Spacer()
                    NavigationLink(value: "Keyboard Setup") {
                        Image(systemName: "keyboard.fill")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundStyle(Color(red: 0.0627, green: 0.0902, blue: 0.1451))
                            .frame(width: 52, height: 52)
                            .background(Color(red: 0.6549, green: 0.7804, blue: 1.0), in: RoundedRectangle(cornerRadius: 16))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(.white.opacity(0.42), lineWidth: 0.8))
                            .shadow(color: Color.black.opacity(0.24), radius: 12, y: 5)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Set up YORY KEYS")
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 6)
                .background(.clear)
            }
            .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
        }
        .preferredColorScheme(.dark)
    }
}

private struct YorySettingsDetailView: View {
    let title: String
    let onSignOut: () -> Void

    @AppStorage("yory.active.language", store: yorySettingsStore) private var activeLanguage = "en-US"
    @AppStorage("yory.emoji.predictions", store: yorySettingsStore) private var emojiPredictions = true
    @AppStorage("yory.emoji.dedicatedKey", store: yorySettingsStore) private var emojiDedicatedKey = true
    @AppStorage("yory.emoji.recentFirst", store: yorySettingsStore) private var emojiRecentFirst = true
    @AppStorage("yory.layout.numberRow", store: yorySettingsStore) private var numberRow = false
    @AppStorage("yory.layout.numberPosition", store: yorySettingsStore) private var numberPosition = "top"
    @AppStorage("yory.layout.longPressDuration", store: yorySettingsStore) private var longPressDuration = 0.45
    @AppStorage("yory.haptics.intensity", store: yorySettingsStore) private var hapticIntensity = 0.45
    @AppStorage("yory.layout.longPressSymbols", store: yorySettingsStore) private var longPressSymbols = true
    @AppStorage("yory.layout.accentedCharacters", store: yorySettingsStore) private var accentedCharacters = true
    @AppStorage("yory.layout.arrowKeys", store: yorySettingsStore) private var arrowKeys = false
    @AppStorage("yory.layout.keyPopups", store: yorySettingsStore) private var keyPopups = true
    @AppStorage("yory.layout.largeText", store: yorySettingsStore) private var largeKeyText = true
    @AppStorage("yory.typing.suggestions", store: yorySettingsStore) private var wordSuggestions = true
    @AppStorage("yory.typing.autoCaps", store: yorySettingsStore) private var autoCaps = true
    @AppStorage("soundEnabled", store: yorySettingsStore) private var soundEnabled = true
    @AppStorage("hapticsEnabled", store: yorySettingsStore) private var hapticsEnabled = true
    @AppStorage("personalLearningEnabled", store: yorySettingsStore) private var personalLearningEnabled = false
    @AppStorage("yory.input.compactMode", store: yorySettingsStore) private var compactMode = false

    var body: some View {
        ZStack {
            yoryGlassBackground()
            List {
                switch title {
                case "Languages":
                    Section("YOUR LANGUAGES") {
                        languageRow("Deutsch (DE)", detail: "Layout: QWERTZ", tag: "de")
                        languageRow("English (US)", detail: "Layout: QWERTY", tag: "en-US")
                        languageRow("العربية (مصر)", detail: "Layout: Arabic 123 (PC)", tag: "ar")
                    }
                    Section {
                        Text("Change the active layout using the language key on the keyboard. The selection is shared with the keyboard extension.")
                            .font(.footnote).foregroundStyle(.white.opacity(0.65))
                        Button("＋ Add languages") {}
                            .disabled(true)
                    }
                case "Emoji":
                    Section {
                        glassToggle("Emoji predictions", detail: "Show emoji suggestions in the prediction bar", isOn: $emojiPredictions)
                        glassToggle("Dedicated emoji key", detail: "Show an emoji shortcut on the keyboard", isOn: $emojiDedicatedKey)
                        glassToggle("Recent emoji first", detail: "Keep recently used emoji at the beginning", isOn: $emojiRecentFirst)
                    }
                case "Layout & keys":
                    Section {
                        glassToggle("Number row", detail: "Always show a number row above letters", isOn: $numberRow)
                        Picker("Number position", selection: $numberPosition) {
                            Text("Top").tag("top")
                            Text("Bottom").tag("bottom")
                        }
                        .pickerStyle(.segmented)
                        .listRowBackground(Rectangle().fill(.ultraThinMaterial))
                        glassToggle("Long-press symbols", detail: "Show secondary symbols on letter keys", isOn: $longPressSymbols)
                        glassToggle("Accented characters", detail: "Long-press to see available accents", isOn: $accentedCharacters)
                        glassToggle("Arrow keys", detail: "Show cursor movement keys", isOn: $arrowKeys)
                        glassToggle("Key pop-ups", detail: "Show a pop-up when tapping keys", isOn: $keyPopups)
                        glassToggle("Large key text", detail: "Use larger labels on keyboard keys", isOn: $largeKeyText)
                        VStack(alignment: .leading, spacing: 7) {
                            HStack {
                                Text("Long-press duration").foregroundStyle(.white)
                                Spacer()
                                Text("\(Int(longPressDuration * 1000)) ms").foregroundStyle(.white.opacity(0.6))
                            }
                            Slider(value: $longPressDuration, in: 0.2...0.9, step: 0.05)
                                .tint(Color(red: 0.62, green: 0.76, blue: 1.0))
                        }
                        .listRowBackground(Rectangle().fill(.ultraThinMaterial))
                    }
                    Section("KEYBOARD SIZE") {
                        Text("Resize, spacing, corner radius and lettering are controlled in Themes.")
                            .foregroundStyle(.white.opacity(0.65))
                    }
                case "Sound & vibration":
                    Section {
                        glassToggle("Keypress sound", detail: "Play a sound when a key is pressed", isOn: $soundEnabled)
                        glassToggle("Keypress vibration", detail: "Use haptic feedback for key presses", isOn: $hapticsEnabled)
                        VStack(alignment: .leading, spacing: 7) {
                            HStack {
                                Text("Haptic intensity").foregroundStyle(.white)
                                Spacer()
                                Text("\(Int(hapticIntensity * 100))%").foregroundStyle(.white.opacity(0.6))
                            }
                            Slider(value: $hapticIntensity, in: 0...1, step: 0.05)
                                .tint(Color(red: 0.62, green: 0.76, blue: 1.0))
                        }
                        .listRowBackground(Rectangle().fill(.ultraThinMaterial))
                        Text("Key click sound volume follows the iOS keyboard/system sound settings.")
                            .font(.footnote).foregroundStyle(.white.opacity(0.6))
                            .listRowBackground(Rectangle().fill(.ultraThinMaterial))
                    }
                case "Keyboard Setup":
                    Section("ADD YORY KEYS") {
                        settingsInfo("Step 1", "Open Settings > General > Keyboard > Keyboards.")
                        settingsInfo("Step 2", "Choose Add New Keyboard, then select YORY KEYS.")
                        settingsInfo("Step 3", "Select YORY KEYS from the globe key in a text field.")
                    }
                    Section("OPTIONAL ACCESS") {
                        settingsInfo("Full Access", "Basic typing works without Full Access. Enable it only if a specific optional keyboard feature requires it and you choose to use that feature.")
                    }
                case "Rich input":
                    Section {
                        glassToggle("Compact mode", detail: "Show fewer suggestions in a shorter bar", isOn: $compactMode)
                        settingsInfo("Clipboard", "Clipboard history is not available in the iOS keyboard preview yet.")
                        settingsInfo("Voice typing", "Voice input is not connected in this preview.")
                        settingsInfo("Online search", "No external search service is enabled in this preview.")
                    }
                case "Typing":
                    Section {
                        glassToggle("Word suggestions", detail: "Show local word suggestions while typing", isOn: $wordSuggestions)
                        glassToggle("Auto-capitalization", detail: "Capitalize the first letter of sentences", isOn: $autoCaps)
                        glassToggle("Personal dictionary", detail: "Learn accepted words locally on this device", isOn: $personalLearningEnabled)
                    }
                    Section("PERSONAL DICTIONARY") {
                        settingsInfo("On-device learning", "Accepted words can be learned locally. Sensitive password, numeric, email and URL fields are excluded.")
                    }
                case "Privacy":
                    Section {
                        settingsInfo("Incognito mode", "Use the eye icon on the keyboard toolbar to pause local word learning and suggestions for the current typing session.")
                        settingsInfo("Private by design", "YORY does not send typed text, passwords or keystrokes to a server.")
                        settingsInfo("On-device storage", "Personal dictionary and optional clipboard history remain on this device.")
                        settingsInfo("Sensitive fields", "Learning and clipboard capture are disabled for sensitive fields.")
                        settingsInfo("Permissions", "The keyboard does not request camera, contacts or location access.")
                    }
                case "Account":
                    settingsInfo("YORY account", "An account is optional. Create or sign in for cloud features; your keyboard, themes and local settings continue to work offline.")
                    Button("Connect account / sign out", action: onSignOut)
                        .listRowBackground(Rectangle().fill(.ultraThinMaterial))
                case "YORY Rewards", "Achievements":
                    settingsInfo("Coming to YORY", "This section is included in the settings structure. Rewards and achievement tracking are not connected in this preview.")
                case "Help & Feedback":
                    settingsInfo("Keyboard setup", "Enable YORY KEYS in Settings → General → Keyboard → Keyboards, then select it from the keyboard switcher.")
                    settingsInfo("Feedback", "Feedback submission is not connected in this preview.")
                default:
                    settingsInfo("YORY KEYS", "A privacy-first multilingual keyboard with customizable glass themes.")
                    settingsInfo("Preview build", "Version 0.1.0 · iOS preview")
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
    }

    private func languageRow(_ name: String, detail: String, tag: String) -> some View {
        Button {
            activeLanguage = tag
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(name).foregroundStyle(.white)
                    Text(detail).font(.footnote).foregroundStyle(.white.opacity(0.6))
                }
                Spacer()
                Image(systemName: activeLanguage == tag ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(activeLanguage == tag ? Color.blue : Color.white.opacity(0.35))
            }
            .padding(.vertical, 5)
        }
        .listRowBackground(Rectangle().fill(.ultraThinMaterial))
    }

    private func glassToggle(_ label: String, detail: String, isOn: Binding<Bool>) -> some View {
        Toggle(isOn: isOn) {
            VStack(alignment: .leading, spacing: 4) {
                Text(label).foregroundStyle(.white)
                Text(detail).font(.footnote).foregroundStyle(.white.opacity(0.58))
            }
        }
        .tint(Color(red: 0.62, green: 0.76, blue: 1.0))
        .listRowBackground(Rectangle().fill(.ultraThinMaterial))
    }

    private func settingsInfo(_ heading: String, _ detail: String) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(heading).foregroundStyle(.white).font(.body.weight(.medium))
            Text(detail).foregroundStyle(.white.opacity(0.63)).font(.footnote)
        }
        .padding(.vertical, 5)
        .listRowBackground(Rectangle().fill(.ultraThinMaterial))
    }
}

private func yoryGlassBackground() -> some View {
    LinearGradient(
        colors: [Color(red: 0.12, green: 0.16, blue: 0.25), Color(red: 0.025, green: 0.035, blue: 0.065), Color(red: 0.10, green: 0.09, blue: 0.17)],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    ).ignoresSafeArea()
}
