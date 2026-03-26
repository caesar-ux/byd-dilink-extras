package com.byd.dilink.extras.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TireRotationDao {
    @Insert
    suspend fun insert(record: TireRotationRecord): Long

    @Delete
    suspend fun delete(record: TireRotationRecord)

    @Query("SELECT * FROM tire_rotation_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<TireRotationRecord>>

    @Query("SELECT * FROM tire_rotation_records ORDER BY date DESC LIMIT 1")
    fun getLatestRecord(): Flow<TireRotationRecord?>
}
