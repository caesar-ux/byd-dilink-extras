package com.byd.dilink.extras.data.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_records")
data class BatteryRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val voltage: Double,
    val condition: String,
    val engineState: String,
    val notes: String?
)
