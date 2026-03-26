package com.byd.dilink.extras.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargeDao {
    @Insert
    suspend fun insert(record: ChargeRecord): Long

    @Update
    suspend fun update(record: ChargeRecord)

    @Delete
    suspend fun delete(record: ChargeRecord)

    @Query("SELECT * FROM charge_records ORDER BY date DESC")
    fun getAllByDate(): Flow<List<ChargeRecord>>

    @Query("SELECT * FROM charge_records ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): ChargeRecord?

    @Query("SELECT COALESCE(SUM(kwhCharged), 0.0) FROM charge_records")
    suspend fun getTotalKwh(): Double

    @Query("SELECT COALESCE(SUM(totalCostIqd), 0.0) FROM charge_records")
    suspend fun getTotalCost(): Double

    @Query("SELECT * FROM charge_records ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ChargeRecord>>
}
