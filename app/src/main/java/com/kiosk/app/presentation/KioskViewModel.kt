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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KioskState(
    val url: String = KioskConfig.URL_Test,
    val isOnline: Boolean = false,
    val isPageLoading: Boolean = false,
    val lastError: String? = null,
    val retryCount: Int = 0
)

@HiltViewModel
class KioskViewModel @Inject constructor(
    networkMonitor: NetworkMonitor
) : ViewModel() {

    private companion object {
        const val MAX_RETRIES = 10
    }

    private val _state = MutableStateFlow(KioskState())
    val state: StateFlow<KioskState> = _state.asStateFlow()

    private var retryJob: Job? = null
    private var webViewReload: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                onConnectivityChanged(online)
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
        _state.update { it.copy(lastError = message, isPageLoading = false) }

        if (_state.value.isOnline) {
            scheduleRetry()
        }
    }

    private fun onConnectivityChanged(online: Boolean) {
        val wasOnline = _state.value.isOnline
        _state.update { it.copy(isOnline = online) }

        if (online) {
            if (wasOnline && _state.value.lastError == null) return

            retryJob?.cancel()
            retryJob = null
            _state.update { it.copy(lastError = null, retryCount = 0) }
            webViewReload?.invoke()
        } else {
            retryJob?.cancel()
            retryJob = null
            _state.update { it.copy(isPageLoading = false, retryCount = 0) }
        }
    }

    private fun scheduleRetry() {
        if (retryJob?.isActive == true) return

        retryJob = viewModelScope.launch {
            var attempt = 0

            while (attempt < MAX_RETRIES && isActive) {
                delay(backoffMs(attempt))
                attempt++

                _state.update { it.copy(retryCount = attempt) }

                if (!_state.value.isOnline) break
                if (_state.value.isPageLoading) continue

                _state.update { it.copy(lastError = null) }
                webViewReload?.invoke()
            }
        }
    }

    private fun backoffMs(attempt: Int): Long {
        val base = 2000L
        val max = 30000L
        return (base * (1L shl attempt.coerceAtMost(4))).coerceAtMost(max)
    }
}
