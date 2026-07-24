package com.buildwclaude.alarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** App-wide settings and small bits of persisted state, backed by Jetpack DataStore. */
data class AppSettings(
    val defaultSnoozeMinutes: Int = 5,
    val flashEnabled: Boolean = true,
    val vibrateDefault: Boolean = true,
    val gradualVolume: Boolean = true,
    val riddleDifficulty: String = "any", // "easy" | "medium" | "hard" | "any"
    val batteryCardDismissed: Boolean = false,
)

class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            defaultSnoozeMinutes = p[SNOOZE] ?: 5,
            flashEnabled = p[FLASH] ?: true,
            vibrateDefault = p[VIBRATE] ?: true,
            gradualVolume = p[GRADUAL] ?: true,
            riddleDifficulty = p[DIFFICULTY] ?: "any",
            batteryCardDismissed = p[BATTERY_CARD] ?: false,
        )
    }

    suspend fun setSnooze(min: Int) = edit { it[SNOOZE] = min }
    suspend fun setFlash(on: Boolean) = edit { it[FLASH] = on }
    suspend fun setVibrateDefault(on: Boolean) = edit { it[VIBRATE] = on }
    suspend fun setGradualVolume(on: Boolean) = edit { it[GRADUAL] = on }
    suspend fun setDifficulty(d: String) = edit { it[DIFFICULTY] = d }
    suspend fun setBatteryCardDismissed(done: Boolean) = edit { it[BATTERY_CARD] = done }

    // Recently-used riddle ids, stored as a comma-separated list.
    val usedRiddleIds: Flow<List<Int>> = context.dataStore.data.map { p ->
        (p[USED_RIDDLES] ?: "").split(",").mapNotNull { it.trim().toIntOrNull() }
    }
    suspend fun setUsedRiddleIds(ids: List<Int>) =
        edit { it[USED_RIDDLES] = ids.joinToString(",") }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    companion object {
        private val SNOOZE = intPreferencesKey("default_snooze")
        private val FLASH = booleanPreferencesKey("flash_enabled")
        private val VIBRATE = booleanPreferencesKey("vibrate_default")
        private val GRADUAL = booleanPreferencesKey("gradual_volume")
        private val DIFFICULTY = stringPreferencesKey("riddle_difficulty")
        private val BATTERY_CARD = booleanPreferencesKey("battery_card_dismissed")
        private val USED_RIDDLES = stringPreferencesKey("used_riddle_ids")
    }
}
