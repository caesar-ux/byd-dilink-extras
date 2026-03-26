package com.byd.dilink.extras.tireguard.ui

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byd.dilink.extras.tireguard.viewmodel.TireGuardViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onNavigateToLogTires: () -> Unit,
    onNavigateToLogBattery: () -> Unit,
    onNavigateToTireHistory: (String) -> Unit,
    onNavigateToBatteryHistory: () -> Unit,
    onNavigateToRotationTracker: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: TireGuardViewModel = hiltViewModel()
) {
    val latestTires by viewModel.latestTirePressures.collectAsStateWithLifecycle()
    val latestBattery by viewModel.latestBatteryVoltage.collectAsStateWithLifecycle()
    val reminderState by viewModel.reminderState.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tire & Battery Guard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToRotationTracker) {
                        Icon(Icons.Default.Autorenew, contentDescription = "Rotation Tracker")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DiLinkBackground,
                    titleContentColor = DiLinkTextPrimary
                )
            )
        },
        containerColor = DiLinkBackground
    ) { padding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CarTopViewDiagram(
                        flPressure = latestTires?.flBar,
                        frPressure = latestTires?.frBar,
                        rlPressure = latestTires?.rlBar,
                        rrPressure = latestTires?.rrBar,
                        onTireTap = onNavigateToTireHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (reminderState.showReminder) {
                        ReminderBanner(message = reminderState.message)
                    }
                    BatteryCard(
                        voltage = latestBattery?.voltage,
                        lastCheckDate = latestBattery?.date,
                        onClick = onNavigateToBatteryHistory
                    )
                    ActionButtons(
                        onLogTires = onNavigateToLogTires,
                        onLogBattery = onNavigateToLogBattery
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (reminderState.showReminder) {
                    ReminderBanner(message = reminderState.message)
                }
                CarTopViewDiagram(
                    flPressure = latestTires?.flBar,
                    frPressure = latestTires?.frBar,
                    rlPressure = latestTires?.rlBar,
                    rrPressure = latestTires?.rrBar,
                    onTireTap = onNavigateToTireHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                )
                BatteryCard(
                    voltage = latestBattery?.voltage,
                    lastCheckDate = latestBattery?.date,
                    onClick = onNavigateToBatteryHistory
                )
                ActionButtons(
                    onLogTires = onNavigateToLogTires,
                    onLogBattery = onNavigateToLogBattery
                )
            }
        }
    }
}

// ── Car Top-View Diagram (Canvas) ──────────────────────────────────

fun pressureColor(pressure: Double?): Color {
    if (pressure == null) return Color.Gray
    return when {
        pressure < 2.1 -> StatusRed
        pressure > 2.7 -> StatusRed
        pressure < 2.3 -> StatusYellow
        pressure > 2.5 -> StatusYellow
        else -> StatusGreen
    }
}

@Composable
fun CarTopViewDiagram(
    flPressure: Double?,
    frPressure: Double?,
    rlPressure: Double?,
    rrPressure: Double?,
    onTireTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Car body dimensions
            val carW = w * 0.42f
            val carH = h * 0.72f
            val carLeft = (w - carW) / 2f
            val carTop = (h - carH) / 2f
            val hoodH = carH * 0.15f
            val trunkH = carH * 0.10f

            // Draw car body
            drawCarBody(carLeft, carTop, carW, carH, hoodH, trunkH)

            // Tire dimensions
            val tireW = w * 0.14f
            val tireH = h * 0.12f
            val tireGap = 6.dp.toPx()

            // FL tire
            val flX = carLeft - tireW - tireGap
            val flY = carTop + hoodH + carH * 0.05f
            drawTireBox(flX, flY, tireW, tireH, flPressure, "FL", textMeasurer)

            // FR tire
            val frX = carLeft + carW + tireGap
            val frY = flY
            drawTireBox(frX, frY, tireW, tireH, frPressure, "FR", textMeasurer)

            // RL tire
            val rlX = flX
            val rlY = carTop + carH - trunkH - carH * 0.05f - tireH
            drawTireBox(rlX, rlY, tireW, tireH, rlPressure, "RL", textMeasurer)

            // RR tire
            val rrX = frX
            val rrY = rlY
            drawTireBox(rrX, rrY, tireW, tireH, rrPressure, "RR", textMeasurer)

            // Axle lines
            val axleStroke = 2.dp.toPx()
            // Front axle
            drawLine(
                color = DiLinkSurfaceVariant,
                start = Offset(flX + tireW, flY + tireH / 2),
                end = Offset(frX, frY + tireH / 2),
                strokeWidth = axleStroke
            )
            // Rear axle
            drawLine(
                color = DiLinkSurfaceVariant,
                start = Offset(rlX + tireW, rlY + tireH / 2),
                end = Offset(rrX, rrY + tireH / 2),
                strokeWidth = axleStroke
            )
        }

        // Invisible tap targets for each tire
        val boxMod = Modifier.fillMaxSize()
        Box(modifier = Modifier.fillMaxSize()) {
            // FL tap area - top left quadrant
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight(0.5f)
                    .align(Alignment.TopStart)
                    .clickable { onTireTap("fl") }
            )
            // FR tap area - top right quadrant
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight(0.5f)
                    .align(Alignment.TopEnd)
                    .clickable { onTireTap("fr") }
            )
            // RL tap area - bottom left quadrant
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomStart)
                    .clickable { onTireTap("rl") }
            )
            // RR tap area - bottom right quadrant
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomEnd)
                    .clickable { onTireTap("rr") }
            )
        }
    }
}

