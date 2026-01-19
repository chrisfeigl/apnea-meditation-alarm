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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apneaalarm.data.UserPreferences
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSettingsScreen(
    preferences: UserPreferences,
    onNavigateBack: () -> Unit,
    onNavigateToAudioFiles: () -> Unit,
    onIntroBowlVolumeChanged: (Int) -> Unit,
    onBreathChimeVolumeChanged: (Int) -> Unit,
    onHoldChimeVolumeChanged: (Int) -> Unit,
    onFadeInIntroBowlChanged: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Settings") },
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

            // Audio Files Card
            AudioFilesCard(
                preferences = preferences,
                onNavigateToAudioFiles = onNavigateToAudioFiles
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Volume Settings Card
            VolumeSettingsCard(
                preferences = preferences,
                onIntroBowlVolumeChanged = onIntroBowlVolumeChanged,
                onBreathChimeVolumeChanged = onBreathChimeVolumeChanged,
                onHoldChimeVolumeChanged = onHoldChimeVolumeChanged,
                onFadeInIntroBowlChanged = onFadeInIntroBowlChanged
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AudioFilesCard(
    preferences: UserPreferences,
    onNavigateToAudioFiles: () -> Unit
) {
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
}

@Composable
private fun VolumeSettingsCard(
    preferences: UserPreferences,
    onIntroBowlVolumeChanged: (Int) -> Unit,
    onBreathChimeVolumeChanged: (Int) -> Unit,
    onHoldChimeVolumeChanged: (Int) -> Unit,
    onFadeInIntroBowlChanged: (Boolean) -> Unit
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
                text = "Volume Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Volume multipliers (1-10) are applied to system alarm volume",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Intro Bowl Volume
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (preferences.fadeInIntroBowl)
                            "Gradually increase volume over 48 seconds"
                        else
                            "Play at full volume immediately",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = preferences.fadeInIntroBowl,
                    onCheckedChange = onFadeInIntroBowlChanged
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Breath Chime Volume
            VolumeSlider(
                label = "Breath Chime Volume",
                value = preferences.breathChimeVolumeMultiplier,
                onValueChange = onBreathChimeVolumeChanged
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hold Chime Volume
            VolumeSlider(
                label = "Hold Chime Volume",
                value = preferences.holdChimeVolumeMultiplier,
                onValueChange = onHoldChimeVolumeChanged
            )
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
