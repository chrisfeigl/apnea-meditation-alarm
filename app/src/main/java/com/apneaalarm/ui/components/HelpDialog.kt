package com.apneaalarm.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

object HelpContent {
    val newSession = """
        Configure a manual session before starting:

        • Intention: Choose Relaxation or Training mode. Selecting a mode resets the parameters to defaults for that style.

        • Manual Settings: Enable to customize H, R0, Rn, N, and p values directly instead of using mode defaults.

        • Session Preview: Shows the calculated intervals, intensity factor, and total duration.

        • Audio Settings: Adjust volume levels for each sound. Releasing a slider plays a preview at that volume. Tap "Audio Files" to change the sounds.

        Tap "Save Session" to save the configuration for later use, or "Start Session" to begin immediately.
    """.trimIndent()

    val session = """
        The intro bowl fades in to help you relax and prepare.

        Chimes signal transitions between breathing and holding. The hold chime consists of three tones:
        • First chime: Prepare for the interval
        • Second chime: Slowly and completely empty your lungs through your nose
        • Third chime: Breathe in through your nose, then hold

        You can follow the breathing intervals shown on screen, or just listen for the tones to guide you.

        The session ends with a continuous bowl sound that plays until you stop it.

        If this is an alarm-triggered session with snooze enabled, you can snooze to restart after a few minutes.
    """.trimIndent()

    val settings = """
        Global settings that apply across the app:

        • Max Breath Hold (M): Your personal best breath hold time in seconds. All session calculations are based on this value. Update it as your capacity improves.

        The alarms list shows all your scheduled alarms. Each alarm has its own independent settings:
        • Toggle the switch to enable/disable an alarm
        • Tap an alarm to edit its settings
        • Tap + to add a new alarm
    """.trimIndent()

    val alarmEdit = """
        Configure an individual alarm:

        • Time & Days: When the alarm should trigger. Select which days of the week.

        • Snooze: Enable to allow snoozing the alarm. Set the snooze duration in minutes.

        • Intention: Choose Relaxation or Training mode. Selecting a mode resets the session parameters.

        • Manual Settings: Customize H, R0, Rn, N, and p values for this alarm.

        • Audio Settings: Adjust volumes and change sounds for this alarm's sessions.

        Each alarm is completely independent - changes here don't affect other alarms.
    """.trimIndent()

    val savedSessions = """
        Your saved session configurations for quick access.

        • Tap a session card to expand and see details
        • Start: Begin the session immediately
        • Edit: Modify the session settings
        • Delete: Remove the saved session

        Saved sessions are separate from alarms - they're for manual starts only. To schedule a session, create an alarm instead.
    """.trimIndent()

    val editSavedSession = """
        Edit a saved session configuration:

        • Session Name: Tap "Rename" to change the name.

        • Intention: Choose Relaxation or Training mode. Selecting a mode resets the parameters.

        • Manual Settings: Customize H, R0, Rn, N, and p values.

        • Audio Settings: Adjust volumes and change sounds.

        Tap "Save" in the top bar to save your changes, or "Start Session" to begin with the current settings.
    """.trimIndent()
}
