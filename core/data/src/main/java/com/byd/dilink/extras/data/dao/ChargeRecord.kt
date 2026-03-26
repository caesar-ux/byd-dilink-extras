package com.byd.dilink.extras.data.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charge_records")
data class ChargeRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val odometerKm: Int,
    val kwhCharged: Double,
    val totalCostIqd: Double,
    val costPerKwh: Double,
    val source: String,
    val startSocPercent: Int?,
    val endSocPercent: Int?,
    val durationMin: Int?,
    val notes: String?
)
