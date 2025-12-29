package com.kiosk.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosk.app.data.net.NetworkMonitor
import com.kiosk.app.utils.KioskConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class KioskState(

    val url: String = KioskConfig.URL_Test,
    val isOnline: Boolean = true,
    val isPageLoading: Boolean = false,
    val lastError: String? = null,
    val retryCount: Int = 0
)

@HiltViewModel
class KioskViewModel @Inject constructor(
    networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _state = MutableStateFlow(KioskState())
    val state: StateFlow<KioskState> = _state.asStateFlow()

    private var retryJob: Job? = null

    private var webViewReload: (() -> Unit)? = null

    init {

        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _state.update { it.copy(isOnline = online) }
                if (online) {
                    retryJob?.cancel()
                    retryJob = null
                    _state.update { it.copy(lastError = null, retryCount = 0) }
                    webViewReload?.invoke()
                }
            }
        }
    }

    fun bindReloadAction(block: () -> Unit) {
        webViewReload = block
    }

    fun onPageStarted() {
        _state.update { it.copy(isPageLoading = true) }
    }

        fun onPageLoaded() {

        retryJob?.cancel()
        retryJob = null

        _state.update {
            it.copy(
                isPageLoading = false,
                lastError = null,
                retryCount = 0
            )
        }
    }

    fun onWebError(message: String) {
        _state.update { it.copy(lastError = message) }

        if (retryJob == null) {
            scheduleRetry()
        }
    }

    private fun scheduleRetry() {
        if (retryJob?.isActive == true) return

        retryJob = viewModelScope.launch {
            var attempt = 0

            while (attempt < 10) {
                delay(backoffMs(attempt))
                attempt++

                _state.update { it.copy(retryCount = attempt) }

                if (_state.value.isOnline && !_state.value.isPageLoading) {
                    webViewReload?.invoke()
                }
            }
        }
    }

    private fun backoffMs(attempt: Int): Long {
        val base = 2000L   // 2 секунды
        val max = 30000L  // максимум 30 секунд
        return (base * (1L shl attempt.coerceAtMost(4))).coerceAtMost(max)
    }
}
