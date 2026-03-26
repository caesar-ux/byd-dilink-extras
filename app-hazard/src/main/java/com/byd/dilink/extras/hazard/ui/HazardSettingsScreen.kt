package com.byd.dilink.extras.hazard.ui

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.byd.dilink.extras.hazard.viewmodel.HazardViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*

@Composable
fun HazardSettingsScreen(
    onBack: () -> Unit,
    viewModel: HazardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopBarWithBack(title = "Hazard Settings", onBack = onBack) },
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

            // Warning Distance
            item {
                SectionCard {
                    Text(
                        text = "Warning Distance",
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Distance at which approaching hazard warnings are triggered",
                        color = DiLinkTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    val distanceOptions = listOf(200, 500, 1000)
                    val distanceLabels = listOf("200m", "500m", "1km")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        distanceOptions.forEachIndexed { index, distance ->
                            FilterChip(
                                selected = state.warningDistanceMeters == distance,
                                onClick = { viewModel.updateWarningDistance(distance) },
                                label = { Text(distanceLabels[index]) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = DiLinkSurfaceVariant,
                                    selectedContainerColor = HazardOrange.copy(alpha = 0.3f),
                                    labelColor = DiLinkTextSecondary,
                                    selectedLabelColor = HazardOrange
                                )
                            )
                        }
                    }
                }
            }

            // Sound settings
            item {
                SectionCard {
                    Text(
                        text = "Warning Sound",
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Sound", color = DiLinkTextPrimary, fontSize = 15.sp)
                        Switch(
                            checked = state.warningSoundEnabled,
                            onCheckedChange = { viewModel.updateWarningSoundEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HazardOrange,
                                checkedTrackColor = HazardOrange.copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (state.warningSoundEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Text("Volume", color = DiLinkTextSecondary, fontSize = 13.sp)
                        Slider(
                            value = state.warningVolume,
                            onValueChange = { viewModel.updateWarningVolume(it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = HazardOrange,
                                activeTrackColor = HazardOrange
                            )
                        )
                    }
                }
            }

            // Auto-record
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Record",
                                color = DiLinkTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Start recording when speed exceeds 10 km/h",
                                color = DiLinkTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        Switch(
                            checked = state.autoRecord,
                            onCheckedChange = { viewModel.updateAutoRecord(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = HazardOrange,
                                checkedTrackColor = HazardOrange.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Hazard expiry
            item {
                SectionCard {
                    Text(
                        text = "Hazard Expiry",
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Auto-delete hazards older than the specified period",
                        color = DiLinkTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    val expiryOptions = listOf(0, 30, 60, 90)
                    val expiryLabels = listOf("Never", "30 days", "60 days", "90 days")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        expiryOptions.forEachIndexed { index, days ->
                            FilterChip(
                                selected = state.hazardExpiryDays == days,
                                onClick = { viewModel.updateHazardExpiryDays(days) },
                                label = { Text(expiryLabels[index], fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = DiLinkSurfaceVariant,
                                    selectedContainerColor = HazardOrange.copy(alpha = 0.3f),
                                    labelColor = DiLinkTextSecondary,
                                    selectedLabelColor = HazardOrange
                                )
                            )
                        }
                    }
                }
            }

            // Export/Import
            item {
                SectionCard {
                    Text(
                        text = "Data Management",
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.exportToFile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HazardOrange.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = HazardOrange)
                        Spacer(Modifier.width(8.dp))
                        Text("Export to JSON", color = HazardOrange, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { showImportDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DiLinkCyan.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = DiLinkCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Import from JSON", color = DiLinkCyan, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { showClearDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear All Hazards", color = StatusRed, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Clear all dialog
    if (showClearDialog) {
        ConfirmDialog(
            title = "Clear All Hazards",
            message = "This will permanently delete all saved hazards. This action cannot be undone.",
            confirmText = "Clear All",
            onConfirm = {
                viewModel.clearAllHazards()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    // Import dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Hazards", color = DiLinkTextPrimary) },
            text = {
                Column {
                    Text(
                        "Paste JSON data below:",
                        color = DiLinkTextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HazardOrange,
                            unfocusedBorderColor = DiLinkSurfaceVariant,
                            focusedTextColor = DiLinkTextPrimary,
                            unfocusedTextColor = DiLinkTextPrimary,
                            cursorColor = HazardOrange
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (importText.isNotBlank()) {
                            viewModel.importFromJson(importText)
                            importText = ""
                            showImportDialog = false
                        }
                    }
                ) {
                    Text("Import", color = HazardOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = DiLinkTextSecondary)
                }
            },
            containerColor = DiLinkSurfaceElevated
        )
    }
}
