package com.byd.dilink.extras.fuelcost.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.byd.dilink.extras.data.dao.FuelRecord
import com.byd.dilink.extras.fuelcost.viewmodel.FuelCostViewModel
import com.byd.dilink.extras.fuelcost.viewmodel.LogFormViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*

@Composable
fun LogFuelScreen(
    onBack: () -> Unit,
    fuelCostViewModel: FuelCostViewModel = hiltViewModel(),
    logFormViewModel: LogFormViewModel = viewModel()
) {
    val formState by logFormViewModel.fuelForm.collectAsState()
    val mainState by fuelCostViewModel.uiState.collectAsState()

    // Pre-fill odometer from latest
    LaunchedEffect(mainState.currentOdometer) {
        if (formState.odometer.isEmpty() && mainState.currentOdometer > 0) {
            logFormViewModel.updateFuelOdometer(mainState.currentOdometer.toString())
        }
    }

    // Pre-fill default price
    LaunchedEffect(mainState.defaultFuelPriceIqd) {
        if (formState.pricePerLiter == "750") {
            logFormViewModel.updateFuelPricePerLiter(mainState.defaultFuelPriceIqd.toInt().toString())
        }
    }

    Scaffold(
        topBar = { TopBarWithBack(title = "Log Fuel Purchase", onBack = onBack) },
        containerColor = DiLinkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Odometer
            item {
                FormField(
                    value = formState.odometer,
                    onValueChange = { logFormViewModel.updateFuelOdometer(it) },
                    label = "Odometer (km)",
                    icon = Icons.Default.Speed,
                    keyboardType = KeyboardType.Number
                )
            }

            // Liters
            item {
                FormField(
                    value = formState.liters,
                    onValueChange = { logFormViewModel.updateFuelLiters(it) },
                    label = "Liters",
                    icon = Icons.Default.LocalGasStation,
                    keyboardType = KeyboardType.Decimal
                )
            }

            // Total cost and price per liter side by side
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FormField(
                        value = formState.totalCost,
                        onValueChange = { logFormViewModel.updateFuelTotalCost(it) },
                        label = "Total Cost (IQD)",
                        icon = Icons.Default.AttachMoney,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    FormField(
                        value = formState.pricePerLiter,
                        onValueChange = { logFormViewModel.updateFuelPricePerLiter(it) },
                        label = "Price/Liter (IQD)",
                        icon = Icons.Default.Paid,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Fuel type
            item {
                SectionCard {
                    Text("Fuel Type", color = DiLinkTextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("Regular", "Premium").forEach { type ->
                            FilterChip(
                                selected = formState.fuelType == type,
                                onClick = { logFormViewModel.updateFuelType(type) },
                                label = { Text(type, fontSize = 14.sp) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = DiLinkSurfaceVariant,
                                    selectedContainerColor = StatusOrange.copy(alpha = 0.3f),
                                    labelColor = DiLinkTextSecondary,
                                    selectedLabelColor = StatusOrange
                                )
                            )
                        }
                    }
                }
            }

            // Full tank toggle
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Full Tank", color = DiLinkTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "For accurate consumption calculation",
                                color = DiLinkTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = formState.isFullTank,
                            onCheckedChange = { logFormViewModel.updateFullTank(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StatusOrange,
                                checkedTrackColor = StatusOrange.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Station name
            item {
                FormField(
                    value = formState.stationName,
                    onValueChange = { logFormViewModel.updateFuelStation(it) },
                    label = "Station Name (optional)",
                    icon = Icons.Default.LocationOn,
                    keyboardType = KeyboardType.Text
                )
            }

            // Notes
            item {
                FormField(
                    value = formState.notes,
                    onValueChange = { logFormViewModel.updateFuelNotes(it) },
                    label = "Notes (optional)",
                    icon = Icons.Default.Notes,
                    keyboardType = KeyboardType.Text
                )
            }

            // Save button
            item {
                Button(
                    onClick = {
                        val record = FuelRecord(
                            date = System.currentTimeMillis(),
                            odometerKm = formState.odometer.toIntOrNull() ?: 0,
                            liters = formState.liters.toDoubleOrNull() ?: 0.0,
                            totalCostIqd = formState.totalCost.toDoubleOrNull() ?: 0.0,
                            pricePerLiter = formState.pricePerLiter.toDoubleOrNull() ?: 0.0,
                            fuelType = formState.fuelType,
                            isFullTank = formState.isFullTank,
                            stationName = formState.stationName.ifBlank { null },
                            notes = formState.notes.ifBlank { null }
                        )
                        fuelCostViewModel.insertFuelRecord(record)
                        // Also update odometer
                        formState.odometer.toIntOrNull()?.let {
                            fuelCostViewModel.updateOdometer(it)
                        }
                        logFormViewModel.resetFuelForm()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOrange),
                    enabled = formState.isValid
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Fuel Record", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
internal fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, color = DiLinkTextMuted, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = DiLinkTextSecondary, modifier = Modifier.size(20.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FuelGreen,
            unfocusedBorderColor = DiLinkSurfaceVariant,
            focusedTextColor = DiLinkTextPrimary,
            unfocusedTextColor = DiLinkTextPrimary,
            cursorColor = FuelGreen,
            focusedLabelColor = FuelGreen,
            unfocusedLabelColor = DiLinkTextMuted
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
