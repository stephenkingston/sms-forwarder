package com.smsforwarder.app.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.smsforwarder.app.data.ConfigStore
import com.smsforwarder.app.data.ForwardingConfig
import com.smsforwarder.app.mail.GmailSender
import com.smsforwarder.app.mail.SendResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class TestStatus(val success: Boolean, val message: String)

data class OnboardingState(
    val senderEmail: String = "",
    val appPassword: String = "",
    val recipientEmail: String = "",
    val testing: Boolean = false,
    val testResult: TestStatus? = null,
    val permissionsGranted: Boolean = false
) {
    val canTest: Boolean
        get() = senderEmail.contains("@") && appPassword.isNotBlank() && recipientEmail.contains("@")
    val canFinish: Boolean
        get() = canTest && testResult?.success == true
}

class OnboardingViewModel(private val configStore: ConfigStore) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun onSenderChange(value: String) =
        _state.update { it.copy(senderEmail = value, testResult = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(appPassword = value, testResult = null) }

    fun onRecipientChange(value: String) =
        _state.update { it.copy(recipientEmail = value, testResult = null) }

    fun onPermissionResult(granted: Boolean) =
        _state.update { it.copy(permissionsGranted = granted) }

    suspend fun sendTest() {
        val s = _state.value
        if (!s.canTest) return
        _state.update { it.copy(testing = true, testResult = null) }
        val result = withContext(Dispatchers.IO) {
            GmailSender.sendTest(
                ForwardingConfig(
                    senderEmail = s.senderEmail.trim(),
                    appPassword = s.appPassword,
                    recipientEmail = s.recipientEmail.trim()
                )
            )
        }
        val status = when (result) {
            is SendResult.Success -> TestStatus(
                success = true,
                message = "Test email sent. Check your inbox to confirm."
            )
            is SendResult.Failure -> TestStatus(
                success = false,
                message = result.reason
            )
        }
        _state.update { it.copy(testing = false, testResult = status) }
    }

    fun saveAndFinish() {
        val s = _state.value
        configStore.save(
            ForwardingConfig(
                senderEmail = s.senderEmail.trim(),
                appPassword = s.appPassword,
                recipientEmail = s.recipientEmail.trim()
            )
        )
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val store = ConfigStore(context.applicationContext)
                    return OnboardingViewModel(store) as T
                }
            }
    }
}
