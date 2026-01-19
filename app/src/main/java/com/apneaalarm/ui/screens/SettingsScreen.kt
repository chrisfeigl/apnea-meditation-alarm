package com.apneaalarm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apneaalarm.data.UserPreferences
import com.apneaalarm.ui.components.DurationPickerDialog
import com.apneaalarm.ui.components.TimePickerDialog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onNavigateBack: () -> Unit,
    onNavigateToAudioFiles: () -> Unit,
    onAlarmEnabledChanged: (Boolean) -> Unit,
    onAlarmTimeChanged: (Int, Int) -> Unit,
    onBreathHoldChanged: (Int) -> Unit,
    onIntroBowlVolumeChanged: (Int) -> Unit,
    onBreathChimeVolumeChanged: (Int) -> Unit,
    onHoldChimeVolumeChanged: (Int) -> Unit,
    onBreathingIntervalMaxChanged: (Int) -> Unit,
    onBreathingIntervalMinChanged: (Int) -> Unit,
    onNumberOfIntervalsChanged: (Int) -> Unit,
    onPFactorChanged: (Float) -> Unit,
    onSnoozeDurationChanged: (Int) -> Unit,
    onFadeInIntroBowlChanged: (Boolean) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Back")
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
                        Button(onClick = { showTimePicker = true }) {
                            Text("Change")
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${preferences.snoozeDurationMinutes} min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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

            Spacer(modifier = Modifier.height(16.dp))

            // Breath Hold Settings Card
            SettingsCard(title = "Breath Hold Duration") {
                Text(
                    text = "Your maximum static breath hold",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Max: ${formatDuration(preferences.maxStaticBreathHoldDurationSeconds)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Training: ${formatDuration(preferences.breathHoldDurationSeconds)} (75%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = { showDurationPicker = true }) {
                        Text("Change")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Files Card - Link to Audio Files screen
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Audio Files",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Customize the sounds used for bowl and chimes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status summary
                    val customCount = listOfNotNull(
                        preferences.customIntroBowlUri,
                        preferences.customBreathChimeUri,
                        preferences.customHoldChimeUri
                    ).size

                    Text(
                        text = if (customCount > 0) "$customCount custom sound(s) configured" else "Using default sounds",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onNavigateToAudioFiles,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Manage Audio Files")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Volume Settings Card
            SettingsCard(title = "Volume Settings") {
                Text(
                    text = "Volume multipliers (1-10) are applied to system alarm volume",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                VolumeSlider(
                    label = "Intro Bowl Volume",
                    value = preferences.introBowlVolumeMultiplier,
                    onValueChange = onIntroBowlVolumeChanged
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Fade-in toggle for intro bowl
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fade In Intro Bowl",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (preferences.fadeInIntroBowl)
                                "Gradually increase volume over 48 seconds"
                            else
                                "Play at full volume immediately",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.fadeInIntroBowl,
                        onCheckedChange = onFadeInIntroBowlChanged
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                VolumeSlider(
                    label = "Breath Chime Volume",
                    value = preferences.breathChimeVolumeMultiplier,
                    onValueChange = onBreathChimeVolumeChanged
                )

                Spacer(modifier = Modifier.height(16.dp))

                VolumeSlider(
                    label = "Hold Chime Volume",
                    value = preferences.holdChimeVolumeMultiplier,
                    onValueChange = onHoldChimeVolumeChanged
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Breathing Interval Settings Card
            SettingsCard(title = "Breathing Interval Settings") {
                Text(
                    text = "Controls the time between breath holds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                NumberInput(
                    label = "Max Breathing Interval (seconds)",
                    value = preferences.breathingIntervalDurationMaxSeconds,
                    onValueChange = onBreathingIntervalMaxChanged,
                    minValue = 3,
                    maxValue = 300
                )

                Spacer(modifier = Modifier.height(16.dp))

                NumberInput(
                    label = "Min Breathing Interval (seconds)",
                    value = preferences.breathingIntervalDurationMinSeconds,
                    onValueChange = onBreathingIntervalMinChanged,
                    minValue = 1,
                    maxValue = 60
                )

                Spacer(modifier = Modifier.height(16.dp))

                NumberInput(
                    label = "Number of Cycles",
                    value = preferences.numberOfIntervals,
                    onValueChange = onNumberOfIntervalsChanged,
                    minValue = 1,
                    maxValue = 30
                )

                Spacer(modifier = Modifier.height(16.dp))

                PFactorSlider(
                    value = preferences.pFactor,
                    onValueChange = onPFactorChanged
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview of breathing intervals
            SettingsCard(title = "Breathing Interval Preview") {
                // Total session time
                val totalSeconds = preferences.totalSessionTimeSeconds
                val totalMinutes = totalSeconds / 60
                val remainingSeconds = totalSeconds % 60
                val totalTimeText = if (totalMinutes > 0) {
                    "${totalMinutes}m ${remainingSeconds}s"
                } else {
                    "${remainingSeconds}s"
                }

                Text(
                    text = "Total Session Time: $totalTimeText",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "(excludes intro bowl)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Duration for each breathing interval:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                for (i in 0 until preferences.numberOfIntervals.coerceAtMost(10)) {
                    val duration = preferences.breathingIntervalDuration(i)
                    Text(
                        text = "Cycle ${i + 1}: ${duration}s",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (preferences.numberOfIntervals > 10) {
                    Text(
                        text = "... and ${preferences.numberOfIntervals - 10} more cycles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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

    if (showDurationPicker) {
        DurationPickerDialog(
            initialSeconds = preferences.maxStaticBreathHoldDurationSeconds,
            onDurationSelected = {
                onBreathHoldChanged(it)
                showDurationPicker = false
            },
            onDismiss = { showDurationPicker = false }
        )
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

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            content()
        }
    }
}

@Composable
private fun VolumeSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${sliderValue.roundToInt()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue.roundToInt()) },
            valueRange = 1f..10f,
            steps = 8
        )
    }
}

@Composable
private fun PFactorSlider(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Progression Factor (p)",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = String.format("%.2f", sliderValue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "Lower = faster decrease, Higher = slower decrease",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue) },
            valueRange = 0.1f..2.0f
        )
    }
}

@Composable
private fun NumberInput(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int,
    maxValue: Int
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newText ->
            textValue = newText.filter { it.isDigit() }
            textValue.toIntOrNull()?.let { newValue ->
                if (newValue in minValue..maxValue) {
                    onValueChange(newValue)
                }
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        supportingText = {
            Text("Range: $minValue - $maxValue")
        }
    )
}
