package com.buildwclaude.alarm.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buildwclaude.alarm.data.AlarmEntity
import com.buildwclaude.alarm.data.RepeatDays
import com.buildwclaude.alarm.ui.AlarmViewModel
import com.buildwclaude.alarm.ui.TimeText
import com.buildwclaude.alarm.ui.neu.AnalogClock
import com.buildwclaude.alarm.ui.neu.Neu
import com.buildwclaude.alarm.ui.neu.NeuSwitch
import com.buildwclaude.alarm.ui.neu.neuRaised
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: AlarmViewModel = viewModel(),
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Neu.Background)) {
        Column(Modifier.fillMaxSize()) {
            // Header: serif title + soft settings button.
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 28.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Alarm",
                    fontFamily = FontFamily.Serif,
                    fontSize = 34.sp,
                    color = Neu.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                NeuIconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Neu.TextPrimary)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item { SetupBanners(viewModel) }

                item {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(8.dp))
                        AnalogClock(diameter = 250.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMMM d, yyyy")),
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            color = Neu.TextSecondary,
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }

                if (alarms.isEmpty()) {
                    item { EmptyState() }
                } else {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { viewModel.toggle(alarm, it) },
                            onClick = { onEditAlarm(alarm.id) },
                            onDelete = { viewModel.delete(alarm) },
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }

        // Floating soft "+" button.
        NeuFab(
            onClick = onAddAlarm,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp),
        )
    }
}

@Composable
private fun NeuIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .neuRaised(cornerRadius = 24.dp, offset = 5.dp, blur = 10.dp)
            .clip(CircleShape)
            .combinedClickableCompat(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun NeuFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(66.dp)
            .neuRaised(cornerRadius = 33.dp, surface = Neu.Surface, offset = 7.dp, blur = 14.dp)
            .clip(CircleShape)
            .combinedClickableCompat(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Add alarm", tint = Neu.Accent, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No alarms yet", fontFamily = FontFamily.Serif, fontSize = 22.sp, color = Neu.TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("Tap + to add your first alarm", fontSize = 14.sp, color = Neu.TextSecondary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete alarm?") },
            text = { Text("This alarm will be removed.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .alpha(if (alarm.enabled) 1f else 0.45f)
            .neuRaised(cornerRadius = 22.dp)
            .combinedClickable(onClick = onClick, onLongClick = { confirmDelete = true })
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    TimeText.hourMinute(alarm.hour, alarm.minute),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    color = Neu.TextPrimary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    TimeText.amPm(alarm.hour),
                    fontSize = 15.sp,
                    color = Neu.TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            val subtitle = buildString {
                if (alarm.label.isNotBlank()) append(alarm.label).append(" · ")
                append(RepeatDays.summary(alarm.repeatMask))
            }
            Text(subtitle, fontFamily = FontFamily.Serif, fontSize = 13.sp, color = Neu.TextSecondary)
        }
        NeuSwitch(checked = alarm.enabled, onCheckedChange = onToggle)
    }
}

/** combinedClickable with only an onClick, kept tidy for the icon buttons. */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit): Modifier =
    this.combinedClickable(onClick = onClick)
