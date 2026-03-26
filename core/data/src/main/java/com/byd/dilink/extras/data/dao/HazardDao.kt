package com.byd.dilink.extras.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HazardDao {
    @Insert
    suspend fun insert(record: HazardRecord): Long

    @Delete
    suspend fun delete(record: HazardRecord)

    @Query("SELECT * FROM hazards ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HazardRecord>>

    @Query("SELECT * FROM hazards WHERE type = :type ORDER BY timestamp DESC")
    fun getByType(type: String): Flow<List<HazardRecord>>

    @Query("DELETE FROM hazards WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    @Query("SELECT COUNT(*) FROM hazards")
    fun getCount(): Flow<Int>

    @Query("SELECT * FROM hazards")
    suspend fun getAllOnce(): List<HazardRecord>

    @Insert
    suspend fun insertAll(records: List<HazardRecord>)

    @Query("DELETE FROM hazards")
    suspend fun deleteAll()

    @Update
    suspend fun update(record: HazardRecord)
}
