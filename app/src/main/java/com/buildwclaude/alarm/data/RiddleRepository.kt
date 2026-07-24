package com.buildwclaude.alarm.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * Loads the bundled riddles and picks one at random per firing, avoiding repeats until the
 * pool (for the chosen difficulty) is exhausted. Recently-used ids persist via [SettingsStore].
 */
class RiddleRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val settings = SettingsStore(context)

    private val all: List<Riddle> by lazy { loadFromAssets() }

    private fun loadFromAssets(): List<Riddle> = try {
        val text = context.assets.open("riddles.json").bufferedReader().use { it.readText() }
        json.decodeFromString<List<Riddle>>(text)
    } catch (t: Throwable) {
        Log.e(TAG, "Failed to load riddles.json; using a fallback", t)
        listOf(
            Riddle(
                id = -1,
                question = "What has hands but cannot clap?",
                answer = "clock",
                acceptableAnswers = listOf("a clock", "clocks"),
                difficulty = "easy",
            ),
        )
    }

    /** Choose the next riddle, honouring the difficulty filter and the no-repeat rule. */
    suspend fun pickNext(): Riddle {
        val difficulty = settings.settings.first().riddleDifficulty
        val filtered = all.filter { difficulty == "any" || it.difficulty == difficulty }
            .ifEmpty { all }

        val used = settings.usedRiddleIds.first().toSet()
        var available = filtered.filter { it.id !in used }
        val newUsedBase: List<Int>
        if (available.isEmpty()) {
            // Pool exhausted — start a fresh cycle.
            available = filtered
            newUsedBase = emptyList()
        } else {
            newUsedBase = used.toList()
        }

        val chosen = available.random()
        runCatching { settings.setUsedRiddleIds(newUsedBase + chosen.id) }
        return chosen
    }

    companion object {
        private const val TAG = "RiddleRepository"
    }
}
