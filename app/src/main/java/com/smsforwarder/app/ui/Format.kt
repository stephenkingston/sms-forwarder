package com.smsforwarder.app.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object Format {
    private val timeOnly = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dayAndTime = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    private val full = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())

    fun relative(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            diff < TimeUnit.DAYS.toMillis(1) -> timeOnly.format(Date(timestamp))
            else -> dayAndTime.format(Date(timestamp))
        }
    }

    fun fullDateTime(timestamp: Long): String = full.format(Date(timestamp))
}
