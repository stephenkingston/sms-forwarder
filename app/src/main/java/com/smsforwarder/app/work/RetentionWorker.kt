package com.smsforwarder.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smsforwarder.app.data.Repository
import java.util.concurrent.TimeUnit

class RetentionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RETENTION_DAYS)
        Repository.get(applicationContext).pruneOlderThan(cutoff)
        return Result.success()
    }

    companion object {
        const val TAG = "retention_prune"
        private const val RETENTION_DAYS = 7L
    }
}
