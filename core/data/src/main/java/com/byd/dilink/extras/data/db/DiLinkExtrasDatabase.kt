package com.byd.dilink.extras.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.byd.dilink.extras.data.dao.*

@Database(
    entities = [
        TirePressureRecord::class,
        BatteryRecord::class,
        TireRotationRecord::class,
        TasbeehSession::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DiLinkExtrasDatabase : RoomDatabase() {
    abstract fun tirePressureDao(): TirePressureDao
    abstract fun batteryDao(): BatteryDao
    abstract fun tireRotationDao(): TireRotationDao
    abstract fun tasbeehDao(): TasbeehDao
}
