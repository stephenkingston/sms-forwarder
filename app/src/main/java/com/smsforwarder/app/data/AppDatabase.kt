package com.smsforwarder.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class StatusConverter {
    @TypeConverter
    fun toStatus(value: String): MessageStatus = MessageStatus.valueOf(value)

    @TypeConverter
    fun fromStatus(status: MessageStatus): String = status.name
}

@Database(
    entities = [Message::class, DailyStat::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(StatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun dailyStatDao(): DailyStatDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "sms_forwarder.db"
            ).build().also { instance = it }
        }
    }
}
