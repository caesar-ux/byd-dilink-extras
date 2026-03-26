package com.byd.dilink.extras.hazard.ui

import android.Manifest
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.byd.dilink.extras.data.repository.HazardRepository
import com.byd.dilink.extras.hazard.model.HazardType
import com.byd.dilink.extras.hazard.viewmodel.HazardViewModel
import com.byd.dilink.extras.hazard.viewmodel.NearbyHazard
import com.byd.dilink.extras.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveDriveScreen(
    onNavigateToList: () -> Unit,
    onNavigateToRoute: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HazardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.startLocationUpdates()
        }
    }

    // Request permission on first composition
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Warning sound
    LaunchedEffect(state.shouldPlayWarning) {
        if (state.shouldPlayWarning) {
            try {
                val toneGen = ToneGenerator(
                    AudioManager.STREAM_NOTIFICATION,
                    (state.warningVolume * 100).toInt()
                )
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                kotlinx.coroutines.delay(400)
                toneGen.release()
            } catch (_: Exception) { }
            viewModel.clearWarningSound()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiLinkBackground)
    ) {
        // Top status bar
        TopStatusBar(
            isRecording = state.isRecording,
            nearbyCount = state.nearbyCount,
            onNavigateToList = onNavigateToList,
            onNavigateToRoute = onNavigateToRoute,
            onNavigateToSettings = onNavigateToSettings
        )

        // Warning banner
        AnimatedVisibility(
            visible = state.closestWarning != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            state.closestWarning?.let { warning ->
                WarningBanner(warning)
            }
        }

        // Radar display
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            HazardRadar(
                nearbyHazards = state.nearbyHazards,
                heading = state.currentHeading,
                warningDistance = state.warningDistanceMeters.toFloat()
            )
        }

        // Speed display
        SpeedDisplay(
            speedKmh = state.currentSpeed,
            isRecording = state.isRecording,
            onToggleRecording = { viewModel.toggleRecording() }
        )

        // Quick-add buttons
        QuickAddButtons(
            showMore = state.showMoreTypes,
            onToggleMore = { viewModel.toggleShowMoreTypes() },
            onAddHazard = { type -> viewModel.addHazard(type) }
        )
    }
}

@Composable
private fun TopStatusBar(
    isRecording: Boolean,
    nearbyCount: Int,
    onNavigateToList: () -> Unit,
    onNavigateToRoute: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiLinkSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        if (isRecording) StatusGreen else StatusRed,
                        CircleShape
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isRecording) "Recording" else "Stopped",
                color = DiLinkTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Text(
            text = "$nearbyCount hazards nearby",
            color = DiLinkTextSecondary,
            fontSize = 14.sp
        )

        Row {
            IconButton(onClick = onNavigateToList, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.List, "Hazard List", tint = DiLinkTextSecondary)
            }
            IconButton(onClick = onNavigateToRoute, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Navigation, "Route Hazards", tint = DiLinkTextSecondary)
            }
            IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Settings, "Settings", tint = DiLinkTextSecondary)
            }
        }
    }
}