private fun DrawScope.drawCarBody(
    left: Float, top: Float,
    width: Float, height: Float,
    hoodH: Float, trunkH: Float
) {
    val bodyColor = DiLinkSurfaceVariant
    val outlineColor = DiLinkTextSecondary
    val strokeW = 2.dp.toPx()

    // Main body rectangle
    val bodyPath = Path().apply {
        // Start from top-left, with rounded hood
        val hoodNarrow = width * 0.12f
        // Hood (narrower front)
        moveTo(left + hoodNarrow, top)
        // Top-left curve of hood
        quadraticBezierTo(left, top, left, top + hoodH)
        // Left side
        lineTo(left, top + height - trunkH)
        // Bottom-left curve (trunk)
        quadraticBezierTo(left, top + height, left + hoodNarrow, top + height)
        // Bottom edge
        lineTo(left + width - hoodNarrow, top + height)
        // Bottom-right curve
        quadraticBezierTo(left + width, top + height, left + width, top + height - trunkH)
        // Right side
        lineTo(left + width, top + hoodH)
        // Top-right curve of hood
        quadraticBezierTo(left + width, top, left + width - hoodNarrow, top)
        close()
    }

    drawPath(bodyPath, bodyColor, style = Fill)
    drawPath(bodyPath, outlineColor, style = Stroke(width = strokeW))

    // Windshield
    val wsTop = top + hoodH + height * 0.04f
    val wsH = height * 0.14f
    val wsInset = width * 0.1f
    drawRoundRect(
        color = DiLinkSurfaceElevated,
        topLeft = Offset(left + wsInset, wsTop),
        size = Size(width - wsInset * 2, wsH),
        cornerRadius = CornerRadius(8.dp.toPx())
    )

    // Rear window
    val rwTop = top + height - trunkH - height * 0.04f - height * 0.10f
    val rwH = height * 0.10f
    drawRoundRect(
        color = DiLinkSurfaceElevated,
        topLeft = Offset(left + wsInset, rwTop),
        size = Size(width - wsInset * 2, rwH),
        cornerRadius = CornerRadius(8.dp.toPx())
    )

    // Center line (hood to trunk)
    drawLine(
        color = outlineColor.copy(alpha = 0.3f),
        start = Offset(left + width / 2, top + 4.dp.toPx()),
        end = Offset(left + width / 2, top + hoodH),
        strokeWidth = 1.dp.toPx()
    )

    // Side mirrors
    val mirrorW = width * 0.06f
    val mirrorH = height * 0.03f
    val mirrorY = wsTop + wsH * 0.3f
    // Left mirror
    drawRoundRect(
        color = outlineColor,
        topLeft = Offset(left - mirrorW, mirrorY),
        size = Size(mirrorW, mirrorH),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
    // Right mirror
    drawRoundRect(
        color = outlineColor,
        topLeft = Offset(left + width, mirrorY),
        size = Size(mirrorW, mirrorH),
        cornerRadius = CornerRadius(2.dp.toPx())
    )

    // Headlights
    val hlW = width * 0.18f
    val hlH = height * 0.025f
    val hlY = top + hoodH * 0.2f
    drawRoundRect(
        color = StatusYellow.copy(alpha = 0.5f),
        topLeft = Offset(left + width * 0.08f, hlY),
        size = Size(hlW, hlH),
        cornerRadius = CornerRadius(4.dp.toPx())
    )
    drawRoundRect(
        color = StatusYellow.copy(alpha = 0.5f),
        topLeft = Offset(left + width - width * 0.08f - hlW, hlY),
        size = Size(hlW, hlH),
        cornerRadius = CornerRadius(4.dp.toPx())
    )

    // Tail lights
    val tlY = top + height - trunkH * 0.5f
    drawRoundRect(
        color = StatusRed.copy(alpha = 0.5f),
        topLeft = Offset(left + width * 0.08f, tlY),
        size = Size(hlW, hlH),
        cornerRadius = CornerRadius(4.dp.toPx())
    )
    drawRoundRect(
        color = StatusRed.copy(alpha = 0.5f),
        topLeft = Offset(left + width - width * 0.08f - hlW, tlY),
        size = Size(hlW, hlH),
        cornerRadius = CornerRadius(4.dp.toPx())
    )
}

private fun DrawScope.drawTireBox(
    x: Float, y: Float,
    width: Float, height: Float,
    pressure: Double?,
    label: String,
    textMeasurer: TextMeasurer
) {
    val color = pressureColor(pressure)
    val strokeW = 2.dp.toPx()

    // Tire background
    drawRoundRect(
        color = color.copy(alpha = 0.2f),
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(6.dp.toPx())
    )
    // Tire border
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(6.dp.toPx()),
        style = Stroke(width = strokeW)
    )

    // Pressure text
    val pressureText = pressure?.let { String.format("%.1f", it) } ?: "—"
    val textResult = textMeasurer.measure(
        text = AnnotatedString(pressureText),
        style = TextStyle(
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    )
    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(
            x + (width - textResult.size.width) / 2f,
            y + (height - textResult.size.height) / 2f - 4.dp.toPx()
        )
    )

    // Label text
    val labelResult = textMeasurer.measure(
        text = AnnotatedString(label),
        style = TextStyle(
            color = DiLinkTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    )
    drawText(
        textLayoutResult = labelResult,
        topLeft = Offset(
            x + (width - labelResult.size.width) / 2f,
            y + (height - textResult.size.height) / 2f + textResult.size.height - 4.dp.toPx()
        )
    )
}

// ── Battery Card ───────────────────────────────────────────────────

@Composable
fun BatteryCard(
    voltage: Double?,
    lastCheckDate: Long?,
    onClick: () -> Unit
) {
    val statusColor = when {
        voltage == null -> Color.Gray
        voltage < 12.0 || voltage > 13.0 -> StatusRed
        voltage < 12.4 -> StatusYellow
        else -> StatusGreen
    }
    val statusText = when {
        voltage == null -> "No data"
        voltage < 12.0 -> "❌ Critical"
        voltage > 13.0 -> "❌ Overcharge"
        voltage < 12.4 -> "⚠\uFE0F Low"
        else -> "✅ Good"
    }
    val daysAgo = lastCheckDate?.let {
        val diff = System.currentTimeMillis() - it
        TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }

    SectionCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "12V Battery",
                    style = MaterialTheme.typography.titleMedium,
                    color = DiLinkTextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = voltage?.let { "${String.format("%.1f", it)}V" } ?: "—",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor
                )
                if (daysAgo != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Last checked: ${if (daysAgo == 0) "today" else "$daysAgo days ago"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DiLinkTextMuted
                    )
                }
            }
        }
    }
}

