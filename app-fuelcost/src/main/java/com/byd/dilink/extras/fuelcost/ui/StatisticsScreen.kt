package com.byd.dilink.extras.fuelcost.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.byd.dilink.extras.fuelcost.viewmodel.FuelCostViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: FuelCostViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val textMeasurer = rememberTextMeasurer()

    Scaffold(
        topBar = { TopBarWithBack(title = "Statistics", onBack = onBack) },
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

            // Stacked bar chart - Monthly fuel vs electric cost
            item {
                SectionCard {
                    Text(
                        text = "Monthly Cost Breakdown",
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    if (state.monthlyBreakdowns.isNotEmpty()) {
                        StackedBarChart(
                            breakdowns = state.monthlyBreakdowns.map {
                                Triple(it.yearMonth, it.fuelCost, it.electricCost)
                            },
                            textMeasurer = textMeasurer
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No data yet", color = DiLinkTextMuted, fontSize = 16.sp)
                        }
                    }

                    // Legend
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LegendItem("Petrol", StatusOrange)
                        Spacer(Modifier.width(24.dp))
                        LegendItem("Electric", FuelGreen)
                    }
                }
            }

            // Cost per km trend line chart
            item {
                SectionCard {
                    Text(
                        text = "Cost/km Trend",
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    if (state.monthlyBreakdowns.isNotEmpty()) {
                        CostTrendLineChart(
                            breakdowns = state.monthlyBreakdowns.map {
                                Triple(it.yearMonth, it.fuelCostPerKm, it.electricCostPerKm)
                            },
                            textMeasurer = textMeasurer
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No data yet", color = DiLinkTextMuted, fontSize = 16.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LegendItem("Petrol/km", StatusOrange)
                        Spacer(Modifier.width(24.dp))
                        LegendItem("Electric/km", FuelGreen)
                    }
                }
            }

            // Pie chart - EV/Hybrid/Fuel split
            item {
                SectionCard {
                    Text(
                        text = "Driving Mode Split",
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    EvSplitPieChart(
                        evPercentage = state.evPercentage.toFloat(),
                        textMeasurer = textMeasurer
                    )

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LegendItem("Electric", FuelGreen)
                        Spacer(Modifier.width(24.dp))
                        LegendItem("Petrol", StatusOrange)
                    }
                }
            }

            // Monthly breakdown cards
            item {
                Text(
                    text = "Monthly Breakdown",
                    color = DiLinkTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            items(state.monthlyBreakdowns) { month ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DiLinkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = month.yearMonth,
                            color = DiLinkCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        MonthDetailRow("Fuel Cost", viewModel.formatCurrency(month.fuelCost), StatusOrange)
                        MonthDetailRow("Electric Cost", viewModel.formatCurrency(month.electricCost), FuelGreen)
                        MonthDetailRow("Total km", "${month.totalKm} km", DiLinkTextPrimary)
                        MonthDetailRow("Fuel/km", viewModel.formatCurrency(month.fuelCostPerKm), StatusOrange)
                        MonthDetailRow("Electric/km", viewModel.formatCurrency(month.electricCostPerKm), FuelGreen)
                        MonthDetailRow("Savings", viewModel.formatCurrency(month.savings),
                            if (month.savings >= 0) FuelGreen else StatusRed
                        )
                        MonthDetailRow("EV %", String.format("%.0f%%", month.evPercentage), FuelGreen)
                    }
                }
            }

            // Lifetime totals
            item {
                SectionCard {
                    Text(
                        text = "Lifetime Totals",
                        color = DiLinkTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    val lt = state.lifetimeTotals
                    LifetimeRow("Total Fuel", "${String.format("%.1f", lt.totalFuelLiters)} L")
                    LifetimeRow("Fuel Cost", viewModel.formatCurrency(lt.totalFuelCostIqd))
                    LifetimeRow("Total Electricity", "${String.format("%.1f", lt.totalElectricKwh)} kWh")
                    LifetimeRow("Electric Cost", viewModel.formatCurrency(lt.totalElectricCostIqd))
                    LifetimeRow("Total km Driven", "${String.format("%,d", lt.totalKmDriven)} km")
                    LifetimeRow("Avg L/100km", String.format("%.1f", lt.avgLPer100Km))
                    LifetimeRow("Avg kWh/100km", String.format("%.1f", lt.avgKwhPer100Km))
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = DiLinkSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Savings vs Pure Petrol",
                            color = DiLinkTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = viewModel.formatCurrency(lt.totalSavingsVsBenchmark),
                            color = if (lt.totalSavingsVsBenchmark >= 0) FuelGreen else StatusRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StackedBarChart(
    breakdowns: List<Triple<String, Double, Double>>,
    textMeasurer: TextMeasurer
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 60f
        val chartWidth = canvasWidth - padding * 2
        val chartHeight = canvasHeight - padding

        if (breakdowns.isEmpty()) return@Canvas

        val maxTotal = breakdowns.maxOfOrNull { it.second + it.third } ?: 1.0
        val barWidth = (chartWidth / breakdowns.size) * 0.6f
        val gap = (chartWidth / breakdowns.size) * 0.4f

        // Y-axis gridlines
        for (i in 0..4) {
            val y = chartHeight - (chartHeight * i / 4)
            drawLine(
                color = Color(0xFF2A2A2A),
                start = Offset(padding, y),
                end = Offset(canvasWidth - padding / 2, y),
                strokeWidth = 1f
            )
            val label = String.format("%.0fk", maxTotal * i / 4 / 1000)
            val labelResult = textMeasurer.measure(
                text = AnnotatedString(label),
                style = TextStyle(color = Color(0xFF666666), fontSize = 10.sp)
            )
            drawText(
                textLayoutResult = labelResult,
                topLeft = Offset(2f, y - labelResult.size.height / 2)
            )
        }

        // Bars
        breakdowns.forEachIndexed { index, (month, fuelCost, electricCost) ->
            val x = padding + index * (barWidth + gap) + gap / 2
            val total = fuelCost + electricCost
            val fuelHeight = if (maxTotal > 0) (fuelCost / maxTotal * chartHeight).toFloat() else 0f
            val electricHeight = if (maxTotal > 0) (electricCost / maxTotal * chartHeight).toFloat() else 0f

            // Electric (bottom)
            if (electricHeight > 0) {
                drawRoundRect(
                    color = Color(0xFF00E676),
                    topLeft = Offset(x, chartHeight - electricHeight),
                    size = Size(barWidth, electricHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            // Fuel (top, stacked)
            if (fuelHeight > 0) {
                drawRoundRect(
                    color = Color(0xFFFF9800),
                    topLeft = Offset(x, chartHeight - electricHeight - fuelHeight),
                    size = Size(barWidth, fuelHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            // Month label
            val monthLabel = if (month.length >= 7) month.substring(5) else month
            val monthResult = textMeasurer.measure(
                text = AnnotatedString(monthLabel),
                style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 10.sp)
            )
            drawText(
                textLayoutResult = monthResult,
                topLeft = Offset(
                    x + barWidth / 2 - monthResult.size.width / 2,
                    chartHeight + 8
                )
            )
        }
    }
}

@Composable
private fun CostTrendLineChart(
    breakdowns: List<Triple<String, Double, Double>>,
    textMeasurer: TextMeasurer
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 60f
        val chartWidth = canvasWidth - padding * 2
        val chartHeight = canvasHeight - padding

        if (breakdowns.size < 1) return@Canvas

        val allValues = breakdowns.flatMap { listOf(it.second, it.third) }.filter { it > 0 }
        val maxVal = allValues.maxOrNull() ?: 1.0
        val minVal = 0.0

        // Grid
        for (i in 0..4) {
            val y = chartHeight - (chartHeight * i / 4)
            drawLine(
                color = Color(0xFF2A2A2A),
                start = Offset(padding, y),
                end = Offset(canvasWidth - padding / 2, y),
                strokeWidth = 1f
            )
            val label = String.format("%.0f", maxVal * i / 4)
            val labelResult = textMeasurer.measure(
                text = AnnotatedString(label),
                style = TextStyle(color = Color(0xFF666666), fontSize = 10.sp)
            )
            drawText(
                textLayoutResult = labelResult,
                topLeft = Offset(2f, y - labelResult.size.height / 2)
            )
        }

        // Draw lines
        fun drawTrendLine(values: List<Double>, color: Color) {
            if (values.size < 2) {
                // Single point - draw a dot
                if (values.size == 1 && values[0] > 0) {
                    val x = padding + chartWidth / 2
                    val y = chartHeight - ((values[0] - minVal) / (maxVal - minVal) * chartHeight).toFloat()
                    drawCircle(color = color, radius = 6f, center = Offset(x, y))
                }
                return
            }
            val path = Path()
            val step = chartWidth / (values.size - 1)
            values.forEachIndexed { index, value ->
                val x = padding + step * index
                val y = chartHeight - ((value - minVal) / (maxVal - minVal) * chartHeight).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                // Draw point
                drawCircle(color = color, radius = 5f, center = Offset(x, y))
            }
            drawPath(path, color = color, style = Stroke(width = 3f, cap = StrokeCap.Round))
        }

        drawTrendLine(breakdowns.map { it.second }, Color(0xFFFF9800)) // fuel
        drawTrendLine(breakdowns.map { it.third }, Color(0xFF00E676)) // electric

        // X-axis labels
        breakdowns.forEachIndexed { index, (month, _, _) ->
            val step = if (breakdowns.size > 1) chartWidth / (breakdowns.size - 1) else chartWidth / 2
            val x = padding + step * index
            val monthLabel = if (month.length >= 7) month.substring(5) else month
            val monthResult = textMeasurer.measure(
                text = AnnotatedString(monthLabel),
                style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 10.sp)
            )
            drawText(
                textLayoutResult = monthResult,
                topLeft = Offset(x - monthResult.size.width / 2, chartHeight + 8)
            )
        }
    }
}

@Composable
private fun EvSplitPieChart(
    evPercentage: Float,
    textMeasurer: TextMeasurer
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = min(centerX, centerY) - 20f

        val evAngle = evPercentage / 100f * 360f
        val petrolAngle = 360f - evAngle

        // Petrol arc (orange)
        drawArc(
            color = Color(0xFFFF9800),
            startAngle = -90f + evAngle,
            sweepAngle = petrolAngle,
            useCenter = true,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2)
        )

        // Electric arc (green)
        drawArc(
            color = Color(0xFF00E676),
            startAngle = -90f,
            sweepAngle = evAngle,
            useCenter = true,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2)
        )

        // Center hole (donut chart)
        drawCircle(
            color = Color(0xFF1C1C1C),
            radius = radius * 0.55f,
            center = Offset(centerX, centerY)
        )

        // Center text
        val evText = String.format("%.0f%%", evPercentage)
        val evResult = textMeasurer.measure(
            text = AnnotatedString(evText),
            style = TextStyle(
                color = Color(0xFF00E676),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        )
        drawText(
            textLayoutResult = evResult,
            topLeft = Offset(
                centerX - evResult.size.width / 2,
                centerY - evResult.size.height / 2 - 8
            )
        )

        val evLabel = "Electric"
        val evLabelResult = textMeasurer.measure(
            text = AnnotatedString(evLabel),
            style = TextStyle(color = Color(0xFFAAAAAA), fontSize = 12.sp)
        )
        drawText(
            textLayoutResult = evLabelResult,
            topLeft = Offset(
                centerX - evLabelResult.size.width / 2,
                centerY + evResult.size.height / 2 - 8
            )
        )

        // Segment labels on the arcs
        if (evPercentage > 5 && evPercentage < 95) {
            // Petrol % label
            val petrolMidAngle = Math.toRadians((-90.0 + evAngle + petrolAngle / 2))
            val labelRadius = radius * 0.8f
            val px = centerX + labelRadius * cos(petrolMidAngle).toFloat()
            val py = centerY + labelRadius * sin(petrolMidAngle).toFloat()
            val petrolText = String.format("%.0f%%", 100 - evPercentage)
            val petrolResult = textMeasurer.measure(
                text = AnnotatedString(petrolText),
                style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
            drawText(
                textLayoutResult = petrolResult,
                topLeft = Offset(px - petrolResult.size.width / 2, py - petrolResult.size.height / 2)
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2)
        }
        Spacer(Modifier.width(6.dp))
        Text(text = label, color = DiLinkTextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun MonthDetailRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = DiLinkTextSecondary, fontSize = 14.sp)
        Text(text = value, color = valueColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
private fun LifetimeRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = DiLinkTextSecondary, fontSize = 14.sp)
        Text(text = value, color = DiLinkTextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}
