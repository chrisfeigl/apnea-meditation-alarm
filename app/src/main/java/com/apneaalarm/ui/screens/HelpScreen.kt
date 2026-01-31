package com.apneaalarm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit,
    initialSection: String? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            HelpSectionCard(
                title = "Getting Started",
                content = """
                    Set your maximum static breath hold (M) in Settings. This is your personal best breath hold time in a relaxed state.

                    The app calculates all session parameters based on this value, so it's important to set it accurately. You can update it anytime as your capacity improves.
                """.trimIndent()
            )

            HelpSectionCard(
                title = "Training Modes",
                content = """
                    Relaxation: Gentle sessions at ~60% of your max with longer breathing intervals. Good for daily practice and meditation.

                    Training: Intense sessions at ~90% of your max with shorter breathing intervals. Designed for building breath-hold capacity.

                    Selecting a mode automatically configures the session parameters for that style of training.
                """.trimIndent()
            )

            HelpSectionCard(
                title = "Home Screen",
                content = """
                    The home screen shows your next scheduled alarm and last manual session for quick access.

                    • Next Alarm: Tap to edit the alarm settings
                    • Last Session: Tap "Repeat" to start the same session again
                    • New Session: Configure and start a custom session
                    • Load Session: Start from a saved configuration
                    • Settings: Manage alarms and global settings
                """.trimIndent()
            )

            HelpSectionCard(
                title = "New Session",
                content = """
                    Configure a manual session before starting:

                    • Intention: Choose Relaxation or Training mode
                    • Manual Settings: Enable to customize H, R0, Rn, N, and p values directly
                    • Session Preview: See the calculated intervals and intensity
                    • Audio Settings: Adjust volume levels and sounds

                    Tap "Save Session" to save the configuration for later, or "Start Session" to begin immediately.
                """.trimIndent()
            )

            HelpSectionCard(
                title = "During a Session",
                content = """
                    The intro bowl fades in to help you relax and prepare for the session.

                    Chimes signal transitions between breathing and holding. The hold chime consists of three tones:
                    • First chime: Prepare for the interval
                    • Second chime: Slowly and completely empty your lungs through your nose
                    • Third chime: Breathe in through your nose, then hold

                    You can follow the breathing intervals shown on screen, or just listen for the tones to guide you.

                    The session ends with a continuous bowl sound that plays until you stop it.

                    If this is an alarm-triggered session with snooze enabled, you can snooze to restart after a few minutes.
                """.trimIndent()
            )

            HelpSectionCard(
                title = "Alarms",
                content = """
                    Set multiple alarms for different times and days. Each alarm has its own independent settings:

                    • Time and days of the week
                    • Snooze on/off and duration
                    • Training mode and session parameters
                    • Audio settings

                    Alarms can be enabled or disabled individually. Tap an alarm in Settings to edit it, or add new alarms with the + button.
                """.trimIndent()
            )

            HelpSectionCard(
                title = "Saved Sessions",
                content = """
                    Save any session configuration with a custom name for quick access later.

                    From the saved sessions list, you can:
                    • Tap a session to expand and see details
                    • Start the session directly
                    • Edit the session settings
                    • Delete sessions you no longer need

                    Saved sessions are separate from alarms - they're for manual starts only.
                """.trimIndent()
            )

            HelpSectionCard(
                title = "Settings",
                content = """
                    Global settings that apply across the app:

                    • Max Breath Hold (M): Your personal best in seconds. All session calculations are based on this value.

                    The settings screen also shows all your alarms. Tap an alarm to edit it, toggle the switch to enable/disable, or tap + to add a new alarm.
                """.trimIndent()
            )

            HelpSectionCard(
                title = "Understanding the Numbers",
                content = """
                    Intensity Factor: A measure of how challenging the session is. Higher values mean harder sessions. Based on the ratio of breath hold time to breathing time.

                    Session Parameters:
                    • H: Breath hold duration in seconds
                    • R0: First (longest) breathing interval
                    • Rn: Last (shortest) breathing interval
                    • N: Number of breath-hold intervals
                    • p: Curve factor controlling how quickly intervals decrease

                    The breathing intervals gradually decrease from R0 to Rn over the course of the session, following a curve determined by p.
                """.trimIndent()
            )

            HelpSectionCard(
                title = "Audio Files",
                content = """
                    Customize the sounds used during sessions:

                    • Intro/Outro Bowl: Plays at the start (fading in) and continuously at the end
                    • Hold Chime: Three-tone signal to begin each breath hold
                    • Breathing Chime: Signal to begin each breathing interval

                    You can use the default bundled sounds or select your own audio files. Preview sounds before selecting them.
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HelpSectionCard(
    title: String,
    content: String
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
            )
        }
    }
}
