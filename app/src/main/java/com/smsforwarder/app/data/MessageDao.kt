package com.smsforwarder.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert
    suspend fun insert(message: Message): Long

    @Update
    suspend fun update(message: Message)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun findById(id: Long): Message?

    @Query("SELECT * FROM messages ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE status = :status ORDER BY receivedAt DESC")
    fun observeByStatus(status: MessageStatus): Flow<List<Message>>

    @Query("DELETE FROM messages WHERE receivedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int
}

@Dao
interface DailyStatDao {

    @Query("SELECT sentCount FROM daily_stats WHERE date = :date")
    suspend fun countFor(date: String): Int?

    @Query("SELECT sentCount FROM daily_stats WHERE date = :date")
    fun observeCount(date: String): Flow<Int?>

    @Query("INSERT OR REPLACE INTO daily_stats(date, sentCount) VALUES(:date, COALESCE((SELECT sentCount FROM daily_stats WHERE date = :date), 0) + 1)")
    suspend fun increment(date: String)
}
