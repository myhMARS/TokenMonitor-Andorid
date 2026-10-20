package com.tokenmonitor.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tokenmonitor.app.R
import com.tokenmonitor.app.data.HubPreferences
import com.tokenmonitor.app.data.SubscriptionConflictException
import com.tokenmonitor.app.data.TokenMonitorRepository
import com.tokenmonitor.app.data.model.DeviceRecord
import com.tokenmonitor.app.data.model.HubBuild
import com.tokenmonitor.app.data.model.LimitProvider
import com.tokenmonitor.app.data.model.PeriodWindows
import com.tokenmonitor.app.data.model.SubscriptionRecord
import com.tokenmonitor.app.data.model.SubscriptionsWriteRequest
import com.tokenmonitor.app.data.model.SessionInfo
import com.tokenmonitor.app.data.model.SseStatsEvent
import com.tokenmonitor.app.data.model.StatsResponse
import com.tokenmonitor.app.data.model.UsagePeriod
import com.tokenmonitor.app.data.model.HistoryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val hubRole: String = "",
    val hubRuntime: String? = null,
    val hubBuild: HubBuild? = null,
    val staleAfterMs: Long? = null,
    val periodWindows: PeriodWindows? = null,
    val projectsIncomplete: Boolean = false,
    val subscriptions: List<SubscriptionRecord> = emptyList(),
    val subscriptionsUpdatedAt: String = "",
    val subscriptionsLoading: Boolean = false,
    val modelAliases: Map<String, String> = emptyMap(),
    val modelAliasGrouping: String = "off",
    val modelAliasesRevision: Int = -1,
    val isStreamConnected: Boolean = false,
    val isRefreshing: Boolean = false,
    val deletingDeviceId: String? = null,
    val snackbarMessage: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = HubPreferences(application)
    private val repository = TokenMonitorRepository()

    private var streamJob: Job? = null

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

    fun setConnectionError(message: String) {
        _uiState.value = _uiState.value.copy(connectionError = message)
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
                        hubRole = health.role,
                        hubRuntime = health.runtime,
                        hubBuild = health.hubBuild,
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
                    hubRole = health.role,
                    hubRuntime = health.runtime,
                    hubBuild = health.hubBuild,
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
        streamJob?.cancel()
        streamJob = null
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
        loadSubscriptions()
        loadModelAliases()
    }

    private suspend fun loadModelAliases() {
        repository.getModelAliases().onSuccess { response ->
            _uiState.value = _uiState.value.copy(
                modelAliases = response.value?.modelAliases.orEmpty(),
                modelAliasGrouping = response.value?.modelAliasGrouping ?: "off",
                modelAliasesRevision = response.revision
            )
        }
    }

    fun refreshSubscriptions() {
        viewModelScope.launch { loadSubscriptions() }
    }

    fun saveSubscription(record: SubscriptionRecord) {
        val current = _uiState.value.subscriptions
        val next = if (current.any { it.id == record.id }) {
            current.map { if (it.id == record.id) record else it }
        } else {
            current + record
        }
        persistSubscriptions(next, R.string.subscriptions_saved)
    }

    fun deleteSubscription(id: String) {
        persistSubscriptions(
            _uiState.value.subscriptions.filterNot { it.id == id },
            R.string.subscriptions_deleted
        )
    }

    private fun persistSubscriptions(next: List<SubscriptionRecord>, successMessage: Int) {
        viewModelScope.launch {
            val baseUpdatedAt = _uiState.value.subscriptionsUpdatedAt
            repository.putSubscriptions(SubscriptionsWriteRequest(next, baseUpdatedAt)).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        subscriptions = response.subscriptions ?: next,
                        subscriptionsUpdatedAt = response.updatedAt,
                        snackbarMessage = getApplication<Application>().getString(successMessage)
                    )
                },
                onFailure = { error ->
                    loadSubscriptions()
                    val message = if (error is SubscriptionConflictException) {
                        getApplication<Application>().getString(R.string.subscriptions_conflict)
                    } else {
                        getApplication<Application>().getString(
                            R.string.subscriptions_save_failed,
                            error.message ?: ""
                        )
                    }
                    _uiState.value = _uiState.value.copy(snackbarMessage = message)
                }
            )
        }
    }

    private suspend fun loadSubscriptions() {
        _uiState.value = _uiState.value.copy(subscriptionsLoading = true)
        repository.getSubscriptions().fold(
            onSuccess = { response ->
                _uiState.value = _uiState.value.copy(
                    subscriptions = response.subscriptions ?: emptyList(),
                    subscriptionsUpdatedAt = response.updatedAt,
                    subscriptionsLoading = false
                )
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(subscriptionsLoading = false)
            }
        )
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
            sessions = allSessions.sortedByDescending { it.lastUsedAt ?: it.startedAt ?: "" },
            staleAfterMs = stats.staleAfterMs,
            periodWindows = stats.periodWindows,
            projectsIncomplete = stats.projectsIncomplete
        )

        val remoteAliasRevision = stats.syncSettingsRevisions?.get("modelAliases")
        if (remoteAliasRevision != null && remoteAliasRevision != _uiState.value.modelAliasesRevision) {
            viewModelScope.launch { loadModelAliases() }
        }

        val remoteSubscriptions = stats.subscriptionsUpdatedAt
        val localSubscriptions = _uiState.value.subscriptionsUpdatedAt
        if (!remoteSubscriptions.isNullOrEmpty() &&
            localSubscriptions.isNotEmpty() &&
            remoteSubscriptions != localSubscriptions
        ) {
            viewModelScope.launch { loadSubscriptions() }
        }
    }

    private fun startSseStream() {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            var backoffMs = 1_000L
            while (repository.isConfigured) {
                var receivedEvent = false
                repository.statsStream().collect { event ->
                    receivedEvent = true
                    backoffMs = 1_000L
                    handleSseEvent(event)
                    if (!_uiState.value.isStreamConnected) {
                        _uiState.value = _uiState.value.copy(isStreamConnected = true)
                    }
                }
                if (!repository.isConfigured) break
                _uiState.value = _uiState.value.copy(isStreamConnected = false)
                delay(if (receivedEvent) 1_000L else backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
            }
        }
    }

    private fun handleSseEvent(event: SseStatsEvent) {
        val stats = event.stats ?: return
        if (event.type == "freshness") {
            applyFreshness(stats)
            return
        }
        extractStatsData(stats)
        if (!stats.devices.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(devices = stats.devices)
        }
    }

    private fun applyFreshness(freshness: StatsResponse) {
        val updates = freshness.devices.orEmpty().associateBy { it.deviceId }
        if (updates.isNotEmpty()) {
            val merged = _uiState.value.devices.map { device ->
                val update = updates[device.deviceId] ?: return@map device
                device.copy(
                    updatedAt = update.updatedAt ?: device.updatedAt,
                    receivedAt = update.receivedAt ?: device.receivedAt,
                    ageMs = update.ageMs ?: device.ageMs,
                    stale = update.stale
                )
            }
            _uiState.value = _uiState.value.copy(devices = merged)
        }
        freshness.staleAfterMs?.let { threshold ->
            _uiState.value = _uiState.value.copy(staleAfterMs = threshold)
        }
    }
}
