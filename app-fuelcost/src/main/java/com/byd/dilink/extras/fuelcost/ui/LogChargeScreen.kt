package com.byd.dilink.extras.fuelcost.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.byd.dilink.extras.data.dao.ChargeRecord
import com.byd.dilink.extras.fuelcost.viewmodel.FuelCostViewModel
import com.byd.dilink.extras.fuelcost.viewmodel.LogFormViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*

@Composable
fun LogChargeScreen(
    onBack: () -> Unit,
    fuelCostViewModel: FuelCostViewModel = hiltViewModel(),
    logFormViewModel: LogFormViewModel = viewModel()
) {
    val formState by logFormViewModel.chargeForm.collectAsState()
    val mainState by fuelCostViewModel.uiState.collectAsState()

    // Pre-fill odometer and battery capacity
    LaunchedEffect(mainState.currentOdometer) {
        if (formState.odometer.isEmpty() && mainState.currentOdometer > 0) {
            logFormViewModel.updateChargeOdometer(mainState.currentOdometer.toString())
        }
    }

    LaunchedEffect(mainState.batteryCapacityKwh) {
        logFormViewModel.setBatteryCapacity(mainState.batteryCapacityKwh)
    }

    Scaffold(
        topBar = { TopBarWithBack(title = "Log Charging Session", onBack = onBack) },
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
                    onValueChange = { logFormViewModel.updateChargeOdometer(it) },
                    label = "Odometer (km)",
                    icon = Icons.Default.Speed,
                    keyboardType = KeyboardType.Number
                )
            }

            // kWh charged
            item {
                FormField(
                    value = formState.kwhCharged,
                    onValueChange = { logFormViewModel.updateChargeKwh(it) },
                    label = "kWh Charged",
                    icon = Icons.Default.FlashOn,
                    keyboardType = KeyboardType.Decimal
                )
            }

            // SOC% estimation
            item {
                SectionCard {
                    Text(
                        text = "Or estimate from SOC%",
                        color = DiLinkTextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Battery: ${mainState.batteryCapacityKwh} kWh",
                        color = DiLinkTextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FormField(
                            value = formState.startSocPercent,
                            onValueChange = { logFormViewModel.updateChargeStartSoc(it) },
                            label = "Start SOC %",
                            icon = Icons.Default.BatteryAlert,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        FormField(
                            value = formState.endSocPercent,
                            onValueChange = { logFormViewModel.updateChargeEndSoc(it) },
                            label = "End SOC %",
                            icon = Icons.Default.BatteryFull,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Cost
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FormField(
                        value = formState.totalCost,
                        onValueChange = { logFormViewModel.updateChargeTotalCost(it) },
                        label = "Total Cost (IQD)",
                        icon = Icons.Default.AttachMoney,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    FormField(
                        value = formState.costPerKwh,
                        onValueChange = { logFormViewModel.updateChargeCostPerKwh(it) },
                        label = "Cost/kWh (IQD)",
                        icon = Icons.Default.Paid,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Charging source
            item {
                SectionCard {
                    Text("Charging Source", color = DiLinkTextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Home", "Public", "Free").forEach { source ->
                            FilterChip(
                                selected = formState.source == source,
                                onClick = { logFormViewModel.updateChargeSource(source) },
                                label = { Text(source, fontSize = 14.sp) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = DiLinkSurfaceVariant,
                                    selectedContainerColor = FuelGreen.copy(alpha = 0.3f),
                                    labelColor = DiLinkTextSecondary,
                                    selectedLabelColor = FuelGreen
                                )
                            )
                        }
                    }
                }
            }

            // Duration
            item {
                FormField(
                    value = formState.durationMin,
                    onValueChange = { logFormViewModel.updateChargeDuration(it) },
                    label = "Duration (minutes, optional)",
                    icon = Icons.Default.Timer,
                    keyboardType = KeyboardType.Number
                )
            }

            // Notes
            item {
                FormField(
                    value = formState.notes,
                    onValueChange = { logFormViewModel.updateChargeNotes(it) },
                    label = "Notes (optional)",
                    icon = Icons.Default.Notes,
                    keyboardType = KeyboardType.Text
                )
            }

            // Save button
            item {
                Button(
                    onClick = {
                        val kwh = formState.calculateKwh()
                        val record = ChargeRecord(
                            date = System.currentTimeMillis(),
                            odometerKm = formState.odometer.toIntOrNull() ?: 0,
                            kwhCharged = kwh,
                            totalCostIqd = formState.totalCost.toDoubleOrNull() ?: 0.0,
                            costPerKwh = if (kwh > 0) (formState.totalCost.toDoubleOrNull()
                                ?: 0.0) / kwh else 0.0,
                            source = formState.source,
                            startSocPercent = formState.startSocPercent.toIntOrNull(),
                            endSocPercent = formState.endSocPercent.toIntOrNull(),
                            durationMin = formState.durationMin.toIntOrNull(),
                            notes = formState.notes.ifBlank { null }
                        )
                        fuelCostViewModel.insertChargeRecord(record)
                        // Also update odometer
                        formState.odometer.toIntOrNull()?.let {
                            fuelCostViewModel.updateOdometer(it)
                        }
                        logFormViewModel.resetChargeForm()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FuelGreen),
                    enabled = formState.isValid
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Charge Record", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
