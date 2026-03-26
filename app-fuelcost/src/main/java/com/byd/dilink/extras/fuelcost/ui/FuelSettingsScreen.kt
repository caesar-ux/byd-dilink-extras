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
import com.byd.dilink.extras.fuelcost.viewmodel.FuelCostViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*

@Composable
fun FuelSettingsScreen(
    onBack: () -> Unit,
    viewModel: FuelCostViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopBarWithBack(title = "Fuel Settings", onBack = onBack) },
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

            // Battery capacity
            item {
                SectionCard {
                    Text(
                        text = "Battery Capacity",
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Select your BYD DM-i battery variant",
                        color = DiLinkTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(8.3, 18.3).forEach { capacity ->
                            FilterChip(
                                selected = state.batteryCapacityKwh == capacity,
                                onClick = { viewModel.updateBatteryCapacity(capacity) },
                                label = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "$capacity kWh",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            if (capacity == 8.3) "Standard" else "Long Range",
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f).height(64.dp),
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

            // Default fuel price
            item {
                SectionCard {
                    Text(
                        text = "Default Fuel Price",
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    var fuelPriceText by remember(state.defaultFuelPriceIqd) {
                        mutableStateOf(state.defaultFuelPriceIqd.toInt().toString())
                    }

                    OutlinedTextField(
                        value = fuelPriceText,
                        onValueChange = { value ->
                            fuelPriceText = value
                            value.toDoubleOrNull()?.let { viewModel.updateDefaultFuelPrice(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Price per liter (IQD)") },
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
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Iraq average: ~750 IQD/L (Regular)",
                        color = DiLinkTextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Default electricity price
            item {
                SectionCard {
                    Text(
                        text = "Default Electricity Price",
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    var electricPriceText by remember(state.defaultElectricityPriceIqd) {
                        mutableStateOf(state.defaultElectricityPriceIqd.toInt().toString())
                    }

                    OutlinedTextField(
                        value = electricPriceText,
                        onValueChange = { value ->
                            electricPriceText = value
                            value.toDoubleOrNull()?.let { viewModel.updateDefaultElectricityPrice(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Price per kWh (IQD)") },
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
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Benchmark consumption
            item {
                SectionCard {
                    Text(
                        text = "Benchmark Consumption",
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Used to calculate savings vs a pure petrol car",
                        color = DiLinkTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    var benchmarkText by remember(state.benchmarkLPer100Km) {
                        mutableStateOf(state.benchmarkLPer100Km.toString())
                    }

                    OutlinedTextField(
                        value = benchmarkText,
                        onValueChange = { value ->
                            benchmarkText = value
                            value.toDoubleOrNull()?.let { viewModel.updateBenchmark(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("L/100km benchmark") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Default: 7.0 L/100km (typical sedan in Iraq)",
                        color = DiLinkTextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Currency
            item {
                SectionCard {
                    Text(
                        text = "Currency",
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = !state.useUsd,
                            onClick = { viewModel.updateUseUsd(false) },
                            label = {
                                Text("IQD", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DiLinkSurfaceVariant,
                                selectedContainerColor = FuelGreen.copy(alpha = 0.3f),
                                labelColor = DiLinkTextSecondary,
                                selectedLabelColor = FuelGreen
                            )
                        )
                        FilterChip(
                            selected = state.useUsd,
                            onClick = { viewModel.updateUseUsd(true) },
                            label = {
                                Text("USD", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DiLinkSurfaceVariant,
                                selectedContainerColor = FuelGreen.copy(alpha = 0.3f),
                                labelColor = DiLinkTextSecondary,
                                selectedLabelColor = FuelGreen
                            )
                        )
                    }

                    if (state.useUsd) {
                        Spacer(Modifier.height(8.dp))

                        var exchangeText by remember(state.usdExchangeRate) {
                            mutableStateOf(state.usdExchangeRate.toInt().toString())
                        }

                        OutlinedTextField(
                            value = exchangeText,
                            onValueChange = { value ->
                                exchangeText = value
                                value.toDoubleOrNull()?.let { viewModel.updateExchangeRate(it) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Exchange Rate (IQD per USD)") },
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
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
