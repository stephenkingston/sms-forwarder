package com.smsforwarder.app.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smsforwarder.app.data.ForwardingConfig
import com.smsforwarder.app.data.Message
import com.smsforwarder.app.data.MessageStatus
import com.smsforwarder.app.data.Repository
import com.smsforwarder.app.work.SendWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StatusFilter { ALL, SENT, FAILED, PENDING }

data class DashboardState(
    val messages: List<Message> = emptyList(),
    val sentToday: Int = 0,
    val cap: Int = 500,
    val capReached: Boolean = false,
    val filter: StatusFilter = StatusFilter.ALL,
    val config: ForwardingConfig? = null
) {
    val filtered: List<Message>
        get() = when (filter) {
            StatusFilter.ALL -> messages
            StatusFilter.SENT -> messages.filter { it.status == MessageStatus.SENT }
            StatusFilter.FAILED -> messages.filter { it.status == MessageStatus.FAILED_PERMANENT || it.status == MessageStatus.BLOCKED_QUOTA }
            StatusFilter.PENDING -> messages.filter { it.status == MessageStatus.PENDING }
        }
}

class DashboardViewModel(private val repo: Repository, private val context: Context) : ViewModel() {

    private val filterFlow = kotlinx.coroutines.flow.MutableStateFlow(StatusFilter.ALL)

    val state: StateFlow<DashboardState> = combine(
        repo.observeMessages(),
        repo.observeTodayCount().map { it ?: 0 },
        filterFlow,
        repo.configStore.config
    ) { messages, sent, filter, config ->
        val cap = config?.dailyCap ?: 500
        DashboardState(
            messages = messages,
            sentToday = sent,
            cap = cap,
            capReached = sent >= cap,
            filter = filter,
            config = config
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    fun setFilter(filter: StatusFilter) {
        filterFlow.value = filter
    }

    fun retry(message: Message) {
        viewModelScope.launch {
            repo.update(
                message.copy(
                    status = MessageStatus.PENDING,
                    nextRetryAt = null
                )
            )
            SendWorker.enqueue(context, message.id)
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = context.applicationContext
                    return DashboardViewModel(Repository.get(app), app) as T
                }
            }
    }
}
