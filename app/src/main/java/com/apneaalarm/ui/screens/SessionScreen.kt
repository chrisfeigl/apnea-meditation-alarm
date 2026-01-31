package com.apneaalarm.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.apneaalarm.ui.components.HelpContent
import com.apneaalarm.ui.components.HelpDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apneaalarm.session.SessionProgress
import com.apneaalarm.session.SessionState

@Composable
fun SessionScreen(
    progress: SessionProgress,
    onStop: () -> Unit,
    onNavigateHome: () -> Unit,
    onSkipIntro: () -> Unit,
    onSnooze: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    snoozeDurationMinutes: Int,
    snoozeEnabled: Boolean
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = when (val state = progress.state) {
            is SessionState.IntroBowl -> Color(0xFF263238)
            is SessionState.PreHoldCountdown -> Color(0xFF37474F)
            is SessionState.Holding -> Color(0xFF1A237E)
            is SessionState.Breathing -> Color(0xFF1B5E20)
            is SessionState.Finishing -> Color(0xFF4A148C)
            is SessionState.Paused -> when (state.previousState) {
                is SessionState.IntroBowl -> Color(0xFF263238)
                is SessionState.PreHoldCountdown -> Color(0xFF37474F)
                is SessionState.Holding -> Color(0xFF1A237E)
                is SessionState.Breathing -> Color(0xFF1B5E20)
                is SessionState.Finishing -> Color(0xFF4A148C)
                else -> Color(0xFF424242)
            }
            else -> MaterialTheme.colorScheme.background
        },
        label = "backgroundColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Home button in top-left corner - TEXT BASED
        TextButton(
            onClick = onNavigateHome,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = "← Home",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }

        // Help button in top-right corner
        TextButton(
            onClick = { showHelpDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Text(
                text = "?",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            when (val state = progress.state) {
                is SessionState.Idle -> {
                    Text(
                        text = "Starting...",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(64.dp))
                    SessionButtons(onStop = onStop, onPause = onPause, onHome = onNavigateHome)
                }

                is SessionState.IntroBowl -> {
                    IntroBowlContent(state)
                    Spacer(modifier = Modifier.height(48.dp))
                    IntroBowlButtons(
                        onSkipIntro = onSkipIntro,
                        onSnooze = onSnooze,
                        onPause = onPause,
                        onStop = onStop,
                        snoozeDurationMinutes = snoozeDurationMinutes,
                        snoozeEnabled = snoozeEnabled
                    )
                }

                is SessionState.PreHoldCountdown -> {
                    PreHoldCountdownContent(state)
                    Spacer(modifier = Modifier.height(64.dp))
                    SessionButtons(onStop = onStop, onPause = onPause, onHome = onNavigateHome)
                }

                is SessionState.Holding -> {
                    HoldingContent(state)
                    Spacer(modifier = Modifier.height(64.dp))
                    SessionButtons(onStop = onStop, onPause = onPause, onHome = onNavigateHome)
                }

                is SessionState.Breathing -> {
                    BreathingContent(state)
                    Spacer(modifier = Modifier.height(64.dp))
                    SessionButtons(onStop = onStop, onPause = onPause, onHome = onNavigateHome)
                }

                is SessionState.Finishing -> {
                    FinishingContent(onStop = onStop, onPause = onPause, onHome = onNavigateHome, totalCycles = progress.totalCycles)
                }

                is SessionState.Stopped -> {
                    Text(
                        text = "Session Ended",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onNavigateHome,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Return Home")
                    }
                }

                is SessionState.Paused -> {
                    PausedContent(
                        previousState = state.previousState,
                        onResume = onResume,
                        onStop = onStop,
                        onHome = onNavigateHome
                    )
                }
            }
        }
    }

    // Help Dialog
    if (showHelpDialog) {
        HelpDialog(
            title = "During a Session",
            content = HelpContent.session,
            onDismiss = { showHelpDialog = false }
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun IntroBowlButtons(
    onSkipIntro: () -> Unit,
    onSnooze: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    snoozeDurationMinutes: Int,
    snoozeEnabled: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Skip Intro and Pause buttons side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Skip Intro button - prominent
            Button(
                onClick = onSkipIntro,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B5E20)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = "SKIP INTRO",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            // Pause button
            OutlinedButton(
                onClick = onPause,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF90CAF9)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = "PAUSE",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF90CAF9)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (snoozeEnabled) {
            // Snooze and Cancel buttons side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Snooze button
                OutlinedButton(
                    onClick = onSnooze,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFFB74D)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Text(
                        text = "SNOOZE",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFB74D)
                    )
                }

                // Cancel/Stop button
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Text(
                        text = "CANCEL",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
            }
        } else {
            // Only Cancel button (full width)
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "CANCEL",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SessionButtons(onStop: () -> Unit, onPause: () -> Unit, onHome: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Pause button - prominent
        Button(
            onClick = onPause,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1565C0)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "PAUSE",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Stop button
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = "STOP",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Home button - TEXT BASED
            OutlinedButton(
                onClick = onHome,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = "← HOME",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun IntroBowlContent(state: SessionState.IntroBowl) {
    val progressValue by animateFloatAsState(
        targetValue = state.elapsedSeconds.toFloat() / 54f,
        label = "progress"
    )

    val phaseText = when (state.phase) {
        SessionState.IntroBowl.IntroBowlPhase.FADING_IN -> "Waking Up"
        SessionState.IntroBowl.IntroBowlPhase.HOLDING -> "Bowl at Full"
        SessionState.IntroBowl.IntroBowlPhase.FADING_OUT -> "Preparing"
    }

    val subText = when (state.phase) {
        SessionState.IntroBowl.IntroBowlPhase.FADING_IN -> "Bowl fading in..."
        SessionState.IntroBowl.IntroBowlPhase.HOLDING -> "Bowl sustaining..."
        SessionState.IntroBowl.IntroBowlPhase.FADING_OUT -> "Bowl fading out..."
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = phaseText,
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFFB0BEC5)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = progressValue,
                modifier = Modifier.size(200.dp),
                strokeWidth = 8.dp,
                color = Color(0xFFB0BEC5),
            )
            Text(
                text = "${54 - state.elapsedSeconds}",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = subText,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PreHoldCountdownContent(state: SessionState.PreHoldCountdown) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Get Ready",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFFFFB74D)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = (4 - state.countdownSeconds).toFloat() / 4f,
                modifier = Modifier.size(200.dp),
                strokeWidth = 8.dp,
                color = Color(0xFFFFB74D),
            )
            Text(
                text = if (state.countdownSeconds > 0) "${state.countdownSeconds}" else "HOLD",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (state.countdownSeconds > 0) "Breathe deeply..." else "Hold your breath!",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun HoldingContent(state: SessionState.Holding) {
    val remaining = state.targetSeconds - state.elapsedSeconds
    val progressValue by animateFloatAsState(
        targetValue = state.elapsedSeconds.toFloat() / state.targetSeconds,
        label = "progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HOLD",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            ),
            color = Color(0xFF64B5F6)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cycle ${state.cycleIndex + 1} of ${state.totalCycles}",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = progressValue,
                modifier = Modifier.size(240.dp),
                strokeWidth = 12.dp,
                color = Color(0xFF64B5F6),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$remaining",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = "seconds",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        if (state.isCountdown) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Almost there!",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFB74D)
            )
        }
    }
}

@Composable
private fun BreathingContent(state: SessionState.Breathing) {
    val remaining = state.targetSeconds - state.elapsedSeconds
    val progressValue by animateFloatAsState(
        targetValue = state.elapsedSeconds.toFloat() / state.targetSeconds,
        label = "progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BREATHE",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            ),
            color = Color(0xFF81C784)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cycle ${state.cycleIndex + 1} of ${state.totalCycles}",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = progressValue,
                modifier = Modifier.size(240.dp),
                strokeWidth = 12.dp,
                color = Color(0xFF81C784),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$remaining",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = "seconds",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        if (state.isCountdown) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Get ready to hold!",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFFB74D)
            )
        }
    }
}

