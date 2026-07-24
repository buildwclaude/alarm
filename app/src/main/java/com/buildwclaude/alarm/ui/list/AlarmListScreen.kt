package com.buildwclaude.alarm.ui.list

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buildwclaude.alarm.data.AlarmEntity
import com.buildwclaude.alarm.data.RepeatDays
import com.buildwclaude.alarm.ui.AlarmViewModel
import com.buildwclaude.alarm.ui.TimeText
import com.buildwclaude.alarm.ui.band.TimeBand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: AlarmViewModel = viewModel(),
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarms") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarm) {
                Icon(Icons.Filled.Add, contentDescription = "Add alarm")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SetupBanners(viewModel)

            if (alarms.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { viewModel.toggle(alarm, it) },
                            onClick = { onEditAlarm(alarm.id) },
                            onDelete = { viewModel.delete(alarm) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No alarms yet", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap + to add your first alarm",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    // Subtle day/night tint so each card hints at its time of day.
    val band = TimeBand.forHour(alarm.hour)
    val tint = band.cardTint()

    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete alarm?") },
            text = { Text("This alarm will be removed.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = tint),
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = onClick,
            onLongClick = { confirmDelete = true },
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp).alpha(if (alarm.enabled) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        TimeText.hourMinute(alarm.hour, alarm.minute),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        TimeText.amPm(alarm.hour),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                val subtitle = buildString {
                    if (alarm.label.isNotBlank()) append(alarm.label).append(" · ")
                    append(RepeatDays.summary(alarm.repeatMask))
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}
