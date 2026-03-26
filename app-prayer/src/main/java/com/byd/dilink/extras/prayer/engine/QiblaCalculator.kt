package com.byd.dilink.extras.prayer.engine

import kotlin.math.*

/**
 * Calculates Qibla direction (great-circle bearing) and distance
 * from any location to the Kaaba in Makkah.
 *
 * Kaaba coordinates: 21.4225°N, 39.8262°E
 */
object QiblaCalculator {

    private const val KAABA_LAT = 21.4225
    private const val KAABA_LON = 39.8262
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculate the initial bearing (azimuth) from the given location to the Kaaba.
     *
     * @param lat Observer latitude in degrees
     * @param lon Observer longitude in degrees
     * @return Bearing in degrees from North (0-360)
     */
    fun qiblaBearing(lat: Double, lon: Double): Double {
        val lat1 = Math.toRadians(lat)
        val lat2 = Math.toRadians(KAABA_LAT)
        val dLon = Math.toRadians(KAABA_LON - lon)

        val x = sin(dLon) * cos(lat2)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        val bearing = Math.toDegrees(atan2(x, y))
        return (bearing + 360.0) % 360.0
    }

    /**
     * Calculate the great-circle distance from the given location to the Kaaba
     * using the Haversine formula.
     *
     * @param lat Observer latitude in degrees
     * @param lon Observer longitude in degrees
     * @return Distance in kilometers
     */
    fun distanceToMakkah(lat: Double, lon: Double): Double {
        val lat1 = Math.toRadians(lat)
        val lat2 = Math.toRadians(KAABA_LAT)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(KAABA_LON - lon)

        val a = sin(dLat / 2).pow(2) +
                cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_KM * c
    }
}
