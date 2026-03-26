package com.byd.dilink.extras.prayer.ui

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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byd.dilink.extras.prayer.model.AsrMethod
import com.byd.dilink.extras.prayer.model.CalculationMethod
import com.byd.dilink.extras.prayer.model.PrayerName
import com.byd.dilink.extras.prayer.viewmodel.PrayerViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerSettingsScreen(
    onBack: () -> Unit,
    viewModel: PrayerViewModel = hiltViewModel()
) {
    val calculationMethod by viewModel.calculationMethod.collectAsStateWithLifecycle()
    val asrMethod by viewModel.asrMethod.collectAsStateWithLifecycle()
    val use24h by viewModel.use24h.collectAsStateWithLifecycle()
    val vibration by viewModel.tasbeehVibration.collectAsStateWithLifecycle()
    val sound by viewModel.tasbeehSound.collectAsStateWithLifecycle()
    val defaultGoal by viewModel.defaultTasbeehGoal.collectAsStateWithLifecycle()
    val adjustments by viewModel.manualAdjustments.collectAsStateWithLifecycle()
    val useAutoLocation by viewModel.useAutoLocation.collectAsStateWithLifecycle()
    val manualLat by viewModel.manualLatitude.collectAsStateWithLifecycle()
    val manualLon by viewModel.manualLongitude.collectAsStateWithLifecycle()
    val manualTz by viewModel.manualTimezone.collectAsStateWithLifecycle()

    var latStr by remember(manualLat) { mutableStateOf(manualLat.toString()) }
    var lonStr by remember(manualLon) { mutableStateOf(manualLon.toString()) }
    var tzStr by remember(manualTz) { mutableStateOf(manualTz.toString()) }

    Scaffold(
        topBar = { TopBarWithBack(title = "Prayer Settings", onBack = onBack) },
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
            // Calculation Method
            SectionCard {
                Text(
                    "Calculation Method",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                CalculationMethod.entries.forEach { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = calculationMethod == method,
                            onClick = { viewModel.setCalculationMethod(method) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PrayerEmerald
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(method.displayName, color = DiLinkTextPrimary)
                            val desc = buildString {
                                append("Fajr: ${method.fajrAngle}°")
                                if (method.ishaAngle != null) append(", Isha: ${method.ishaAngle}°")
                                if (method.ishaMinutes != null) append(", Isha: ${method.ishaMinutes}min")
                            }
                            Text(desc, color = DiLinkTextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Asr Method
            SectionCard {
                Text(
                    "Asr Juristic Method",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                AsrMethod.entries.forEach { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = asrMethod == method,
                            onClick = { viewModel.setAsrMethod(method) },
                            colors = RadioButtonDefaults.colors(selectedColor = PrayerEmerald)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(method.displayName, color = DiLinkTextPrimary)
                    }
                }
            }

            // Manual Adjustments
            SectionCard {
                Text(
                    "Manual Adjustments (minutes)",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                val prayerNames = PrayerName.entries
                prayerNames.forEachIndexed { index, prayer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${prayer.english} (${prayer.arabic})",
                            color = DiLinkTextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val newAdj = adjustments.toMutableList()
                                    newAdj[index] = newAdj[index] - 1
                                    viewModel.setManualAdjustments(newAdj.toIntArray())
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "-1", tint = DiLinkTextPrimary)
                            }
                            Text(
                                "${if (adjustments[index] >= 0) "+" else ""}${adjustments[index]}",
                                color = if (adjustments[index] != 0) PrayerGold else DiLinkTextMuted,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(40.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(
                                onClick = {
                                    val newAdj = adjustments.toMutableList()
                                    newAdj[index] = newAdj[index] + 1
                                    viewModel.setManualAdjustments(newAdj.toIntArray())
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "+1", tint = DiLinkTextPrimary)
                            }
                        }
                    }
                }
            }

            // Time Format
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("24-hour Format", color = DiLinkTextPrimary)
                    Switch(
                        checked = use24h,
                        onCheckedChange = { viewModel.setUse24h(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PrayerEmerald)
                    )
                }
            }

            // Tasbeeh Settings
            SectionCard {
                Text(
                    "Tasbeeh",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibration", color = DiLinkTextSecondary)
                    Switch(
                        checked = vibration,
                        onCheckedChange = { viewModel.setTasbeehVibration(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PrayerEmerald)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sound", color = DiLinkTextSecondary)
                    Switch(
                        checked = sound,
                        onCheckedChange = { viewModel.setTasbeehSound(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PrayerEmerald)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Default Goal", color = DiLinkTextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(33, 99, 100).forEach { g ->
                        FilterChip(
                            selected = defaultGoal == g,
                            onClick = { viewModel.setDefaultTasbeehGoal(g) },
                            label = { Text("$g") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrayerEmerald
                            )
                        )
                    }
                }
            }

            // Location
            SectionCard {
                Text(
                    "Location",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto (GPS)", color = DiLinkTextSecondary)
                    Switch(
                        checked = useAutoLocation,
                        onCheckedChange = { viewModel.setUseAutoLocation(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = PrayerEmerald)
                    )
                }

                if (!useAutoLocation) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = latStr,
                        onValueChange = { latStr = it },
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrayerEmerald,
                            unfocusedBorderColor = DiLinkSurfaceVariant
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = lonStr,
                        onValueChange = { lonStr = it },
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrayerEmerald,
                            unfocusedBorderColor = DiLinkSurfaceVariant
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tzStr,
                        onValueChange = { tzStr = it },
                        label = { Text("Timezone (e.g., +3)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrayerEmerald,
                            unfocusedBorderColor = DiLinkSurfaceVariant
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.setManualLocation(
                                lat = latStr.toDoubleOrNull() ?: 36.19,
                                lon = lonStr.toDoubleOrNull() ?: 44.01,
                                tz = tzStr.toDoubleOrNull() ?: 3.0
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrayerEmerald)
                    ) {
                        Text("Apply Manual Location")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
fun PrayerSettingsPreview() {
    DiLinkExtrasTheme {
        Text("Prayer Settings Preview", color = DiLinkTextPrimary)
    }
}
