package com.byd.dilink.extras.prayer.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byd.dilink.extras.data.preferences.PrayerPrefsKeys
import com.byd.dilink.extras.data.preferences.prayerPrefs
import com.byd.dilink.extras.prayer.engine.PrayerTimeCalculator
import com.byd.dilink.extras.prayer.engine.QiblaCalculator
import com.byd.dilink.extras.prayer.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs

// Iraqi cities with coordinates
private data class CityInfo(val name: String, val lat: Double, val lon: Double)

private val IRAQI_CITIES = listOf(
    CityInfo("Erbil", 36.19, 44.01),
    CityInfo("Baghdad", 33.31, 44.37),
    CityInfo("Basra", 30.51, 47.78),
    CityInfo("Sulaymaniyah", 35.56, 45.43),
    CityInfo("Mosul", 36.34, 43.12),
    CityInfo("Kirkuk", 35.47, 44.39),
    CityInfo("Najaf", 32.00, 44.34),
    CityInfo("Karbala", 32.62, 44.02),
    CityInfo("Duhok", 36.87, 42.95),
    CityInfo("Nasiriyah", 31.04, 46.26),
    CityInfo("Samarra", 34.20, 43.88),
    CityInfo("Fallujah", 33.35, 43.78),
    CityInfo("Tikrit", 34.60, 43.68),
    CityInfo("Ramadi", 33.43, 43.30),
    CityInfo("Hillah", 32.48, 44.42)
)

