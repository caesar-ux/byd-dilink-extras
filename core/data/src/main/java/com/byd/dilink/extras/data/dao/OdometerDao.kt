package com.byd.dilink.extras.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OdometerDao {
    @Insert
    suspend fun insert(entry: OdometerEntry): Long

    @Query("SELECT * FROM odometer_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): OdometerEntry?

    @Query("SELECT * FROM odometer_entries ORDER BY date DESC")
    fun getAll(): Flow<List<OdometerEntry>>

    @Query("SELECT * FROM odometer_entries ORDER BY date ASC")
    suspend fun getAllOnce(): List<OdometerEntry>
}
