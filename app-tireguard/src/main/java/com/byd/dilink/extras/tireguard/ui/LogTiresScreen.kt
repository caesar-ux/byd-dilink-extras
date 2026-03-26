package com.byd.dilink.extras.tireguard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun LogTiresScreen(
    onBack: () -> Unit,
    viewModel: TireGuardViewModel = hiltViewModel()
) {
    var flValue by remember { mutableStateOf("") }
    var frValue by remember { mutableStateOf("") }
    var rlValue by remember { mutableStateOf("") }
    var rrValue by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("") }
    var tireBrand by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var flCondition by remember { mutableStateOf("Good") }
    var frCondition by remember { mutableStateOf("Good") }
    var rlCondition by remember { mutableStateOf("Good") }
    var rrCondition by remember { mutableStateOf("Good") }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val conditions = listOf("Good", "Worn", "Damaged", "New")

    Scaffold(
        topBar = { TopBarWithBack(title = "Log Tire Pressures", onBack = onBack) },
        containerColor = DiLinkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Car-layout input area
            SectionCard {
                Text(
                    "Tire Pressures (bar)",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary
                )
                Spacer(Modifier.height(12.dp))

                // Front tires
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TirePressureInput(
                        label = "FL",
                        value = flValue,
                        onValueChange = { if (it.length <= 4) flValue = it },
                        modifier = Modifier.weight(1f)
                    )
                    // Car hood indicator
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▲ FRONT", color = DiLinkTextMuted, fontSize = 10.sp)
                    }
                    TirePressureInput(
                        label = "FR",
                        value = frValue,
                        onValueChange = { if (it.length <= 4) frValue = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Rear tires
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TirePressureInput(
                        label = "RL",
                        value = rlValue,
                        onValueChange = { if (it.length <= 4) rlValue = it },
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▼ REAR", color = DiLinkTextMuted, fontSize = 10.sp)
                    }
                    TirePressureInput(
                        label = "RR",
                        value = rrValue,
                        onValueChange = { if (it.length <= 4) rrValue = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Use Recommended button
                OutlinedButton(
                    onClick = {
                        flValue = "2.4"; frValue = "2.4"
                        rlValue = "2.4"; rrValue = "2.4"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Recommend, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Use Recommended (2.4 bar)")
                }
            }

            // Condition dropdowns
            SectionCard {
                Text(
                    "Tire Condition",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConditionDropdown("FL", flCondition, conditions, Modifier.weight(1f)) { flCondition = it }
                    ConditionDropdown("FR", frCondition, conditions, Modifier.weight(1f)) { frCondition = it }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConditionDropdown("RL", rlCondition, conditions, Modifier.weight(1f)) { rlCondition = it }
                    ConditionDropdown("RR", rrCondition, conditions, Modifier.weight(1f)) { rrCondition = it }
                }
            }

            // Date & Odometer
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

                // Odometer
                OutlinedTextField(
                    value = odometer,
                    onValueChange = { odometer = it.filter { c -> c.isDigit() } },
                    label = { Text("Odometer (km)") },
                    placeholder = { Text("Optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TireBlue,
                        unfocusedBorderColor = DiLinkSurfaceVariant
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Tire brand
                OutlinedTextField(
                    value = tireBrand,
                    onValueChange = { tireBrand = it },
                    label = { Text("Tire Brand/Model") },
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TireBlue,
                        unfocusedBorderColor = DiLinkSurfaceVariant
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TireBlue,
                        unfocusedBorderColor = DiLinkSurfaceVariant
                    )
                )
            }

            // Save button
            Button(
                onClick = {
                    val fl = flValue.toDoubleOrNull() ?: return@Button
                    val fr = frValue.toDoubleOrNull() ?: return@Button
                    val rl = rlValue.toDoubleOrNull() ?: return@Button
                    val rr = rrValue.toDoubleOrNull() ?: return@Button
                    viewModel.logTires(
                        date = selectedDate,
                        fl = fl, fr = fr, rl = rl, rr = rr,
                        odometerKm = odometer.toIntOrNull(),
                        flCond = flCondition, frCond = frCondition,
                        rlCond = rlCondition, rrCond = rrCondition,
                        tireBrand = tireBrand.ifBlank { null },
                        notes = notes.ifBlank { null }
                    )
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TireBlue),
                enabled = flValue.toDoubleOrNull() != null &&
                        frValue.toDoubleOrNull() != null &&
                        rlValue.toDoubleOrNull() != null &&
                        rrValue.toDoubleOrNull() != null
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
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
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun TirePressureInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pressure = value.toDoubleOrNull()
    val borderColor = if (pressure != null) pressureColor(pressure) else DiLinkSurfaceVariant

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = DiLinkTextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { newVal ->
                if (newVal.length <= 4 && newVal.all { it.isDigit() || it == '.' }) {
                    onValueChange(newVal)
                }
            },
            textStyle = TextStyle(
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (pressure != null) pressureColor(pressure) else DiLinkTextPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            placeholder = { Text("0.0", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionDropdown(
    label: String,
    selected: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "$label: $selected",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = TextStyle(fontSize = 12.sp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = DiLinkSurfaceVariant
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
fun LogTiresScreenPreview() {
    DiLinkExtrasTheme {
        Column(
            modifier = Modifier
                .background(DiLinkBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TirePressureInput("FL", "2.4", {})
            TirePressureInput("FR", "1.9", {})
        }
    }
}
