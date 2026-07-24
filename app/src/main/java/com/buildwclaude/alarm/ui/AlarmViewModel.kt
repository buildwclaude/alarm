package com.buildwclaude.alarm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buildwclaude.alarm.alarm.AlarmScheduler
import com.buildwclaude.alarm.data.AlarmEntity
import com.buildwclaude.alarm.data.AlarmRepository
import com.buildwclaude.alarm.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the alarm list and the add/edit screen. */
class AlarmViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AlarmRepository.get(app)
    private val scheduler = AlarmScheduler(app)
    val settingsStore = SettingsStore(app)

    val alarms: StateFlow<List<AlarmEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun load(id: Int): AlarmEntity? = repo.getById(id)

    /** A blank alarm pre-filled with the user's default vibrate/snooze settings. */
    suspend fun newAlarmDefaults(): AlarmEntity {
        val s = settingsStore.settings.first()
        return AlarmEntity(vibrate = s.vibrateDefault, snoozeMinutes = s.defaultSnoozeMinutes)
    }

    fun save(alarm: AlarmEntity) {
        viewModelScope.launch {
            val id = repo.upsert(alarm)
            val saved = repo.getById(id) ?: alarm.copy(id = id)
            if (saved.enabled) scheduler.schedule(saved) else scheduler.cancel(saved.id)
        }
    }

    fun toggle(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            repo.setEnabled(alarm.id, enabled)
            val updated = alarm.copy(enabled = enabled)
            if (enabled) scheduler.schedule(updated) else scheduler.cancel(alarm.id)
        }
    }

    fun delete(alarm: AlarmEntity) {
        viewModelScope.launch {
            scheduler.cancel(alarm.id)
            repo.delete(alarm)
        }
    }

    fun canScheduleExact(): Boolean = scheduler.canScheduleExact()
}
