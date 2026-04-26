package com.smsforwarder.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStat(
    @PrimaryKey val date: String,
    val sentCount: Int
)
