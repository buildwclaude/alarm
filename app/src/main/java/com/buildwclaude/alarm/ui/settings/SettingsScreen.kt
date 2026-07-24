package com.buildwclaude.alarm.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buildwclaude.alarm.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val s by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            SectionTitle("Alarm behaviour")
            StepperRow(
                label = "Default snooze",
                value = "${s.defaultSnoozeMinutes} min",
                onMinus = { viewModel.setSnooze(s.defaultSnoozeMinutes - 1) },
                onPlus = { viewModel.setSnooze(s.defaultSnoozeMinutes + 1) },
            )
            SwitchRow("Vibrate by default", s.vibrateDefault) { viewModel.setVibrateDefault(it) }
            SwitchRow(
                "Gradual volume",
                s.gradualVolume,
                subtitle = "Fade the alarm in over a few seconds instead of starting at full volume.",
            ) { viewModel.setGradualVolume(it) }

            SectionDivider()
            SectionTitle("Riddle")
            Text(
                "Difficulty",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            DifficultyChips(current = s.riddleDifficulty, onSelect = { viewModel.setDifficulty(it) })

            SectionDivider()
            SectionTitle("Wake-up flash")
            SwitchRow(
                "Flash blue/red/green after the riddle",
                s.flashEnabled,
            ) { viewModel.setFlash(it) }
            FlashWarning()

            SectionDivider()
            SectionTitle("About")
            Text("Riddle Alarm", style = MaterialTheme.typography.titleMedium)
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "A reliable, fully-offline alarm clock. The set-alarm screen looks like the time " +
                    "of day you're setting, you solve a riddle to stop the alarm, and the screen " +
                    "flashes to wake you up. To change the alarm tone, replace " +
                    "res/raw/alarm_default.ogg and rebuild (see the project README).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    subtitle: String? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onMinus) { Text("–", style = MaterialTheme.typography.headlineSmall) }
        Text(value, style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = onPlus) { Text("+", style = MaterialTheme.typography.headlineSmall) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultyChips(current: String, onSelect: (String) -> Unit) {
    val options = listOf("any" to "Any", "easy" to "Easy", "medium" to "Medium", "hard" to "Hard")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, labelText) ->
            FilterChip(
                selected = current == key,
                onClick = { onSelect(key) },
                label = { Text(labelText) },
            )
        }
    }
}

@Composable
private fun FlashWarning() {
    Row(Modifier.padding(top = 8.dp)) {
        Icon(
            Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.height(20.dp),
        )
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(
            "The flash shows solid blue, red and green in sequence. It is kept below 3 flashes " +
                "per second to stay within photosensitive-seizure safety limits, but if flashing " +
                "lights affect you, turn this off.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