@HiltViewModel
class PrayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Default location: Erbil, Iraq
    private val _latitude = MutableStateFlow(36.19)
    private val _longitude = MutableStateFlow(44.01)
    private val _timezone = MutableStateFlow(3.0)

    // Preferences
    val calculationMethod: StateFlow<CalculationMethod> = context.prayerPrefs.data
        .map { prefs ->
            val name = prefs[PrayerPrefsKeys.CALCULATION_METHOD] ?: "UMM_AL_QURA"
            CalculationMethod.entries.find { it.name == name } ?: CalculationMethod.UMM_AL_QURA
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalculationMethod.UMM_AL_QURA)

    val asrMethod: StateFlow<AsrMethod> = context.prayerPrefs.data
        .map { prefs ->
            val name = prefs[PrayerPrefsKeys.ASR_METHOD] ?: "SHAFI"
            AsrMethod.entries.find { it.name == name } ?: AsrMethod.SHAFI
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AsrMethod.SHAFI)

    val use24h: StateFlow<Boolean> = context.prayerPrefs.data
        .map { prefs -> prefs[PrayerPrefsKeys.USE_24H] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val tasbeehVibration: StateFlow<Boolean> = context.prayerPrefs.data
        .map { prefs -> prefs[PrayerPrefsKeys.TASBEEH_VIBRATION] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val tasbeehSound: StateFlow<Boolean> = context.prayerPrefs.data
        .map { prefs -> prefs[PrayerPrefsKeys.TASBEEH_SOUND] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val defaultTasbeehGoal: StateFlow<Int> = context.prayerPrefs.data
        .map { prefs -> prefs[PrayerPrefsKeys.DEFAULT_TASBEEH_GOAL] ?: 33 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 33)

    val useAutoLocation: StateFlow<Boolean> = context.prayerPrefs.data
        .map { prefs -> prefs[PrayerPrefsKeys.USE_AUTO_LOCATION] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val manualLatitude: StateFlow<Double> = context.prayerPrefs.data
        .map { prefs -> prefs[PrayerPrefsKeys.MANUAL_LATITUDE] ?: 36.19 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 36.19)

    val manualLongitude: StateFlow<Double> = context.prayerPrefs.data
        .map { prefs -> prefs[PrayerPrefsKeys.MANUAL_LONGITUDE] ?: 44.01 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 44.01)

    val manualTimezone: StateFlow<Double> = context.prayerPrefs.data
        .map { prefs -> prefs[PrayerPrefsKeys.MANUAL_TIMEZONE] ?: 3.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3.0)

    val manualAdjustments: StateFlow<IntArray> = context.prayerPrefs.data
        .map { prefs ->
            intArrayOf(
                prefs[PrayerPrefsKeys.FAJR_ADJUST] ?: 0,
                prefs[PrayerPrefsKeys.SUNRISE_ADJUST] ?: 0,
                prefs[PrayerPrefsKeys.DHUHR_ADJUST] ?: 0,
                prefs[PrayerPrefsKeys.ASR_ADJUST] ?: 0,
                prefs[PrayerPrefsKeys.MAGHRIB_ADJUST] ?: 0,
                prefs[PrayerPrefsKeys.ISHA_ADJUST] ?: 0
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IntArray(6))

    // Location name derived from coordinates
    val locationName: StateFlow<String> = combine(_latitude, _longitude) { lat, lon ->
        detectCityName(lat, lon)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Erbil, Iraq")

    // Prayer times computed from location + settings
    val prayerTimes: StateFlow<PrayerTimes?> = combine(
        _latitude, _longitude, _timezone,
        calculationMethod, asrMethod, manualAdjustments
    ) { values ->
        val lat = values[0] as Double
        val lon = values[1] as Double
        val tz = values[2] as Double
        val method = values[3] as CalculationMethod
        val asr = values[4] as AsrMethod
        val adj = values[5] as IntArray
        try {
            PrayerTimeCalculator.calculate(
                date = LocalDate.now(),
                latitude = lat,
                longitude = lon,
                timezone = tz,
                method = method,
                asrMethod = asr,
                adjustments = adj
            )
        } catch (_: Exception) { null }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current prayer (which time window we're in)
    val currentPrayer: StateFlow<PrayerName?> = prayerTimes.map { times ->
        if (times == null) return@map null
        val now = LocalTime.now()
        val list = times.toList()
        var current: PrayerName? = null
        for (i in list.indices) {
            if (now >= list[i].second) {
                current = list[i].first
            }
        }
        current
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Next prayer name
    val nextPrayerName: StateFlow<PrayerName?> = prayerTimes.map { times ->
        if (times == null) return@map null
        val now = LocalTime.now()
        times.toList().firstOrNull { it.second.isAfter(now) }?.first
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Countdown string (updates every second)
    private val _countdownTick = MutableStateFlow(0L)
    val nextPrayerCountdown: StateFlow<String> = combine(prayerTimes, _countdownTick) { times, _ ->
        if (times == null) return@combine ""
        val now = LocalTime.now()
        val next = times.toList().firstOrNull { it.second.isAfter(now) }
        if (next != null) {
            val minutesUntil = now.until(next.second, ChronoUnit.MINUTES)
            val hours = minutesUntil / 60
            val mins = minutesUntil % 60
            if (hours > 0) "in ${hours}h ${mins}m" else "in ${mins}m"
        } else {
            "All prayers completed"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Day progress
    val dayProgressFraction: StateFlow<Float> = combine(prayerTimes, _countdownTick) { times, _ ->
        if (times == null) return@combine 0f
        val now = LocalTime.now()
        val fajrMin = times.fajr.toSecondOfDay().toFloat()
        val ishaMin = times.isha.toSecondOfDay().toFloat()
        val nowMin = now.toSecondOfDay().toFloat()
        val range = ishaMin - fajrMin
        if (range <= 0) 0f else ((nowMin - fajrMin) / range).coerceIn(0f, 1f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Qibla
    val qiblaBearing: StateFlow<Double> = _latitude.combine(_longitude) { lat, lon ->
        QiblaCalculator.qiblaBearing(lat, lon)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val distanceToMakkah: StateFlow<Double> = _latitude.combine(_longitude) { lat, lon ->
        QiblaCalculator.distanceToMakkah(lat, lon)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        // Start countdown ticker
        viewModelScope.launch {
            while (true) {
                _countdownTick.value = System.currentTimeMillis()
                delay(1000)
            }
        }

        // Try to get GPS location
        requestLocationUpdate()

        // Watch for manual location changes
        viewModelScope.launch {
            combine(useAutoLocation, manualLatitude, manualLongitude, manualTimezone) { auto, lat, lon, tz ->
                if (!auto) {
                    _latitude.value = lat
                    _longitude.value = lon
                    _timezone.value = tz
                }
            }.collect()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdate() {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val providers = lm.getProviders(true)
            val provider = when {
                providers.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                providers.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> return
            }
            val lastKnown = lm.getLastKnownLocation(provider)
            if (lastKnown != null && useAutoLocation.value) {
                _latitude.value = lastKnown.latitude
                _longitude.value = lastKnown.longitude
            }
        } catch (_: SecurityException) {
            // Permission not granted — use default (Erbil)
        } catch (_: Exception) {
            // Fallback
        }
    }

    private fun detectCityName(lat: Double, lon: Double): String {
        // Simple proximity lookup — if within 30km of known city, show city name
        for (city in IRAQI_CITIES) {
            val dist = QiblaCalculator.distanceToMakkah(lat, lon).let {
                // Use haversine between current and city instead
                val dLat = Math.toRadians(city.lat - lat)
                val dLon = Math.toRadians(city.lon - lon)
                val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                        kotlin.math.cos(Math.toRadians(lat)) * kotlin.math.cos(Math.toRadians(city.lat)) *
                        kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
                val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
                6371.0 * c
            }
            if (dist < 30.0) return "${city.name}, Iraq"
        }
        return "${String.format("%.2f", lat)}°N, ${String.format("%.2f", lon)}°E"
    }

    // ── Settings Actions ──

    fun setCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch {
            context.prayerPrefs.edit { it[PrayerPrefsKeys.CALCULATION_METHOD] = method.name }
        }
    }

    fun setAsrMethod(method: AsrMethod) {
        viewModelScope.launch {
            context.prayerPrefs.edit { it[PrayerPrefsKeys.ASR_METHOD] = method.name }
        }
    }

    fun setUse24h(value: Boolean) {
        viewModelScope.launch {
            context.prayerPrefs.edit { it[PrayerPrefsKeys.USE_24H] = value }
        }
    }

    fun setTasbeehVibration(value: Boolean) {
        viewModelScope.launch {
            context.prayerPrefs.edit { it[PrayerPrefsKeys.TASBEEH_VIBRATION] = value }
        }
    }

    fun setTasbeehSound(value: Boolean) {
        viewModelScope.launch {
            context.prayerPrefs.edit { it[PrayerPrefsKeys.TASBEEH_SOUND] = value }
        }
    }

    fun setDefaultTasbeehGoal(goal: Int) {
        viewModelScope.launch {
            context.prayerPrefs.edit { it[PrayerPrefsKeys.DEFAULT_TASBEEH_GOAL] = goal }
        }
    }

    fun setUseAutoLocation(value: Boolean) {
        viewModelScope.launch {
            context.prayerPrefs.edit { it[PrayerPrefsKeys.USE_AUTO_LOCATION] = value }
            if (value) requestLocationUpdate()
        }
    }

    fun setManualLocation(lat: Double, lon: Double, tz: Double) {
        viewModelScope.launch {
            context.prayerPrefs.edit {
                it[PrayerPrefsKeys.MANUAL_LATITUDE] = lat
                it[PrayerPrefsKeys.MANUAL_LONGITUDE] = lon
                it[PrayerPrefsKeys.MANUAL_TIMEZONE] = tz
            }
        }
    }

    fun setManualAdjustments(adjustments: IntArray) {
        viewModelScope.launch {
            context.prayerPrefs.edit {
                it[PrayerPrefsKeys.FAJR_ADJUST] = adjustments[0]
                it[PrayerPrefsKeys.SUNRISE_ADJUST] = adjustments[1]
                it[PrayerPrefsKeys.DHUHR_ADJUST] = adjustments[2]
                it[PrayerPrefsKeys.ASR_ADJUST] = adjustments[3]
                it[PrayerPrefsKeys.MAGHRIB_ADJUST] = adjustments[4]
                it[PrayerPrefsKeys.ISHA_ADJUST] = adjustments[5]
            }
        }
    }
}