@Composable
private fun WarningBanner(warning: NearbyHazard) {
    val hazardType = HazardType.fromString(warning.record.type)
    val infiniteTransition = rememberInfiniteTransition(label = "warning_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warning_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(hazardType.colorLong).copy(alpha = alpha * 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(hazardType.colorLong),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${hazardType.label} ahead!",
                    color = DiLinkTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "${HazardRepository.formatDistance(warning.distanceMeters)} ${warning.directionLabel}",
                    color = DiLinkTextSecondary,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun HazardRadar(
    nearbyHazards: List<NearbyHazard>,
    heading: Float,
    warningDistance: Float
) {
    val textMeasurer = rememberTextMeasurer()
    val pulseTransition = rememberInfiniteTransition(label = "radar_pulse")
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.minDimension / 2 * 0.9f

        // Distance rings at 500m, 1km, 2km, 5km
        val rings = listOf(500f, 1000f, 2000f, 5000f)
        val ringLabels = listOf("500m", "1km", "2km", "5km")
        val maxDistMeters = 5000f

        // Background
        drawCircle(
            color = Color(0xFF0D1A0D),
            radius = maxRadius,
            center = Offset(centerX, centerY)
        )

        // Concentric distance rings
        rings.forEachIndexed { index, distMeters ->
            val ringRadius = (distMeters / maxDistMeters) * maxRadius
            drawCircle(
                color = Color(0xFF1B3D1B),
                radius = ringRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5f)
            )

            // Ring label
            val labelResult = textMeasurer.measure(
                text = AnnotatedString(ringLabels[index]),
                style = TextStyle(
                    color = Color(0xFF3D6B3D),
                    fontSize = 10.sp
                )
            )
            drawText(
                textLayoutResult = labelResult,
                topLeft = Offset(
                    centerX + ringRadius - labelResult.size.width - 4,
                    centerY - labelResult.size.height - 2
                )
            )
        }

        // Crosshairs
        drawLine(
            color = Color(0xFF1B3D1B),
            start = Offset(centerX, centerY - maxRadius),
            end = Offset(centerX, centerY + maxRadius),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0xFF1B3D1B),
            start = Offset(centerX - maxRadius, centerY),
            end = Offset(centerX + maxRadius, centerY),
            strokeWidth = 1f
        )

        // Cardinal direction labels
        val cardinals = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
        cardinals.forEach { (label, angle) ->
            val adjustedAngle = Math.toRadians((angle - heading).toDouble())
            val labelDist = maxRadius + 12
            val lx = centerX + labelDist * sin(adjustedAngle).toFloat()
            val ly = centerY - labelDist * cos(adjustedAngle).toFloat()
            val result = textMeasurer.measure(
                text = AnnotatedString(label),
                style = TextStyle(
                    color = if (label == "N") StatusGreen else Color(0xFF3D6B3D),
                    fontSize = 12.sp,
                    fontWeight = if (label == "N") FontWeight.Bold else FontWeight.Normal
                )
            )
            drawText(
                textLayoutResult = result,
                topLeft = Offset(lx - result.size.width / 2, ly - result.size.height / 2)
            )
        }

        // Sweep/pulse animation
        val sweepRadius = pulseRadius * maxRadius
        drawCircle(
            color = Color(0xFF00FF00).copy(alpha = 0.15f * (1f - pulseRadius)),
            radius = sweepRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        // Center dot (your position)
        drawCircle(
            color = StatusGreen,
            radius = 8f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = StatusGreen.copy(alpha = 0.3f),
            radius = 16f,
            center = Offset(centerX, centerY)
        )

        // Warning distance ring
        val warningRadius = (warningDistance / maxDistMeters) * maxRadius
        drawCircle(
            color = StatusRed.copy(alpha = 0.2f),
            radius = warningRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        // Hazard dots
        // Rotate hazards based on heading so North is always up
        rotate(degrees = 0f, pivot = Offset(centerX, centerY)) {
            nearbyHazards.forEach { hazard ->
                val dist = hazard.distanceMeters.toFloat()
                if (dist <= maxDistMeters) {
                    val hazardType = HazardType.fromString(hazard.record.type)
                    val hazardColor = Color(hazardType.colorLong)
                    val scaledDist = (dist / maxDistMeters) * maxRadius

                    // Adjust bearing relative to heading
                    val relativeBearing = Math.toRadians((hazard.bearingDeg - heading).toDouble())
                    val hx = centerX + scaledDist * sin(relativeBearing).toFloat()
                    val hy = centerY - scaledDist * cos(relativeBearing).toFloat()

                    // Draw hazard dot
                    val dotRadius = if (dist < warningDistance) 10f else 7f
                    drawCircle(
                        color = hazardColor,
                        radius = dotRadius,
                        center = Offset(hx, hy)
                    )

                    // Pulse effect for hazards within warning distance
                    if (dist < warningDistance) {
                        val pulseSize = dotRadius + 8f * pulseRadius
                        drawCircle(
                            color = hazardColor.copy(alpha = 0.4f * (1f - pulseRadius)),
                            radius = pulseSize,
                            center = Offset(hx, hy),
                            style = Stroke(width = 2f)
                        )
                    }

                    // Type initial letter
                    val initial = hazardType.label.first().toString()
                    val initialResult = textMeasurer.measure(
                        text = AnnotatedString(initial),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = initialResult,
                        topLeft = Offset(
                            hx - initialResult.size.width / 2,
                            hy - initialResult.size.height / 2
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedDisplay(
    speedKmh: Float,
    isRecording: Boolean,
    onToggleRecording: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiLinkSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${speedKmh.toInt()}",
                color = DiLinkTextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "km/h",
                color = DiLinkTextSecondary,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Button(
            onClick = onToggleRecording,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) StatusRed else StatusGreen
            )
        ) {
            Icon(
                if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isRecording) "Stop" else "Record",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun QuickAddButtons(
    showMore: Boolean,
    onToggleMore: () -> Unit,
    onAddHazard: (HazardType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiLinkSurfaceVariant)
            .padding(12.dp)
    ) {
        // Main 4 quick-add buttons + More
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HazardType.quickAdd.forEach { type ->
                HazardQuickButton(
                    type = type,
                    onClick = { onAddHazard(type) },
                    modifier = Modifier.weight(1f)
                )
            }
            // More button
            Button(
                onClick = onToggleMore,
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DiLinkSurfaceElevated
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (showMore) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = DiLinkTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "More",
                        color = DiLinkTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Expanded more types
        AnimatedVisibility(visible = showMore) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HazardType.moreTypes) { type ->
                    HazardQuickButton(
                        type = type,
                        onClick = { onAddHazard(type) },
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HazardQuickButton(
    type: HazardType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color(type.colorLong)
    Button(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.2f)
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Warning,
                contentDescription = type.label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = type.label,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
