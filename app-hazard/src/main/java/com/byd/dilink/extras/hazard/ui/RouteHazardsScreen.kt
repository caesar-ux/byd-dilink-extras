package com.byd.dilink.extras.hazard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.byd.dilink.extras.data.repository.HazardRepository
import com.byd.dilink.extras.hazard.model.HazardType
import com.byd.dilink.extras.hazard.viewmodel.HazardViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RouteHazardsScreen(
    onBack: () -> Unit,
    viewModel: HazardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var startLat by remember { mutableStateOf("") }
    var startLon by remember { mutableStateOf("") }
    var endLat by remember { mutableStateOf("") }
    var endLon by remember { mutableStateOf("") }
    var corridorWidth by remember { mutableStateOf(1000.0) }
    val corridorOptions = listOf(500.0, 1000.0, 2000.0, 5000.0)
    val corridorLabels = listOf("500m", "1km", "2km", "5km")

    Scaffold(
        topBar = { TopBarWithBack(title = "Route Hazards", onBack = onBack) },
        containerColor = DiLinkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Search for hazards along a route",
                    color = DiLinkTextSecondary,
                    fontSize = 14.sp
                )
            }

            // From coordinates
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("From", color = HazardOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        TextButton(
                            onClick = {
                                startLat = state.currentLat.toString()
                                startLon = state.currentLon.toString()
                            }
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = HazardOrange, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Current Location", color = HazardOrange)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CoordinateField(
                            value = startLat,
                            onValueChange = { startLat = it },
                            label = "Latitude",
                            modifier = Modifier.weight(1f)
                        )
                        CoordinateField(
                            value = startLon,
                            onValueChange = { startLon = it },
                            label = "Longitude",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // To coordinates
            item {
                SectionCard {
                    Text("To", color = HazardOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CoordinateField(
                            value = endLat,
                            onValueChange = { endLat = it },
                            label = "Latitude",
                            modifier = Modifier.weight(1f)
                        )
                        CoordinateField(
                            value = endLon,
                            onValueChange = { endLon = it },
                            label = "Longitude",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Corridor width selector
            item {
                Text("Corridor Width", color = DiLinkTextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    corridorOptions.forEachIndexed { index, width ->
                        FilterChip(
                            selected = corridorWidth == width,
                            onClick = { corridorWidth = width },
                            label = { Text(corridorLabels[index]) },
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

            // Search button
            item {
                Button(
                    onClick = {
                        val sLat = startLat.toDoubleOrNull() ?: return@Button
                        val sLon = startLon.toDoubleOrNull() ?: return@Button
                        val eLat = endLat.toDoubleOrNull() ?: return@Button
                        val eLon = endLon.toDoubleOrNull() ?: return@Button
                        viewModel.searchRouteHazards(sLat, sLon, eLat, eLon, corridorWidth)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HazardOrange),
                    enabled = startLat.toDoubleOrNull() != null &&
                            startLon.toDoubleOrNull() != null &&
                            endLat.toDoubleOrNull() != null &&
                            endLon.toDoubleOrNull() != null
                ) {
                    if (state.routeSearchActive) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = DiLinkBackground,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Search Route", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Results
            if (state.routeHazards.isNotEmpty()) {
                // Summary
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionCard {
                        Text(
                            text = "Found ${state.routeHazards.size} hazards along route",
                            color = DiLinkTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        // Count by type
                        val typeCounts = state.routeHazards
                            .groupBy { HazardType.fromString(it.first.type) }
                            .mapValues { it.value.size }
                            .entries
                            .sortedByDescending { it.value }

                        typeCounts.forEach { (type, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color(type.colorLong), CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = type.label,
                                    color = DiLinkTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$count",
                                    color = Color(type.colorLong),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Individual hazards
                items(state.routeHazards) { (hazard, distFromStart) ->
                    val type = HazardType.fromString(hazard.type)
                    val color = Color(type.colorLong)
                    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

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
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(color.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = type.label,
                                    color = DiLinkTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = dateFormat.format(Date(hazard.timestamp)),
                                    color = DiLinkTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = HazardRepository.formatDistance(distFromStart),
                                color = color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            } else if (!state.routeSearchActive && startLat.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hazards found along this route",
                            color = DiLinkTextMuted,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CoordinateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label, color = DiLinkTextMuted, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HazardOrange,
            unfocusedBorderColor = DiLinkSurfaceVariant,
            focusedTextColor = DiLinkTextPrimary,
            unfocusedTextColor = DiLinkTextPrimary,
            cursorColor = HazardOrange,
            focusedLabelColor = HazardOrange
        ),
        shape = RoundedCornerShape(8.dp)
    )
}
