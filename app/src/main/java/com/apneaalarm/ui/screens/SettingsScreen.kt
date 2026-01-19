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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apneaalarm.data.Alarm
import com.apneaalarm.data.IntensityLevel
import com.apneaalarm.data.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    alarms: List<Alarm>,
    onNavigateBack: () -> Unit,
    onNavigateToAlarmEdit: (Long?) -> Unit,
    onAlarmEnabledChanged: (Long, Boolean) -> Unit,
    onMaxBreathHoldChanged: (Int) -> Unit
) {
    val globalM = preferences.maxStaticBreathHoldDurationSeconds

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Global M Setting
            item {
                GlobalMCard(
                    currentValue = globalM,
                    onValueChanged = onMaxBreathHoldChanged
                )
            }

            // Alarms Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alarms",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Button(onClick = { onNavigateToAlarmEdit(null) }) {
                        Text("Add Alarm")
                    }
                }
            }

            // Alarm Cards
            if (alarms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No alarms configured",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap 'Add Alarm' to create your first alarm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        globalM = globalM,
                        onTap = { onNavigateToAlarmEdit(alarm.id) },
                        onEnabledChanged = { enabled -> onAlarmEnabledChanged(alarm.id, enabled) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GlobalMCard(
    currentValue: Int,
    onValueChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Maximum Breath Hold (M)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your personal max static breath hold duration. Used to calculate session intensity.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            var maxBreathHoldText by remember(currentValue) {
                mutableStateOf(currentValue.toString())
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = maxBreathHoldText,
                    onValueChange = { newValue ->
                        maxBreathHoldText = newValue
                        newValue.toIntOrNull()?.let { seconds ->
                            if (seconds in 10..600) {
                                onValueChanged(seconds)
                            }
                        }
                    },
                    modifier = Modifier.width(100.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    suffix = { Text("s") }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = formatTime(currentValue),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmCard(
    alarm: Alarm,
    globalM: Int,
    onTap: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit
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
        onClick = onTap,
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.enabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (alarm.enabled)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Intensity badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = accentColor)
                    ) {
                        Text(
                            text = "$intensityFactor",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (intensityLevel == IntensityLevel.CHALLENGING || intensityLevel == IntensityLevel.HARD_TRAINING)
                                Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Days summary
                val daysText = formatDays(alarm.days)
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (alarm.enabled)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Session summary
                Text(
                    text = "$numberOfIntervals x ${breathHoldDuration}s - ${settings.trainingMode.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (alarm.enabled)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Switch(
                checked = alarm.enabled,
                onCheckedChange = onEnabledChanged
            )
        }
    }
}

private fun formatDays(days: Set<Int>): String {
    if (days.size == 7) return "Every day"
    if (days.isEmpty()) return "No days selected"

    val weekdays = setOf(1, 2, 3, 4, 5)
    val weekends = setOf(6, 7)

    if (days == weekdays) return "Weekdays"
    if (days == weekends) return "Weekends"

    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return days.sorted().joinToString(", ") { dayNames[it - 1] }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0 && seconds > 0) {
        "${minutes}m ${seconds}s"
    } else if (minutes > 0) {
        "${minutes}m"
    } else {
        "${seconds}s"
    }
}
