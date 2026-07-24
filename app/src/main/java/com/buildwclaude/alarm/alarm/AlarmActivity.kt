package com.buildwclaude.alarm.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.buildwclaude.alarm.ui.theme.RiddleAlarmTheme
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The screen that appears over the lock screen when an alarm rings.
 *
 * Step 2: shows the time with Snooze / Dismiss buttons and blocks the back button.
 * Later steps replace the buttons with the riddle, then the flash sequence.
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()

        // Block the back button from dismissing the alarm.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* swallow */ }
        })

        setContent {
            RiddleAlarmTheme {
                RingingScreen(
                    onSnooze = { AlarmService.sendAction(this, AlarmContract.ACTION_SNOOZE); finish() },
                    onDismiss = { AlarmService.sendAction(this, AlarmContract.ACTION_DISMISS); finish() },
                )
            }
        }
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
private fun RingingScreen(onSnooze: () -> Unit, onDismiss: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        var now by remember { mutableStateOf(LocalTime.now()) }
        LaunchedEffect(Unit) {
            while (true) { now = LocalTime.now(); delay(1000) }
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = now.format(DateTimeFormatter.ofPattern("h:mm")),
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = now.format(DateTimeFormatter.ofPattern("a")),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(48.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Dismiss")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth()) {
                Text("Snooze")
            }
        }
    }
}
