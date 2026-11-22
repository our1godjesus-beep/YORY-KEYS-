package app.yory.keys.ime

import android.content.Context
import java.util.Locale

/** Offline prefix-completion baseline. This is not a neural prediction model. */
class LocalWordSuggestions(private val context: Context) {
    private val commonWords = mapOf(
        "en" to listOf(
            "also", "another", "anyone", "anything", "answer", "around", "away",
            "back", "beautiful", "best", "better", "between", "big", "both", "bring",
            "business", "call", "called", "care", "change", "check", "choose", "clear",
            "close", "come", "computer", "correct", "correction", "could", "day",
            "different", "does", "doing", "done", "down", "early", "easy", "enough",
            "every", "everyone", "everything", "example", "fast", "feel", "first",
            "for", "found", "free", "full", "future", "game", "get", "give", "going",
            "great", "group", "happy", "hard", "has", "have", "help", "home", "hope",
            "house", "how", "idea", "important", "into", "just", "keep", "kind",
            "know", "large", "last", "later", "learn", "less", "life", "little",
            "long", "look", "looking", "made", "many", "match", "message", "more",
            "most", "much", "must", "name", "need", "never", "next", "number",
            "often", "other", "our", "over", "part", "people", "place", "please",
            "possible", "power", "quick", "quickly", "read", "ready", "real",
            "really", "right", "same", "screen", "see", "seem", "send", "service",
            "set", "settings", "small", "smooth", "some", "someone", "something",
            "sound", "start", "still", "sure", "take", "talk", "team", "technology",
            "than", "thank", "thanks", "their", "them", "then", "there", "these",
            "thing", "think", "those", "thought", "through", "time", "today",
            "together", "tomorrow", "too", "true", "try", "under", "understand",
            "until", "update", "use", "used", "using", "very", "want", "way",
            "well", "went", "were", "what", "when", "where", "which", "while",
            "whole", "why", "will", "with", "without", "word", "words", "work",
            "working", "world", "write", "writing", "wrong", "year", "you", "your"
        ),
        "ar" to listOf(
            "النهاردة", "النهارده", "الآن", "دلوقتي", "بكرة", "امبارح", "الليلة",
            "الكيبورد", "الرسالة", "رسالة", "رسائل", "المسافة", "مقاس", "مقاسات",
            "تغيير", "تعديل", "الإعدادات", "اعدادات", "المستخدم", "كلام", "كلمة",
            "كلمات", "معلومة", "معلومات", "بسرعة", "سريع", "سريعة", "مناسب",
            "مناسبة", "شكل", "شكله", "تمام", "ممتاز", "جميل", "حلو", "حلوة",
            "محتاج", "محتاجة", "محتاجين", "مفيش", "مش", "اللي", "علشان", "عشان",
            "عايز", "عايزة", "عايزين", "عندي", "عندك", "عندنا", "عندهم", "عامل",
            "عاملة", "عاملين", "ازاي", "إزاي", "ازيك", "إزيك", "ايه", "إيه",
            "ليه", "فين", "امتى", "إمتى", "طيب", "طب", "ماشي", "خلاص", "حاضر",
            "ينفع", "ممكن", "لازم", "برضه", "كمان", "كتير", "قليل", "أكتر",
            "أقل", "أحسن", "أفضل", "أريد", "عاوز", "عاوزة", "هنا", "هناك",
            "هذا", "هذه", "اللغة", "العربية", "الإنجليزية", "الكتابة", "التصحيح",
            "تلقائي", "تلقائيًا", "الأحرف", "الحروف", "المفاتيح", "الشاشة",
            "الارتفاع", "العرض", "المكان", "الحجم", "الوقت", "السرعة", "الدقة",
            "المشروع", "التطبيق", "التصميم", "احترافي", "احترافية", "شركة",
            "برنامج", "نظام", "نفس", "كل", "شيء", "حاجة", "حاجات", "بعض",
            "بشكل", "بالضبط", "مهم", "مميز", "مختلف", "جديد", "قديم", "صحيح",
            "غلط", "كبير", "صغير", "طويل", "قصير", "واسع", "ضيق", "أبيض",
            "أسود", "أزرق", "أحمر", "رمادي", "قوي", "قوية", "رائع", "رائعة"
        )
    )

    private val dictionaries = mapOf(
        "en" to (loadWords(context, "dictionaries/en-US.txt") + commonWords["en"].orEmpty()).distinct(),
        "ar" to (loadWords(context, "dictionaries/ar.txt") + commonWords["ar"].orEmpty()).distinct()
    )

    fun suggest(prefix: String, languageTag: String, limit: Int = 3): List<String> {
        val clean = prefix.trim().lowercase(Locale.ROOT)
        if (clean.length < 2) return emptyList()
        val language = languageTag.substringBefore('-').lowercase(Locale.ROOT)
        val personal = PersonalDictionaryStore.suggestions(context, clean, languageTag, limit)
        val bundled = dictionaries[language].orEmpty()
        val candidates = personal + bundled
        return YorySuggestionText.rankByTypedPrefix(candidates, clean)
            .take(limit.coerceIn(1, 5))
    }

    fun correct(rawWord: String, languageTag: String): String? =
        LocalAutocorrectMatcher.correct(
            rawWord = rawWord,
            languageTag = languageTag,
            candidates = dictionaries[languageTag.substringBefore('-').lowercase(Locale.ROOT)].orEmpty(),
            normalize = YorySuggestionText::normalizeForMatch
        )

    private fun loadWords(context: Context, path: String): List<String> = runCatching {
        context.assets.open(path).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.map(String::trim).filter { it.length in 2..32 }.toList()
        }
    }.getOrDefault(emptyList())
}
