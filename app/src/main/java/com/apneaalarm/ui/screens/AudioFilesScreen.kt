package com.apneaalarm.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apneaalarm.data.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioFilesScreen(
    preferences: UserPreferences,
    onNavigateBack: () -> Unit,
    onIntroBowlUriChanged: (String?) -> Unit,
    onBreathChimeUriChanged: (String?) -> Unit,
    onHoldChimeUriChanged: (String?) -> Unit,
    onPreviewSound: (uri: String?, soundType: String) -> Unit,
    onStopPreview: () -> Unit,
    isPreviewPlaying: Boolean
) {
    // Track which sound is currently being previewed
    var currentlyPreviewingType by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Files") },
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
                currentUri = preferences.customIntroBowlUri,
                onUriSelected = onIntroBowlUriChanged,
                onPreview = {
                    currentlyPreviewingType = "intro_bowl"
                    onPreviewSound(preferences.customIntroBowlUri, "intro_bowl")
                },
                onStopPreview = {
                    currentlyPreviewingType = null
                    onStopPreview()
                },
                isPlaying = isPreviewPlaying && currentlyPreviewingType == "intro_bowl"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hold Chime
            AudioFileCard(
                title = "Hold Chime",
                description = "Played to signal the start of each breath-hold interval.",
                currentUri = preferences.customHoldChimeUri,
                onUriSelected = onHoldChimeUriChanged,
                onPreview = {
                    currentlyPreviewingType = "hold_chime"
                    onPreviewSound(preferences.customHoldChimeUri, "hold_chime")
                },
                onStopPreview = {
                    currentlyPreviewingType = null
                    onStopPreview()
                },
                isPlaying = isPreviewPlaying && currentlyPreviewingType == "hold_chime"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Breathing Chime
            AudioFileCard(
                title = "Breathing Chime",
                description = "Played to signal the start of each breathing interval.",
                currentUri = preferences.customBreathChimeUri,
                onUriSelected = onBreathChimeUriChanged,
                onPreview = {
                    currentlyPreviewingType = "breath_chime"
                    onPreviewSound(preferences.customBreathChimeUri, "breath_chime")
                },
                onStopPreview = {
                    currentlyPreviewingType = null
                    onStopPreview()
                },
                isPlaying = isPreviewPlaying && currentlyPreviewingType == "breath_chime"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Reset all button
            OutlinedButton(
                onClick = {
                    onIntroBowlUriChanged(null)
                    onBreathChimeUriChanged(null)
                    onHoldChimeUriChanged(null)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset All to Default Sounds")
            }

            Spacer(modifier = Modifier.height(24.dp))
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
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
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
                    Text("■ Stop Preview")
                }
            } else {
                Button(
                    onClick = onPreview,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("▶ Preview Sound")
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
