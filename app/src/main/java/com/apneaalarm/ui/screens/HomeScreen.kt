package com.apneaalarm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apneaalarm.data.UserPreferences

@Composable
fun HomeScreen(
    preferences: UserPreferences,
    isSessionActive: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToSession: () -> Unit,
    onStartSession: (skipIntro: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Apnea Alarm",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Return to active session button
        if (isSessionActive) {
            Button(
                onClick = onNavigateToSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(
                    text = "RETURN TO SESSION",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Session summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Session Summary",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${preferences.numberOfIntervals} breath-hold cycles",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "Hold: ${formatDuration(preferences.breathHoldDurationSeconds)} each",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "~${estimateSessionDuration(preferences)} min total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (preferences.alarmEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Alarm: ${String.format("%02d:%02d", preferences.alarmHour, preferences.alarmMinute)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Only show start buttons if no session is active
        if (!isSessionActive) {
            // Start button
            Button(
                onClick = { onStartSession(false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "START SESSION",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Start without intro button
            OutlinedButton(
                onClick = { onStartSession(true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start (Skip Intro)")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Settings button
        OutlinedButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Settings")
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}

private fun estimateSessionDuration(preferences: UserPreferences): Int {
    // 54s intro bowl + 3s silence + 3s countdown + N cycles of (hold + breathe time)
    val holdTime = preferences.breathHoldDurationSeconds

    var totalBreathTime = 0
    for (i in 0 until preferences.numberOfIntervals - 1) {
        totalBreathTime += preferences.breathingIntervalDuration(i)
    }

    val totalSeconds = 54 + 3 + 3 + (holdTime * preferences.numberOfIntervals) + totalBreathTime
    return (totalSeconds / 60) + 1
}
