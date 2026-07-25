package com.buildwclaude.alarm.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buildwclaude.alarm.alarm.Permissions
import com.buildwclaude.alarm.ui.AlarmViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * One-time setup prompts shown above the alarm list: the Android 12+ exact-alarm
 * permission, and the Samsung battery-optimisation deep-link. Both re-check whenever the
 * user returns to the app, so they disappear once handled.
 */
@Composable
fun SetupBanners(viewModel: AlarmViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Bump this on resume to re-read the OS permission state.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }

    val batteryDismissed by remember {
        viewModel.settingsStore.settings.map { it.batteryCardDismissed }
    }.collectAsStateWithLifecycle(initialValue = true)

    Column(Modifier.padding(horizontal = 16.dp)) {
        val canExact = remember(refresh) { Permissions.canScheduleExactAlarms(context) }
        if (!canExact) {
            SetupCard(
                icon = Icons.Filled.Schedule,
                title = "Allow exact alarms",
                body = "Android needs your permission to fire alarms at the exact minute. " +
                    "Without it, alarms may be delayed or skipped.",
                actionLabel = "Open setting",
                onAction = { Permissions.openExactAlarmSettings(context) },
            )
            Spacer(Modifier.height(12.dp))
        }

        val batteryOk = remember(refresh) { Permissions.isIgnoringBatteryOptimizations(context) }
        if (!batteryOk && !batteryDismissed) {
            SetupCard(
                icon = Icons.Filled.BatteryAlert,
                title = "Keep alarms alive on Samsung",
                body = "One UI can close background apps and silence alarms. Tap Allow to let " +
                    "this app run in the background so alarms always ring. (For extra safety, " +
                    "also set it to \"Unrestricted\" under Settings › Battery.)",
                actionLabel = "Allow",
                onAction = { Permissions.requestIgnoreBatteryOptimizations(context) },
                onDismiss = { scope.launch { viewModel.settingsStore.setBatteryCardDismissed(true) } },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SetupCard(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.height(0.dp))
                Text(
                    "  $title",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (onDismiss != null) {
                    TextButton(onClick = onDismiss) { Text("Not now") }
                    Spacer(Modifier.height(0.dp))
                }
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
