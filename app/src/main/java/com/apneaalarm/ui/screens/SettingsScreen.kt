package com.apneaalarm.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apneaalarm.data.TrainingMode
import com.apneaalarm.data.UserPreferences
import com.apneaalarm.ui.components.TimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onNavigateBack: () -> Unit,
    onNavigateToIntervalSettings: () -> Unit,
    onNavigateToAudioSettings: () -> Unit,
    onAlarmEnabledChanged: (Boolean) -> Unit,
    onAlarmTimeChanged: (Int, Int) -> Unit,
    onAlarmDaysChanged: (Set<Int>) -> Unit,
    onSnoozeDurationChanged: (Int) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Alarm Settings Card
            AlarmSettingsCard(
                preferences = preferences,
                onAlarmEnabledChanged = onAlarmEnabledChanged,
                onChangeTimeClick = { showTimePicker = true },
                onAlarmDaysChanged = onAlarmDaysChanged,
                onSnoozeDurationChanged = onSnoozeDurationChanged
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interval Settings Navigation Card
            NavigationCard(
                title = "Interval Settings",
                subtitle = buildIntervalSubtitle(preferences),
                onClick = onNavigateToIntervalSettings
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Settings Navigation Card
            NavigationCard(
                title = "Audio Settings",
                subtitle = buildAudioSubtitle(preferences),
                onClick = onNavigateToAudioSettings
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = preferences.alarmHour,
            initialMinute = preferences.alarmMinute,
            onTimeSelected = { hour, minute ->
                onAlarmTimeChanged(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmSettingsCard(
    preferences: UserPreferences,
    onAlarmEnabledChanged: (Boolean) -> Unit,
    onChangeTimeClick: () -> Unit,
    onAlarmDaysChanged: (Set<Int>) -> Unit,
    onSnoozeDurationChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (preferences.alarmEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
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
                    text = "Alarm",
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = preferences.alarmEnabled,
                    onCheckedChange = onAlarmEnabledChanged
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%02d:%02d", preferences.alarmHour, preferences.alarmMinute),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (preferences.alarmEnabled)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onChangeTimeClick) {
                    Text("Change")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day selection
            Text(
                text = "Repeat",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                dayLabels.forEachIndexed { index, label ->
                    val dayNumber = index + 1
                    val isSelected = dayNumber in preferences.alarmDays
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newDays = if (isSelected) {
                                preferences.alarmDays - dayNumber
                            } else {
                                preferences.alarmDays + dayNumber
                            }
                            onAlarmDaysChanged(newDays)
                        },
                        label = { Text(label) },
                        modifier = Modifier.width(40.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Snooze duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Snooze Duration",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${preferences.snoozeDurationMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(1, 5, 10, 15, 30).forEach { minutes ->
                    TextButton(
                        onClick = { onSnoozeDurationChanged(minutes) }
                    ) {
                        Text(
                            text = "${minutes}m",
                            color = if (preferences.snoozeDurationMinutes == minutes)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Text(
                text = "\u203A",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildIntervalSubtitle(preferences: UserPreferences): String {
    return if (preferences.useManualIntervalSettings) {
        "Manual: ${preferences.numberOfIntervals} intervals, ${preferences.breathHoldDurationSeconds}s hold"
    } else {
        val modeName = when (preferences.trainingMode) {
            TrainingMode.RELAXATION -> "Relaxation"
            TrainingMode.TRAINING -> "Training"
        }
        "$modeName mode, M=${preferences.maxStaticBreathHoldDurationSeconds}s"
    }
}

private fun buildAudioSubtitle(preferences: UserPreferences): String {
    val customCount = listOfNotNull(
        preferences.customIntroBowlUri,
        preferences.customBreathChimeUri,
        preferences.customHoldChimeUri
    ).size

    return if (customCount > 0) {
        "$customCount custom sound(s), volumes configured"
    } else {
        "Default sounds, volumes configured"
    }
}
