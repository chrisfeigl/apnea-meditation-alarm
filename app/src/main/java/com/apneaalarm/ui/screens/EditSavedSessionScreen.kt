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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apneaalarm.R
import com.apneaalarm.data.IntensityLevel
import com.apneaalarm.data.SavedSession
import com.apneaalarm.data.SessionSettings
import com.apneaalarm.data.TrainingMode
import com.apneaalarm.ui.components.HelpContent
import com.apneaalarm.ui.components.HelpDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSavedSessionScreen(
    savedSession: SavedSession,
    globalM: Int,
    onNavigateBack: () -> Unit,
    onSaveSession: (SavedSession) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onStartSession: (SessionSettings) -> Unit
) {
    var sessionName by remember { mutableStateOf(savedSession.name) }
    var settings by remember { mutableStateOf(savedSession.sessionSettings) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showAudioFilesDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Session") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("\u2190 Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showHelpDialog = true }) {
                        Text("?")
                    }
                    TextButton(onClick = {
                        onSaveSession(savedSession.copy(name = sessionName, sessionSettings = settings))
                    }) {
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

            // Session Name Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                            text = "Session Name",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = sessionName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    TextButton(onClick = { showRenameDialog = true }) {
                        Text("Rename")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Intention Card
            EditIntentionCard(
                trainingMode = settings.trainingMode,
                onTrainingModeChanged = { mode ->
                    settings = settings.copy(trainingMode = mode)
                    // Always reset manual params to defaults for the selected mode
                    settings = updateManualParamsForModeEdit(settings, globalM, mode)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Settings Toggle
            EditManualSettingsToggle(
                useManual = settings.useManualIntervalSettings,
                onUseManualChanged = { enabled ->
                    settings = settings.copy(useManualIntervalSettings = enabled)
                    if (enabled) {
                        settings = updateManualParamsForModeEdit(settings, globalM, settings.trainingMode)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Settings Card (shown when manual mode is enabled)
            if (settings.useManualIntervalSettings) {
                EditManualSettingsCard(
                    settings = settings,
                    globalM = globalM,
                    onSettingsChanged = { settings = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Session Preview
            EditSessionPreviewCard(settings = settings, globalM = globalM)

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Settings Card
            EditAudioSettingsCard(
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

            Spacer(modifier = Modifier.height(16.dp))

            // Delete Button
            OutlinedButton(
                onClick = { onDeleteSession(savedSession.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete Session")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(sessionName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Session") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Session Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            sessionName = newName.trim()
                            showRenameDialog = false
                        }
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
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

    // Help Dialog
    if (showHelpDialog) {
        HelpDialog(
            title = "Edit Saved Session",
            content = HelpContent.editSavedSession,
            onDismiss = { showHelpDialog = false }
        )
    }
}

private fun updateManualParamsForModeEdit(settings: SessionSettings, globalM: Int, mode: TrainingMode): SessionSettings {
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
private fun EditIntentionCard(
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
private fun EditManualSettingsToggle(
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
private fun EditManualSettingsCard(
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
            EditNumberInputRow(
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
            EditNumberInputRow(
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
            EditNumberInputRow(
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
            EditNumberInputRow(
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
                    val updated = updateManualParamsForModeEdit(settings, globalM, settings.trainingMode)
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
private fun EditNumberInputRow(
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
private fun EditSessionPreviewCard(settings: SessionSettings, globalM: Int) {
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
                    Text(formatDurationEdit(totalTime), style = MaterialTheme.typography.bodyLarge, color = contentColor)
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
private fun EditAudioSettingsCard(
    settings: SessionSettings,
    onSettingsChanged: (SessionSettings) -> Unit,
    onAudioFilesClick: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

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

                EditVolumeSlider(
                    label = "Intro Bowl",
                    value = settings.introBowlVolumeMultiplier,
                    onValueChange = { onSettingsChanged(settings.copy(introBowlVolumeMultiplier = it)) },
                    onValueChangeFinished = {
                        val volume = settings.introBowlVolumeMultiplier / 10f
                        playPreviewSound("intro_bowl", settings.customIntroBowlUri, volume)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                EditVolumeSlider(
                    label = "Breath Chime",
                    value = settings.breathChimeVolumeMultiplier,
                    onValueChange = { onSettingsChanged(settings.copy(breathChimeVolumeMultiplier = it)) },
                    onValueChangeFinished = {
                        val volume = settings.breathChimeVolumeMultiplier / 10f
                        playPreviewSound("breath_chime", settings.customBreathChimeUri, volume)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                EditVolumeSlider(
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
private fun EditVolumeSlider(
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

private fun formatDurationEdit(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
