package com.byd.dilink.extras.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.byd.dilink.extras.data.dao.*

@Database(
    entities = [
        TirePressureRecord::class,
        BatteryRecord::class,
        TireRotationRecord::class,
        TasbeehSession::class,
        HazardRecord::class,
        FuelRecord::class,
        ChargeRecord::class,
        OdometerEntry::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DiLinkExtrasDatabase : RoomDatabase() {
    abstract fun tirePressureDao(): TirePressureDao
    abstract fun batteryDao(): BatteryDao
    abstract fun tireRotationDao(): TireRotationDao
    abstract fun tasbeehDao(): TasbeehDao
    abstract fun hazardDao(): HazardDao
    abstract fun fuelDao(): FuelDao
    abstract fun chargeDao(): ChargeDao
    abstract fun odometerDao(): OdometerDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `hazards` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `heading` REAL NOT NULL,
                        `speed` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `notes` TEXT,
                        `confirmed` INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `fuel_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `odometerKm` INTEGER NOT NULL,
                        `liters` REAL NOT NULL,
                        `totalCostIqd` REAL NOT NULL,
                        `pricePerLiter` REAL NOT NULL,
                        `fuelType` TEXT NOT NULL,
                        `isFullTank` INTEGER NOT NULL,
                        `stationName` TEXT,
                        `notes` TEXT
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `charge_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `odometerKm` INTEGER NOT NULL,
                        `kwhCharged` REAL NOT NULL,
                        `totalCostIqd` REAL NOT NULL,
                        `costPerKwh` REAL NOT NULL,
                        `source` TEXT NOT NULL,
                        `startSocPercent` INTEGER,
                        `endSocPercent` INTEGER,
                        `durationMin` INTEGER,
                        `notes` TEXT
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `odometer_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `odometerKm` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
