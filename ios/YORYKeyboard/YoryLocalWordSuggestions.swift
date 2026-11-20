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

    static func suggestions(prefix: String, languageTag: String, limit: Int = 3) -> [String] {
        let clean = prefix.lowercased()
        guard !clean.isEmpty else { return [] }
        let language = languageTag.lowercased().hasPrefix("ar") ? "ar" : "en"
        return (words[language] ?? [])
            .filter { $0.hasPrefix(clean) && $0.caseInsensitiveCompare(clean) != .orderedSame }
            .prefix(max(1, min(limit, 5)))
            .map { $0 }
    }
}
