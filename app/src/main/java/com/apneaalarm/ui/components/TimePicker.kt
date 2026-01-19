package com.apneaalarm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hourText by remember { mutableStateOf(String.format("%02d", initialHour)) }
    var minuteText by remember { mutableStateOf(String.format("%02d", initialMinute)) }
    var hourError by remember { mutableStateOf(false) }
    var minuteError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Alarm Time") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { value ->
                            if (value.length <= 2 && value.all { it.isDigit() }) {
                                hourText = value
                                val hour = value.toIntOrNull()
                                hourError = hour == null || hour < 0 || hour > 23
                            }
                        },
                        modifier = Modifier.width(80.dp),
                        textStyle = TextStyle(
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = hourError,
                        label = { Text("Hour") }
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { value ->
                            if (value.length <= 2 && value.all { it.isDigit() }) {
                                minuteText = value
                                val minute = value.toIntOrNull()
                                minuteError = minute == null || minute < 0 || minute > 59
                            }
                        },
                        modifier = Modifier.width(80.dp),
                        textStyle = TextStyle(
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = minuteError,
                        label = { Text("Min") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter hour (0-23) and minute (0-59)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = hourText.toIntOrNull()
                    val minute = minuteText.toIntOrNull()
                    if (hour != null && minute != null &&
                        hour in 0..23 && minute in 0..59) {
                        onTimeSelected(hour, minute)
                    }
                },
                enabled = !hourError && !minuteError &&
                        hourText.isNotEmpty() && minuteText.isNotEmpty()
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DurationPickerDialog(
    initialSeconds: Int,
    onDurationSelected: (seconds: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var minutesText by remember { mutableStateOf((initialSeconds / 60).toString()) }
    var secondsText by remember { mutableStateOf(String.format("%02d", initialSeconds % 60)) }
    var minutesError by remember { mutableStateOf(false) }
    var secondsError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Breath Hold Duration") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { value ->
                            if (value.length <= 2 && value.all { it.isDigit() }) {
                                minutesText = value
                                val minutes = value.toIntOrNull()
                                minutesError = minutes == null || minutes < 0 || minutes > 10
                            }
                        },
                        modifier = Modifier.width(80.dp),
                        textStyle = TextStyle(
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = minutesError,
                        label = { Text("Min") }
                    )

                    Text(
                        text = "m",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { value ->
                            if (value.length <= 2 && value.all { it.isDigit() }) {
                                secondsText = value
                                val seconds = value.toIntOrNull()
                                secondsError = seconds == null || seconds < 0 || seconds > 59
                            }
                        },
                        modifier = Modifier.width(80.dp),
                        textStyle = TextStyle(
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = secondsError,
                        label = { Text("Sec") }
                    )

                    Text(
                        text = "s",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your maximum static breath hold time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutes = minutesText.toIntOrNull() ?: 0
                    val seconds = secondsText.toIntOrNull() ?: 0
                    val totalSeconds = (minutes * 60) + seconds
                    if (totalSeconds > 0) {
                        onDurationSelected(totalSeconds)
                    }
                },
                enabled = !minutesError && !secondsError
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
