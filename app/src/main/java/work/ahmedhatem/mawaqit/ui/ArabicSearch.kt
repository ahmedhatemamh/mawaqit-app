package work.ahmedhatem.mawaqit.ui

/** Makes Arabic spellings comparable: hamza forms of alif, taa marbuta, alif maqsura, diacritics and spaces. */
object ArabicSearch {
    fun normalize(text: String): String = buildString {
        for (c in text.trim()) {
            when (c) {
                'أ', 'إ', 'آ', 'ٱ' -> append('ا')
                'ة' -> append('ه')
                'ى' -> append('ي')
                'ـ', 'ً', 'ٌ', 'ٍ', 'َ', 'ُ', 'ِ', 'ّ', 'ْ' -> Unit
                else -> append(c.lowercaseChar())
            }
        }
    }.replace(Regex("\\s+"), " ")

    fun matches(name: String, query: String): Boolean {
        val q = normalize(query)
        return q.isEmpty() || normalize(name).contains(q)
    }
}
