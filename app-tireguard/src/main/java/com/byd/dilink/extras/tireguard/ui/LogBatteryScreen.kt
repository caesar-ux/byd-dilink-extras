package com.byd.dilink.extras.tireguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.byd.dilink.extras.tireguard.viewmodel.TireGuardViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogBatteryScreen(
    onBack: () -> Unit,
    viewModel: TireGuardViewModel = hiltViewModel()
) {
    var voltageStr by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Good") }
    var engineState by remember { mutableStateOf("Off") }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val conditions = listOf("Good", "Weak", "Dead", "After charge")
    val engineStates = listOf("Off", "Running", "Just started")

    val voltage = voltageStr.toDoubleOrNull()
    val statusColor = when {
        voltage == null -> Color.Gray
        voltage < 12.0 || voltage > 13.0 -> StatusRed
        voltage < 12.4 -> StatusYellow
        else -> StatusGreen
    }

    Scaffold(
        topBar = { TopBarWithBack(title = "Log Battery Voltage", onBack = onBack) },
        containerColor = DiLinkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Voltage input with large field
            SectionCard {
                Text(
                    "Battery Voltage",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary
                )
                Spacer(Modifier.height(16.dp))

                // Color preview circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(statusColor.copy(alpha = 0.2f), CircleShape)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(statusColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Voltage field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = voltageStr,
                        onValueChange = { newVal ->
                            if (newVal.length <= 5 && newVal.all { it.isDigit() || it == '.' }) {
                                voltageStr = newVal
                            }
                        },
                        textStyle = TextStyle(
                            textAlign = TextAlign.Center,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "12.6",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 36.sp,
                                color = DiLinkTextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = statusColor,
                            unfocusedBorderColor = statusColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.width(200.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "V",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = DiLinkTextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Status text
                if (voltage != null) {
                    Spacer(Modifier.height(8.dp))
                    val statusText = when {
                        voltage < 12.0 -> "Critical — battery may be dead"
                        voltage > 13.0 -> "Overcharge warning"
                        voltage < 12.4 -> "Low — consider charging"
                        else -> "Good condition"
                    }
                    Text(
                        statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // Condition & Engine State
            SectionCard {
                // Date
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(dateFormat.format(Date(selectedDate)))
                }

                Spacer(Modifier.height(12.dp))

                // Condition dropdown
                var condExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = condExpanded,
                    onExpandedChange = { condExpanded = it }
                ) {
                    OutlinedTextField(
                        value = condition,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Condition") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(condExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = DiLinkSurfaceVariant,
                            focusedBorderColor = StatusYellow
                        )
                    )
                    ExposedDropdownMenu(expanded = condExpanded, onDismissRequest = { condExpanded = false }) {
                        conditions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = { condition = opt; condExpanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Engine state dropdown
                var engExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = engExpanded,
                    onExpandedChange = { engExpanded = it }
                ) {
                    OutlinedTextField(
                        value = engineState,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Engine State") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(engExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = DiLinkSurfaceVariant,
                            focusedBorderColor = StatusYellow
                        )
                    )
                    ExposedDropdownMenu(expanded = engExpanded, onDismissRequest = { engExpanded = false }) {
                        engineStates.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = { engineState = opt; engExpanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StatusYellow,
                        unfocusedBorderColor = DiLinkSurfaceVariant
                    )
                )
            }

            // Save
            Button(
                onClick = {
                    val v = voltageStr.toDoubleOrNull() ?: return@Button
                    viewModel.logBattery(
                        date = selectedDate,
                        voltage = v,
                        condition = condition,
                        engineState = engineState,
                        notes = notes.ifBlank { null }
                    )
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusYellow),
                enabled = voltage != null
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = DiLinkBackground)
                Spacer(Modifier.width(8.dp))
                Text("Save", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DiLinkBackground)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
fun LogBatteryScreenPreview() {
    DiLinkExtrasTheme {
        // Just show the battery status circle preview
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(StatusGreen.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.BatteryChargingFull,
                contentDescription = null,
                tint = StatusGreen,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
