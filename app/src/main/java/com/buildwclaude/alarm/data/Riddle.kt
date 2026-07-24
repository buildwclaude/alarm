package com.buildwclaude.alarm.data

import kotlinx.serialization.Serializable

@Serializable
data class Riddle(
    val id: Int,
    val question: String,
    val answer: String,
    val acceptableAnswers: List<String> = emptyList(),
    val difficulty: String = "medium",
) {
    /** Every accepted spelling, normalised for forgiving comparison. */
    fun normalizedAnswers(): Set<String> =
        (listOf(answer) + acceptableAnswers).map { RiddleMatching.normalize(it) }.toSet()

    fun isCorrect(input: String): Boolean {
        val n = RiddleMatching.normalize(input)
        return n.isNotEmpty() && n in normalizedAnswers()
    }
}

/** Case/punctuation/article-insensitive answer matching. */
object RiddleMatching {
    private val ARTICLES = listOf("the ", "a ", "an ")

    fun normalize(raw: String): String {
        var s = raw.lowercase().trim()
        s = s.replace(Regex("[^a-z0-9 ]"), " ")   // strip punctuation
        s = s.replace(Regex("\\s+"), " ").trim()   // collapse whitespace
        for (article in ARTICLES) {
            if (s.startsWith(article)) { s = s.removePrefix(article).trim(); break }
        }
        return s
    }
}
