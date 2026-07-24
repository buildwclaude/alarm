package com.buildwclaude.alarm.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buildwclaude.alarm.data.AlarmEntity
import com.buildwclaude.alarm.data.RepeatDays
import com.buildwclaude.alarm.ui.AlarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    alarmId: Int?,
    onDone: () -> Unit,
    viewModel: AlarmViewModel = viewModel(),
) {
    var initial by remember { mutableStateOf<AlarmEntity?>(null) }
    LaunchedEffect(alarmId) {
        initial = if (alarmId == null || alarmId < 0) {
            AlarmEntity()
        } else {
            viewModel.load(alarmId) ?: AlarmEntity()
        }
    }

    val loaded = initial
    if (loaded == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    EditContent(loaded, viewModel, onDone)
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EditContent(alarm: AlarmEntity, viewModel: AlarmViewModel, onDone: () -> Unit) {
    val isNew = alarm.id == 0
    val timeState = rememberTimePickerState(
        initialHour = alarm.hour,
        initialMinute = alarm.minute,
        is24Hour = false,
    )
    var label by rememberSaveable { mutableStateOf(alarm.label) }
    var repeatMask by rememberSaveable { mutableStateOf(alarm.repeatMask) }
    var vibrate by rememberSaveable { mutableStateOf(alarm.vibrate) }
    var snooze by rememberSaveable { mutableStateOf(alarm.snoozeMinutes) }

    fun currentDraft() = alarm.copy(
        hour = timeState.hour,
        minute = timeState.minute,
        label = label.trim(),
        repeatMask = repeatMask,
        vibrate = vibrate,
        snoozeMinutes = snooze,
        enabled = true,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New alarm" else "Edit alarm") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { viewModel.delete(alarm); onDone() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimePicker(state = timeState)

            Spacer(Modifier.height(8.dp))
            // Plain-language safety net (full time-of-day version arrives in Step 3).
            Text(
                NextAlarmText.describe(timeState.hour, timeState.minute, repeatMask),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text("Repeat", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatDays.ORDER.forEachIndexed { index, day ->
                    val selected = RepeatDays.isSet(repeatMask, day)
                    FilterChip(
                        selected = selected,
                        onClick = { repeatMask = RepeatDays.toggle(repeatMask, day) },
                        label = { Text(RepeatDays.LABELS[index]) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SettingRow("Vibrate") {
                Switch(checked = vibrate, onCheckedChange = { vibrate = it })
            }

            Spacer(Modifier.height(8.dp))
            SettingRow("Snooze: $snooze min") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (snooze > 1) snooze-- }) { Text("–", style = MaterialTheme.typography.headlineSmall) }
                    IconButton(onClick = { if (snooze < 60) snooze++ }) { Text("+", style = MaterialTheme.typography.headlineSmall) }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.save(currentDraft()); onDone() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingRow(label: String, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        trailing()
    }
}
