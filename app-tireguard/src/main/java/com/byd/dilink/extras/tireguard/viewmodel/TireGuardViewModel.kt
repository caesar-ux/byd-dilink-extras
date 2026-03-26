package com.byd.dilink.extras.tireguard.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byd.dilink.extras.data.dao.*
import com.byd.dilink.extras.data.preferences.TireGuardPrefsKeys
import com.byd.dilink.extras.data.preferences.tireGuardPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ReminderState(
    val showReminder: Boolean = false,
    val message: String = ""
)

data class TireGuardSettings(
    val recommendedPressure: Double = 2.4,
    val lowThreshold: Double = 2.1,
    val highThreshold: Double = 2.7,
    val batteryLowThreshold: Double = 12.0,
    val batteryCriticalThreshold: Double = 11.5,
    val checkIntervalDays: Int = 14,
    val usePsi: Boolean = false,
    val rotationIntervalKm: Int = 10000
)

@HiltViewModel
class TireGuardViewModel @Inject constructor(
    private val tirePressureDao: TirePressureDao,
    private val batteryDao: BatteryDao,
    private val tireRotationDao: TireRotationDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Latest tire pressures
    val latestTirePressures: StateFlow<TirePressureRecord?> = tirePressureDao
        .getLatestRecord()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Latest battery
    val latestBatteryVoltage: StateFlow<BatteryRecord?> = batteryDao
        .getLatestRecord()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All tire history
    val tireHistory: StateFlow<List<TirePressureRecord>> = tirePressureDao
        .getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All battery history
    val batteryHistory: StateFlow<List<BatteryRecord>> = batteryDao
        .getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Rotation records
    val rotationRecords: StateFlow<List<TireRotationRecord>> = tireRotationDao
        .getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    val settingsState: StateFlow<TireGuardSettings> = context.tireGuardPrefs.data
        .map { prefs ->
            TireGuardSettings(
                recommendedPressure = prefs[TireGuardPrefsKeys.RECOMMENDED_PRESSURE] ?: 2.4,
                lowThreshold = prefs[TireGuardPrefsKeys.LOW_THRESHOLD] ?: 2.1,
                highThreshold = prefs[TireGuardPrefsKeys.HIGH_THRESHOLD] ?: 2.7,
                batteryLowThreshold = prefs[TireGuardPrefsKeys.BATTERY_LOW_THRESHOLD] ?: 12.0,
                batteryCriticalThreshold = prefs[TireGuardPrefsKeys.BATTERY_CRITICAL_THRESHOLD] ?: 11.5,
                checkIntervalDays = prefs[TireGuardPrefsKeys.CHECK_INTERVAL_DAYS] ?: 14,
                usePsi = prefs[TireGuardPrefsKeys.USE_PSI] ?: false,
                rotationIntervalKm = prefs[TireGuardPrefsKeys.ROTATION_INTERVAL_KM] ?: 10000
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TireGuardSettings())

    // Reminder state
    val reminderState: StateFlow<ReminderState> = combine(
        latestTirePressures,
        latestBatteryVoltage,
        settingsState
    ) { tires, battery, settings ->
        val now = System.currentTimeMillis()
        val tireCheckIntervalMs = TimeUnit.DAYS.toMillis(settings.checkIntervalDays.toLong())
        val batteryCheckIntervalMs = TimeUnit.DAYS.toMillis(30)

        val tireOverdue = tires?.let { (now - it.date) > tireCheckIntervalMs } ?: true
        val batteryOverdue = battery?.let { (now - it.date) > batteryCheckIntervalMs } ?: true

        when {
            tireOverdue && batteryOverdue -> ReminderState(true, "Time for a tire pressure and battery check!")
            tireOverdue -> ReminderState(true, "Time for a tire pressure check!")
            batteryOverdue -> ReminderState(true, "Time for a battery voltage check!")
            else -> ReminderState(false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderState())

    // Slow leak detection: for each position, check if last 3 consecutive readings
    // each dropped by >0.1 bar
    val slowLeakDetection: StateFlow<Map<String, Boolean>> = tireHistory.map { records ->
        if (records.size < 3) return@map emptyMap()

        val sorted = records.sortedByDescending { it.date }
        val positions = listOf("FL", "FR", "RL", "RR")
        val result = mutableMapOf<String, Boolean>()

        positions.forEach { pos ->
            val pressures = sorted.take(3).map { rec ->
                when (pos) {
                    "FL" -> rec.flBar
                    "FR" -> rec.frBar
                    "RL" -> rec.rlBar
                    "RR" -> rec.rrBar
                    else -> 0.0
                }
            }
            // pressures[0] is most recent, pressures[2] is oldest
            // Check if each older reading is > next by 0.1 bar (indicating decline)
            val leaking = pressures.size == 3 &&
                    (pressures[2] - pressures[1]) > 0.1 &&
                    (pressures[1] - pressures[0]) > 0.1
            result[pos] = leaking
        }
        result.toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Battery degradation warning: if last 3 readings show steady decline
    val batteryDegradationWarning: StateFlow<Boolean> = batteryHistory.map { records ->
        if (records.size < 3) return@map false
        val sorted = records.sortedByDescending { it.date }.take(3)
        val voltages = sorted.map { it.voltage }
        // voltages[0] most recent, voltages[2] oldest
        voltages[2] > voltages[1] && voltages[1] > voltages[0] &&
                (voltages[2] - voltages[0]) > 0.3
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ── Actions ──

    fun logTires(
        date: Long,
        fl: Double, fr: Double, rl: Double, rr: Double,
        odometerKm: Int?,
        flCond: String, frCond: String, rlCond: String, rrCond: String,
        tireBrand: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            tirePressureDao.insert(
                TirePressureRecord(
                    date = date,
                    odometerKm = odometerKm,
                    flBar = fl, frBar = fr, rlBar = rl, rrBar = rr,
                    flCondition = flCond, frCondition = frCond,
                    rlCondition = rlCond, rrCondition = rrCond,
                    tireBrand = tireBrand,
                    notes = notes
                )
            )
        }
    }

    fun logBattery(
        date: Long,
        voltage: Double,
        condition: String,
        engineState: String,
        notes: String?
    ) {
        viewModelScope.launch {
            batteryDao.insert(
                BatteryRecord(
                    date = date,
                    voltage = voltage,
                    condition = condition,
                    engineState = engineState,
                    notes = notes
                )
            )
        }
    }

    fun recordRotation(
        date: Long,
        odometerKm: Int,
        pattern: String,
        notes: String?
    ) {
        viewModelScope.launch {
            tireRotationDao.insert(
                TireRotationRecord(
                    date = date,
                    odometerKm = odometerKm,
                    pattern = pattern,
                    notes = notes
                )
            )
        }
    }

    fun deleteTireRecord(record: TirePressureRecord) {
        viewModelScope.launch {
            tirePressureDao.delete(record)
        }
    }

    fun deleteBatteryRecord(record: BatteryRecord) {
        viewModelScope.launch {
            batteryDao.delete(record)
        }
    }

    fun saveSettings(
        recommendedPressure: Double,
        lowThreshold: Double,
        highThreshold: Double,
        batteryLow: Double,
        batteryCritical: Double,
        usePsi: Boolean,
        checkIntervalDays: Int,
        rotationIntervalKm: Int
    ) {
        viewModelScope.launch {
            context.tireGuardPrefs.edit { prefs ->
                prefs[TireGuardPrefsKeys.RECOMMENDED_PRESSURE] = recommendedPressure
                prefs[TireGuardPrefsKeys.LOW_THRESHOLD] = lowThreshold
                prefs[TireGuardPrefsKeys.HIGH_THRESHOLD] = highThreshold
                prefs[TireGuardPrefsKeys.BATTERY_LOW_THRESHOLD] = batteryLow
                prefs[TireGuardPrefsKeys.BATTERY_CRITICAL_THRESHOLD] = batteryCritical
                prefs[TireGuardPrefsKeys.USE_PSI] = usePsi
                prefs[TireGuardPrefsKeys.CHECK_INTERVAL_DAYS] = checkIntervalDays
                prefs[TireGuardPrefsKeys.ROTATION_INTERVAL_KM] = rotationIntervalKm
            }
        }
    }
}
