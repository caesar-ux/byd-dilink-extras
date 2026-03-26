package com.byd.dilink.extras.data.repository

import com.byd.dilink.extras.data.dao.HazardDao
import com.byd.dilink.extras.data.dao.HazardRecord
import kotlinx.coroutines.flow.Flow
import kotlin.math.*

class HazardRepository(private val hazardDao: HazardDao) {

    fun getAll(): Flow<List<HazardRecord>> = hazardDao.getAll()

    fun getByType(type: String): Flow<List<HazardRecord>> = hazardDao.getByType(type)

    fun getCount(): Flow<Int> = hazardDao.getCount()

    suspend fun insert(record: HazardRecord): Long = hazardDao.insert(record)

    suspend fun delete(record: HazardRecord) = hazardDao.delete(record)

    suspend fun update(record: HazardRecord) = hazardDao.update(record)

    suspend fun deleteOlderThan(cutoffTimestamp: Long) = hazardDao.deleteOlderThan(cutoffTimestamp)

    suspend fun getAllOnce(): List<HazardRecord> = hazardDao.getAllOnce()

    suspend fun insertAll(records: List<HazardRecord>) = hazardDao.insertAll(records)

    suspend fun deleteAll() = hazardDao.deleteAll()

    /**
     * Returns hazards within the given radius (in meters) from a center point.
     */
    suspend fun getHazardsWithinRadius(
        centerLat: Double,
        centerLon: Double,
        radiusMeters: Double
    ): List<Pair<HazardRecord, Double>> {
        val allHazards = hazardDao.getAllOnce()
        return allHazards.mapNotNull { hazard ->
            val distance = haversineDistance(centerLat, centerLon, hazard.latitude, hazard.longitude)
            if (distance <= radiusMeters) {
                Pair(hazard, distance)
            } else null
        }.sortedBy { it.second }
    }

    /**
     * Returns hazards within a corridor between two points.
     * The corridor is defined by a width (in meters) around the great circle path.
     */
    suspend fun getHazardsAlongCorridor(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        corridorWidthMeters: Double
    ): List<Pair<HazardRecord, Double>> {
        val allHazards = hazardDao.getAllOnce()
        val halfWidth = corridorWidthMeters / 2.0
        val routeDistance = haversineDistance(startLat, startLon, endLat, endLon)

        return allHazards.mapNotNull { hazard ->
            val perpDist = perpendicularDistance(
                startLat, startLon, endLat, endLon,
                hazard.latitude, hazard.longitude
            )
            val distFromStart = haversineDistance(startLat, startLon, hazard.latitude, hazard.longitude)
            val distFromEnd = haversineDistance(endLat, endLon, hazard.latitude, hazard.longitude)

            // Check if the hazard is within the corridor (between start and end, and within width)
            if (perpDist <= halfWidth && distFromStart <= routeDistance + halfWidth && distFromEnd <= routeDistance + halfWidth) {
                Pair(hazard, distFromStart)
            } else null
        }.sortedBy { it.second }
    }

    companion object {
        /**
         * Haversine distance between two coordinates in meters.
         */
        fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val R = 6371000.0 // Earth radius in meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return R * c
        }

        /**
         * Calculate the initial bearing from point 1 to point 2 in degrees (0-360).
         */
        fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val lat1Rad = Math.toRadians(lat1)
            val lat2Rad = Math.toRadians(lat2)
            val dLonRad = Math.toRadians(lon2 - lon1)

            val x = sin(dLonRad) * cos(lat2Rad)
            val y = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLonRad)
            val bearingRad = atan2(x, y)
            return (Math.toDegrees(bearingRad) + 360) % 360
        }

        /**
         * Approximate perpendicular distance from a point to a great circle path
         * defined by two endpoints, in meters.
         */
        fun perpendicularDistance(
            pathLat1: Double, pathLon1: Double,
            pathLat2: Double, pathLon2: Double,
            pointLat: Double, pointLon: Double
        ): Double {
            val R = 6371000.0
            // Angular distance from start to point
            val d13 = haversineDistance(pathLat1, pathLon1, pointLat, pointLon) / R
            // Bearing from start to end
            val theta13 = Math.toRadians(bearing(pathLat1, pathLon1, pointLat, pointLon))
            // Bearing from start to point
            val theta12 = Math.toRadians(bearing(pathLat1, pathLon1, pathLat2, pathLon2))

            // Cross-track distance
            val dXt = asin(sin(d13) * sin(theta13 - theta12))
            return abs(dXt * R)
        }

        /**
         * Returns a human-readable direction label for a bearing in degrees.
         */
        fun directionLabel(bearingDeg: Double): String {
            val normalized = ((bearingDeg % 360) + 360) % 360
            return when {
                normalized < 22.5 -> "N"
                normalized < 67.5 -> "NE"
                normalized < 112.5 -> "E"
                normalized < 157.5 -> "SE"
                normalized < 202.5 -> "S"
                normalized < 247.5 -> "SW"
                normalized < 292.5 -> "W"
                normalized < 337.5 -> "NW"
                else -> "N"
            }
        }

        /**
         * Formats a distance in meters to a human-readable string.
         */
        fun formatDistance(meters: Double): String {
            return when {
                meters < 1000 -> "${meters.toInt()}m"
                else -> String.format("%.1fkm", meters / 1000.0)
            }
        }
    }
}
