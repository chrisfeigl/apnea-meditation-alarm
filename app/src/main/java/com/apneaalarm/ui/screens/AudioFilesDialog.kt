package com.apneaalarm.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apneaalarm.R
import com.apneaalarm.data.SessionSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioFilesDialog(
    settings: SessionSettings,
    onSettingsChanged: (SessionSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Track currently playing preview
    var currentlyPlaying by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Cleanup media player on dismiss
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun stopPreview() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentlyPlaying = null
    }

    fun playPreview(soundType: String, customUri: String?) {
        stopPreview()
        currentlyPlaying = soundType

        try {
            mediaPlayer = if (customUri != null) {
                MediaPlayer.create(context, Uri.parse(customUri))
            } else {
                // Use default bundled sounds
                when (soundType) {
                    "intro_bowl" -> MediaPlayer.create(context, R.raw.bowl)
                    "breath_chime" -> MediaPlayer.create(context, R.raw.chime_breath)
                    "hold_chime" -> MediaPlayer.create(context, R.raw.chime_hold)
                    else -> null
                }
            }

            mediaPlayer?.setOnCompletionListener {
                stopPreview()
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            stopPreview()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            stopPreview()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Audio Files",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Customize the sounds used during your breathing session. Select your own audio files or use the default bundled sounds.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Intro/Outro Bowl
            AudioFileCard(
                title = "Intro & Outro Bowl",
                description = "Played at the start (fading in gradually) and continuously at the end of the session until you stop it.",
                currentUri = settings.customIntroBowlUri,
                onUriSelected = { uri ->
                    onSettingsChanged(settings.copy(customIntroBowlUri = uri))
                },
                onPreview = { playPreview("intro_bowl", settings.customIntroBowlUri) },
                onStopPreview = { stopPreview() },
                isPlaying = currentlyPlaying == "intro_bowl"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hold Chime
            AudioFileCard(
                title = "Hold Chime",
                description = "Played to signal the start of each breath-hold interval.",
                currentUri = settings.customHoldChimeUri,
                onUriSelected = { uri ->
                    onSettingsChanged(settings.copy(customHoldChimeUri = uri))
                },
                onPreview = { playPreview("hold_chime", settings.customHoldChimeUri) },
                onStopPreview = { stopPreview() },
                isPlaying = currentlyPlaying == "hold_chime"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Breathing Chime
            AudioFileCard(
                title = "Breathing Chime",
                description = "Played to signal the start of each breathing interval.",
                currentUri = settings.customBreathChimeUri,
                onUriSelected = { uri ->
                    onSettingsChanged(settings.copy(customBreathChimeUri = uri))
                },
                onPreview = { playPreview("breath_chime", settings.customBreathChimeUri) },
                onStopPreview = { stopPreview() },
                isPlaying = currentlyPlaying == "breath_chime"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Reset all button
            OutlinedButton(
                onClick = {
                    onSettingsChanged(settings.copy(
                        customIntroBowlUri = null,
                        customBreathChimeUri = null,
                        customHoldChimeUri = null
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset All to Default Sounds")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AudioFileCard(
    title: String,
    description: String,
    currentUri: String?,
    onUriSelected: (String?) -> Unit,
    onPreview: () -> Unit,
    onStopPreview: () -> Unit,
    isPlaying: Boolean
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistable permission for the URI
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Permission might already exist or not be available
            }
            onUriSelected(it.toString())
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Current status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Status: ",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (currentUri != null) {
                    Text(
                        text = "Custom file",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Default (bundled)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (currentUri != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getFileName(currentUri),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview / Stop button
            if (isPlaying) {
                Button(
                    onClick = onStopPreview,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text("\u25A0 Stop Preview")
                }
            } else {
                Button(
                    onClick = onPreview,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("\u25B6 Preview Sound")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File selection buttons
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        launcher.launch(arrayOf("audio/*"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (currentUri != null) "Change File" else "Select File")
                }

                if (currentUri != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { onUriSelected(null) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}

private fun getFileName(uri: String): String {
    return try {
        val path = Uri.parse(uri).lastPathSegment ?: uri
        if (path.contains("/")) {
            path.substringAfterLast("/")
        } else {
            path
        }
    } catch (e: Exception) {
        uri.takeLast(30)
    }
}
