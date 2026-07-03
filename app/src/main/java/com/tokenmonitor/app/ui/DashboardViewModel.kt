package com.tokenmonitor.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tokenmonitor.app.R
import com.tokenmonitor.app.data.HubPreferences
import com.tokenmonitor.app.data.TokenMonitorRepository
import com.tokenmonitor.app.data.model.DeviceRecord
import com.tokenmonitor.app.data.model.LimitProvider
import com.tokenmonitor.app.data.model.SessionInfo
import com.tokenmonitor.app.data.model.StatsResponse
import com.tokenmonitor.app.data.model.UsagePeriod
import com.tokenmonitor.app.data.model.HistoryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isConfigured: Boolean = false,
    val hubUrl: String = "",
    val hubSecret: String = "",
    val isConnecting: Boolean = true,
    val connectionError: String? = null,
    val isConnected: Boolean = false,
    val today: UsagePeriod? = null,
    val month: UsagePeriod? = null,
    val allTime: UsagePeriod? = null,
    val limits: List<LimitProvider> = emptyList(),
    val devices: List<DeviceRecord> = emptyList(),
    val sessions: List<SessionInfo> = emptyList(),
    val history: HistoryResponse? = null,
    val selectedTab: Int = 0,
    val deviceCount: Int = 0,
    val hubVersion: Int = 0,
    val hubNow: String = "",
    val isRefreshing: Boolean = false,
    val deletingDeviceId: String? = null,
    val snackbarMessage: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = HubPreferences(application)
    private val repository = TokenMonitorRepository()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedUrl = preferences.hubUrl.first()
            val savedSecret = preferences.hubSecret.first()
            if (savedUrl.isNotBlank()) {
                _uiState.value = _uiState.value.copy(
                    hubUrl = savedUrl,
                    hubSecret = savedSecret,
                    isConfigured = true
                )
                repository.configure(savedUrl, savedSecret)
                connectAndLoad()
            } else {
                _uiState.value = _uiState.value.copy(isConnecting = false)
            }
        }
    }

    fun updateHubUrl(url: String) {
        _uiState.value = _uiState.value.copy(hubUrl = url)
    }

    fun updateHubSecret(secret: String) {
        _uiState.value = _uiState.value.copy(hubSecret = secret)
    }

    fun connect() {
        val url = _uiState.value.hubUrl.trimEnd('/')
        val secret = _uiState.value.hubSecret

        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(connectionError = getApplication<Application>().getString(R.string.setup_error_url_required))
            return
        }

        _uiState.value = _uiState.value.copy(isConnecting = true, connectionError = null)

        viewModelScope.launch {
            preferences.saveHubConfig(url, secret)
            repository.configure(url, secret)

            val healthResult = repository.health()
            healthResult.fold(
                onSuccess = { health ->
                    _uiState.value = _uiState.value.copy(
                        isConfigured = true,
                        isConnecting = false,
                        isConnected = true,
                        deviceCount = health.deviceCount,
                        hubVersion = health.version,
                        hubNow = health.now,
                        isRefreshing = true
                    )
                    loadAllData()
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                    startSseStream()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        connectionError = getApplication<Application>().getString(R.string.setup_error_connection_failed, error.message ?: "")
                    )
                }
            )
        }
    }

    private suspend fun connectAndLoad() {
        _uiState.value = _uiState.value.copy(isConnecting = true, connectionError = null)
        val healthResult = repository.health()
        healthResult.fold(
            onSuccess = { health ->
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    isConnected = true,
                    deviceCount = health.deviceCount,
                    hubVersion = health.version,
                    hubNow = health.now,
                    isRefreshing = true
                )
                loadAllData()
                _uiState.value = _uiState.value.copy(isRefreshing = false)
                startSseStream()
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    connectionError = getApplication<Application>().getString(R.string.setup_error_connection_failed, error.message ?: "")
                )
            }
        )
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            loadAllData()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun disconnect() {
        repository.close()
        _uiState.value = DashboardUiState(
            isConnecting = false,
            hubUrl = _uiState.value.hubUrl,
            hubSecret = _uiState.value.hubSecret
        )
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingDeviceId = deviceId)
            val result = repository.deleteDevice(deviceId)
            result.fold(
                onSuccess = {
                    loadDevices()
                    _uiState.value = _uiState.value.copy(
                        deletingDeviceId = null,
                        snackbarMessage = getApplication<Application>().getString(R.string.device_deleted)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        deletingDeviceId = null,
                        snackbarMessage = getApplication<Application>().getString(R.string.device_delete_failed, error.message ?: "")
                    )
                }
            )
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    private suspend fun loadAllData() {
        loadStats()
        loadDevices()
        loadHistory()
    }

    private suspend fun loadStats() {
        repository.getStats().onSuccess { stats ->
            extractStatsData(stats)
        }
    }

    private suspend fun loadDevices() {
        repository.getDevices().onSuccess { response ->
            _uiState.value = _uiState.value.copy(
                devices = response.devices ?: emptyList()
            )
        }
    }

    private suspend fun loadHistory() {
        repository.getHistory().onSuccess { history ->
            _uiState.value = _uiState.value.copy(history = history)
        }
    }

    private fun extractStatsData(stats: StatsResponse) {
        val periods = stats.periods
        val allSessions = mutableListOf<SessionInfo>()

        periods?.today?.sessions?.let { allSessions.addAll(it.values) }
        periods?.month?.sessions?.let { monthSessions ->
            val existingKeys = allSessions.map { "${it.client}:${it.sessionId}" }.toSet()
            monthSessions.values.forEach { session ->
                val key = "${session.client}:${session.sessionId}"
                if (key !in existingKeys) {
                    allSessions.add(session)
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            today = periods?.today,
            month = periods?.month,
            allTime = periods?.allTime,
            limits = stats.limits?.providers ?: emptyList(),
            sessions = allSessions.sortedByDescending { it.lastUsedAt ?: it.startedAt ?: "" }
        )
    }

    private fun startSseStream() {
        viewModelScope.launch {
            repository.statsStream().collect { event ->
                event.stats?.let { stats ->
                    extractStatsData(stats)
                    if (!stats.devices.isNullOrEmpty()) {
                        _uiState.value = _uiState.value.copy(devices = stats.devices)
                    }
                }
            }
        }
    }
}
