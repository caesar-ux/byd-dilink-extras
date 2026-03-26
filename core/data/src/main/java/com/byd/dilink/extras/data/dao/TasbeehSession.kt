package com.byd.dilink.extras.data.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasbeeh_sessions")
data class TasbeehSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val dhikrText: String,
    val count: Int,
    val goal: Int
)
