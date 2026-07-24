package com.buildwclaude.alarm.alarm

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buildwclaude.alarm.data.Riddle
import com.buildwclaude.alarm.data.RiddleRepository
import com.buildwclaude.alarm.ui.theme.RiddleAlarmTheme
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * The screen shown over the lock screen when an alarm rings. You must solve a riddle to
 * stop it. After it is solved (Step 5 adds the flash), Snooze / Dismiss appear.
 */
class AlarmActivity : ComponentActivity() {

    /** True once the user has chosen snooze/dismiss, so onStop must not relaunch us. */
    private var resolving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* swallow — can't back out of a ringing alarm */ }
        })

        setContent {
            RiddleAlarmTheme(darkTheme = true, dynamicColor = false) {
                AlarmFlow(
                    onSolved = { AlarmService.sendAction(this, AlarmContract.ACTION_SILENCE) },
                    onSnooze = { resolveWith(AlarmContract.ACTION_SNOOZE) },
                    onDismiss = { resolveWith(AlarmContract.ACTION_DISMISS) },
                )
            }
        }
    }

    private fun resolveWith(action: String) {
        resolving = true
        AlarmService.sendAction(this, action)
        finish()
    }

    override fun onStop() {
        super.onStop()
        // Anti-cheat: if the alarm is still active and the user tried to leave, come back.
        if (!resolving && AlarmService.Active.isActive && !isFinishing) {
            AlarmService.sendAction(this, AlarmContract.ACTION_RELAUNCH)
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

private enum class Phase { RIDDLE, RESOLVE }

@Composable
private fun AlarmFlow(onSolved: () -> Unit, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    var phase by remember { mutableStateOf(Phase.RIDDLE) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Crossfade(targetState = phase, animationSpec = tween(400), label = "phase") { p ->
            when (p) {
                Phase.RIDDLE -> RiddleScreen(onSolvedOrSkipped = {
                    onSolved()
                    phase = Phase.RESOLVE
                })
                Phase.RESOLVE -> ResolveScreen(onSnooze = onSnooze, onDismiss = onDismiss)
            }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun RiddleScreen(onSolvedOrSkipped: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var riddle by remember { mutableStateOf(AlarmService.Active.riddle) }
    var answer by remember { mutableStateOf("") }
    var wrongAttempts by remember { mutableIntStateOf(0) }
    var showWrong by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }

    // Skip hatch appears only after a few minutes (based on ring start, survives relaunch).
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            val start = AlarmService.Active.ringStartElapsed.takeIf { it > 0 } ?: SystemClock.elapsedRealtime()
            elapsed = SystemClock.elapsedRealtime() - start
            delay(1000)
        }
    }
    val showSkip = elapsed >= AlarmContract.SKIP_RIDDLE_AFTER_MS

    // Pick a riddle once per firing.
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        if (riddle == null) {
            val r = runCatching { RiddleRepository(context).pickNext() }.getOrNull()
            if (r != null) { AlarmService.Active.riddle = r; riddle = r }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus(); keyboard?.show() }
    LaunchedEffect(wrongAttempts) {
        if (wrongAttempts > 0) {
            shake.snapTo(0f)
            listOf(18f, -16f, 12f, -10f, 6f, 0f).forEach { shake.animateTo(it, tween(45)) }
        }
    }

    fun submit() {
        val r = riddle ?: return
        if (r.isCorrect(answer)) {
            keyboard?.hide()
            onSolvedOrSkipped()
        } else {
            showWrong = true
            answer = ""
            wrongAttempts++
        }
    }

    val r = riddle
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        var now by remember { mutableStateOf(LocalTime.now()) }
        LaunchedEffect(Unit) { while (true) { now = LocalTime.now(); delay(1000) } }
        Text(
            now.format(DateTimeFormatter.ofPattern("h:mm a")),
            fontSize = 40.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(36.dp))

        if (r == null) {
            CircularProgressIndicator()
        } else {
            Text(
                r.question,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it; showWrong = false },
                singleLine = true,
                placeholder = { Text("Your answer") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shake.value.roundToInt(), 0) }
                    .focusRequester(focusRequester),
            )
            Spacer(Modifier.height(12.dp))
            if (showWrong) {
                Text(
                    "Not quite — try again",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
            }
            Button(onClick = { submit() }, modifier = Modifier.fillMaxWidth()) { Text("Answer") }
        }

        Spacer(Modifier.height(40.dp))
        if (showSkip) {
            // Small, unglamorous escape hatch for genuine emergencies.
            TextButton(onClick = { keyboard?.hide(); onSolvedOrSkipped() }) {
                Text(
                    "Skip riddle",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun ResolveScreen(onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val snoozeMin = AlarmService.Active.snoozeMinutes
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        var now by remember { mutableStateOf(LocalTime.now()) }
        LaunchedEffect(Unit) { while (true) { now = LocalTime.now(); delay(1000) } }
        Text(
            now.format(DateTimeFormatter.ofPattern("h:mm a")),
            fontSize = 52.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Riddle solved. Good morning!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) { Text("Dismiss", fontSize = 20.sp) }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
        ) { Text("Snooze ($snoozeMin min)", fontSize = 20.sp) }
    }
}
