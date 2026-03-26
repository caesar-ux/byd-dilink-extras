package com.byd.dilink.extras.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Insert
    suspend fun insert(record: FuelRecord): Long

    @Update
    suspend fun update(record: FuelRecord)

    @Delete
    suspend fun delete(record: FuelRecord)

    @Query("SELECT * FROM fuel_records ORDER BY date DESC")
    fun getAllByDate(): Flow<List<FuelRecord>>

    @Query("SELECT * FROM fuel_records ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): FuelRecord?

    @Query("SELECT COALESCE(SUM(liters), 0.0) FROM fuel_records")
    suspend fun getTotalLiters(): Double

    @Query("SELECT COALESCE(SUM(totalCostIqd), 0.0) FROM fuel_records")
    suspend fun getTotalCost(): Double

    @Query("SELECT * FROM fuel_records ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<FuelRecord>>
}
