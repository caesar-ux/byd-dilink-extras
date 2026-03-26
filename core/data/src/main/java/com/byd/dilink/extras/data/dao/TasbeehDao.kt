package com.byd.dilink.extras.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TasbeehDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: TasbeehSession): Long

    @Query("SELECT * FROM tasbeeh_sessions WHERE date = :dayTimestamp")
    fun getSessionsForDay(dayTimestamp: Long): Flow<List<TasbeehSession>>

    @Query("SELECT SUM(count) FROM tasbeeh_sessions WHERE date = :dayTimestamp")
    fun getDailyTotal(dayTimestamp: Long): Flow<Int?>

    @Query("SELECT * FROM tasbeeh_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<TasbeehSession>>
}
