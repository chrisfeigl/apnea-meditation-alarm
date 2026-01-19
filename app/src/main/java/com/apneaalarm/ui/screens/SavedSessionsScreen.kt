package com.apneaalarm.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.apneaalarm.R
import com.apneaalarm.data.IntensityLevel
import com.apneaalarm.data.SavedSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSessionsScreen(
    savedSessions: List<SavedSession>,
    globalM: Int,
    onNavigateBack: () -> Unit,
    onSessionSelected: (SavedSession) -> Unit,
    onDeleteSession: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Sessions") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("\u2190 Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (savedSessions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No saved sessions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Create a new session and save it to see it here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                items(savedSessions, key = { it.id }) { session ->
                    SavedSessionCard(
                        session = session,
                        globalM = globalM,
                        onTap = { onSessionSelected(session) },
                        onDelete = { onDeleteSession(session.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedSessionCard(
    session: SavedSession,
    globalM: Int,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    val settings = session.sessionSettings
    val intensityLevel = settings.intensityLevel(globalM)
    val intensityFactor = settings.intensityFactor(globalM)
    val numberOfIntervals = settings.numberOfIntervals()
    val breathHoldDuration = settings.breathHoldDurationSeconds(globalM)
    val totalTime = settings.totalSessionTimeSeconds(globalM)

    val accentColor = when (intensityLevel) {
        IntensityLevel.CALM -> Color(0xFF4CAF50)
        IntensityLevel.CHALLENGING -> Color(0xFFFFC107)
        IntensityLevel.HARD_TRAINING -> Color(0xFFFF9800)
        IntensityLevel.ADVANCED -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onTap,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$numberOfIntervals x ${breathHoldDuration}s holds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Text(
                    text = "${settings.trainingMode.name.lowercase().replaceFirstChar { it.uppercase() }} - ${formatDuration(totalTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Intensity indicator
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = accentColor
                    )
                ) {
                    Text(
                        text = "$intensityFactor",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (intensityLevel == IntensityLevel.CHALLENGING || intensityLevel == IntensityLevel.HARD_TRAINING)
                            Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_stop),
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
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
