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
            "يمكن", "يلا",
            "ازيك", "إزيك", "ازيكوا", "إزيكوا", "ايه", "إيه", "عايز", "عايزة", "عايزين",
            "عامل", "عاملة", "عاملين", "كده", "ده", "دي", "دول", "مش", "مفيش", "ليه",
            "ازاي", "إزاي", "ازايك", "دلوقتي", "امبارح", "بكرة", "النهارده", "بقى",
            "برضه", "عشان", "علشان", "أوي", "قوي", "معلش", "ماشي", "خلاص", "حاضر",
            "ينفع", "فين", "امتى", "إمتى", "طب", "طيب", "محتاج", "محتاجة", "محتاجين",
            "لسه", "لسة", "كتير", "أهلا", "اهلا", "منور", "منورة", "حلوة", "حلوين"
        ]
    ]


    private static let extraWords: [String: [String]] = [
        "en": [
            "also", "anyone", "anything", "answer", "away", "back", "beautiful",
            "business", "call", "called", "care", "check", "clear", "close",
            "computer", "correct", "correction", "could", "day", "does", "doing",
            "down", "early", "enough", "every", "everyone", "fast", "feel",
            "first", "found", "free", "full", "game", "going", "group", "happy",
            "hard", "home", "hope", "house", "into", "just", "keep", "kind",
            "know", "large", "last", "later", "learn", "less", "life", "little",
            "long", "look", "looking", "made", "many", "match", "message", "most",
            "must", "name", "need", "never", "next", "number", "often", "other",
            "over", "part", "people", "place", "possible", "power", "quick",
            "quickly", "read", "ready", "real", "same", "screen", "see", "send",
            "service", "settings", "small", "someone", "sound", "start", "still",
            "take", "talk", "team", "typing", "writing", "tomorrow", "together",
            "until", "using", "way", "well", "went", "were", "while", "whole",
            "without", "word", "words", "working", "wrong", "year", "your"
        ],
        "ar": [
            "النهاردة", "النهارده", "دلوقتي", "بكرة", "امبارح", "الكيبورد",
            "الرسالة", "رسالة", "رسائل", "المسافة", "مقاس", "مقاسات", "تغيير",
            "تعديل", "الإعدادات", "اعدادات", "المستخدم", "كلام", "كلمة", "كلمات",
            "معلومة", "معلومات", "بسرعة", "سريع", "سريعة", "مناسب", "مناسبة",
            "شكله", "تمام", "ممتاز", "جميل", "محتاج", "محتاجة", "محتاجين",
            "مفيش", "اللي", "علشان", "عاوز", "عاوزة", "عامل", "عاملة", "فين",
            "امتى", "طب", "ماشي", "حاضر", "برضه", "كتير", "قليل", "أكتر",
            "أقل", "أحسن", "العربية", "الإنجليزية", "التصحيح", "تلقائي",
            "تلقائيًا", "المفاتيح", "الشاشة", "الارتفاع", "العرض", "المكان",
            "الحجم", "السرعة", "الدقة", "احترافي", "احترافية", "شركة", "برنامج",
            "نظام", "حاجة", "حاجات", "بعض", "غلط", "واسع", "ضيق", "رائع", "رائعة"
        ]
    ]

    private static let commonCorrections: [String: [String: String]] = [
        "en": [
            "teh": "the", "adn": "and", "thier": "their", "becuase": "because",
            "recieve": "receive", "freind": "friend", "definately": "definitely",
            "seperate": "separate", "wierd": "weird", "untill": "until",
            "goverment": "government", "occured": "occurred", "adress": "address",
            "langauge": "language", "keybaord": "keyboard", "taht": "that",
            "whcih": "which", "hlelo": "hello", "wrold": "world", "mesage": "message"
        ],
        "ar": [
            "لاكن": "لكن", "هاذا": "هذا", "هاذه": "هذه", "الغه": "اللغة",
            "الكيبور": "الكيبورد", "التصمبم": "التصميم", "المشروغ": "المشروع",
            "بسرعه": "بسرعة", "الان": "الآن"
        ]
    ]

    private static let personalWordsKey = "yory.personal.words.v1"
    private static let legacyClearRequestedKey = "yory.personal.words.clearLegacyRequested"
    private static var cachedPersonalWords: [String: [String: Int]]?
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
            cachedPersonalWords = [:]
            return
        }

        if shared.object(forKey: personalWordsKey) == nil,
           let legacy = UserDefaults.standard.dictionary(forKey: personalWordsKey) {
            shared.set(legacy, forKey: personalWordsKey)
            UserDefaults.standard.removeObject(forKey: personalWordsKey)
            cachedPersonalWords = legacy as? [String: [String: Int]]
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
        cachedPersonalWords = all
    }

    static func clearPersonalWords() {
        cachedPersonalWords = [:]
        localDefaults.removeObject(forKey: personalWordsKey)
        UserDefaults.standard.removeObject(forKey: personalWordsKey)
        localDefaults.set(false, forKey: legacyClearRequestedKey)
    }

    private static func personalSuggestions(prefix: String, language: String, limit: Int) -> [String] {
        guard YoryThemeReader.personalLearningEnabled else { return [] }
        prepareLocalStorage()
        if cachedPersonalWords == nil {
            cachedPersonalWords = localDefaults.dictionary(forKey: personalWordsKey) as? [String: [String: Int]] ?? [:]
        }
        let all = cachedPersonalWords ?? [:]
        return (all[language] ?? [:])
            .filter { matchesPrefix($0.key, prefix: prefix) }
            .sorted { $0.value > $1.value }
            .prefix(max(1, min(limit, 5)))
            .map { $0.key }
    }

    /// Search-only Arabic normalization. Returned words keep their original spelling.
    private static func normalizeForMatch(_ raw: String) -> String {
        var normalized = String.UnicodeScalarView()
        for scalar in raw.precomposedStringWithCanonicalMapping.unicodeScalars {
            let value = scalar.value
            if value == 0x0640 ||
                (value >= 0x064B && value <= 0x065F) ||
                value == 0x0670 ||
                (value >= 0x06D6 && value <= 0x06ED) {
                continue
            }
            switch value {
            case 0x0623, 0x0625, 0x0622, 0x0671: normalized.append(UnicodeScalar(0x0627)!)
            case 0x06CC: normalized.append(UnicodeScalar(0x064A)!)
            case 0x06A9: normalized.append(UnicodeScalar(0x0643)!)
            default: normalized.append(scalar)
            }
        }
        return String(normalized).lowercased()
    }

    private static func matchesPrefix(_ candidate: String, prefix: String) -> Bool {
        let normalizedCandidate = normalizeForMatch(candidate)
        let normalizedPrefix = normalizeForMatch(prefix)
        return !normalizedPrefix.isEmpty &&
            normalizedCandidate != normalizedPrefix &&
            normalizedCandidate.hasPrefix(normalizedPrefix)
    }

    static func correct(_ rawWord: String, languageTag: String) -> String? {
        let word = rawWord.trimmingCharacters(in: .whitespacesAndNewlines)
        guard (3...32).contains(word.count) else { return nil }
        let language = String(languageTag.lowercased().split(separator: "-").first ?? "en")
        let folded = word.lowercased()
        if let explicit = commonCorrections[language]?[folded] {
            return preserveCase(word, replacement: explicit)
        }

        let candidates = (words[language] ?? []) + (extraWords[language] ?? [])
        let normalizedInput = normalizeForMatch(word)
        if candidates.contains(where: { normalizeForMatch($0) == normalizedInput }) {
            return nil
        }

        var best: [String] = []
        var bestDistance = 2
        for candidate in candidates {
            if abs(candidate.count - word.count) > 1 { continue }
            guard let distance = editDistanceAtMostOne(folded, candidate.lowercased()) else { continue }
            if distance < bestDistance {
                bestDistance = distance
                best = [candidate]
            } else if distance == bestDistance && !best.contains(where: { $0.caseInsensitiveCompare(candidate) == .orderedSame }) {
                best.append(candidate)
            }
            if bestDistance == 1 && best.count > 1 { break }
        }
        guard bestDistance == 1, best.count == 1 else { return nil }
        return preserveCase(word, replacement: best[0])
    }

    private static func editDistanceAtMostOne(_ first: String, _ second: String) -> Int? {
        let left = Array(first)
        let right = Array(second)
        if left == right { return 0 }
        if abs(left.count - right.count) > 1 { return nil }
        if left.count == right.count {
            let indexes = left.indices.filter { left[$0] != right[$0] }
            if indexes.count == 1 { return 1 }
            if indexes.count == 2 {
                let a = indexes[0]
                let b = indexes[1]
                if b == a + 1 && left[a] == right[b] && left[b] == right[a] { return 1 }
            }
            return nil
        }
        let shorter = left.count < right.count ? left : right
        let longer = left.count < right.count ? right : left
        var i = 0
        var j = 0
        var edits = 0
        while i < shorter.count && j < longer.count {
            if shorter[i] == longer[j] {
                i += 1; j += 1
            } else {
                edits += 1
                if edits > 1 { return nil }
                j += 1
            }
        }
        if j < longer.count || i < shorter.count { edits += 1 }
        return edits == 1 ? 1 : nil
    }

    private static func preserveCase(_ original: String, replacement: String) -> String {
        if original == original.uppercased() { return replacement.uppercased() }
        guard let first = original.first, first.isUppercase else { return replacement }
        return replacement.prefix(1).uppercased() + replacement.dropFirst()
    }

    static func suggestions(prefix: String, languageTag: String, limit: Int = 3) -> [String] {
        let clean = prefix.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return [] }
        let language = String(languageTag.lowercased().split(separator: "-").first ?? "en")
        let personal = personalSuggestions(prefix: clean, language: language, limit: limit)
        let bundled = ((words[language] ?? []) + (extraWords[language] ?? []))
            .filter { matchesPrefix($0, prefix: clean) }
        var direct: [String] = []
        var normalizedMatches: [String] = []
        var seen = Set<String>()
        for word in personal + bundled {
            let unique = word.lowercased()
            guard seen.insert(unique).inserted, matchesPrefix(word, prefix: clean) else { continue }
            if word.lowercased().hasPrefix(clean.lowercased()) {
                direct.append(word)
            } else {
                normalizedMatches.append(word)
            }
        }
        return Array((direct + normalizedMatches).prefix(max(1, min(limit, 5))))
    }
}
