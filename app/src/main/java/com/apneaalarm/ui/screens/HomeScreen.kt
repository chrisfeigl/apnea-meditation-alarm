package com.apneaalarm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apneaalarm.data.Alarm
import com.apneaalarm.data.IntensityLevel
import com.apneaalarm.data.SessionSettings
import com.apneaalarm.data.UserPreferences

@Composable
fun HomeScreen(
    preferences: UserPreferences,
    alarms: List<Alarm>,
    isSessionActive: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToSession: () -> Unit,
    onNavigateToNewSession: () -> Unit,
    onNavigateToSavedSessions: () -> Unit,
    onNavigateToAlarmEdit: (Long) -> Unit,
    onRepeatLastSession: () -> Unit
) {
    val globalM = preferences.maxStaticBreathHoldDurationSeconds

    // Find next alarm across all enabled alarms
    val nextAlarm = alarms
        .filter { it.enabled }
        .mapNotNull { alarm ->
            alarm.getNextAlarmInfo()?.let { info ->
                alarm.getMinutesUntilNextAlarm()?.let { minutes ->
                    Triple(alarm, info, minutes)
                }
            }
        }
        .minByOrNull { (_, _, minutes) -> minutes }
        ?.let { (alarm, info, _) -> alarm to info }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Apnea Alarm",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

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

        // Next Alarm Tile
        nextAlarm?.let { (alarm, info) ->
            NextAlarmTile(
                alarm = alarm,
                dayName = info.first,
                time = info.second,
                globalM = globalM,
                onClick = { onNavigateToAlarmEdit(alarm.id) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Last Session Tile
        preferences.lastSessionSettings?.let { lastSettings ->
            LastSessionTile(
                settings = lastSettings,
                globalM = globalM,
                onRepeat = onRepeatLastSession
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // New Session Button
        if (!isSessionActive) {
            Button(
                onClick = onNavigateToNewSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "NEW SESSION",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Load Session Button
            OutlinedButton(
                onClick = onNavigateToSavedSessions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Load Saved Session")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Settings Button
        OutlinedButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Settings")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NextAlarmTile(
    alarm: Alarm,
    dayName: String,
    time: String,
    globalM: Int,
    onClick: () -> Unit
) {
    val settings = alarm.sessionSettings
    val intensityLevel = settings.intensityLevel(globalM)
    val intensityFactor = settings.intensityFactor(globalM)
    val numberOfIntervals = settings.numberOfIntervals()
    val breathHoldDuration = settings.breathHoldDurationSeconds(globalM)

    val accentColor = when (intensityLevel) {
        IntensityLevel.CALM -> Color(0xFF4CAF50)
        IntensityLevel.CHALLENGING -> Color(0xFFFFC107)
        IntensityLevel.HARD_TRAINING -> Color(0xFFFF9800)
        IntensityLevel.ADVANCED -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Next Alarm",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = accentColor)
                ) {
                    Text(
                        text = "$intensityFactor",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (intensityLevel == IntensityLevel.CHALLENGING || intensityLevel == IntensityLevel.HARD_TRAINING)
                            Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "  $dayName",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$numberOfIntervals x ${breathHoldDuration}s holds - ${settings.trainingMode.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun LastSessionTile(
    settings: SessionSettings,
    globalM: Int,
    onRepeat: () -> Unit
) {
    val intensityLevel = settings.intensityLevel(globalM)
    val intensityFactor = settings.intensityFactor(globalM)
    val numberOfIntervals = settings.numberOfIntervals()
    val breathHoldDuration = settings.breathHoldDurationSeconds(globalM)
    val totalTime = settings.totalSessionTimeSeconds(globalM)

    val containerColor = when (intensityLevel) {
        IntensityLevel.CALM -> Color(0xFF4CAF50)
        IntensityLevel.CHALLENGING -> Color(0xFFFFC107)
        IntensityLevel.HARD_TRAINING -> Color(0xFFFF9800)
        IntensityLevel.ADVANCED -> Color(0xFFF44336)
    }
    val contentColor = when (intensityLevel) {
        IntensityLevel.CALM -> Color.White
        IntensityLevel.CHALLENGING -> Color.Black
        IntensityLevel.HARD_TRAINING -> Color.Black
        IntensityLevel.ADVANCED -> Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last Session",
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Intensity $intensityFactor",
                        style = MaterialTheme.typography.titleLarge,
                        color = contentColor
                    )
                    Text(
                        text = intensityLevel.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Breath Holds",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$numberOfIntervals x ${breathHoldDuration}s",
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatDuration(totalTime),
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRepeat,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = contentColor.copy(alpha = 0.2f),
                    contentColor = contentColor
                )
            ) {
                Text("REPEAT")
            }
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
