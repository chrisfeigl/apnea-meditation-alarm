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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apneaalarm.data.IntensityLevel
import com.apneaalarm.data.TrainingMode
import com.apneaalarm.data.UserPreferences
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalSettingsScreen(
    preferences: UserPreferences,
    onNavigateBack: () -> Unit,
    onTrainingModeChanged: (TrainingMode) -> Unit,
    onMaxBreathHoldChanged: (Int) -> Unit,
    onUseManualChanged: (Boolean) -> Unit,
    onManualSettingsChanged: (h: Int?, r0: Int?, rn: Int?, n: Int?, p: Float?) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interval Settings") },
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

            // Intention & Max Breath Hold Card
            TrainingModeCard(
                preferences = preferences,
                onTrainingModeChanged = { mode ->
                    onTrainingModeChanged(mode)
                    // Auto-reset manual params to match the selected mode
                    val m = preferences.maxStaticBreathHoldDurationSeconds
                    when (mode) {
                        TrainingMode.RELAXATION -> {
                            val h = (0.60 * m).toInt()
                            val r0 = (1.25 * h).toInt()
                            val rn = (0.25 * h).toInt().coerceAtLeast(3)
                            onManualSettingsChanged(h, r0, rn, 6, 1.4f)
                        }
                        TrainingMode.TRAINING -> {
                            val h = (0.90 * m).toInt()
                            val r0 = (0.50 * h).toInt()
                            val rn = (0.12 * h).toInt().coerceAtLeast(3)
                            onManualSettingsChanged(h, r0, rn, 8, 0.75f)
                        }
                    }
                },
                onMaxBreathHoldChanged = onMaxBreathHoldChanged
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Settings Toggle
            ManualSettingsToggle(
                useManual = preferences.useManualIntervalSettings,
                onUseManualChanged = { enabled ->
                    onUseManualChanged(enabled)
                    // When enabling manual mode, initialize params to current training mode
                    if (enabled) {
                        val m = preferences.maxStaticBreathHoldDurationSeconds
                        when (preferences.trainingMode) {
                            TrainingMode.RELAXATION -> {
                                val h = (0.60 * m).toInt()
                                val r0 = (1.25 * h).toInt()
                                val rn = (0.25 * h).toInt().coerceAtLeast(3)
                                onManualSettingsChanged(h, r0, rn, 6, 1.4f)
                            }
                            TrainingMode.TRAINING -> {
                                val h = (0.90 * m).toInt()
                                val r0 = (0.50 * h).toInt()
                                val rn = (0.12 * h).toInt().coerceAtLeast(3)
                                onManualSettingsChanged(h, r0, rn, 8, 0.75f)
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Settings Card (shown when manual mode is enabled)
            if (preferences.useManualIntervalSettings) {
                ManualSettingsCard(
                    preferences = preferences,
                    onManualSettingsChanged = onManualSettingsChanged
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Session Preview (always shown)
            SessionPreviewCard(preferences = preferences)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TrainingModeCard(
    preferences: UserPreferences,
    onTrainingModeChanged: (TrainingMode) -> Unit,
    onMaxBreathHoldChanged: (Int) -> Unit
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
                text = "Intention",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onTrainingModeChanged(TrainingMode.RELAXATION) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (preferences.trainingMode == TrainingMode.RELAXATION)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (preferences.trainingMode == TrainingMode.RELAXATION)
                            MaterialTheme.colorScheme.onSecondary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Relaxation")
                }

                OutlinedButton(
                    onClick = { onTrainingModeChanged(TrainingMode.TRAINING) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (preferences.trainingMode == TrainingMode.TRAINING)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (preferences.trainingMode == TrainingMode.TRAINING)
                            MaterialTheme.colorScheme.onTertiary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Training")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Maximum Breath Hold (M)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            var maxBreathHoldText by remember(preferences.maxStaticBreathHoldDurationSeconds) {
                mutableStateOf(preferences.maxStaticBreathHoldDurationSeconds.toString())
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
                                onMaxBreathHoldChanged(seconds)
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
                    text = formatTime(preferences.maxStaticBreathHoldDurationSeconds),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!preferences.useManualIntervalSettings) {
                Spacer(modifier = Modifier.height(12.dp))

                val holdPercent = when (preferences.trainingMode) {
                    TrainingMode.RELAXATION -> "60%"
                    TrainingMode.TRAINING -> "90%"
                }

                Text(
                    text = "Breath hold will be $holdPercent of M = ${preferences.breathHoldDurationSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ManualSettingsToggle(
    useManual: Boolean,
    onUseManualChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = "Manual Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (useManual) "Manually configure H, R0, Rn, N, p"
                    else "Using mode-based calculations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Switch(
                checked = useManual,
                onCheckedChange = onUseManualChanged
            )
        }
    }
}

@Composable
private fun ManualSettingsCard(
    preferences: UserPreferences,
    onManualSettingsChanged: (h: Int?, r0: Int?, rn: Int?, n: Int?, p: Float?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Manual Interval Parameters",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // H - Breath Hold Duration
            var hText by remember(preferences.manualBreathHoldDurationSeconds) {
                mutableStateOf(preferences.manualBreathHoldDurationSeconds.toString())
            }
            NumberInputRow(
                label = "H (Breath Hold)",
                value = hText,
                suffix = "s",
                description = "Duration of each breath hold",
                onValueChange = { newValue ->
                    hText = newValue
                    newValue.toIntOrNull()?.let { onManualSettingsChanged(it, null, null, null, null) }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // R0 - Max Breathing Interval
            var r0Text by remember(preferences.manualR0Seconds) {
                mutableStateOf(preferences.manualR0Seconds.toString())
            }
            NumberInputRow(
                label = "R0 (Max Breathing)",
                value = r0Text,
                suffix = "s",
                description = "First breathing interval",
                onValueChange = { newValue ->
                    r0Text = newValue
                    newValue.toIntOrNull()?.let { onManualSettingsChanged(null, it, null, null, null) }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rn - Min Breathing Interval
            var rnText by remember(preferences.manualRnSeconds) {
                mutableStateOf(preferences.manualRnSeconds.toString())
            }
            NumberInputRow(
                label = "Rn (Min Breathing)",
                value = rnText,
                suffix = "s",
                description = "Final breathing interval",
                onValueChange = { newValue ->
                    rnText = newValue
                    newValue.toIntOrNull()?.let { onManualSettingsChanged(null, null, it, null, null) }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // N - Number of Intervals
            var nText by remember(preferences.manualNumberOfIntervals) {
                mutableStateOf(preferences.manualNumberOfIntervals.toString())
            }
            NumberInputRow(
                label = "N (Intervals)",
                value = nText,
                suffix = "",
                description = "Number of breath hold cycles",
                onValueChange = { newValue ->
                    nText = newValue
                    newValue.toIntOrNull()?.let { onManualSettingsChanged(null, null, null, it, null) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // p - Curve Factor (slider)
            var pValue by remember(preferences.manualPFactor) {
                mutableFloatStateOf(preferences.manualPFactor)
            }
            Text(
                text = "p (Curve Factor): %.2f".format(pValue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "How quickly breathing intervals shorten",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
            )
            Slider(
                value = pValue,
                onValueChange = { pValue = it },
                onValueChangeFinished = { onManualSettingsChanged(null, null, null, null, pValue) },
                valueRange = 0.1f..2.0f,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Quick start",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
                Text(
                    text = "Gradual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    val m = preferences.maxStaticBreathHoldDurationSeconds
                    when (preferences.trainingMode) {
                        TrainingMode.RELAXATION -> {
                            val h = (0.60 * m).toInt()
                            val r0 = (1.25 * h).toInt()
                            val rn = (0.25 * h).toInt().coerceAtLeast(3)
                            onManualSettingsChanged(h, r0, rn, 6, 1.4f)
                        }
                        TrainingMode.TRAINING -> {
                            val h = (0.90 * m).toInt()
                            val r0 = (0.50 * h).toInt()
                            val rn = (0.12 * h).toInt().coerceAtLeast(3)
                            onManualSettingsChanged(h, r0, rn, 8, 0.75f)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset to ${preferences.trainingMode.name.lowercase().replaceFirstChar { it.uppercase() }} Defaults")
            }
        }
    }
}

@Composable
private fun NumberInputRow(
    label: String,
    value: String,
    suffix: String,
    description: String? = null,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(100.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            suffix = if (suffix.isNotEmpty()) { { Text(suffix) } } else null
        )
    }
}

@Composable
private fun SessionPreviewCard(preferences: UserPreferences) {
    // Color based on intensity level
    val containerColor = when (preferences.intensityLevel) {
        IntensityLevel.CALM -> Color(0xFF4CAF50)           // Green
        IntensityLevel.CHALLENGING -> Color(0xFFFFC107)    // Amber
        IntensityLevel.HARD_TRAINING -> Color(0xFFFF9800)    // Orange
        IntensityLevel.ADVANCED -> Color(0xFFF44336)       // Red
    }
    val contentColor = when (preferences.intensityLevel) {
        IntensityLevel.CALM -> Color.White
        IntensityLevel.CHALLENGING -> Color.Black
        IntensityLevel.HARD_TRAINING -> Color.Black
        IntensityLevel.ADVANCED -> Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with title and intensity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Session Preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Intensity ${preferences.intensityFactor}",
                        style = MaterialTheme.typography.titleLarge,
                        color = contentColor
                    )
                    Text(
                        text = preferences.intensityLevel.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.8f)
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
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${preferences.numberOfIntervals} x ${preferences.breathHoldDurationSeconds}s",
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
                        text = formatTime(preferences.totalSessionTimeSeconds),
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Breathing Intervals",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Show all breathing intervals (N intervals for N breath holds)
            val intervals = (0 until preferences.numberOfIntervals).map { i ->
                preferences.breathingIntervalDuration(i)
            }

            Text(
                text = intervals.joinToString(" \u2192 ") { "${it}s" },
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}
