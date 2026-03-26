package com.byd.dilink.extras.prayer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byd.dilink.extras.prayer.viewmodel.QiblaViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    onBack: () -> Unit,
    viewModel: QiblaViewModel = hiltViewModel()
) {
    val heading by viewModel.currentHeading.collectAsStateWithLifecycle()
    val qiblaBearing by viewModel.qiblaBearing.collectAsStateWithLifecycle()
    val distanceKm by viewModel.distanceToMakkah.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopBarWithBack(title = "Qibla Compass", onBack = onBack) },
        containerColor = DiLinkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Compass
            QiblaCompass(
                heading = heading,
                qiblaBearing = qiblaBearing,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .weight(1f, fill = false)
            )

            Spacer(Modifier.height(24.dp))

            // Info below compass
            SectionCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Qibla Direction",
                        style = MaterialTheme.typography.titleMedium,
                        color = DiLinkTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${String.format("%.1f", qiblaBearing)}° from North",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrayerGold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Distance to Makkah: ${String.format("%,.0f", distanceKm)} km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DiLinkTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (heading != 0f) "Compass heading: ${heading.toInt()}°" else "Rotate device to calibrate",
                        style = MaterialTheme.typography.bodySmall,
                        color = DiLinkTextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun QiblaCompass(
    heading: Float,
    qiblaBearing: Double,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f - 16.dp.toPx()

        // Rotate entire compass based on heading
        rotate(-heading, center) {
            // Outer circle
            drawCircle(
                color = DiLinkSurfaceVariant,
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner circle
            drawCircle(
                color = DiLinkSurface,
                radius = radius - 4.dp.toPx(),
                center = center,
                style = Fill
            )

            // Tick marks every 15°
            for (angle in 0 until 360 step 15) {
                val isCardinal = angle % 90 == 0
                val isMajor = angle % 30 == 0
                val tickLen = when {
                    isCardinal -> 20.dp.toPx()
                    isMajor -> 14.dp.toPx()
                    else -> 8.dp.toPx()
                }
                val tickWidth = if (isCardinal) 2.5.dp.toPx() else 1.dp.toPx()
                val angleRad = Math.toRadians(angle.toDouble()).toFloat()

                val outerX = center.x + (radius - 4.dp.toPx()) * sin(angleRad)
                val outerY = center.y - (radius - 4.dp.toPx()) * cos(angleRad)
                val innerX = center.x + (radius - 4.dp.toPx() - tickLen) * sin(angleRad)
                val innerY = center.y - (radius - 4.dp.toPx() - tickLen) * cos(angleRad)

                val tickColor = when {
                    angle == 0 -> StatusRed
                    isCardinal -> DiLinkTextPrimary
                    else -> DiLinkTextMuted
                }

                drawLine(
                    color = tickColor,
                    start = Offset(outerX, outerY),
                    end = Offset(innerX, innerY),
                    strokeWidth = tickWidth
                )
            }

            // Cardinal direction labels
            val cardinals = listOf(
                0f to "N", 90f to "E", 180f to "S", 270f to "W"
            )
            val labelRadius = radius - 40.dp.toPx()

            cardinals.forEach { (angle, label) ->
                val angleRad = Math.toRadians(angle.toDouble()).toFloat()
                val x = center.x + labelRadius * sin(angleRad)
                val y = center.y - labelRadius * cos(angleRad)
                val color = if (label == "N") StatusRed else DiLinkTextPrimary
                val textResult = textMeasurer.measure(
                    AnnotatedString(label),
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                )
                drawText(
                    textResult,
                    topLeft = Offset(
                        x - textResult.size.width / 2f,
                        y - textResult.size.height / 2f
                    )
                )
            }

            // Degree labels at 30° intervals
            for (deg in 30 until 360 step 30) {
                if (deg % 90 == 0) continue
                val angleRad = Math.toRadians(deg.toDouble()).toFloat()
                val x = center.x + (labelRadius - 8.dp.toPx()) * sin(angleRad)
                val y = center.y - (labelRadius - 8.dp.toPx()) * cos(angleRad)
                val textResult = textMeasurer.measure(
                    AnnotatedString("$deg"),
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = DiLinkTextMuted
                    )
                )
                drawText(
                    textResult,
                    topLeft = Offset(
                        x - textResult.size.width / 2f,
                        y - textResult.size.height / 2f
                    )
                )
            }

            // Qibla line
            val qiblaAngleRad = Math.toRadians(qiblaBearing).toFloat()
            val qiblaEndX = center.x + (radius - 24.dp.toPx()) * sin(qiblaAngleRad)
            val qiblaEndY = center.y - (radius - 24.dp.toPx()) * cos(qiblaAngleRad)

            // Qibla line glow
            drawLine(
                color = PrayerGold.copy(alpha = 0.3f),
                start = center,
                end = Offset(qiblaEndX, qiblaEndY),
                strokeWidth = 6.dp.toPx()
            )
            // Qibla line
            drawLine(
                color = PrayerGold,
                start = center,
                end = Offset(qiblaEndX, qiblaEndY),
                strokeWidth = 3.dp.toPx()
            )

            // Qibla arrowhead
            val arrowSize = 14.dp.toPx()
            val arrowAngle = Math.toRadians(qiblaBearing).toFloat()
            val tipX = qiblaEndX
            val tipY = qiblaEndY
            val leftAngle = arrowAngle - Math.toRadians(150.0).toFloat()
            val rightAngle = arrowAngle + Math.toRadians(150.0).toFloat()

            val arrowPath = Path().apply {
                moveTo(tipX, tipY)
                lineTo(
                    tipX + arrowSize * sin(leftAngle),
                    tipY - arrowSize * cos(leftAngle)
                )
                lineTo(
                    tipX + arrowSize * sin(rightAngle),
                    tipY - arrowSize * cos(rightAngle)
                )
                close()
            }
            drawPath(arrowPath, PrayerGold, style = Fill)

            // "Qibla" label near arrow
            val labelX = center.x + (radius * 0.55f) * sin(qiblaAngleRad)
            val labelY = center.y - (radius * 0.55f) * cos(qiblaAngleRad)
            val qiblaLabel = textMeasurer.measure(
                AnnotatedString("Qibla"),
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrayerGold
                )
            )
            drawText(
                qiblaLabel,
                topLeft = Offset(
                    labelX - qiblaLabel.size.width / 2f,
                    labelY - qiblaLabel.size.height / 2f
                )
            )

            // Center dot
            drawCircle(color = DiLinkTextPrimary, radius = 4.dp.toPx(), center = center)
            drawCircle(color = DiLinkBackground, radius = 2.dp.toPx(), center = center)
        }

        // Current heading indicator at top (outside rotation)
        val indicatorPath = Path().apply {
            moveTo(center.x, center.y - radius + 2.dp.toPx())
            lineTo(center.x - 8.dp.toPx(), center.y - radius - 10.dp.toPx())
            lineTo(center.x + 8.dp.toPx(), center.y - radius - 10.dp.toPx())
            close()
        }
        drawPath(indicatorPath, PrayerGold, style = Fill)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 400, heightDp = 500)
@Composable
fun QiblaCompassPreview() {
    DiLinkExtrasTheme {
        QiblaCompass(
            heading = 30f,
            qiblaBearing = 220.0,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
    }
}
