package com.byd.dilink.extras.hazard.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.byd.dilink.extras.data.dao.HazardRecord
import com.byd.dilink.extras.data.repository.HazardRepository
import com.byd.dilink.extras.hazard.model.HazardType
import com.byd.dilink.extras.hazard.viewmodel.HazardViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HazardListScreen(
    onBack: () -> Unit,
    viewModel: HazardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val filteredHazards = viewModel.getFilteredHazards()
    var selectedHazard by remember { mutableStateOf<HazardRecord?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var hazardToDelete by remember { mutableStateOf<HazardRecord?>(null) }

    Scaffold(
        topBar = { TopBarWithBack(title = "Hazard List", onBack = onBack) },
        containerColor = DiLinkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search hazards...", color = DiLinkTextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = DiLinkTextSecondary)
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = DiLinkTextSecondary)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HazardOrange,
                    unfocusedBorderColor = DiLinkSurfaceVariant,
                    focusedTextColor = DiLinkTextPrimary,
                    unfocusedTextColor = DiLinkTextPrimary,
                    cursorColor = HazardOrange
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Type filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HazardType.entries.toList()) { type ->
                    val selected = type in state.selectedTypeFilters
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.toggleTypeFilter(type) },
                        label = { Text(type.label, fontSize = 12.sp) },
                        modifier = Modifier.height(36.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DiLinkSurfaceVariant,
                            selectedContainerColor = Color(type.colorLong).copy(alpha = 0.3f),
                            labelColor = DiLinkTextSecondary,
                            selectedLabelColor = Color(type.colorLong)
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Count
            Text(
                text = "${filteredHazards.size} hazards",
                color = DiLinkTextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Hazard list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredHazards, key = { it.id }) { hazard ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                hazardToDelete = hazard
                                showDeleteDialog = true
                            }
                            false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(StatusRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(end = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, "Delete", tint = StatusRed)
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true
                    ) {
                        HazardListItem(
                            hazard = hazard,
                            currentLat = state.currentLat,
                            currentLon = state.currentLon,
                            onClick = { selectedHazard = hazard }
                        )
                    }
                }
            }
        }
    }

    // Detail bottom sheet
    if (selectedHazard != null) {
        HazardDetailSheet(
            hazard = selectedHazard!!,
            currentLat = state.currentLat,
            currentLon = state.currentLon,
            onDismiss = { selectedHazard = null },
            onDelete = {
                hazardToDelete = selectedHazard
                showDeleteDialog = true
                selectedHazard = null
            }
        )
    }

    // Delete confirmation
    if (showDeleteDialog && hazardToDelete != null) {
        ConfirmDialog(
            title = "Delete Hazard",
            message = "Remove this ${HazardType.fromString(hazardToDelete!!.type).label} hazard?",
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteHazard(hazardToDelete!!)
                showDeleteDialog = false
                hazardToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                hazardToDelete = null
            }
        )
    }
}

@Composable
private fun HazardListItem(
    hazard: HazardRecord,
    currentLat: Double,
    currentLon: Double,
    onClick: () -> Unit
) {
    val type = HazardType.fromString(hazard.type)
    val color = Color(type.colorLong)
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }

    val distance = if (currentLat != 0.0 || currentLon != 0.0) {
        HazardRepository.haversineDistance(currentLat, currentLon, hazard.latitude, hazard.longitude)
    } else null

    val bearing = if (currentLat != 0.0 || currentLon != 0.0) {
        HazardRepository.bearing(currentLat, currentLon, hazard.latitude, hazard.longitude)
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DiLinkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.label,
                    color = DiLinkTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = dateFormat.format(Date(hazard.timestamp)),
                    color = DiLinkTextSecondary,
                    fontSize = 13.sp
                )
                val itemNotes = hazard.notes
                if (!itemNotes.isNullOrBlank()) {
                    Text(
                        text = itemNotes,
                        color = DiLinkTextMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            // Distance and direction
            if (distance != null && bearing != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = HazardRepository.formatDistance(distance),
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = HazardRepository.directionLabel(bearing),
                        color = DiLinkTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HazardDetailSheet(
    hazard: HazardRecord,
    currentLat: Double,
    currentLon: Double,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val type = HazardType.fromString(hazard.type)
    val color = Color(type.colorLong)
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()) }

    val distance = if (currentLat != 0.0 || currentLon != 0.0) {
        HazardRepository.haversineDistance(currentLat, currentLon, hazard.latitude, hazard.longitude)
    } else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DiLinkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = type.label,
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    if (distance != null) {
                        Text(
                            text = HazardRepository.formatDistance(distance) + " away",
                            color = color,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Details
            DetailRow("Date", dateFormat.format(Date(hazard.timestamp)))
            DetailRow("GPS", "${String.format("%.6f", hazard.latitude)}, ${String.format("%.6f", hazard.longitude)}")
            DetailRow("Speed", "${hazard.speed.toInt()} km/h")
            DetailRow("Heading", "${hazard.heading.toInt()}°")
            DetailRow("Confirmed", "${hazard.confirmed}×")
            val detailNotes = hazard.notes
            if (!detailNotes.isNullOrBlank()) {
                DetailRow("Notes", detailNotes)
            }

            Spacer(Modifier.height(24.dp))

            // Delete button
            Button(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed)
                Spacer(Modifier.width(8.dp))
                Text("Delete Hazard", color = StatusRed, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = DiLinkTextSecondary, fontSize = 14.sp)
        Text(text = value, color = DiLinkTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
