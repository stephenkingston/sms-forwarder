package com.smsforwarder.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.smsforwarder.app.work.RetentionWorker
import java.util.concurrent.TimeUnit

class SmsForwarderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleRetention()
    }

    private fun scheduleRetention() {
        val request = PeriodicWorkRequestBuilder<RetentionWorker>(1, TimeUnit.DAYS)
            .addTag(RetentionWorker.TAG)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RetentionWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