// ── Reminder Banner ────────────────────────────────────────────────

@Composable
fun ReminderBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StatusYellow.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Warning",
                tint = StatusYellow,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                color = StatusYellow,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Action Buttons ─────────────────────────────────────────────────

@Composable
fun ActionButtons(
    onLogTires: () -> Unit,
    onLogBattery: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LargeIconButton(
            text = "Log Tires",
            icon = Icons.Default.TireRepair,
            color = TireBlue,
            modifier = Modifier.weight(1f),
            height = 56.dp,
            onClick = onLogTires
        )
        LargeIconButton(
            text = "Log Battery",
            icon = Icons.Default.BatteryChargingFull,
            color = StatusYellow,
            modifier = Modifier.weight(1f),
            height = 56.dp,
            onClick = onLogBattery
        )
    }
}

// ── Preview ────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 400, heightDp = 700)
@Composable
fun OverviewScreenPreview() {
    DiLinkExtrasTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DiLinkBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReminderBanner("Time for a tire pressure check!")
            CarTopViewDiagram(
                flPressure = 2.4,
                frPressure = 2.3,
                rlPressure = 2.1,
                rrPressure = 2.5,
                onTireTap = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )
            BatteryCard(
                voltage = 12.6,
                lastCheckDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2),
                onClick = {}
            )
            ActionButtons(onLogTires = {}, onLogBattery = {})
        }
    }
}
