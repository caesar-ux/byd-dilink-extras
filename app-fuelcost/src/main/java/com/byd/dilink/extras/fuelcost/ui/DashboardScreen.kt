package com.byd.dilink.extras.fuelcost.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.byd.dilink.extras.fuelcost.viewmodel.FuelCostViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onNavigateToLogFuel: () -> Unit,
    onNavigateToLogCharge: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: FuelCostViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showOdometerDialog by remember { mutableStateOf(false) }
    var odometerInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DiLinkBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DM-i Cost Tracker",
                    color = DiLinkTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onNavigateToStatistics) {
                        Icon(Icons.Default.BarChart, "Statistics", tint = FuelGreen)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = DiLinkTextSecondary)
                    }
                }
            }
        }

        // Cost per km cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Petrol card
                CostCard(
                    title = "Petrol",
                    icon = Icons.Default.LocalGasStation,
                    costPerKm = state.costPerKm.fuelCostPerKm,
                    totalCost = state.costPerKm.totalFuelCost,
                    color = StatusOrange,
                    formatCurrency = { viewModel.formatCurrency(it) },
                    currencyLabel = viewModel.currencyLabel(),
                    modifier = Modifier.weight(1f)
                )
                // Electric card
                CostCard(
                    title = "Electric",
                    icon = Icons.Default.FlashOn,
                    costPerKm = state.costPerKm.electricCostPerKm,
                    totalCost = state.costPerKm.totalElectricCost,
                    color = FuelGreen,
                    formatCurrency = { viewModel.formatCurrency(it) },
                    currencyLabel = viewModel.currencyLabel(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Savings card
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = FuelGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Savings vs Pure Petrol",
                            color = DiLinkTextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = viewModel.formatCurrency(state.monthlySavings),
                            color = if (state.monthlySavings >= 0) FuelGreen else StatusRed,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // EV% bar
        item {
            SectionCard {
                Text(
                    text = "Electric vs Petrol Split",
                    color = DiLinkTextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))

                val evPct = state.evPercentage.coerceIn(0.0, 100.0).toFloat() / 100f

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    val barWidth = size.width
                    val barHeight = size.height
                    val cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)

                    // Background (petrol)
                    drawRoundRect(
                        color = Color(0xFFFF9800).copy(alpha = 0.3f),
                        size = Size(barWidth, barHeight),
                        cornerRadius = cornerRadius
                    )

                    // EV portion
                    if (evPct > 0f) {
                        drawRoundRect(
                            color = Color(0xFF00E676),
                            size = Size(barWidth * evPct, barHeight),
                            cornerRadius = cornerRadius
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${String.format("%.0f", state.evPercentage)}% electric",
                        color = FuelGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${String.format("%.0f", 100 - state.evPercentage)}% petrol",
                        color = StatusOrange,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Current odometer
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Odometer",
                            color = DiLinkTextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${String.format("%,d", state.currentOdometer)} km",
                            color = DiLinkTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            odometerInput = if (state.currentOdometer > 0) state.currentOdometer.toString() else ""
                            showOdometerDialog = true
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Update")
                    }
                }
            }
        }

        // Action buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LargeIconButton(
                    text = "Log Fuel",
                    icon = Icons.Default.LocalGasStation,
                    color = StatusOrange,
                    modifier = Modifier.weight(1f),
                    height = 64.dp,
                    onClick = onNavigateToLogFuel
                )
                LargeIconButton(
                    text = "Log Charge",
                    icon = Icons.Default.FlashOn,
                    color = FuelGreen,
                    modifier = Modifier.weight(1f),
                    height = 64.dp,
                    onClick = onNavigateToLogCharge
                )
            }
        }

        // Recent entries
        if (state.recentFuelRecords.isNotEmpty() || state.recentChargeRecords.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Entries",
                    color = DiLinkTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

            items(state.recentFuelRecords.take(3)) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DiLinkSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = StatusOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${record.liters}L ${record.fuelType}",
                                color = DiLinkTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = dateFormat.format(Date(record.date)),
                                color = DiLinkTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = viewModel.formatCurrency(record.totalCostIqd),
                            color = StatusOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            items(state.recentChargeRecords.take(3)) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DiLinkSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = FuelGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${record.kwhCharged} kWh (${record.source})",
                                color = DiLinkTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = dateFormat.format(Date(record.date)),
                                color = DiLinkTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = viewModel.formatCurrency(record.totalCostIqd),
                            color = FuelGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    // Odometer dialog
    if (showOdometerDialog) {
        AlertDialog(
            onDismissRequest = { showOdometerDialog = false },
            title = { Text("Update Odometer", color = DiLinkTextPrimary) },
            text = {
                OutlinedTextField(
                    value = odometerInput,
                    onValueChange = { odometerInput = it },
                    label = { Text("Odometer (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FuelGreen,
                        unfocusedBorderColor = DiLinkSurfaceVariant,
                        focusedTextColor = DiLinkTextPrimary,
                        unfocusedTextColor = DiLinkTextPrimary,
                        cursorColor = FuelGreen,
                        focusedLabelColor = FuelGreen,
                        unfocusedLabelColor = DiLinkTextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        odometerInput.toIntOrNull()?.let {
                            viewModel.updateOdometer(it)
                        }
                        showOdometerDialog = false
                    }
                ) {
                    Text("Save", color = FuelGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOdometerDialog = false }) {
                    Text("Cancel", color = DiLinkTextSecondary)
                }
            },
            containerColor = DiLinkSurfaceElevated
        )
    }
}

@Composable
private fun CostCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    costPerKm: Double,
    totalCost: Double,
    color: Color,
    formatCurrency: (Double) -> String,
    currencyLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DiLinkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = DiLinkTextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = String.format("%.1f", costPerKm),
                color = color,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$currencyLabel/km",
                color = DiLinkTextSecondary,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Total: ${formatCurrency(totalCost)}",
                color = DiLinkTextMuted,
                fontSize = 12.sp
            )
        }
    }
}
