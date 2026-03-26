package com.byd.dilink.extras.tireguard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byd.dilink.extras.data.dao.BatteryRecord
import com.byd.dilink.extras.tireguard.viewmodel.TireGuardViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

fun batteryColor(voltage: Double): androidx.compose.ui.graphics.Color {
    return when {
        voltage < 12.0 || voltage > 13.0 -> StatusRed
        voltage < 12.4 -> StatusYellow
        else -> StatusGreen
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryHistoryScreen(
    onBack: () -> Unit,
    viewModel: TireGuardViewModel = hiltViewModel()
) {
    val records by viewModel.batteryHistory.collectAsStateWithLifecycle()
    val degradationWarning by viewModel.batteryDegradationWarning.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = { TopBarWithBack(title = "Battery History", onBack = onBack) },
        containerColor = DiLinkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Degradation warning
            if (degradationWarning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusYellow.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusYellow)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "⚠️ Battery voltage appears to be declining steadily. Consider testing or replacing.",
                            color = StatusYellow,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Voltage chart
            val chartData = records.sortedBy { it.date }.takeLast(10).map {
                ChartPoint(it.date, it.voltage)
            }
            if (chartData.isNotEmpty()) {
                VoltageLineChart(
                    data = chartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No battery data yet", color = DiLinkTextMuted)
                }
            }

            // Records list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sorted = records.sortedByDescending { it.date }
                items(sorted, key = { it.id }) { record ->
                    BatteryRecordRow(record, dateFormat)
                }
            }
        }
    }
}

@Composable
fun BatteryRecordRow(record: BatteryRecord, dateFormat: SimpleDateFormat) {
    val color = batteryColor(record.voltage)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DiLinkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    dateFormat.format(Date(record.date)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DiLinkTextPrimary
                )
                Text(
                    "${record.condition} • ${record.engineState}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DiLinkTextMuted
                )
                if (!record.notes.isNullOrBlank()) {
                    Text(
                        record.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = DiLinkTextMuted
                    )
                }
            }
            Text(
                "${String.format("%.1f", record.voltage)}V",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// ── Voltage Line Chart ─────────────────────────────────────────────

@Composable
fun VoltageLineChart(
    data: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val leftPad = 40.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val chartW = size.width - leftPad - 16.dp.toPx()
        val chartH = size.height - bottomPad - 8.dp.toPx()

        val minV = (data.minOf { it.pressure } - 0.5).coerceAtLeast(10.0)
        val maxV = (data.maxOf { it.pressure } + 0.5).coerceAtMost(15.0)
        val vRange = maxV - minV
        val minDate = data.first().date
        val maxDate = data.last().date
        val dateRange = (maxDate - minDate).coerceAtLeast(1)

        fun xFor(date: Long) = leftPad + ((date - minDate).toFloat() / dateRange) * chartW
        fun yFor(v: Double) = 8.dp.toPx() + chartH * (1f - ((v - minV) / vRange).toFloat())

        // Ideal range band (12.4 - 12.8V)
        val idealTop = yFor(12.8)
        val idealBottom = yFor(12.4)
        drawRect(
            color = StatusGreen.copy(alpha = 0.1f),
            topLeft = Offset(leftPad, idealTop),
            size = Size(chartW, idealBottom - idealTop)
        )

        // Grid
        val gridValues = listOf(11.0, 11.5, 12.0, 12.4, 12.8, 13.0, 13.5).filter { it in minV..maxV }
        gridValues.forEach { v ->
            val y = yFor(v)
            drawLine(
                color = DiLinkSurfaceVariant,
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartW, y),
                strokeWidth = 1.dp.toPx()
            )
            val label = textMeasurer.measure(
                AnnotatedString(String.format("%.1f", v)),
                style = TextStyle(fontSize = 9.sp, color = DiLinkTextMuted)
            )
            drawText(label, topLeft = Offset(2.dp.toPx(), y - label.size.height / 2f))
        }

        // Line
        if (data.size > 1) {
            val path = Path()
            data.forEachIndexed { i, pt ->
                val x = xFor(pt.date)
                val y = yFor(pt.pressure)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, StatusYellow, style = Stroke(width = 2.dp.toPx()))
        }

        // Points
        data.forEach { pt ->
            val x = xFor(pt.date)
            val y = yFor(pt.pressure)
            val c = batteryColor(pt.pressure)
            drawCircle(color = c, radius = 4.dp.toPx(), center = Offset(x, y))
            drawCircle(color = DiLinkBackground, radius = 2.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 400, heightDp = 250)
@Composable
fun VoltageChartPreview() {
    DiLinkExtrasTheme {
        val now = System.currentTimeMillis()
        val day = 86400000L
        VoltageLineChart(
            data = listOf(
                ChartPoint(now - 6 * day, 12.8),
                ChartPoint(now - 4 * day, 12.6),
                ChartPoint(now - 2 * day, 12.4),
                ChartPoint(now, 12.2)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        )
    }
}
