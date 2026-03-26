package com.byd.dilink.extras.data.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_records")
data class FuelRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val odometerKm: Int,
    val liters: Double,
    val totalCostIqd: Double,
    val pricePerLiter: Double,
    val fuelType: String,
    val isFullTank: Boolean,
    val stationName: String?,
    val notes: String?
)
