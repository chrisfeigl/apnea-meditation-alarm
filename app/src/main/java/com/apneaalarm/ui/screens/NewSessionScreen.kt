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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apneaalarm.data.IntensityLevel
import com.apneaalarm.data.SessionSettings
import com.apneaalarm.data.TrainingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSessionScreen(
    initialSettings: SessionSettings,
    globalM: Int,
    onNavigateBack: () -> Unit,
    onStartSession: (SessionSettings) -> Unit,
    onSaveSession: (name: String, settings: SessionSettings) -> Unit
) {
    var settings by remember { mutableStateOf(initialSettings) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showAudioFilesDialog by remember { mutableStateOf(false) }
    var sessionName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Session") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("\u2190 Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showSaveDialog = true }) {
                        Text("Save")
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

            // Intention Card
            IntentionCard(
                trainingMode = settings.trainingMode,
                onTrainingModeChanged = { mode ->
                    settings = settings.copy(trainingMode = mode)
                    // Auto-update manual params when mode changes
                    if (!settings.useManualIntervalSettings) {
                        settings = updateManualParamsForMode(settings, globalM, mode)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Settings Toggle
            ManualSettingsToggleCard(
                useManual = settings.useManualIntervalSettings,
                onUseManualChanged = { enabled ->
                    settings = settings.copy(useManualIntervalSettings = enabled)
                    if (enabled) {
                        settings = updateManualParamsForMode(settings, globalM, settings.trainingMode)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Settings Card (shown when manual mode is enabled)
            if (settings.useManualIntervalSettings) {
                ManualSettingsCardForSession(
                    settings = settings,
                    globalM = globalM,
                    onSettingsChanged = { settings = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Session Preview
            SessionPreviewCardForSettings(settings = settings, globalM = globalM)

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Settings Card (expandable)
            AudioSettingsCardForSession(
                settings = settings,
                onSettingsChanged = { settings = it },
                onAudioFilesClick = { showAudioFilesDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Start Session Button
            Button(
                onClick = { onStartSession(settings) },
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
    }

    // Save Session Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Session") },
            text = {
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Session Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (sessionName.isNotBlank()) {
                            onSaveSession(sessionName.trim(), settings)
                            showSaveDialog = false
                            sessionName = ""
                        }
                    },
                    enabled = sessionName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Audio Files Dialog
    if (showAudioFilesDialog) {
        AudioFilesDialog(
            settings = settings,
            onSettingsChanged = { settings = it },
            onDismiss = { showAudioFilesDialog = false }
        )
    }
}

private fun updateManualParamsForMode(settings: SessionSettings, globalM: Int, mode: TrainingMode): SessionSettings {
    return when (mode) {
        TrainingMode.RELAXATION -> {
            val h = (0.60 * globalM).toInt()
            val r0 = (1.25 * h).toInt()
            val rn = (0.25 * h).toInt().coerceAtLeast(3)
            settings.copy(
                manualBreathHoldDurationSeconds = h,
                manualR0Seconds = r0,
                manualRnSeconds = rn,
                manualNumberOfIntervals = 6,
                manualPFactor = 1.4f
            )
        }
        TrainingMode.TRAINING -> {
            val h = (0.90 * globalM).toInt()
            val r0 = (0.50 * h).toInt()
            val rn = (0.12 * h).toInt().coerceAtLeast(3)
            settings.copy(
                manualBreathHoldDurationSeconds = h,
                manualR0Seconds = r0,
                manualRnSeconds = rn,
                manualNumberOfIntervals = 8,
                manualPFactor = 0.75f
            )
        }
    }
}

@Composable
private fun IntentionCard(
    trainingMode: TrainingMode,
    onTrainingModeChanged: (TrainingMode) -> Unit
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
                        containerColor = if (trainingMode == TrainingMode.RELAXATION)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (trainingMode == TrainingMode.RELAXATION)
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
                        containerColor = if (trainingMode == TrainingMode.TRAINING)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (trainingMode == TrainingMode.TRAINING)
                            MaterialTheme.colorScheme.onTertiary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Training")
                }
            }
        }
    }
}

@Composable
private fun ManualSettingsToggleCard(
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
private fun ManualSettingsCardForSession(
    settings: SessionSettings,
    globalM: Int,
    onSettingsChanged: (SessionSettings) -> Unit
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
            var hText by remember(settings.manualBreathHoldDurationSeconds) {
                mutableStateOf(settings.manualBreathHoldDurationSeconds.toString())
            }
            NumberInputRowForSession(
                label = "H (Breath Hold)",
                value = hText,
                suffix = "s",
                description = "Duration of each breath hold",
                onValueChange = { newValue ->
                    hText = newValue
                    newValue.toIntOrNull()?.let {
                        onSettingsChanged(settings.copy(manualBreathHoldDurationSeconds = it))
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // R0 - Max Breathing Interval
            var r0Text by remember(settings.manualR0Seconds) {
                mutableStateOf(settings.manualR0Seconds.toString())
            }
            NumberInputRowForSession(
                label = "R0 (Max Breathing)",
                value = r0Text,
                suffix = "s",
                description = "First breathing interval",
                onValueChange = { newValue ->
                    r0Text = newValue
                    newValue.toIntOrNull()?.let {
                        onSettingsChanged(settings.copy(manualR0Seconds = it))
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rn - Min Breathing Interval
            var rnText by remember(settings.manualRnSeconds) {
                mutableStateOf(settings.manualRnSeconds.toString())
            }
            NumberInputRowForSession(
                label = "Rn (Min Breathing)",
                value = rnText,
                suffix = "s",
                description = "Final breathing interval",
                onValueChange = { newValue ->
                    rnText = newValue
                    newValue.toIntOrNull()?.let {
                        onSettingsChanged(settings.copy(manualRnSeconds = it))
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // N - Number of Intervals
            var nText by remember(settings.manualNumberOfIntervals) {
                mutableStateOf(settings.manualNumberOfIntervals.toString())
            }
            NumberInputRowForSession(
                label = "N (Intervals)",
                value = nText,
                suffix = "",
                description = "Number of breath hold cycles",
                onValueChange = { newValue ->
                    nText = newValue
                    newValue.toIntOrNull()?.let {
                        onSettingsChanged(settings.copy(manualNumberOfIntervals = it))
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // p - Curve Factor (slider)
            var pValue by remember(settings.manualPFactor) {
                mutableFloatStateOf(settings.manualPFactor)
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
                onValueChangeFinished = { onSettingsChanged(settings.copy(manualPFactor = pValue)) },
                valueRange = 0.1f..2.0f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    val updated = updateManualParamsForMode(settings, globalM, settings.trainingMode)
                    onSettingsChanged(updated)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset to ${settings.trainingMode.name.lowercase().replaceFirstChar { it.uppercase() }} Defaults")
            }
        }
    }
}

@Composable
private fun NumberInputRowForSession(
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
private fun SessionPreviewCardForSettings(settings: SessionSettings, globalM: Int) {
    val intensityFactor = settings.intensityFactor(globalM)
    val intensityLevel = settings.intensityLevel(globalM)

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

    val numberOfIntervals = settings.numberOfIntervals()
    val breathHoldDuration = settings.breathHoldDurationSeconds(globalM)
    val totalTime = settings.totalSessionTimeSeconds(globalM)
    val intervals = (0 until numberOfIntervals).map { i ->
        settings.breathingIntervalDuration(i, globalM)
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

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Breathing Intervals",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = intervals.joinToString(" \u2192 ") { "${it}s" },
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioSettingsCardForSession(
    settings: SessionSettings,
    onSettingsChanged: (SessionSettings) -> Unit,
    onAudioFilesClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (expanded) "\u25B2" else "\u25BC",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // Volume sliders
                VolumeSlider(
                    label = "Intro Bowl",
                    value = settings.introBowlVolumeMultiplier,
                    onValueChange = {
                        onSettingsChanged(settings.copy(introBowlVolumeMultiplier = it))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                VolumeSlider(
                    label = "Breath Chime",
                    value = settings.breathChimeVolumeMultiplier,
                    onValueChange = {
                        onSettingsChanged(settings.copy(breathChimeVolumeMultiplier = it))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                VolumeSlider(
                    label = "Hold Chime",
                    value = settings.holdChimeVolumeMultiplier,
                    onValueChange = {
                        onSettingsChanged(settings.copy(holdChimeVolumeMultiplier = it))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Fade in toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fade In Intro Bowl",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = settings.fadeInIntroBowl,
                        onCheckedChange = {
                            onSettingsChanged(settings.copy(fadeInIntroBowl = it))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Audio Files button
                OutlinedButton(
                    onClick = onAudioFilesClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Audio Files")
                }
            }
        }
    }
}

@Composable
private fun VolumeSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${value * 10}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth()
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
