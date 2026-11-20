import Foundation

/// Compact offline prefix suggestions for the first iOS keyboard slice.
/// This is a starter word list, not a trained prediction or autocorrect model.
enum YoryLocalWordSuggestions {
    private static let words: [String: [String]] = [
        "en": [
            "about", "above", "account", "actually", "after", "again", "always",
            "amazing", "another", "app", "application", "around", "awesome",
            "beautiful", "because", "before", "best", "better", "between",
            "build", "change", "choose", "complete", "create", "design",
            "different", "easy", "everything", "example", "experience",
            "features", "friend", "from", "future", "get", "give", "good",
            "great", "hello", "help", "here", "how", "idea", "important",
            "improve", "keyboard", "language", "like", "love", "make",
            "maybe", "more", "new", "nice", "now", "only", "please",
            "powerful", "project", "really", "right", "smooth", "something",
            "style", "technology", "thank", "that", "their", "there",
            "these", "thing", "this", "today", "together", "understand",
            "update", "very", "want", "welcome", "what", "when", "where",
            "which", "will", "with", "work", "world", "write", "yory"
        ],
        "ar": [
            "ابدا", "ابدأ", "أحتاج", "أحاول", "أريد", "أستطيع", "أستخدم",
            "أشياء", "أفضل", "أفكار", "أكثر", "الآن", "الأمر", "التطبيق",
            "التصميم", "التخصيص", "الحافظة", "الحروف", "الذكاء", "الخصوصية",
            "الكتابة", "الكلمات", "الكيبورد", "اللغة", "المستخدم", "المشروع",
            "المنصة", "النظام", "اليوم", "بالضبط", "بشكل", "بسرعة", "بعض",
            "بناء", "تطبيق", "تمام", "جديد", "جدا", "جميل", "حقيقي", "حلو",
            "خاصة", "خلال", "رائع", "سريع", "شكرا", "شكل", "صحيح", "صورة",
            "عربي", "عشان", "عندي", "فكرة", "فيه", "قوي", "كبير", "كمان",
            "كل", "لازم", "لذلك", "محتاج", "ممكن", "من", "مهم", "مميز",
            "مشروع", "مع", "نظام", "نفس", "هذا", "هذه", "هو", "هي", "يكون",
            "يمكن", "يلا"
        ]
    ]


    private static let personalWordsKey = "yory.personal.words.v1"
    private static let legacyClearRequestedKey = "yory.personal.words.clearLegacyRequested"
    private static var localDefaults: UserDefaults {
        UserDefaults(suiteName: "group.app.yory.keys") ?? .standard
    }

    /// Move an existing extension-private dictionary into App Group storage once.
    /// A host-app clear request takes precedence so legacy data cannot reappear afterward.
    private static func prepareLocalStorage() {
        let shared = localDefaults
        if shared.bool(forKey: legacyClearRequestedKey) {
            shared.removeObject(forKey: personalWordsKey)
            UserDefaults.standard.removeObject(forKey: personalWordsKey)
            shared.set(false, forKey: legacyClearRequestedKey)
            return
        }

        if shared.object(forKey: personalWordsKey) == nil,
           let legacy = UserDefaults.standard.dictionary(forKey: personalWordsKey) {
            shared.set(legacy, forKey: personalWordsKey)
            UserDefaults.standard.removeObject(forKey: personalWordsKey)
        }
    }

    /// Personal words remain local in the shared App Group container so the host app
    /// can honor a user's explicit clear-data request. No network sync is performed.
    static func learnAcceptedWord(_ rawWord: String, languageTag: String) {
        guard YoryThemeReader.personalLearningEnabled else { return }
        prepareLocalStorage()
        let word = rawWord.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard word.count >= 2, word.count <= 32,
              word.range(of: "^[\\p{L}\\p{M}]{2,32}$", options: .regularExpression) != nil else {
            return
        }
        let language = String(languageTag.lowercased().split(separator: "-").first ?? "en")
        let defaults = localDefaults
        var all = defaults.dictionary(forKey: personalWordsKey) as? [String: [String: Int]] ?? [:]
        var languageWords = all[language] ?? [:]
        languageWords[word] = min((languageWords[word] ?? 0) + 1, 100)
        let rankedWords = languageWords.sorted { $0.value > $1.value }.prefix(1500)
        var boundedWords: [String: Int] = [:]
        for entry in rankedWords {
            boundedWords[entry.key] = entry.value
        }
        languageWords = boundedWords
        all[language] = languageWords
        defaults.set(all, forKey: personalWordsKey)
    }

    static func clearPersonalWords() {
        localDefaults.removeObject(forKey: personalWordsKey)
        UserDefaults.standard.removeObject(forKey: personalWordsKey)
        localDefaults.set(false, forKey: legacyClearRequestedKey)
    }

    private static func personalSuggestions(prefix: String, language: String, limit: Int) -> [String] {
        guard YoryThemeReader.personalLearningEnabled else { return [] }
        prepareLocalStorage()
        let all = localDefaults.dictionary(forKey: personalWordsKey) as? [String: [String: Int]] ?? [:]
        return (all[language] ?? [:])
            .filter { $0.key.hasPrefix(prefix) && $0.key != prefix }
            .sorted { $0.value > $1.value }
            .prefix(max(1, min(limit, 5)))
            .map { $0.key }
    }

    static func suggestions(prefix: String, languageTag: String, limit: Int = 3) -> [String] {
        let clean = prefix.lowercased()
        guard !clean.isEmpty else { return [] }
        let language = String(languageTag.lowercased().split(separator: "-").first ?? "en")
        let personal = personalSuggestions(prefix: clean, language: language, limit: limit)
        let bundled = (words[language] ?? [])
            .filter { $0.hasPrefix(clean) && $0.caseInsensitiveCompare(clean) != .orderedSame }
        return (personal + bundled)
            .reduce(into: [String]()) { result, word in
                if !result.contains(where: { $0.caseInsensitiveCompare(word) == .orderedSame }) {
                    result.append(word)
                }
            }
            .prefix(max(1, min(limit, 5)))
            .map { $0 }
    }
}
