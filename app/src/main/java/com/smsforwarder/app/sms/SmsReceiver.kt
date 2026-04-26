package com.smsforwarder.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.smsforwarder.app.data.Repository
import com.smsforwarder.app.work.SendWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val grouped = messages.groupBy { it.originatingAddress ?: "Unknown" }
        val pending = goAsync()
        val app = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = Repository.get(app)
                grouped.forEach { (sender, parts) ->
                    val body = parts.sortedBy { it.timestampMillis }
                        .joinToString("") { it.messageBody ?: "" }
                    val timestamp = parts.minOf { it.timestampMillis }
                    val id = repo.insertPending(sender, body, timestamp)

                    val request = OneTimeWorkRequestBuilder<SendWorker>()
                        .setInputData(workDataOf(SendWorker.KEY_MESSAGE_ID to id))
                        .addTag(SendWorker.TAG)
                        .build()
                    WorkManager.getInstance(app).enqueue(request)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
