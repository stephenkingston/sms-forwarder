package com.smsforwarder.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Repository(context: Context) {

    private val db = AppDatabase.get(context)
    private val messageDao = db.messageDao()
    private val statDao = db.dailyStatDao()
    val configStore = ConfigStore(context)

    fun observeMessages(): Flow<List<Message>> = messageDao.observeAll()

    fun observeTodayCount(): Flow<Int?> = statDao.observeCount(today())

    suspend fun insertPending(sender: String, body: String, receivedAt: Long): Long {
        return messageDao.insert(
            Message(
                sender = sender,
                body = body,
                receivedAt = receivedAt,
                status = MessageStatus.PENDING
            )
        )
    }

    suspend fun findById(id: Long): Message? = messageDao.findById(id)

    suspend fun update(message: Message) = messageDao.update(message)

    suspend fun todayCount(): Int = statDao.countFor(today()) ?: 0

    suspend fun incrementToday() = statDao.increment(today())

    suspend fun pruneOlderThan(cutoff: Long): Int = messageDao.deleteOlderThan(cutoff)

    private fun today(): String =
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    companion object {
        @Volatile private var instance: Repository? = null
        fun get(context: Context): Repository = instance ?: synchronized(this) {
            instance ?: Repository(context.applicationContext).also { instance = it }
        }
    }
}
