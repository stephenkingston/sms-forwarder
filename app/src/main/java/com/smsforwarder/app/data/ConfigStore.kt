package com.smsforwarder.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ForwardingConfig(
    val senderEmail: String,
    val appPassword: String,
    val recipientEmail: String,
    val dailyCap: Int = 500
) {
    fun isComplete(): Boolean =
        senderEmail.isNotBlank() && appPassword.isNotBlank() && recipientEmail.isNotBlank()
}

class ConfigStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "sms_forwarder_config",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _config = MutableStateFlow(load())
    val config: StateFlow<ForwardingConfig?> = _config.asStateFlow()

    private fun load(): ForwardingConfig? {
        val sender = prefs.getString(KEY_SENDER, null) ?: return null
        val pass = prefs.getString(KEY_PASS, null) ?: return null
        val recipient = prefs.getString(KEY_RECIPIENT, null) ?: return null
        val cap = prefs.getInt(KEY_CAP, 500)
        return ForwardingConfig(sender, pass, recipient, cap)
    }

    fun save(config: ForwardingConfig) {
        prefs.edit()
            .putString(KEY_SENDER, config.senderEmail.trim())
            .putString(KEY_PASS, config.appPassword)
            .putString(KEY_RECIPIENT, config.recipientEmail.trim())
            .putInt(KEY_CAP, config.dailyCap)
            .apply()
        _config.value = load()
    }

    fun clear() {
        prefs.edit().clear().apply()
        _config.value = null
    }

    companion object {
        private const val KEY_SENDER = "sender_email"
        private const val KEY_PASS = "app_password"
        private const val KEY_RECIPIENT = "recipient_email"
        private const val KEY_CAP = "daily_cap"
    }
}
