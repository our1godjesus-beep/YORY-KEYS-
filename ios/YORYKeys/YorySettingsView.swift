import SwiftUI
import Network

private let yorySettingsStore = UserDefaults(suiteName: "group.app.yory.keys")

private struct YorySettingsItem: Identifiable {
    let title: String
    let subtitle: String
    let symbol: String
    var id: String { title }
}

struct ContentView: View {
    let onSignOut: () -> Void
    let canOfferRegistration: Bool
    let onRegister: () -> Void
    @StateObject private var networkStatus = YoryNetworkReachability()
    @AppStorage("yory.registration.invite.nextAllowed") private var nextRegistrationInviteAt = 0.0
    @State private var isRegistrationInvitePresented = false

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
                        YorySettingsDetailView(
                            title: title,
                            onSignOut: onSignOut,
                            canOfferRegistration: canOfferRegistration,
                            onRegister: onRegister
                        )
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
        .task {
            try? await Task.sleep(nanoseconds: 1_500_000_000)
            evaluateRegistrationInvitation()
        }
        .onChange(of: networkStatus.isOnline) { isOnline in
            guard isOnline else { return }
            Task {
                try? await Task.sleep(nanoseconds: 1_500_000_000)
                evaluateRegistrationInvitation()
            }
        }
        .onChange(of: canOfferRegistration) { canOffer in
            guard canOffer else { return }
            Task {
                try? await Task.sleep(nanoseconds: 1_500_000_000)
                evaluateRegistrationInvitation()
            }
        }
        .sheet(isPresented: $isRegistrationInvitePresented) {
            YoryRegistrationInviteSheet {
                isRegistrationInvitePresented = false
                onRegister()
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
    }

    private func evaluateRegistrationInvitation() {
        guard canOfferRegistration, networkStatus.isOnline, !isRegistrationInvitePresented else { return }
        let now = Date().timeIntervalSince1970
        guard now >= nextRegistrationInviteAt else { return }
        // Save the cooldown before presentation so relaunching cannot show the invite repeatedly.
        nextRegistrationInviteAt = now + (30 * 24 * 60 * 60)
        isRegistrationInvitePresented = true
    }
}

private struct YorySettingsDetailView: View {
    let title: String
    let onSignOut: () -> Void
    let canOfferRegistration: Bool
    let onRegister: () -> Void

    @AppStorage("yory.active.language", store: yorySettingsStore) private var activeLanguage = "en-US"
    @State private var addedLanguageTags: [String] = {
        let stored = yorySettingsStore?.stringArray(forKey: "yory.added.languages.v1") ?? ["en-US", "ar"]
        return stored.isEmpty ? ["en-US", "ar"] : stored
    }()
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
    @AppStorage("yory.typing.autoCorrect", store: yorySettingsStore) private var autoCorrect = true
    @AppStorage("yory.typing.autoCaps", store: yorySettingsStore) private var autoCaps = true
    @AppStorage("soundEnabled", store: yorySettingsStore) private var soundEnabled = true
    @AppStorage("hapticsEnabled", store: yorySettingsStore) private var hapticsEnabled = true
    @AppStorage("personalLearningEnabled", store: yorySettingsStore) private var personalLearningEnabled = false
    @AppStorage("yory.input.compactMode", store: yorySettingsStore) private var compactMode = false
    @State private var showClearLearningConfirmation = false
    @State private var privacyMessage = ""