@Composable
private fun FinishingContent(onStop: () -> Unit, onPause: () -> Unit, onHome: () -> Unit, totalCycles: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Session Complete!",
            style = MaterialTheme.typography.displaySmall,
            color = Color(0xFFCE93D8),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "$totalCycles cycles finished",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Large prominent STOP button
        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F)
            ),
            shape = CircleShape,
            modifier = Modifier.size(200.dp)
        ) {
            Text(
                text = "STOP",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tap STOP to end the session",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Pause and Home buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPause,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF90CAF9)
                )
            ) {
                Text(
                    text = "PAUSE",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            OutlinedButton(
                onClick = onHome,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "← HOME",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun PausedContent(
    previousState: SessionState,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onHome: () -> Unit
) {
    val pausedPhaseText = when (previousState) {
        is SessionState.IntroBowl -> "Intro"
        is SessionState.PreHoldCountdown -> "Get Ready"
        is SessionState.Holding -> "Hold - Cycle ${previousState.cycleIndex + 1}"
        is SessionState.Breathing -> "Breathe - Cycle ${previousState.cycleIndex + 1}"
        is SessionState.Finishing -> "Outro"
        else -> "Session"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PAUSED",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            ),
            color = Color(0xFF90CAF9)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = pausedPhaseText,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Large Resume button
        Button(
            onClick = onResume,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1B5E20)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Text(
                text = "RESUME",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Stop button
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = "STOP",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }

            // Home button
            OutlinedButton(
                onClick = onHome,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Text(
                    text = "← HOME",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}
