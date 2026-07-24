package com.buildwclaude.alarm.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buildwclaude.alarm.data.AppSettings
import com.buildwclaude.alarm.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)

    val settings: StateFlow<AppSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setSnooze(min: Int) = launch { store.setSnooze(min.coerceIn(1, 60)) }
    fun setFlash(on: Boolean) = launch { store.setFlash(on) }
    fun setVibrateDefault(on: Boolean) = launch { store.setVibrateDefault(on) }
    fun setGradualVolume(on: Boolean) = launch { store.setGradualVolume(on) }
    fun setDifficulty(d: String) = launch { store.setDifficulty(d) }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }
}
