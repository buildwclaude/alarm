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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    // Time is tracked as a 12-hour index (0=12 … 11=11), a minute, and AM/PM.
    var hourIndex by rememberSaveableInt(alarm.hour % 12)
    var minute by rememberSaveableInt(alarm.minute)
    var isPm by remember { mutableStateOf(alarm.hour >= 12) }

    var label by remember { mutableStateOf(alarm.label) }
    var repeatMask by rememberSaveableInt(alarm.repeatMask)
    var vibrate by remember { mutableStateOf(alarm.vibrate) }
    var snooze by rememberSaveableInt(alarm.snoozeMinutes)

    val hour24 = to24(hourIndex, isPm)
    val minutesOfDay = hour24 * 60 + minute
    val band = TimeBand.forMinutes(minutesOfDay)
    val pal = band.palette()

    // ~300 ms cross-fades between palettes — no jarring snaps.
    val c0 by animateColorAsState(pal.skyTop, tween(300), label = "c0")
    val c1 by animateColorAsState(pal.skyMid, tween(300), label = "c1")
    val c2 by animateColorAsState(pal.skyBottom, tween(300), label = "c2")
    val onColor by animateColorAsState(pal.onColor, tween(300), label = "on")
    val starAlpha = if (pal.isDark) 1f else 0f

    fun draft() = alarm.copy(
        hour = hour24, minute = minute, label = label.trim(),
        repeatMask = repeatMask, vibrate = vibrate, snoozeMinutes = snooze, enabled = true,
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    fun saveAndConfirm() {
        viewModel.save(draft())
        val whenText = NextAlarmText.describe(hour24, minute, repeatMask)
        val exact = if (com.buildwclaude.alarm.alarm.Permissions.canScheduleExactAlarms(context)) {
            ""
        } else {
            "  ⚠ Allow exact alarms so it can ring."
        }
        android.widget.Toast.makeText(context, "Alarm set · $whenText$exact", android.widget.Toast.LENGTH_LONG).show()
        onDone()
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c0, c1, c2)))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // Transparent top bar over the sky.
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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

            // Sky + scroll-wheel picker.
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                SkyView(minutesOfDay, starAlpha, Modifier.fillMaxSize())

                // Faint highlight behind the centred row.
                Box(
                    Modifier.align(Alignment.Center).fillMaxWidth().height(60.dp)
                        .padding(horizontal = 24.dp)
                        .background(onColor.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                )

                Row(
                    Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    WheelPicker(
                        count = 12,
                        initialIndex = alarm.hour % 12,
                        label = { if (it == 0) "12" else it.toString() },
                        onCentered = { hourIndex = it },
                        color = onColor,
                        modifier = Modifier.width(84.dp),
                    )
                    Text(":", color = onColor, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    WheelPicker(
                        count = 60,
                        initialIndex = alarm.minute,
                        label = { "%02d".format(it) },
                        onCentered = { minute = it },
                        color = onColor,
                        modifier = Modifier.width(84.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    AmPmToggle(isPm = isPm, onColor = onColor, onChange = { isPm = it })
                }
            }

            // Plain-language safety net.
            Text(
                NextAlarmText.describe(hour24, minute, repeatMask),
                color = onColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            )

            // The rest of the form sits on the normal themed surface for readability.
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Repeat", style = MaterialTheme.typography.labelLarge)
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
private fun AmPmToggle(isPm: Boolean, onColor: Color, onChange: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AmPmPill("AM", selected = !isPm, onColor = onColor) { onChange(false) }
        Spacer(Modifier.height(10.dp))
        AmPmPill("PM", selected = isPm, onColor = onColor) { onChange(true) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmPmPill(text: String, selected: Boolean, onColor: Color, onClick: () -> Unit) {
    val bg = if (selected) onColor else Color.Transparent
    val fg = if (selected) invert(onColor) else onColor.copy(alpha = 0.6f)
    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, onColor.copy(alpha = if (selected) 1f else 0.4f)),
        onClick = onClick,
    ) {
        Text(
            text,
            color = fg,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )
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

/** rememberSaveable that reliably persists an Int across rotation. */
@Composable
private fun rememberSaveableInt(initial: Int) =
    androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initial) }

private fun to24(hourIndex: Int, isPm: Boolean): Int = when {
    !isPm && hourIndex == 0 -> 0     // 12 AM
    !isPm -> hourIndex               // 1–11 AM
    isPm && hourIndex == 0 -> 12     // 12 PM
    else -> hourIndex + 12           // 1–11 PM
}

/** Pick black or white to contrast against [c] for the selected AM/PM pill text. */
private fun invert(c: Color): Color {
    val luminance = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
    return if (luminance > 0.5f) Color(0xFF101010) else Color.White
}
