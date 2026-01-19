package com.apneaalarm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apneaalarm.data.TrainingMode
import kotlinx.coroutines.delay

// Default values used when skipping setup
private const val DEFAULT_BREATH_HOLD_SECONDS = 60

@Composable
fun WelcomeScreen(
    onComplete: (TrainingMode, Int) -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    var selectedMode by remember { mutableStateOf<TrainingMode?>(null) }
    var recordedBreathHold by remember { mutableIntStateOf(0) }

    when (currentPage) {
        0 -> WelcomeIntroPage(
            onNext = { currentPage = 1 }
        )
        1 -> RelaxationModePage(
            onBack = { currentPage = 0 },
            onNext = { currentPage = 2 }
        )
        2 -> IntenseModePage(
            onBack = { currentPage = 1 },
            onNext = { currentPage = 3 }
        )
        3 -> ModeSelectionPage(
            onBack = { currentPage = 2 },
            onModeSelected = { mode ->
                selectedMode = mode
                currentPage = 4
            },
            onSkip = {
                // Skip with default Relaxation mode
                onComplete(TrainingMode.RELAXATION, DEFAULT_BREATH_HOLD_SECONDS)
            }
        )
        4 -> BreathHoldTimerPage(
            onBack = { currentPage = 3 },
            onComplete = { seconds ->
                recordedBreathHold = seconds
                selectedMode?.let { mode ->
                    onComplete(mode, seconds)
                }
            },
            onSkip = {
                // Skip with selected mode and default breath hold
                val mode = selectedMode ?: TrainingMode.RELAXATION
                onComplete(mode, DEFAULT_BREATH_HOLD_SECONDS)
            }
        )
    }
}

@Composable
private fun WelcomeIntroPage(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "This app supports two distinct ways of training with breath holds, and it's important to choose the one that matches your intention.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Next", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun RelaxationModePage(
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button at top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = onBack) {
                Text("\u2190 Back")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Relaxation Mode",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "Relaxation / Meditation mode is designed to calm the nervous system: breath holds are kept well below your maximum, recovery time is generous at first, and carbon dioxide builds slowly.\n\nMost people finish these sessions feeling steadier, quieter, and more relaxed than when they started.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(24.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Next", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun IntenseModePage(
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button at top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = onBack) {
                Text("\u2190 Back")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Intense Mode",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "Intense CO\u2082 Training mode is designed for performance and tolerance: breath holds are closer to your maximum, recovery times shorten quickly, and discomfort appears earlier.\n\nThis mode builds resilience and CO\u2082 tolerance but is physically and mentally demanding, and should be used sparingly.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(24.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Next", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ModeSelectionPage(
    onBack: () -> Unit,
    onModeSelected: (TrainingMode) -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button at top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = onBack) {
                Text("\u2190 Back")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Choose Your Path",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Choose calm when you want regulation;\nchoose intense when you want adaptation.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { onModeSelected(TrainingMode.RELAXATION) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = "Relaxation / Meditation",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { onModeSelected(TrainingMode.INTENSE) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(
                text = "Intense CO\u2082 Training",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Skip option and note
        Text(
            text = "You can change this later in Settings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onSkip) {
            Text("Skip for now")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BreathHoldTimerPage(
    onBack: () -> Unit,
    onComplete: (Int) -> Unit,
    onSkip: () -> Unit
) {
    var timerState by remember { mutableStateOf(TimerState.NOT_STARTED) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var startTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(timerState) {
        if (timerState == TimerState.RUNNING) {
            startTime = System.currentTimeMillis()
            while (timerState == TimerState.RUNNING) {
                elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                delay(100)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button at top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = onBack) {
                Text("\u2190 Back")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Record Your Breath Hold",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "Before the app can generate a safe and accurate training plan, it needs to know your maximum static breath hold.\n\n" +
                        "This is simply the longest time you can comfortably hold your breath while resting, without moving, swimming, or pushing through panic.\n\n" +
                        "To measure it, sit or lie down somewhere safe and relaxed. Breathe normally for a minute or two\u2014do not hyperventilate\u2014then take a single calm breath in and hold.\n\n" +
                        "Start the timer when you begin the hold and stop it the moment you feel a strong, clear urge to breathe or when the hold becomes uncomfortable. There is no benefit to forcing it longer.\n\n" +
                        "The app's timer will record this duration, and it will be used only to scale your training safely to your own physiology.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Timer display
        Card(
            modifier = Modifier.size(180.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (timerState) {
                    TimerState.RUNNING -> MaterialTheme.colorScheme.primaryContainer
                    TimerState.STOPPED -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatTime(elapsedSeconds),
                    style = MaterialTheme.typography.displayLarge,
                    color = when (timerState) {
                        TimerState.RUNNING -> MaterialTheme.colorScheme.onPrimaryContainer
                        TimerState.STOPPED -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (timerState == TimerState.RUNNING) {
                    Text(
                        text = "HOLDING",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (timerState) {
            TimerState.NOT_STARTED -> {
                Button(
                    onClick = { timerState = TimerState.RUNNING },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Start Timer", style = MaterialTheme.typography.titleMedium)
                }
            }
            TimerState.RUNNING -> {
                Button(
                    onClick = { timerState = TimerState.STOPPED },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Stop", style = MaterialTheme.typography.titleMedium)
                }
            }
            TimerState.STOPPED -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your breath hold: ${formatTime(elapsedSeconds)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                timerState = TimerState.NOT_STARTED
                                elapsedSeconds = 0
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Text("Try Again")
                        }

                        Button(
                            onClick = { onComplete(elapsedSeconds) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            enabled = elapsedSeconds >= 10
                        ) {
                            Text("Continue")
                        }
                    }

                    if (elapsedSeconds < 10) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Hold for at least 10 seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Skip option and note
        Text(
            text = "You can change this later in Settings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onSkip) {
            Text("Skip for now (use 60s default)")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private enum class TimerState {
    NOT_STARTED, RUNNING, STOPPED
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
