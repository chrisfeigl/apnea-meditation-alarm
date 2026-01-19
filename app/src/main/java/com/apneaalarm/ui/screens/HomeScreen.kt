package com.apneaalarm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.apneaalarm.data.IntensityLevel
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

        // Session summary card with intensity color coding
        val intensityColor = when (preferences.intensityLevel) {
            IntensityLevel.CALM -> Color(0xFF4CAF50)           // Green
            IntensityLevel.CHALLENGING -> Color(0xFFFFC107)    // Amber
            IntensityLevel.HARD_TRAINING -> Color(0xFFFF9800)    // Orange
            IntensityLevel.ADVANCED -> Color(0xFFF44336)       // Red
        }
        val intensityContentColor = when (preferences.intensityLevel) {
            IntensityLevel.CALM -> Color.White
            IntensityLevel.CHALLENGING -> Color.Black
            IntensityLevel.HARD_TRAINING -> Color.Black
            IntensityLevel.ADVANCED -> Color.White
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = intensityColor
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header row with title and intensity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Session Summary",
                        style = MaterialTheme.typography.titleMedium,
                        color = intensityContentColor
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Intensity ${preferences.intensityFactor}",
                            style = MaterialTheme.typography.titleLarge,
                            color = intensityContentColor
                        )
                        Text(
                            text = preferences.intensityLevel.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = intensityContentColor.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Breath Holds",
                            style = MaterialTheme.typography.labelSmall,
                            color = intensityContentColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${preferences.numberOfIntervals} x ${preferences.breathHoldDurationSeconds}s",
                            style = MaterialTheme.typography.bodyLarge,
                            color = intensityContentColor
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = intensityContentColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatDuration(preferences.totalSessionTimeSeconds),
                            style = MaterialTheme.typography.bodyLarge,
                            color = intensityContentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Breathing Intervals",
                    style = MaterialTheme.typography.labelSmall,
                    color = intensityContentColor.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Show all breathing intervals
                val intervals = (0 until preferences.numberOfIntervals).map { i ->
                    preferences.breathingIntervalDuration(i)
                }

                Text(
                    text = intervals.joinToString(" \u2192 ") { "${it}s" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = intensityContentColor
                )

                preferences.getNextAlarmInfo()?.let { (dayName, time) ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Next alarm: $dayName at $time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = intensityContentColor
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
