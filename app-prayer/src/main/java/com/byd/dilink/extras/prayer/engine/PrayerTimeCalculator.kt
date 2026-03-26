package com.byd.dilink.extras.prayer.engine

import com.byd.dilink.extras.prayer.model.AsrMethod
import com.byd.dilink.extras.prayer.model.CalculationMethod
import com.byd.dilink.extras.prayer.model.PrayerTimes
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.*

/**
 * Pure Kotlin prayer time calculator implementing the astronomical algorithms
 * from praytimes.org. No external libraries or internet required.
 *
 * Key formulas:
 * 1. Julian Date from Gregorian date
 * 2. Sun declination and Equation of Time from Julian Date
 * 3. Mid-day (Dhuhr) = 12 + timezone - longitude/15 - equationOfTime
 * 4. Time for angle = midDay ± arccos((-sin(angle) - sin(decl)*sin(lat)) / (cos(decl)*cos(lat))) / 15
 * 5. Asr = midDay + arccos((sin(acot(f+tan(|decl-lat|))) - sin(decl)*sin(lat)) / (cos(decl)*cos(lat))) / 15
 */
object PrayerTimeCalculator {

    // ── Trigonometric helpers (degree-based) ─────────────────────

    private fun dsin(d: Double): Double = sin(Math.toRadians(d))
    private fun dcos(d: Double): Double = cos(Math.toRadians(d))
    private fun dtan(d: Double): Double = tan(Math.toRadians(d))
    private fun darcsin(x: Double): Double = Math.toDegrees(asin(x.coerceIn(-1.0, 1.0)))
    private fun darccos(x: Double): Double = Math.toDegrees(acos(x.coerceIn(-1.0, 1.0)))
    private fun darctan(x: Double): Double = Math.toDegrees(atan(x))
    private fun darctan2(y: Double, x: Double): Double = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double): Double = Math.toDegrees(atan(1.0 / x))

    private fun fixAngle(a: Double): Double {
        val result = a - 360.0 * floor(a / 360.0)
        return if (result < 0) result + 360.0 else result
    }

    private fun fixHour(h: Double): Double {
        val result = h - 24.0 * floor(h / 24.0)
        return if (result < 0) result + 24.0 else result
    }

    // ── Julian Date ──────────────────────────────────────────────

    /**
     * Convert Gregorian date to Julian Date Number.
     */
    fun toJulianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    // ── Sun Position ─────────────────────────────────────────────

    /**
     * Sun declination angle in degrees for a given Julian Date.
     */
    fun sunDeclination(jd: Double): Double {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2.0 * g))
        val e = 23.439 - 0.00000036 * d
        return darcsin(dsin(e) * dsin(l))
    }

    /**
     * Equation of Time in hours for a given Julian Date.
     * Based on praytimes.org algorithm:
     * EqT = (q/15 - fixHour(RA/15)) where q is mean longitude and RA is right ascension.
     */
    fun equationOfTime(jd: Double): Double {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2.0 * g))
        val e = 23.439 - 0.00000036 * d
        var ra = darctan2(dcos(e) * dsin(l), dcos(l))
        ra = fixAngle(ra)
        // Equation of time = difference between mean solar time and apparent solar time
        return q / 15.0 - fixHour(ra / 15.0)
    }

    // ── Core Computations ────────────────────────────────────────

    /**
     * Compute mid-day (Dhuhr) time in hours (local time).
     * The solar noon at Greenwich = 12 - EqT (where EqT is in hours).
     * Convert to local time: add timezone offset, subtract longitude correction.
     * midDay = 12 + timezone - longitude/15 - EqT
     */
    fun computeMidDay(jd: Double, longitude: Double, timezone: Double): Double {
        val eqt = equationOfTime(jd)
        val noon = 12.0 - eqt
        // Convert from Greenwich to local time
        return fixHour(noon + timezone - longitude / 15.0)
    }

    /**
     * Compute the time when the sun reaches a given angle below the horizon.
     *
     * @param jd Julian Date
     * @param angle Sun angle below horizon (positive value)
     * @param latitude Observer latitude in degrees
     * @param longitude Observer longitude in degrees
     * @param timezone Timezone offset in hours
     * @param afterNoon true for afternoon (Maghrib, Isha), false for morning (Fajr, Sunrise)
     * @return Time in hours
     */
    fun computeTimeForAngle(
        jd: Double,
        angle: Double,
        latitude: Double,
        longitude: Double,
        timezone: Double,
        afterNoon: Boolean
    ): Double {
        val decl = sunDeclination(jd)
        val midDay = computeMidDay(jd, longitude, timezone)

        val cosHA = (-dsin(angle) - dsin(decl) * dsin(latitude)) /
                (dcos(decl) * dcos(latitude))

        // If cosHA is out of range, the sun never reaches that angle at this latitude
        val ha = darccos(cosHA.coerceIn(-1.0, 1.0)) / 15.0

        return if (afterNoon) midDay + ha else midDay - ha
    }

    /**
     * Compute Asr prayer time.
     *
     * The shadow factor determines the Asr time:
     * - Shafi'i (factor=1): shadow = object_height + noon_shadow
     * - Hanafi (factor=2): shadow = 2 * object_height + noon_shadow
     *
     * The angle at which this shadow ratio occurs:
     * angle = acot(factor + tan(|declination - latitude|))
     *
     * @param jd Julian Date
     * @param factor Shadow factor (1 for Shafi'i, 2 for Hanafi)
     * @param latitude Observer latitude in degrees
     * @param longitude Observer longitude in degrees
     * @param timezone Timezone offset in hours
     * @return Asr time in hours
     */
    fun computeAsr(
        jd: Double,
        factor: Int,
        latitude: Double,
        longitude: Double,
        timezone: Double
    ): Double {
        val decl = sunDeclination(jd)
        val midDay = computeMidDay(jd, longitude, timezone)

        // The angle where shadow = factor * object + noon_shadow
        val angle = darccot(factor.toDouble() + dtan(abs(decl - latitude)))

        val cosHA = (dsin(angle) - dsin(decl) * dsin(latitude)) /
                (dcos(decl) * dcos(latitude))

        val ha = darccos(cosHA.coerceIn(-1.0, 1.0)) / 15.0

        return midDay + ha
    }

    // ── Main Calculation ─────────────────────────────────────────

    /**
     * Calculate all six prayer times for a given date and location.
     *
     * @param date The date
     * @param latitude Observer latitude in degrees (positive = North)
     * @param longitude Observer longitude in degrees (positive = East)
     * @param timezone Timezone offset in hours (e.g., +3 for Baghdad)
     * @param method Calculation method (angles for Fajr/Isha)
     * @param asrMethod Asr juristic method (Shafi'i or Hanafi)
     * @param adjustments Manual adjustments in minutes for each prayer [fajr, sunrise, dhuhr, asr, maghrib, isha]
     * @return PrayerTimes with all six times
     */
    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        timezone: Double,
        method: CalculationMethod,
        asrMethod: AsrMethod,
        adjustments: IntArray = IntArray(6)
    ): PrayerTimes {
        val jd = toJulianDate(date.year, date.monthValue, date.dayOfMonth)

        // Fajr: sun at method.fajrAngle degrees below horizon, before noon
        val fajrHours = computeTimeForAngle(
            jd, method.fajrAngle, latitude, longitude, timezone, afterNoon = false
        ) + (adjustments.getOrElse(0) { 0 }) / 60.0

        // Sunrise: sun at 0.833° below horizon (accounting for atmospheric refraction + sun radius)
        val sunriseHours = computeTimeForAngle(
            jd, 0.833, latitude, longitude, timezone, afterNoon = false
        ) + (adjustments.getOrElse(1) { 0 }) / 60.0

        // Dhuhr: mid-day + 2 minutes safety margin
        val dhuhrHours = computeMidDay(jd, longitude, timezone) +
                2.0 / 60.0 + (adjustments.getOrElse(2) { 0 }) / 60.0

        // Asr
        val asrHours = computeAsr(
            jd, asrMethod.shadowFactor, latitude, longitude, timezone
        ) + (adjustments.getOrElse(3) { 0 }) / 60.0

        // Maghrib (sunset): sun at 0.833° below horizon, after noon
        val maghribHours = computeTimeForAngle(
            jd, 0.833, latitude, longitude, timezone, afterNoon = true
        ) + (adjustments.getOrElse(4) { 0 }) / 60.0

        // Isha: either fixed minutes after Maghrib or angle-based
        val ishaHours = if (method.ishaMinutes != null) {
            maghribHours + method.ishaMinutes / 60.0
        } else {
            computeTimeForAngle(
                jd, method.ishaAngle!!, latitude, longitude, timezone, afterNoon = true
            )
        } + (adjustments.getOrElse(5) { 0 }) / 60.0

        return PrayerTimes(
            fajr = hoursToLocalTime(fajrHours),
            sunrise = hoursToLocalTime(sunriseHours),
            dhuhr = hoursToLocalTime(dhuhrHours),
            asr = hoursToLocalTime(asrHours),
            maghrib = hoursToLocalTime(maghribHours),
            isha = hoursToLocalTime(ishaHours)
        )
    }

    // ── Utility ──────────────────────────────────────────────────

    /**
     * Convert fractional hours to LocalTime, handling overflow gracefully.
     */
    private fun hoursToLocalTime(hours: Double): LocalTime {
        val h = fixHour(hours)
        val totalMinutes = Math.round(h * 60.0).toInt()
        val hr = (totalMinutes / 60).coerceIn(0, 23)
        val min = (totalMinutes % 60).coerceIn(0, 59)
        return LocalTime.of(hr, min)
    }

    // ── Approximate Hijri Date ───────────────────────────────────

    /**
     * Simple approximate Gregorian-to-Hijri conversion.
     * Uses the Kuwaiti algorithm approximation. Not perfectly accurate
     * but sufficient for display purposes.
     */
    fun approximateHijriDate(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
        val jd = toJulianDate(year, month, day)
        // Epoch: Julian date of Hijri epoch (July 16, 622 CE)
        val hijriEpoch = 1948439.5
        val daysSinceEpoch = jd - hijriEpoch
        // Average Hijri month = 29.530588853 days (synodic month)
        val lunarYear = 354.36667
        val hijriYear = (daysSinceEpoch / lunarYear).toInt() + 1
        val remainingDays = daysSinceEpoch - (hijriYear - 1) * lunarYear
        val hijriMonth = ((remainingDays / 29.530588853) + 1).toInt().coerceIn(1, 12)
        val hijriDay = (remainingDays - (hijriMonth - 1) * 29.530588853 + 1).toInt().coerceIn(1, 30)
        return Triple(hijriYear, hijriMonth, hijriDay)
    }

    val hijriMonthNames = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )
}