    var body: some View {
        ZStack {
            yoryGlassBackground()
            List {
                switch title {
                case "Languages":
                    Section("YOUR LANGUAGES") {
                        languageRow("English (US)", detail: "QWERTY · starter word list", tag: "en-US", isAdded: addedLanguageTags.contains("en-US"))
                        languageRow("العربية (مصر)", detail: "Arabic 123 (PC) · starter word list", tag: "ar", isAdded: addedLanguageTags.contains("ar"))
                        languageRow("Deutsch (DE)", detail: "QWERTZ · layout only", tag: "de", isAdded: addedLanguageTags.contains("de"))
                        languageRow("Español", detail: "QWERTY · layout only", tag: "es", isAdded: addedLanguageTags.contains("es"))
                        languageRow("Français", detail: "AZERTY · layout only", tag: "fr", isAdded: addedLanguageTags.contains("fr"))
                        languageRow("Português", detail: "QWERTY · layout only", tag: "pt", isAdded: addedLanguageTags.contains("pt"))
                        languageRow("Italiano", detail: "QWERTY · layout only", tag: "it", isAdded: addedLanguageTags.contains("it"))
                    }
                    Section {
                        Text("Tap a language name to make it active. Add languages to include them in the spacebar swipe cycle. The keyboard switches only through languages you add here. English and Arabic have starter word lists; the remaining installed layouts are available for typing and local personal learning.")
                            .font(.footnote).foregroundStyle(.white.opacity(0.65))
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
                        glassToggle("Auto-correction", detail: "Correct confident local spelling errors when you insert a space or punctuation mark. Offline packs are currently available for English and Arabic.", isOn: $autoCorrect)
                        glassToggle("Word suggestions", detail: "Show local word suggestions while typing", isOn: $wordSuggestions)
                        glassToggle("Auto-capitalization", detail: "Capitalize the first letter of sentences", isOn: $autoCaps)
                        glassToggle("Personal dictionary", detail: "Learn accepted words locally on this device", isOn: $personalLearningEnabled)
                        settingsInfo("iOS supplementary lexicon", "The iOS system-provided lexicon may include saved names and text shortcuts. YORY can use it for local suggestions and corrections; it is not uploaded by YORY.")
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
                    Section("LOCAL DATA") {
                        Button(role: .destructive) {
                            showClearLearningConfirmation = true
                        } label: {
                            Label("Clear personal dictionary", systemImage: "trash")
                        }
                        Text("The dictionary is stored locally for the keyboard extension and can be cleared here.")
                            .font(.footnote)
                            .foregroundStyle(.white.opacity(0.62))
                        if !privacyMessage.isEmpty {
                            Text(privacyMessage)
                                .font(.footnote)
                                .foregroundStyle(Color(red: 0.65, green: 0.78, blue: 1.0))
                        }
                    }
                case "Account":
                    settingsInfo("YORY account", "An account is optional. Your keyboard, themes and local settings continue to work offline.")
                    if canOfferRegistration {
                        Button("Create account / sign in", action: onRegister)
                            .listRowBackground(Rectangle().fill(.ultraThinMaterial))
                    } else {
                        Button("Sign out", role: .destructive, action: onSignOut)
                            .listRowBackground(Rectangle().fill(.ultraThinMaterial))
                    }
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
        .confirmationDialog(
            "Clear your personal dictionary?",
            isPresented: $showClearLearningConfirmation,
            titleVisibility: .visible
        ) {
            Button("Clear dictionary", role: .destructive) {
                yorySettingsStore?.removeObject(forKey: "yory.personal.words.v1")
                yorySettingsStore?.set(true, forKey: "yory.personal.words.clearLegacyRequested")
                privacyMessage = "Personal dictionary cleared from this device. Legacy local data will also be removed when the keyboard next opens."
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This deletes the locally learned words. The Personal Learning setting will remain unchanged.")
        }
        .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
    }

    private func languageRow(_ name: String, detail: String, tag: String, isAdded: Bool) -> some View {
        HStack(spacing: 12) {
            Button {
                activeLanguage = tag
                if !addedLanguageTags.contains(tag) {
                    addedLanguageTags.append(tag)
                    yorySettingsStore?.set(addedLanguageTags, forKey: "yory.added.languages.v1")
                }
            } label: {
                VStack(alignment: .leading, spacing: 4) {
                    Text(name).foregroundStyle(.white)
                    Text(detail).font(.footnote).foregroundStyle(.white.opacity(0.6))
                    if activeLanguage == tag {
                        Text("Active layout").font(.caption2.weight(.semibold)).foregroundStyle(Color(red: 0.65, green: 0.78, blue: 1.0))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            Button {
                setLanguageAdded(tag, enabled: !addedLanguageTags.contains(tag))
            } label: {
                VStack(spacing: 4) {
                    Image(systemName: isAdded ? "checkmark.circle.fill" : "plus.circle")
                        .font(.system(size: 20, weight: .semibold))
                    Text(isAdded ? "Added" : "Add")
                        .font(.caption2.weight(.medium))
                }
                .foregroundStyle(isAdded ? Color(red: 0.65, green: 0.78, blue: 1.0) : .white.opacity(0.62))
                .frame(width: 54, height: 48)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(isAdded ? "Remove \(name) from keyboard" : "Add \(name) to keyboard")
        }
        .padding(.vertical, 5)
        .listRowBackground(Rectangle().fill(.ultraThinMaterial))
    }

    private func setLanguageAdded(_ tag: String, enabled: Bool) {
        if enabled {
            if !addedLanguageTags.contains(tag) { addedLanguageTags.append(tag) }
        } else {
            guard addedLanguageTags.count > 1 else { return }
            addedLanguageTags.removeAll { $0 == tag }
        }
        yorySettingsStore?.set(addedLanguageTags, forKey: "yory.added.languages.v1")
        if !enabled && activeLanguage == tag {
            activeLanguage = addedLanguageTags.first ?? "en-US"
        }
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


// MARK: - Registration invitation

private final class YoryNetworkReachability: ObservableObject {
    @Published private(set) var isOnline = false
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "app.yory.registration-reachability")

    init() {
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                self?.isOnline = path.status == .satisfied
            }
        }
        monitor.start(queue: queue)
    }

    deinit {
        monitor.cancel()
    }
}

private struct YoryRegistrationInviteCopy {
    let title: String
    let message: String
    let privacy: String
    let primary: String
    let secondary: String
}

private func yoryRegistrationInviteCopy() -> YoryRegistrationInviteCopy {
    let preferred = Locale.preferredLanguages.first ?? Locale.current.identifier
    let locale = Locale(identifier: preferred)
    let language = locale.languageCode ?? "en"
    let region = Locale.current.regionCode ?? locale.regionCode ?? ""
    let supported = Set(["ar", "de", "es", "fr", "it", "pt", "en"])
    let resolved = yoryInviteLanguage(for: region) ?? (supported.contains(language) ? language : "en")

    switch resolved {
    case "ar":
        return YoryRegistrationInviteCopy(
            title: "تجربتك مع YORY لسه قدامها المزيد",
            message: "أنشئ حساب YORY لتكون مستعدًا للخدمات السحابية القادمة عند إطلاقها.",
            privacy: "ستظل لوحة المفاتيح متاحة دون إنترنت. كتاباتك وقاموسك المحلي يظلان على جهازك.",
            primary: "إنشاء حساب",
            secondary: "ليس الآن"
        )
    case "de":
        return YoryRegistrationInviteCopy(
            title: "Mit YORY geht noch mehr",
            message: "Erstelle ein YORY-Konto, um für kommende Cloud-Funktionen bereit zu sein.",
            privacy: "Deine Tastatur bleibt offline verfügbar. Deine Texte und dein lokales Wörterbuch bleiben auf deinem Gerät.",
            primary: "Konto erstellen",
            secondary: "Nicht jetzt"
        )
    case "es":
        return YoryRegistrationInviteCopy(
            title: "Tu experiencia con YORY puede crecer",
            message: "Crea una cuenta YORY para estar preparado cuando lleguen las próximas funciones en la nube.",
            privacy: "El teclado seguirá funcionando sin conexión. Tus textos y tu diccionario local permanecen en tu dispositivo.",
            primary: "Crear cuenta",
            secondary: "Ahora no"
        )
    case "fr":
        return YoryRegistrationInviteCopy(
            title: "Votre expérience YORY peut aller plus loin",
            message: "Créez un compte YORY pour être prêt lorsque les prochaines fonctions cloud seront disponibles.",
            privacy: "Votre clavier reste disponible hors ligne. Vos textes et votre dictionnaire personnel restent sur votre appareil.",
            primary: "Créer un compte",
            secondary: "Pas maintenant"
        )
    case "it":
        return YoryRegistrationInviteCopy(
            title: "Con YORY c'è ancora molto da scoprire",
            message: "Crea un account YORY per essere pronto quando arriveranno le prossime funzioni cloud.",
            privacy: "La tastiera continuerà a funzionare offline. I tuoi testi e il dizionario personale restano sul dispositivo.",
            primary: "Crea account",
            secondary: "Non ora"
        )
    case "pt":
        return YoryRegistrationInviteCopy(
            title: "Sua experiência com YORY pode ir além",
            message: "Crie uma conta YORY para estar pronto quando os próximos recursos na nuvem chegarem.",
            privacy: "Seu teclado continuará funcionando offline. Seus textos e seu dicionário pessoal ficam no dispositivo.",
            primary: "Criar conta",
            secondary: "Agora não"
        )
    default:
        return YoryRegistrationInviteCopy(
            title: "There's more room to grow with YORY",
            message: "Create a YORY account to be ready for upcoming cloud features when they arrive.",
            privacy: "Your keyboard stays available offline. Your typing and local dictionary remain on this device.",
            primary: "Create account",
            secondary: "Not now"
        )
    }
}

private func yoryInviteLanguage(for region: String) -> String? {
    switch region.uppercased() {
    case "EG", "SA", "AE", "JO", "KW", "QA", "BH", "OM", "IQ", "MA", "DZ", "TN", "LY", "YE":
        return "ar"
    case "DE", "AT":
        return "de"
    case "ES", "MX", "AR", "CO", "CL", "PE", "VE", "UY", "EC":
        return "es"
    case "FR", "BE", "MC":
        return "fr"
    case "IT":
        return "it"
    case "PT", "BR", "AO", "MZ":
        return "pt"
    default:
        return nil
    }
}

private struct YoryRegistrationInviteSheet: View {
    @Environment(\.dismiss) private var dismiss
    let onRegister: () -> Void
    private let copy = yoryRegistrationInviteCopy()

    private let mint = Color(red: 0.44, green: 0.84, blue: 0.78)
    private let violet = Color(red: 0.66, green: 0.61, blue: 0.98)

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.075, green: 0.095, blue: 0.13), Color(red: 0.045, green: 0.055, blue: 0.08)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView(.vertical) {
                VStack(spacing: 15) {
                Image(systemName: "icloud.and.arrow.up")
                    .font(.system(size: 27, weight: .medium))
                    .foregroundStyle(mint)
                    .frame(width: 62, height: 62)
                    .background(mint.opacity(0.12), in: Circle())
                    .overlay(Circle().stroke(mint.opacity(0.28), lineWidth: 1))

                Text("YORY STUDIO")
                    .font(.system(size: 10, weight: .bold, design: .rounded))
                    .tracking(2.1)
                    .foregroundStyle(violet)

                Text(copy.title)
                    .font(.system(size: 23, weight: .semibold, design: .rounded))
                    .foregroundStyle(Color(red: 0.96, green: 0.96, blue: 0.95))
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)

                Text(copy.message)
                    .font(.system(size: 14, weight: .regular))
                    .lineSpacing(3)
                    .foregroundStyle(.white.opacity(0.72))
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)

                Button {
                    dismiss()
                    onRegister()
                } label: {
                    Text(copy.primary)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(Color(red: 0.04, green: 0.12, blue: 0.12))
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(mint, in: RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)

                Button(copy.secondary) {
                    dismiss()
                }
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(.white.opacity(0.88))
                .frame(maxWidth: .infinity)
                .frame(height: 40)

                Text(copy.privacy)
                    .font(.system(size: 11))
                    .lineSpacing(2)
                    .foregroundStyle(.white.opacity(0.50))
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal, 26)
            .padding(.top, 18)
            .padding(.bottom, 22)
            .frame(maxWidth: 520)
                }
            }
        }
    }
}
