package com.byd.dilink.extras.tireguard.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byd.dilink.extras.tireguard.viewmodel.TireGuardViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*

private const val BAR_TO_PSI = 14.5038

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TireSettingsScreen(
    onBack: () -> Unit,
    viewModel: TireGuardViewModel = hiltViewModel()
) {
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()

    var recommendedPressure by remember(settings) { mutableStateOf(settings.recommendedPressure.toString()) }
    var lowThreshold by remember(settings) { mutableStateOf(settings.lowThreshold.toString()) }
    var highThreshold by remember(settings) { mutableStateOf(settings.highThreshold.toString()) }
    var batteryLow by remember(settings) { mutableStateOf(settings.batteryLowThreshold.toString()) }
    var batteryCritical by remember(settings) { mutableStateOf(settings.batteryCriticalThreshold.toString()) }
    var usePsi by remember(settings) { mutableStateOf(settings.usePsi) }
    var checkInterval by remember(settings) { mutableIntStateOf(settings.checkIntervalDays) }
    var rotationInterval by remember(settings) { mutableStateOf(settings.rotationIntervalKm.toString()) }

    val intervalOptions = listOf(7, 14, 30)

    Scaffold(
        topBar = { TopBarWithBack(title = "Settings", onBack = onBack) },
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
            // Tire Pressure Settings
            SectionCard {
                Text(
                    "Tire Pressure",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                val unitLabel = if (usePsi) "PSI" else "bar"

                OutlinedTextField(
                    value = recommendedPressure,
                    onValueChange = { recommendedPressure = it },
                    label = { Text("Recommended Pressure ($unitLabel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TireBlue,
                        unfocusedBorderColor = DiLinkSurfaceVariant
                    )
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = lowThreshold,
                    onValueChange = { lowThreshold = it },
                    label = { Text("Low Pressure Threshold ($unitLabel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StatusYellow,
                        unfocusedBorderColor = DiLinkSurfaceVariant
                    )
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = highThreshold,
                    onValueChange = { highThreshold = it },
                    label = { Text("High Pressure Threshold ($unitLabel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StatusRed,
                        unfocusedBorderColor = DiLinkSurfaceVariant
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Unit toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pressure Unit", color = DiLinkTextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("bar", color = if (!usePsi) TireBlue else DiLinkTextMuted)
                        Switch(
                            checked = usePsi,
                            onCheckedChange = { usePsi = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = TireBlue)
                        )
                        Text("PSI", color = if (usePsi) TireBlue else DiLinkTextMuted)
                    }
                }
            }

            // Battery settings
            SectionCard {
                Text(
                    "Battery Voltage",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = batteryLow,
                    onValueChange = { batteryLow = it },
                    label = { Text("Low Voltage Threshold (V)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StatusYellow,
                        unfocusedBorderColor = DiLinkSurfaceVariant
                    )
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = batteryCritical,
                    onValueChange = { batteryCritical = it },
                    label = { Text("Critical Voltage Threshold (V)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StatusRed,
                        unfocusedBorderColor = DiLinkSurfaceVariant
                    )
                )
            }

            // Check Reminder Interval
            SectionCard {
                Text(
                    "Check Reminder",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    intervalOptions.forEach { days ->
                        FilterChip(
                            selected = checkInterval == days,
                            onClick = { checkInterval = days },
                            label = { Text("${days}d") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TireBlue
                            )
                        )
                    }
                }
            }

            // Tire Rotation Interval
            SectionCard {
                Text(
                    "Tire Rotation",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = rotationInterval,
                    onValueChange = { rotationInterval = it.filter { c -> c.isDigit() } },
                    label = { Text("Rotation Interval (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TireBlue,
                        unfocusedBorderColor = DiLinkSurfaceVariant
                    )
                )
            }

            // Save button
            Button(
                onClick = {
                    viewModel.saveSettings(
                        recommendedPressure = recommendedPressure.toDoubleOrNull() ?: 2.4,
                        lowThreshold = lowThreshold.toDoubleOrNull() ?: 2.1,
                        highThreshold = highThreshold.toDoubleOrNull() ?: 2.7,
                        batteryLow = batteryLow.toDoubleOrNull() ?: 12.0,
                        batteryCritical = batteryCritical.toDoubleOrNull() ?: 11.5,
                        usePsi = usePsi,
                        checkIntervalDays = checkInterval,
                        rotationIntervalKm = rotationInterval.toIntOrNull() ?: 10000
                    )
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TireBlue)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Settings", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
fun TireSettingsPreview() {
    DiLinkExtrasTheme {
        Text("Settings Preview", color = DiLinkTextPrimary)
    }
}
