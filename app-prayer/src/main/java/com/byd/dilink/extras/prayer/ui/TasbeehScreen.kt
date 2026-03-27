package com.byd.dilink.extras.prayer.ui

import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byd.dilink.extras.prayer.viewmodel.TasbeehViewModel
import com.byd.dilink.extras.ui.components.*
import com.byd.dilink.extras.ui.theme.*

data class Dhikr(
    val arabic: String,
    val transliteration: String
)

val PRESET_DHIKR = listOf(
    Dhikr("سبحان الله", "SubhanAllah"),
    Dhikr("الحمد لله", "Alhamdulillah"),
    Dhikr("الله أكبر", "Allahu Akbar"),
    Dhikr("لا إله إلا الله", "La ilaha illallah")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbeehScreen(
    onBack: () -> Unit,
    viewModel: TasbeehViewModel = hiltViewModel()
) {
    val counter by viewModel.counter.collectAsStateWithLifecycle()
    val currentDhikr by viewModel.currentDhikr.collectAsStateWithLifecycle()
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val loopMode by viewModel.loopMode.collectAsStateWithLifecycle()
    val dailyTotal by viewModel.dailyTotal.collectAsStateWithLifecycle()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()

    var showResetDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showCustomDhikrDialog by remember { mutableStateOf(false) }

    // Pulse animation
    var pulseCenter by remember { mutableStateOf(Offset.Zero) }
    var pulseKey by remember { mutableIntStateOf(0) }
    val pulseRadius = remember { Animatable(0f) }
    val pulseAlpha = remember { Animatable(0f) }

    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }
    val toneGenerator = remember {
        try { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 30) } catch (_: Exception) { null }
    }

    LaunchedEffect(pulseKey) {
        if (pulseKey > 0) {
            pulseRadius.snapTo(0f)
            pulseAlpha.snapTo(0.6f)
            kotlinx.coroutines.coroutineScope {
                launch {
                    pulseRadius.animateTo(200f, tween(400, easing = FastOutSlowInEasing))
                }
                launch {
                    pulseAlpha.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasbeeh Counter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Loop mode toggle
                    IconButton(onClick = { viewModel.toggleLoopMode() }) {
                        Icon(
                            Icons.Default.Loop,
                            contentDescription = "Loop Mode",
                            tint = if (loopMode) PrayerEmerald else DiLinkTextMuted
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Counter display
            Spacer(Modifier.height(16.dp))

            Text(
                text = "$counter",
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = DiLinkTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "/ $goal",
                fontSize = 20.sp,
                color = DiLinkTextMuted,
                textAlign = TextAlign.Center
            )

            // Progress bar
            LinearProgressIndicator(
                progress = { (counter.toFloat() / goal).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 8.dp)
                    .height(4.dp),
                color = PrayerEmerald,
                trackColor = DiLinkSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // Dhikr text
            Text(
                text = currentDhikr.arabic,
                fontSize = 24.sp,
                color = PrayerGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = currentDhikr.transliteration,
                fontSize = 18.sp,
                color = DiLinkTextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // HUGE TAP AREA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        color = DiLinkSurface,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.increment()

                        // Haptic feedback
                        if (vibrationEnabled) {
                            vibrator?.let {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    it.vibrate(
                                        VibrationEffect.createOneShot(
                                            20,
                                            VibrationEffect.DEFAULT_AMPLITUDE
                                        )
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    it.vibrate(20)
                                }
                            }
                        }

                        // Sound
                        if (soundEnabled) {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
                        }

                        // Pulse animation
                        pulseKey++
                    },
                contentAlignment = Alignment.Center
            ) {
                // Pulse effect
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (pulseAlpha.value > 0f) {
                        drawCircle(
                            color = PrayerEmerald.copy(alpha = pulseAlpha.value),
                            radius = pulseRadius.value.dp.toPx(),
                            center = Offset(size.width / 2, size.height / 2)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = "Tap to count",
                        tint = DiLinkTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "TAP TO COUNT",
                        color = DiLinkTextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Controls row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reset button
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset", color = StatusRed)
                }
                // Goal presets
                listOf(33, 99, 100).forEach { g ->
                    OutlinedButton(
                        onClick = { viewModel.setGoal(g) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (goal == g) ButtonDefaults.outlinedButtonColors(
                            containerColor = PrayerEmerald.copy(alpha = 0.2f)
                        ) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("$g", color = if (goal == g) PrayerEmerald else DiLinkTextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Daily total
            Text(
                "Today: $dailyTotal",
                color = DiLinkTextMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Dhikr selector buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PRESET_DHIKR.forEach { dhikr ->
                    val isSelected = currentDhikr.arabic == dhikr.arabic
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setDhikr(dhikr) },
                        label = {
                            Text(
                                dhikr.arabic,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrayerEmerald,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Custom button
            TextButton(onClick = { showCustomDhikrDialog = true }) {
                Text("Custom Dhikr", color = PrayerGold, fontSize = 12.sp)
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    // Reset confirmation dialog
    if (showResetDialog) {
        ConfirmDialog(
            title = "Reset Counter",
            message = "Are you sure you want to reset the counter to 0?",
            confirmText = "Reset",
            onConfirm = {
                viewModel.reset()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    // Custom dhikr dialog
    if (showCustomDhikrDialog) {
        var customArabic by remember { mutableStateOf("") }
        var customTranslit by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomDhikrDialog = false },
            title = { Text("Custom Dhikr") },
            text = {
                Column {
                    OutlinedTextField(
                        value = customArabic,
                        onValueChange = { customArabic = it },
                        label = { Text("Arabic Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customTranslit,
                        onValueChange = { customTranslit = it },
                        label = { Text("Transliteration") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customArabic.isNotBlank()) {
                        viewModel.setDhikr(
                            Dhikr(customArabic, customTranslit.ifBlank { customArabic })
                        )
                    }
                    showCustomDhikrDialog = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDhikrDialog = false }) { Text("Cancel") }
            },
            containerColor = DiLinkSurfaceElevated
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 400, heightDp = 700)
@Composable
fun TasbeehScreenPreview() {
    DiLinkExtrasTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DiLinkBackground)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "33",
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = DiLinkTextPrimary
            )
            Text("/ 33", fontSize = 20.sp, color = DiLinkTextMuted)
            Spacer(Modifier.height(16.dp))
            Text("سبحان الله", fontSize = 24.sp, color = PrayerGold)
            Text("SubhanAllah", fontSize = 18.sp, color = DiLinkTextMuted)
        }
    }
}
