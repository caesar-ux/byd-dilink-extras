package com.byd.dilink.extras.tireguard.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byd.dilink.extras.data.dao.TireRotationRecord
import com.byd.dilink.extras.tireguard.viewmodel.TireGuardViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotationTrackerScreen(
    onBack: () -> Unit,
    viewModel: TireGuardViewModel = hiltViewModel()
) {
    val rotations by viewModel.rotationRecords.collectAsStateWithLifecycle()
    var selectedPattern by remember { mutableStateOf("Front to Back") }
    var odometerStr by remember { mutableStateOf("") }
    var showAnimate by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    val patterns = listOf("Front to Back", "Cross Pattern")
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Animation
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(showAnimate) {
        if (showAnimate) {
            animProgress.snapTo(0f)
            animProgress.animateTo(1f, animationSpec = tween(1200, easing = LinearEasing))
            showAnimate = false
        }
    }

    val lastRotation = rotations.firstOrNull()
    val lastOdometer = lastRotation?.odometerKm
    val currentOdometer = odometerStr.toIntOrNull()
    val kmSinceRotation = if (lastOdometer != null && currentOdometer != null) {
        currentOdometer - lastOdometer
    } else null

    Scaffold(
        topBar = { TopBarWithBack(title = "Tire Rotation Tracker", onBack = onBack) },
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
            // Rotation diagram
            SectionCard {
                Text(
                    "Rotation Pattern",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary
                )
                Spacer(Modifier.height(8.dp))

                // Pattern selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    patterns.forEach { pattern ->
                        FilterChip(
                            selected = selectedPattern == pattern,
                            onClick = { selectedPattern = pattern },
                            label = { Text(pattern) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TireBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Canvas diagram
                RotationDiagram(
                    pattern = selectedPattern,
                    animProgress = animProgress.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showAnimate = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Show Rotation Animation")
                }
            }

            // Record rotation
            SectionCard {
                Text(
                    "Record Rotation",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextPrimary
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = odometerStr,
                    onValueChange = { odometerStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Current Odometer (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TireBlue,
                        unfocusedBorderColor = DiLinkSurfaceVariant
                    )
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TireBlue,
                        unfocusedBorderColor = DiLinkSurfaceVariant
                    )
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        val odo = odometerStr.toIntOrNull() ?: return@Button
                        val patternCode = if (selectedPattern == "Front to Back") "FrontToBack" else "Cross"
                        viewModel.recordRotation(
                            date = System.currentTimeMillis(),
                            odometerKm = odo,
                            pattern = patternCode,
                            notes = notes.ifBlank { null }
                        )
                        odometerStr = ""
                        notes = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TireBlue),
                    enabled = odometerStr.toIntOrNull() != null
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Record Rotation", fontWeight = FontWeight.Bold)
                }
            }

            // Status cards
            if (lastRotation != null) {
                SectionCard {
                    Text(
                        "Last Rotation",
                        style = MaterialTheme.typography.titleMedium,
                        color = DiLinkTextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Date: ${dateFormat.format(Date(lastRotation.date))}",
                        color = DiLinkTextSecondary
                    )
                    Text(
                        "Odometer: ${lastRotation.odometerKm} km",
                        color = DiLinkTextSecondary
                    )
                    Text(
                        "Pattern: ${lastRotation.pattern}",
                        color = DiLinkTextSecondary
                    )
                    if (kmSinceRotation != null) {
                        Spacer(Modifier.height(8.dp))
                        val nextDueKm = 10000 - kmSinceRotation
                        if (nextDueKm <= 0) {
                            Text(
                                "⚠️ Rotation overdue by ${-nextDueKm} km!",
                                color = StatusRed,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "Next rotation in ~$nextDueKm km",
                                color = StatusGreen
                            )
                        }
                    }
                }
            }

            // History
            if (rotations.size > 1) {
                SectionCard {
                    Text(
                        "Rotation History",
                        style = MaterialTheme.typography.titleMedium,
                        color = DiLinkTextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    rotations.forEach { record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                dateFormat.format(Date(record.date)),
                                color = DiLinkTextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${record.odometerKm} km • ${record.pattern}",
                                color = DiLinkTextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (record != rotations.last()) {
                            HorizontalDivider(color = DiLinkSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun RotationDiagram(
    pattern: String,
    animProgress: Float,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Car outline
        val carW = w * 0.35f
        val carH = h * 0.8f
        val carLeft = (w - carW) / 2f
        val carTop = (h - carH) / 2f

        // Simplified car body
        drawRoundRect(
            color = DiLinkSurfaceVariant,
            topLeft = Offset(carLeft, carTop),
            size = Size(carW, carH),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // Tire boxes
        val tireW = w * 0.12f
        val tireH = h * 0.10f
        val gap = 8.dp.toPx()

        // Positions: FL, FR, RL, RR
        val flPos = Offset(carLeft - tireW - gap, carTop + carH * 0.12f)
        val frPos = Offset(carLeft + carW + gap, carTop + carH * 0.12f)
        val rlPos = Offset(carLeft - tireW - gap, carTop + carH * 0.72f)
        val rrPos = Offset(carLeft + carW + gap, carTop + carH * 0.72f)

        val positions = mapOf("FL" to flPos, "FR" to frPos, "RL" to rlPos, "RR" to rrPos)

        // Draw tire boxes
        positions.forEach { (label, pos) ->
            drawRoundRect(
                color = TireBlue.copy(alpha = 0.3f),
                topLeft = pos,
                size = Size(tireW, tireH),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = TireBlue,
                topLeft = pos,
                size = Size(tireW, tireH),
                cornerRadius = CornerRadius(4.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
            val labelResult = textMeasurer.measure(
                AnnotatedString(label),
                style = TextStyle(fontSize = 11.sp, color = TireBlue, fontWeight = FontWeight.Bold)
            )
            drawText(
                labelResult,
                topLeft = Offset(
                    pos.x + (tireW - labelResult.size.width) / 2f,
                    pos.y + (tireH - labelResult.size.height) / 2f
                )
            )
        }

        // Rotation arrows (animated)
        if (animProgress > 0f) {
            val arrows = if (pattern == "Front to Back") {
                listOf("FL" to "RL", "FR" to "RR", "RL" to "FL", "RR" to "FR")
            } else {
                listOf("FL" to "RR", "FR" to "RL", "RL" to "FR", "RR" to "FL")
            }

            arrows.forEach { (from, to) ->
                val fromPos = positions[from]!!
                val toPos = positions[to]!!
                val fromCenter = Offset(fromPos.x + tireW / 2, fromPos.y + tireH / 2)
                val toCenter = Offset(toPos.x + tireW / 2, toPos.y + tireH / 2)

                val currentEnd = Offset(
                    fromCenter.x + (toCenter.x - fromCenter.x) * animProgress,
                    fromCenter.y + (toCenter.y - fromCenter.y) * animProgress
                )

                drawLine(
                    color = StatusGreen,
                    start = fromCenter,
                    end = currentEnd,
                    strokeWidth = 2.dp.toPx()
                )

                // Arrowhead
                if (animProgress > 0.8f) {
                    val dx = toCenter.x - fromCenter.x
                    val dy = toCenter.y - fromCenter.y
                    val len = kotlin.math.sqrt(dx * dx + dy * dy)
                    val ux = dx / len * 8.dp.toPx()
                    val uy = dy / len * 8.dp.toPx()

                    val arrowPath = Path().apply {
                        moveTo(currentEnd.x, currentEnd.y)
                        lineTo(currentEnd.x - ux - uy * 0.5f, currentEnd.y - uy + ux * 0.5f)
                        moveTo(currentEnd.x, currentEnd.y)
                        lineTo(currentEnd.x - ux + uy * 0.5f, currentEnd.y - uy - ux * 0.5f)
                    }
                    drawPath(arrowPath, StatusGreen, style = Stroke(width = 2.dp.toPx()))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 350, heightDp = 250)
@Composable
fun RotationDiagramPreview() {
    DiLinkExtrasTheme {
        RotationDiagram(
            pattern = "Front to Back",
            animProgress = 1f,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
    }
}
