package com.byd.dilink.extras.prayer.ui

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byd.dilink.extras.prayer.engine.PrayerTimeCalculator
import com.byd.dilink.extras.prayer.model.PrayerName
import com.byd.dilink.extras.prayer.model.PrayerTimes
import com.byd.dilink.extras.prayer.viewmodel.PrayerViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    onNavigateToQibla: () -> Unit,
    onNavigateToTasbeeh: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: PrayerViewModel = hiltViewModel()
) {
    val prayerTimes by viewModel.prayerTimes.collectAsStateWithLifecycle()
    val currentPrayer by viewModel.currentPrayer.collectAsStateWithLifecycle()
    val nextPrayerCountdown by viewModel.nextPrayerCountdown.collectAsStateWithLifecycle()
    val nextPrayerName by viewModel.nextPrayerName.collectAsStateWithLifecycle()
    val dayProgress by viewModel.dayProgressFraction.collectAsStateWithLifecycle()
    val locationName by viewModel.locationName.collectAsStateWithLifecycle()
    val use24h by viewModel.use24h.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val today = LocalDate.now()
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val dateStr = "${today.dayOfMonth} ${today.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${today.year}"

    // Hijri date
    val (hijriY, hijriM, hijriD) = PrayerTimeCalculator.approximateHijriDate(
        today.year, today.monthValue, today.dayOfMonth
    )
    val hijriMonthName = PrayerTimeCalculator.hijriMonthNames.getOrElse(hijriM - 1) { "" }
    val hijriStr = "$hijriD $hijriMonthName $hijriY AH"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("☪ Prayer Times", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                actions = {
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
                // Left: header + prayer list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateLocationHeader(dayName, dateStr, hijriStr, locationName)
                    if (prayerTimes != null) {
                        PrayerTimesList(prayerTimes!!, currentPrayer, use24h)
                    }
                }
                // Right: countdown + progress + nav
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NextPrayerCard(nextPrayerName, nextPrayerCountdown)
                    DayProgressBar(dayProgress)
                    NavigationButtons(onNavigateToQibla, onNavigateToTasbeeh)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DateLocationHeader(dayName, dateStr, hijriStr, locationName)
                if (prayerTimes != null) {
                    PrayerTimesList(prayerTimes!!, currentPrayer, use24h)
                }
                NextPrayerCard(nextPrayerName, nextPrayerCountdown)
                DayProgressBar(dayProgress)
                NavigationButtons(onNavigateToQibla, onNavigateToTasbeeh)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun DateLocationHeader(dayName: String, dateStr: String, hijriStr: String, locationName: String) {
    SectionCard {
        Text(
            "$dayName, $dateStr",
            style = MaterialTheme.typography.titleMedium,
            color = DiLinkTextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            hijriStr,
            style = MaterialTheme.typography.bodyMedium,
            color = PrayerGold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            locationName,
            style = MaterialTheme.typography.bodySmall,
            color = DiLinkTextSecondary
        )
    }
}

@Composable
fun PrayerTimesList(
    prayerTimes: PrayerTimes,
    currentPrayer: PrayerName?,
    use24h: Boolean
) {
    val timeFormat = if (use24h) {
        DateTimeFormatter.ofPattern("HH:mm")
    } else {
        DateTimeFormatter.ofPattern("hh:mm a")
    }
    val now = LocalTime.now()

    prayerTimes.toList().forEach { (name, time) ->
        val isCurrent = name == currentPrayer
        val isPast = time.isBefore(now) && !isCurrent

        PrayerTimeRow(
            name = name,
            time = time.format(timeFormat),
            isCurrent = isCurrent,
            isPast = isPast
        )
    }
}

@Composable
fun PrayerTimeRow(
    name: PrayerName,
    time: String,
    isCurrent: Boolean,
    isPast: Boolean
) {
    val bgColor = when {
        isCurrent -> PrayerEmerald.copy(alpha = 0.2f)
        else -> Color.Transparent
    }
    val textColor = when {
        isCurrent -> PrayerEmerald
        isPast -> DiLinkTextMuted
        else -> DiLinkTextPrimary
    }
    val arabicColor = when {
        isCurrent -> PrayerEmerald.copy(alpha = 0.8f)
        isPast -> DiLinkTextMuted.copy(alpha = 0.5f)
        else -> DiLinkTextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCurrent) bgColor else DiLinkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isCurrent) PrayerEmerald else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        name.english,
                        color = textColor,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Text(
                        name.arabic,
                        color = arabicColor,
                        fontSize = 14.sp
                    )
                }
            }
            Text(
                time,
                color = textColor,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun NextPrayerCard(nextPrayerName: PrayerName?, countdown: String) {
    if (nextPrayerName == null) return

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Next: ${nextPrayerName.english}",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrayerGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    countdown,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DiLinkTextPrimary
                )
            }
        }
    }
}

@Composable
fun DayProgressBar(progress: Float) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Fajr", color = DiLinkTextMuted, fontSize = 12.sp)
            Text(
                "${(progress * 100).toInt()}% of day",
                color = DiLinkTextSecondary,
                fontSize = 12.sp
            )
            Text("Isha", color = DiLinkTextMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            // Background
            drawRoundRect(
                color = DiLinkSurfaceVariant,
                size = size,
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            // Progress
            drawRoundRect(
                color = PrayerEmerald,
                size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}

@Composable
fun NavigationButtons(
    onQibla: () -> Unit,
    onTasbeeh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LargeIconButton(
            text = "Qibla",
            icon = Icons.Default.Explore,
            color = PrayerGold,
            modifier = Modifier.weight(1f),
            height = 56.dp,
            onClick = onQibla
        )
        LargeIconButton(
            text = "Tasbeeh",
            icon = Icons.Default.TouchApp,
            color = PrayerEmerald,
            modifier = Modifier.weight(1f),
            height = 56.dp,
            onClick = onTasbeeh
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 400, heightDp = 700)
@Composable
fun PrayerTimesScreenPreview() {
    DiLinkExtrasTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DiLinkBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateLocationHeader("Thursday", "26 March 2026", "2 Ramadan 1448 AH", "Erbil, Iraq")
            PrayerTimeRow(PrayerName.FAJR, "04:52", isCurrent = false, isPast = true)
            PrayerTimeRow(PrayerName.SUNRISE, "06:11", isCurrent = false, isPast = true)
            PrayerTimeRow(PrayerName.DHUHR, "12:18", isCurrent = true, isPast = false)
            PrayerTimeRow(PrayerName.ASR, "15:42", isCurrent = false, isPast = false)
            PrayerTimeRow(PrayerName.MAGHRIB, "18:24", isCurrent = false, isPast = false)
            PrayerTimeRow(PrayerName.ISHA, "19:47", isCurrent = false, isPast = false)
            NextPrayerCard(PrayerName.ASR, "in 2h 35m")
            DayProgressBar(0.62f)
            NavigationButtons(onQibla = {}, onTasbeeh = {})
        }
    }
}
