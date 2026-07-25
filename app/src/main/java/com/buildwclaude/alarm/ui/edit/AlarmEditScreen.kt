package com.buildwclaude.alarm.ui.edit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buildwclaude.alarm.alarm.Permissions
import com.buildwclaude.alarm.data.AlarmEntity
import com.buildwclaude.alarm.data.RepeatDays
import com.buildwclaude.alarm.ui.AlarmViewModel
import com.buildwclaude.alarm.ui.band.TimeBand

@Composable
fun AlarmEditScreen(
    alarmId: Int?,
    onDone: () -> Unit,
    viewModel: AlarmViewModel = viewModel(),
) {
    var initial by remember { mutableStateOf<AlarmEntity?>(null) }
    LaunchedEffect(alarmId) {
        initial = if (alarmId == null || alarmId < 0) {
            viewModel.newAlarmDefaults()
        } else {
            viewModel.load(alarmId) ?: viewModel.newAlarmDefaults()
        }
    }
    val loaded = initial
    if (loaded == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    EditContent(loaded, viewModel, onDone)
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EditContent(alarm: AlarmEntity, viewModel: AlarmViewModel, onDone: () -> Unit) {
    val isNew = alarm.id == 0

    // Standard Android time picker state — reliably reports the exact hour/minute.
    val timeState = rememberTimePickerState(
        initialHour = alarm.hour,
        initialMinute = alarm.minute,
        is24Hour = false,
    )
    var textEntry by rememberSaveable { mutableStateOf(false) }
    var label by rememberSaveable { mutableStateOf(alarm.label) }
    var repeatMask by rememberSaveable { mutableStateOf(alarm.repeatMask) }
    var vibrate by rememberSaveable { mutableStateOf(alarm.vibrate) }
    var snooze by rememberSaveable { mutableStateOf(alarm.snoozeMinutes) }

    val hour24 = timeState.hour
    val minute = timeState.minute
    val minutesOfDay = hour24 * 60 + minute
    val pal = TimeBand.forMinutes(minutesOfDay).palette()

    val c0 by animateColorAsState(pal.skyTop, tween(300), label = "c0")
    val c1 by animateColorAsState(pal.skyMid, tween(300), label = "c1")
    val c2 by animateColorAsState(pal.skyBottom, tween(300), label = "c2")
    val onColor by animateColorAsState(pal.onColor, tween(300), label = "on")
    val starAlpha = if (pal.isDark) 1f else 0f

    val context = LocalContext.current
    fun draft() = alarm.copy(
        hour = hour24, minute = minute, label = label.trim(),
        repeatMask = repeatMask, vibrate = vibrate, snoozeMinutes = snooze, enabled = true,
    )
    fun saveAndConfirm() {
        viewModel.save(draft())
        val whenText = NextAlarmText.describe(hour24, minute, repeatMask)
        val warn = if (Permissions.canScheduleExactAlarms(context)) "" else "  ⚠ Allow exact alarms to ring."
        android.widget.Toast.makeText(context, "$whenText$warn", android.widget.Toast.LENGTH_LONG).show()
        onDone()
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c0, c1, c2)))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // Transparent top bar over the sky.
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDone) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = onColor)
                }
                Text(
                    if (isNew) "New alarm" else "Edit alarm",
                    color = onColor,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (!isNew) {
                    IconButton(onClick = { viewModel.delete(alarm); onDone() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = onColor)
                    }
                }
            }

            // Slim time-of-day band (the sun/moon cue) — supportive, not the input.
            Box(Modifier.fillMaxWidth().height(120.dp)) {
                SkyView(minutesOfDay, starAlpha, Modifier.fillMaxSize())
            }

            // Everything the user actually touches sits on a normal, readable surface.
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Header row with a dial <-> keyboard toggle, like the standard clock app.
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Set time",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { textEntry = !textEntry }) {
                            Icon(
                                if (textEntry) Icons.Filled.Schedule else Icons.Filled.Keyboard,
                                contentDescription = if (textEntry) "Use clock dial" else "Type the time",
                            )
                        }
                    }

                    if (textEntry) {
                        TimeInput(state = timeState)
                    } else {
                        TimePicker(state = timeState)
                    }

                    Spacer(Modifier.height(8.dp))
                    // Plain-language confirmation — the final AM/PM safety net.
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            NextAlarmText.describe(hour24, minute, repeatMask),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Repeat",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RepeatDays.ORDER.forEachIndexed { index, day ->
                            FilterChip(
                                selected = RepeatDays.isSet(repeatMask, day),
                                onClick = { repeatMask = RepeatDays.toggle(repeatMask, day) },
                                label = { Text(RepeatDays.LABELS[index]) },
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    SettingRow("Vibrate") { Switch(checked = vibrate, onCheckedChange = { vibrate = it }) }

                    Spacer(Modifier.height(8.dp))
                    SettingRow("Snooze: $snooze min") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (snooze > 1) snooze-- }) {
                                Text("–", style = MaterialTheme.typography.headlineSmall)
                            }
                            IconButton(onClick = { if (snooze < 60) snooze++ }) {
                                Text("+", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { saveAndConfirm() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Save")
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
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
