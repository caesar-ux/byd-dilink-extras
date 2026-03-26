package com.byd.dilink.extras.tireguard.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byd.dilink.extras.data.dao.TirePressureRecord
import com.byd.dilink.extras.tireguard.viewmodel.TireGuardViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TireHistoryScreen(
    initialPosition: String,
    onBack: () -> Unit,
    viewModel: TireGuardViewModel = hiltViewModel()
) {
    val allRecords by viewModel.tireHistory.collectAsStateWithLifecycle()
    val tabs = listOf("FL", "FR", "RL", "RR", "All")
    var selectedTab by remember {
        mutableIntStateOf(
            when (initialPosition.uppercase()) {
                "FL" -> 0; "FR" -> 1; "RL" -> 2; "RR" -> 3; else -> 4
            }
        )
    }

    val slowLeakWarnings by viewModel.slowLeakDetection.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopBarWithBack(title = "Tire History", onBack = onBack) },
        containerColor = DiLinkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DiLinkSurface,
                contentColor = TireBlue,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Slow leak warnings
            val positionKey = tabs[selectedTab]
            val leakWarning = slowLeakWarnings[positionKey.uppercase()]
            if (leakWarning == true) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "⚠️ Possible slow leak detected on $positionKey!",
                            color = StatusRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Pressure chart
            val chartData = extractChartData(allRecords, tabs[selectedTab])
            if (chartData.isNotEmpty()) {
                PressureLineChart(
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
                    Text("No data yet", color = DiLinkTextMuted)
                }
            }

            // Records list
            val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sortedRecords = allRecords.sortedByDescending { it.date }
                items(sortedRecords, key = { it.id }) { record ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteTireRecord(record)
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(StatusRed, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        },
                        enableDismissFromStartToEnd = false
                    ) {
                        TireRecordRow(record, tabs[selectedTab], dateFormat)
                    }
                }
            }
        }
    }
}

@Composable
fun TireRecordRow(
    record: TirePressureRecord,
    position: String,
    dateFormat: SimpleDateFormat
) {
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
                if (record.odometerKm != null) {
                    Text(
                        "${record.odometerKm} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = DiLinkTextMuted
                    )
                }
            }
            when (position.uppercase()) {
                "FL" -> PressureBadge(record.flBar, "FL")
                "FR" -> PressureBadge(record.frBar, "FR")
                "RL" -> PressureBadge(record.rlBar, "RL")
                "RR" -> PressureBadge(record.rrBar, "RR")
                else -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PressureBadge(record.flBar, "FL")
                    PressureBadge(record.frBar, "FR")
                    PressureBadge(record.rlBar, "RL")
                    PressureBadge(record.rrBar, "RR")
                }
            }
        }
    }
}

@Composable
fun PressureBadge(pressure: Double, label: String) {
    val color = pressureColor(pressure)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = DiLinkTextMuted)
        Text(
            String.format("%.1f", pressure),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ── Pressure Line Chart (Canvas) ───────────────────────────────────

data class ChartPoint(val date: Long, val pressure: Double)

fun extractChartData(records: List<TirePressureRecord>, position: String): List<ChartPoint> {
    val sorted = records.sortedBy { it.date }
    return sorted.map { record ->
        val pressure = when (position.uppercase()) {
            "FL" -> record.flBar
            "FR" -> record.frBar
            "RL" -> record.rlBar
            "RR" -> record.rrBar
            else -> (record.flBar + record.frBar + record.rlBar + record.rrBar) / 4.0
        }
        ChartPoint(record.date, pressure)
    }.takeLast(10)
}

@Composable
fun PressureLineChart(
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

        val minP = (data.minOf { it.pressure } - 0.2).coerceAtLeast(1.5)
        val maxP = (data.maxOf { it.pressure } + 0.2).coerceAtMost(3.5)
        val pRange = maxP - minP
        val minDate = data.first().date
        val maxDate = data.last().date
        val dateRange = (maxDate - minDate).coerceAtLeast(1)

        fun xFor(date: Long) = leftPad + ((date - minDate).toFloat() / dateRange) * chartW
        fun yFor(pressure: Double) = 8.dp.toPx() + chartH * (1f - ((pressure - minP) / pRange).toFloat())

        // Ideal range band (2.3 - 2.5)
        val idealTop = yFor(2.5)
        val idealBottom = yFor(2.3)
        drawRect(
            color = StatusGreen.copy(alpha = 0.1f),
            topLeft = Offset(leftPad, idealTop),
            size = Size(chartW, idealBottom - idealTop)
        )

        // Grid lines
        val steps = listOf(1.5, 2.0, 2.3, 2.5, 3.0, 3.5).filter { it in minP..maxP }
        steps.forEach { p ->
            val y = yFor(p)
            drawLine(
                color = DiLinkSurfaceVariant,
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartW, y),
                strokeWidth = 1.dp.toPx()
            )
            val label = textMeasurer.measure(
                AnnotatedString(String.format("%.1f", p)),
                style = TextStyle(fontSize = 9.sp, color = DiLinkTextMuted)
            )
            drawText(label, topLeft = Offset(2.dp.toPx(), y - label.size.height / 2f))
        }

        // Data line
        if (data.size > 1) {
            val path = Path()
            data.forEachIndexed { i, point ->
                val x = xFor(point.date)
                val y = yFor(point.pressure)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, TireBlue, style = Stroke(width = 2.dp.toPx()))
        }

        // Data points
        data.forEach { point ->
            val x = xFor(point.date)
            val y = yFor(point.pressure)
            val color = pressureColor(point.pressure)
            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
            drawCircle(
                color = DiLinkBackground,
                radius = 2.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 400, heightDp = 300)
@Composable
fun PressureChartPreview() {
    DiLinkExtrasTheme {
        val now = System.currentTimeMillis()
        val day = 86400000L
        PressureLineChart(
            data = listOf(
                ChartPoint(now - 9 * day, 2.5),
                ChartPoint(now - 8 * day, 2.4),
                ChartPoint(now - 6 * day, 2.4),
                ChartPoint(now - 4 * day, 2.3),
                ChartPoint(now - 2 * day, 2.2),
                ChartPoint(now, 2.1)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        )
    }
}
