package com.byd.dilink.extras.prayer.model

import java.time.LocalTime

data class PrayerTimes(
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime
) {
    fun toList(): List<Pair<PrayerName, LocalTime>> = listOf(
        PrayerName.FAJR to fajr,
        PrayerName.SUNRISE to sunrise,
        PrayerName.DHUHR to dhuhr,
        PrayerName.ASR to asr,
        PrayerName.MAGHRIB to maghrib,
        PrayerName.ISHA to isha
    )
}
