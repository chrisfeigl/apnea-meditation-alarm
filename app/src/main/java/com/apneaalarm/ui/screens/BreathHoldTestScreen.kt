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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private enum class BreathHoldTimerState {
    NOT_STARTED, RUNNING, STOPPED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathHoldTestScreen(
    currentValue: Int,
    onNavigateBack: () -> Unit,
    onSave: (Int) -> Unit
) {
    var timerState by remember { mutableStateOf(BreathHoldTimerState.NOT_STARTED) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var startTime by remember { mutableLongStateOf(0L) }
    var showManualEntry by remember { mutableStateOf(false) }
    var manualEntryText by remember { mutableStateOf("") }

    LaunchedEffect(timerState) {
        if (timerState == BreathHoldTimerState.RUNNING) {
            startTime = System.currentTimeMillis()
            while (timerState == BreathHoldTimerState.RUNNING) {
                elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                delay(100)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Static Breath Hold Test") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("\u2190 Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Current value display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current M value",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = formatTime(currentValue),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showManualEntry) {
                // Manual entry UI
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Enter your max breath hold duration",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = manualEntryText,
                            onValueChange = { manualEntryText = it },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            suffix = { Text("seconds") },
                            placeholder = { Text("60") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showManualEntry = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    manualEntryText.toIntOrNull()?.let { seconds ->
                                        if (seconds in 10..600) {
                                            onSave(seconds)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = manualEntryText.toIntOrNull()?.let { it in 10..600 } == true
                            ) {
                                Text("Save")
                            }
                        }

                        if (manualEntryText.isNotEmpty() && manualEntryText.toIntOrNull()?.let { it in 10..600 } != true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enter a value between 10 and 600 seconds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Instructions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Your maximum static breath hold (M) is the longest time you can comfortably hold your breath while resting.\n\n" +
                                "To measure: sit or lie down, breathe normally for a minute (no hyperventilation), take a calm breath in and hold.\n\n" +
                                "Start the timer when you begin and stop it when you feel a strong urge to breathe. Don't push through discomfort.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Timer display
                Card(
                    modifier = Modifier.size(160.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (timerState) {
                            BreathHoldTimerState.RUNNING -> MaterialTheme.colorScheme.primaryContainer
                            BreathHoldTimerState.STOPPED -> MaterialTheme.colorScheme.secondaryContainer
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
                            style = MaterialTheme.typography.displayMedium,
                            color = when (timerState) {
                                BreathHoldTimerState.RUNNING -> MaterialTheme.colorScheme.onPrimaryContainer
                                BreathHoldTimerState.STOPPED -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (timerState == BreathHoldTimerState.RUNNING) {
                            Text(
                                text = "HOLDING",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                when (timerState) {
                    BreathHoldTimerState.NOT_STARTED -> {
                        Button(
                            onClick = { timerState = BreathHoldTimerState.RUNNING },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text("Start Timer", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    BreathHoldTimerState.RUNNING -> {
                        Button(
                            onClick = { timerState = BreathHoldTimerState.STOPPED },
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
                    BreathHoldTimerState.STOPPED -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Your breath hold: ${formatTime(elapsedSeconds)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        timerState = BreathHoldTimerState.NOT_STARTED
                                        elapsedSeconds = 0
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Text("Try Again")
                                }

                                Button(
                                    onClick = { onSave(elapsedSeconds) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    enabled = elapsedSeconds >= 10
                                ) {
                                    Text("Save")
                                }
                            }

                            if (elapsedSeconds < 10) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Hold for at least 10 seconds",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Manual entry option
                TextButton(onClick = { showManualEntry = true }) {
                    Text("Enter value manually")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
