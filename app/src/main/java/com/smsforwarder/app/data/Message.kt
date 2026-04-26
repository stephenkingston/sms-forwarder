package com.smsforwarder.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageStatus {
    PENDING,
    SENT,
    FAILED_PERMANENT,
    BLOCKED_QUOTA
}

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val status: MessageStatus,
    val attempts: Int = 0,
    val lastError: String? = null,
    val lastAttemptAt: Long? = null,
    val nextRetryAt: Long? = null,
    val sentAt: Long? = null
)
