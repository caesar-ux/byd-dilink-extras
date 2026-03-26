package com.byd.dilink.extras.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.tireGuardPrefs: DataStore<Preferences> by preferencesDataStore(name = "tire_guard_prefs")
val Context.prayerPrefs: DataStore<Preferences> by preferencesDataStore(name = "prayer_prefs")
val Context.hazardPrefs: DataStore<Preferences> by preferencesDataStore(name = "hazard_prefs")
val Context.fuelCostPrefs: DataStore<Preferences> by preferencesDataStore(name = "fuel_cost_prefs")

object TireGuardPrefsKeys {
    val RECOMMENDED_PRESSURE = doublePreferencesKey("recommended_pressure")
    val LOW_THRESHOLD = doublePreferencesKey("low_threshold")
    val HIGH_THRESHOLD = doublePreferencesKey("high_threshold")
    val BATTERY_LOW_THRESHOLD = doublePreferencesKey("battery_low_threshold")
    val BATTERY_CRITICAL_THRESHOLD = doublePreferencesKey("battery_critical_threshold")
    val CHECK_INTERVAL_DAYS = intPreferencesKey("check_interval_days")
    val USE_PSI = booleanPreferencesKey("use_psi")
    val ROTATION_INTERVAL_KM = intPreferencesKey("rotation_interval_km")
}

object PrayerPrefsKeys {
    val CALCULATION_METHOD = stringPreferencesKey("calculation_method")
    val ASR_METHOD = stringPreferencesKey("asr_method")
    val USE_24H = booleanPreferencesKey("use_24h")
    val FAJR_ADJUST = intPreferencesKey("fajr_adjust")
    val SUNRISE_ADJUST = intPreferencesKey("sunrise_adjust")
    val DHUHR_ADJUST = intPreferencesKey("dhuhr_adjust")
    val ASR_ADJUST = intPreferencesKey("asr_adjust")
    val MAGHRIB_ADJUST = intPreferencesKey("maghrib_adjust")
    val ISHA_ADJUST = intPreferencesKey("isha_adjust")
    val TASBEEH_VIBRATION = booleanPreferencesKey("tasbeeh_vibration")
    val TASBEEH_SOUND = booleanPreferencesKey("tasbeeh_sound")
    val DEFAULT_TASBEEH_GOAL = intPreferencesKey("default_tasbeeh_goal")
    val MANUAL_LATITUDE = doublePreferencesKey("manual_latitude")
    val MANUAL_LONGITUDE = doublePreferencesKey("manual_longitude")
    val MANUAL_TIMEZONE = doublePreferencesKey("manual_timezone")
    val USE_AUTO_LOCATION = booleanPreferencesKey("use_auto_location")
    val USE_AUTO_TIMEZONE = booleanPreferencesKey("use_auto_timezone")
}

object HazardPrefsKeys {
    val WARNING_DISTANCE_METERS = intPreferencesKey("warning_distance_meters")
    val WARNING_SOUND_ENABLED = booleanPreferencesKey("warning_sound_enabled")
    val WARNING_VOLUME = floatPreferencesKey("warning_volume")
    val AUTO_RECORD = booleanPreferencesKey("auto_record")
    val HAZARD_EXPIRY_DAYS = intPreferencesKey("hazard_expiry_days")
}

object FuelCostPrefsKeys {
    val BATTERY_CAPACITY_KWH = doublePreferencesKey("battery_capacity_kwh")
    val DEFAULT_FUEL_PRICE_IQD = doublePreferencesKey("default_fuel_price_iqd")
    val DEFAULT_ELECTRICITY_PRICE_IQD = doublePreferencesKey("default_electricity_price_iqd")
    val BENCHMARK_L_PER_100KM = doublePreferencesKey("benchmark_l_per_100km")
    val USE_USD = booleanPreferencesKey("use_usd")
    val USD_EXCHANGE_RATE = doublePreferencesKey("usd_exchange_rate")
}
