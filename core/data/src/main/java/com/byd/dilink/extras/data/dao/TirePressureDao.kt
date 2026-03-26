package com.byd.dilink.extras.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TirePressureDao {
    @Insert
    suspend fun insert(record: TirePressureRecord): Long

    @Delete
    suspend fun delete(record: TirePressureRecord)

    @Query("SELECT * FROM tire_pressure_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<TirePressureRecord>>

    @Query("SELECT * FROM tire_pressure_records ORDER BY date DESC LIMIT 1")
    fun getLatestRecord(): Flow<TirePressureRecord?>

    @Query("SELECT * FROM tire_pressure_records ORDER BY date DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<TirePressureRecord>>

    @Query("SELECT * FROM tire_pressure_records WHERE id = :id")
    suspend fun getById(id: Long): TirePressureRecord?
}
