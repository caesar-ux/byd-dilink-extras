package com.byd.dilink.extras.data.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tire_pressure_records")
data class TirePressureRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val odometerKm: Int?,
    val flBar: Double,
    val frBar: Double,
    val rlBar: Double,
    val rrBar: Double,
    val flCondition: String?,
    val frCondition: String?,
    val rlCondition: String?,
    val rrCondition: String?,
    val tireBrand: String?,
    val notes: String?
)
