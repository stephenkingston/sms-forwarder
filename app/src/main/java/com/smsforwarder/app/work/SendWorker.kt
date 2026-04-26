package com.smsforwarder.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.smsforwarder.app.data.MessageStatus
import com.smsforwarder.app.data.Repository
import com.smsforwarder.app.mail.GmailSender
import com.smsforwarder.app.mail.SendResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SendWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_MESSAGE_ID, -1L)
        if (id < 0) return Result.failure()

        val repo = Repository.get(applicationContext)
        val message = repo.findById(id) ?: return Result.failure()
        if (message.status == MessageStatus.SENT ||
            message.status == MessageStatus.FAILED_PERMANENT ||
            message.status == MessageStatus.BLOCKED_QUOTA
        ) {
            return Result.success()
        }

        val config = repo.configStore.config.value
        if (config == null || !config.isComplete()) {
            repo.update(
                message.copy(
                    status = MessageStatus.FAILED_PERMANENT,
                    lastError = "App is not configured. Open the app and complete setup.",
                    lastAttemptAt = System.currentTimeMillis()
                )
            )
            return Result.success()
        }

        val sentToday = repo.todayCount()
        if (sentToday >= config.dailyCap) {
            repo.update(
                message.copy(
                    status = MessageStatus.BLOCKED_QUOTA,
                    lastError = "Daily cap of ${config.dailyCap} messages reached.",
                    lastAttemptAt = System.currentTimeMillis()
                )
            )
            return Result.success()
        }

        val attempt = message.attempts + 1
        val now = System.currentTimeMillis()

        val result = withContext(Dispatchers.IO) {
            GmailSender.send(config, message.sender, message.body, message.receivedAt)
        }

        return when (result) {
            is SendResult.Success -> {
                repo.incrementToday()
                repo.update(
                    message.copy(
                        status = MessageStatus.SENT,
                        attempts = attempt,
                        lastAttemptAt = now,
                        sentAt = now,
                        lastError = null,
                        nextRetryAt = null
                    )
                )
                Result.success()
            }
            is SendResult.Failure -> {
                if (!result.transient || attempt >= MAX_ATTEMPTS) {
                    repo.update(
                        message.copy(
                            status = MessageStatus.FAILED_PERMANENT,
                            attempts = attempt,
                            lastAttemptAt = now,
                            lastError = result.reason,
                            nextRetryAt = null
                        )
                    )
                } else {
                    val delaySec = backoffSeconds(attempt)
                    val nextAt = now + delaySec * 1000
                    repo.update(
                        message.copy(
                            status = MessageStatus.PENDING,
                            attempts = attempt,
                            lastAttemptAt = now,
                            lastError = result.reason,
                            nextRetryAt = nextAt
                        )
                    )
                    enqueue(applicationContext, message.id, initialDelaySec = delaySec)
                }
                Result.success()
            }
        }
    }

    private fun backoffSeconds(attempt: Int): Long = when (attempt) {
        1 -> 60L
        2 -> 5 * 60L
        3 -> 30 * 60L
        4 -> 2 * 60 * 60L
        else -> 6 * 60 * 60L
    }

    companion object {
        const val KEY_MESSAGE_ID = "message_id"
        const val TAG = "send_sms_email"
        private const val MAX_ATTEMPTS = 5

        fun enqueue(context: Context, messageId: Long, initialDelaySec: Long = 0L) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<SendWorker>()
                .setInputData(workDataOf(KEY_MESSAGE_ID to messageId))
                .setConstraints(constraints)
                .apply { if (initialDelaySec > 0) setInitialDelay(initialDelaySec, TimeUnit.SECONDS) }
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
