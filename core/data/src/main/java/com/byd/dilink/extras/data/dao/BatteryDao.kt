package com.byd.dilink.extras.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    @Insert
    suspend fun insert(record: BatteryRecord): Long

    @Delete
    suspend fun delete(record: BatteryRecord)

    @Query("SELECT * FROM battery_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<BatteryRecord>>

    @Query("SELECT * FROM battery_records ORDER BY date DESC LIMIT 1")
    fun getLatestRecord(): Flow<BatteryRecord?>

    @Query("SELECT * FROM battery_records ORDER BY date DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<BatteryRecord>>
}
