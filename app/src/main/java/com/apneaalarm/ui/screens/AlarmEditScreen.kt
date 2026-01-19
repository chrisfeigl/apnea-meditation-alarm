package com.apneaalarm.ui.screens

import android.media.MediaPlayer
import android.net.Uri
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apneaalarm.R
import com.apneaalarm.data.Alarm
import com.apneaalarm.data.IntensityLevel
import com.apneaalarm.data.SessionSettings
import com.apneaalarm.data.TrainingMode
import com.apneaalarm.ui.components.TimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    alarm: Alarm?,
    globalM: Int,
    isNewAlarm: Boolean,
    onNavigateBack: () -> Unit,
    onSaveAlarm: (Alarm) -> Unit,
    onDeleteAlarm: (Long) -> Unit
) {
    // Initialize with provided alarm or create new one
    var currentAlarm by remember {
        mutableStateOf(alarm ?: Alarm(
            id = System.currentTimeMillis(),
            enabled = true
        ))
    }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAudioFilesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewAlarm) "New Alarm" else "Edit Alarm") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("\u2190 Back")
                    }
                },
                actions = {
                    TextButton(onClick = { onSaveAlarm(currentAlarm) }) {
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

            // Time and Enabled Card
            TimeAndEnabledCard(
                alarm = currentAlarm,
                onEnabledChanged = { currentAlarm = currentAlarm.copy(enabled = it) },
                onChangeTimeClick = { showTimePicker = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Day Selection Card
            DaySelectionCard(
                days = currentAlarm.days,
                onDaysChanged = { currentAlarm = currentAlarm.copy(days = it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Snooze Settings Card
            SnoozeSettingsCard(
                snoozeEnabled = currentAlarm.snoozeEnabled,
                snoozeDuration = currentAlarm.snoozeDurationMinutes,
                onSnoozeEnabledChanged = { currentAlarm = currentAlarm.copy(snoozeEnabled = it) },
                onSnoozeDurationChanged = { currentAlarm = currentAlarm.copy(snoozeDurationMinutes = it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Intention Card
            AlarmIntentionCard(
                trainingMode = currentAlarm.sessionSettings.trainingMode,
                onTrainingModeChanged = { mode ->
                    val newSettings = currentAlarm.sessionSettings.copy(trainingMode = mode)
                    // Always reset manual params to defaults for the selected mode
                    val updatedSettings = updateManualParamsForModeAlarm(newSettings, globalM, mode)
                    currentAlarm = currentAlarm.copy(sessionSettings = updatedSettings)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Settings Toggle
            AlarmManualSettingsToggle(
                useManual = currentAlarm.sessionSettings.useManualIntervalSettings,
                onUseManualChanged = { enabled ->
                    var newSettings = currentAlarm.sessionSettings.copy(useManualIntervalSettings = enabled)
                    if (enabled) {
                        newSettings = updateManualParamsForModeAlarm(newSettings, globalM, newSettings.trainingMode)
                    }
                    currentAlarm = currentAlarm.copy(sessionSettings = newSettings)
                }
            )

            if (currentAlarm.sessionSettings.useManualIntervalSettings) {
                Spacer(modifier = Modifier.height(16.dp))

                AlarmManualSettingsCard(
                    settings = currentAlarm.sessionSettings,
                    globalM = globalM,
                    onSettingsChanged = { currentAlarm = currentAlarm.copy(sessionSettings = it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Session Preview
            AlarmSessionPreview(
                settings = currentAlarm.sessionSettings,
                globalM = globalM
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Settings
            AlarmAudioSettingsCard(
                settings = currentAlarm.sessionSettings,
                onSettingsChanged = { currentAlarm = currentAlarm.copy(sessionSettings = it) },
                onAudioFilesClick = { showAudioFilesDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Delete Button (only for existing alarms)
            if (!isNewAlarm && alarm != null) {
                OutlinedButton(
                    onClick = { onDeleteAlarm(alarm.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Alarm")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = currentAlarm.hour,
            initialMinute = currentAlarm.minute,
            onTimeSelected = { hour, minute ->
                currentAlarm = currentAlarm.copy(hour = hour, minute = minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    // Audio Files Dialog
    if (showAudioFilesDialog) {
        AudioFilesDialog(
            settings = currentAlarm.sessionSettings,
            onSettingsChanged = { currentAlarm = currentAlarm.copy(sessionSettings = it) },
            onDismiss = { showAudioFilesDialog = false }
        )
    }
}

private fun updateManualParamsForModeAlarm(settings: SessionSettings, globalM: Int, mode: TrainingMode): SessionSettings {
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
private fun TimeAndEnabledCard(
    alarm: Alarm,
    onEnabledChanged: (Boolean) -> Unit,
    onChangeTimeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.enabled)
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
                    text = "Enabled",
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onEnabledChanged
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (alarm.enabled)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onChangeTimeClick) {
                    Text("Change")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySelectionCard(
    days: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit
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
                text = "Repeat",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                dayLabels.forEachIndexed { index, label ->
                    val dayNumber = index + 1
                    val isSelected = dayNumber in days
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newDays = if (isSelected) {
                                days - dayNumber
                            } else {
                                days + dayNumber
                            }
                            onDaysChanged(newDays)
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
        }
    }
}

@Composable
private fun SnoozeSettingsCard(
    snoozeEnabled: Boolean,
    snoozeDuration: Int,
    onSnoozeEnabledChanged: (Boolean) -> Unit,
    onSnoozeDurationChanged: (Int) -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Snooze",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = snoozeEnabled,
                    onCheckedChange = onSnoozeEnabledChanged
                )
            }

            if (snoozeEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

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
                                color = if (snoozeDuration == minutes)
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
}

@Composable
private fun AlarmIntentionCard(
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
private fun AlarmManualSettingsToggle(
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
private fun AlarmManualSettingsCard(
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
            AlarmNumberInputRow(
                label = "H (Breath Hold)",
                value = hText,
                suffix = "s",
                onValueChange = { newValue ->
                    hText = newValue
                    newValue.toIntOrNull()?.let {
                        onSettingsChanged(settings.copy(manualBreathHoldDurationSeconds = it))
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // R0
            var r0Text by remember(settings.manualR0Seconds) {
                mutableStateOf(settings.manualR0Seconds.toString())
            }
            AlarmNumberInputRow(
                label = "R0 (Max Breathing)",
                value = r0Text,
                suffix = "s",
                onValueChange = { newValue ->
                    r0Text = newValue
                    newValue.toIntOrNull()?.let {
                        onSettingsChanged(settings.copy(manualR0Seconds = it))
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rn
            var rnText by remember(settings.manualRnSeconds) {
                mutableStateOf(settings.manualRnSeconds.toString())
            }
            AlarmNumberInputRow(
                label = "Rn (Min Breathing)",
                value = rnText,
                suffix = "s",
                onValueChange = { newValue ->
                    rnText = newValue
                    newValue.toIntOrNull()?.let {
                        onSettingsChanged(settings.copy(manualRnSeconds = it))
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // N
            var nText by remember(settings.manualNumberOfIntervals) {
                mutableStateOf(settings.manualNumberOfIntervals.toString())
            }
            AlarmNumberInputRow(
                label = "N (Intervals)",
                value = nText,
                suffix = "",
                onValueChange = { newValue ->
                    nText = newValue
                    newValue.toIntOrNull()?.let {
                        onSettingsChanged(settings.copy(manualNumberOfIntervals = it))
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // p - Curve Factor
            var pValue by remember(settings.manualPFactor) {
                mutableFloatStateOf(settings.manualPFactor)
            }
            Text(
                text = "p (Curve Factor): %.2f".format(pValue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
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
                    val updated = updateManualParamsForModeAlarm(settings, globalM, settings.trainingMode)
                    onSettingsChanged(updated)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset to Defaults")
            }
        }
    }
}

@Composable
private fun AlarmNumberInputRow(
    label: String,
    value: String,
    suffix: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
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
private fun AlarmSessionPreview(
    settings: SessionSettings,
    globalM: Int
) {
    val intensityLevel = settings.intensityLevel(globalM)
    val intensityFactor = settings.intensityFactor(globalM)
    val numberOfIntervals = settings.numberOfIntervals()
    val breathHoldDuration = settings.breathHoldDurationSeconds(globalM)
    val totalTime = settings.totalSessionTimeSeconds(globalM)
    val intervals = (0 until numberOfIntervals).map { settings.breathingIntervalDuration(it, globalM) }

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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Session Preview", style = MaterialTheme.typography.titleMedium, color = contentColor)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Intensity $intensityFactor", style = MaterialTheme.typography.titleLarge, color = contentColor)
                    Text(intensityLevel.label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.8f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Breath Holds", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
                    Text("$numberOfIntervals x ${breathHoldDuration}s", style = MaterialTheme.typography.bodyLarge, color = contentColor)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Time", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
                    Text(formatDurationAlarm(totalTime), style = MaterialTheme.typography.bodyLarge, color = contentColor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Breathing Intervals", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                intervals.joinToString(" \u2192 ") { "${it}s" },
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmAudioSettingsCard(
    settings: SessionSettings,
    onSettingsChanged: (SessionSettings) -> Unit,
    onAudioFilesClick: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Cleanup media player on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun playPreviewSound(soundType: String, customUri: String?, volume: Float) {
        mediaPlayer?.release()
        try {
            mediaPlayer = if (customUri != null) {
                MediaPlayer.create(context, Uri.parse(customUri))
            } else {
                when (soundType) {
                    "intro_bowl" -> MediaPlayer.create(context, R.raw.bowl)
                    "breath_chime" -> MediaPlayer.create(context, R.raw.chime_breath)
                    "hold_chime" -> MediaPlayer.create(context, R.raw.chime_hold)
                    else -> null
                }
            }
            mediaPlayer?.setVolume(volume, volume)
            mediaPlayer?.setOnCompletionListener { mp ->
                mp.release()
                if (mediaPlayer == mp) mediaPlayer = null
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Audio Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (expanded) "\u25B2" else "\u25BC", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))

                AlarmVolumeSlider(
                    label = "Intro Bowl",
                    value = settings.introBowlVolumeMultiplier,
                    onValueChange = { onSettingsChanged(settings.copy(introBowlVolumeMultiplier = it)) },
                    onValueChangeFinished = {
                        val volume = settings.introBowlVolumeMultiplier / 10f
                        playPreviewSound("intro_bowl", settings.customIntroBowlUri, volume)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                AlarmVolumeSlider(
                    label = "Breath Chime",
                    value = settings.breathChimeVolumeMultiplier,
                    onValueChange = { onSettingsChanged(settings.copy(breathChimeVolumeMultiplier = it)) },
                    onValueChangeFinished = {
                        val volume = settings.breathChimeVolumeMultiplier / 10f
                        playPreviewSound("breath_chime", settings.customBreathChimeUri, volume)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                AlarmVolumeSlider(
                    label = "Hold Chime",
                    value = settings.holdChimeVolumeMultiplier,
                    onValueChange = { onSettingsChanged(settings.copy(holdChimeVolumeMultiplier = it)) },
                    onValueChangeFinished = {
                        val volume = settings.holdChimeVolumeMultiplier / 10f
                        playPreviewSound("hold_chime", settings.customHoldChimeUri, volume)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fade In Intro Bowl", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Switch(
                        checked = settings.fadeInIntroBowl,
                        onCheckedChange = { onSettingsChanged(settings.copy(fadeInIntroBowl = it)) }
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
private fun AlarmVolumeSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${value * 10}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun formatDurationAlarm(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
