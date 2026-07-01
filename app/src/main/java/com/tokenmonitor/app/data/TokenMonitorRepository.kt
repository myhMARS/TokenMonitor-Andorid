package com.tokenmonitor.app.data

import com.tokenmonitor.app.data.model.DevicesResponse
import com.tokenmonitor.app.data.model.HealthResponse
import com.tokenmonitor.app.data.model.HistoryResponse
import com.tokenmonitor.app.data.model.SseStatsEvent
import com.tokenmonitor.app.data.model.StatsResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class TokenMonitorRepository {

    private var api: TokenMonitorApi? = null

    fun configure(baseUrl: String, secret: String) {
        api?.close()
        api = TokenMonitorApi(baseUrl, secret)
    }

    val isConfigured: Boolean get() = api != null

    suspend fun health(): Result<HealthResponse> {
        return api?.health() ?: Result.failure(IllegalStateException("Not configured"))
    }

    suspend fun getStats(): Result<StatsResponse> {
        return api?.getStats() ?: Result.failure(IllegalStateException("Not configured"))
    }

    suspend fun getDevices(): Result<DevicesResponse> {
        return api?.getDevices() ?: Result.failure(IllegalStateException("Not configured"))
    }

    suspend fun getHistory(): Result<HistoryResponse> {
        return api?.getHistory() ?: Result.failure(IllegalStateException("Not configured"))
    }

    suspend fun deleteDevice(deviceId: String): Result<Unit> {
        return api?.deleteDevice(deviceId) ?: Result.failure(IllegalStateException("Not configured"))
    }

    fun statsStream(): Flow<SseStatsEvent> {
        return api?.statsStream() ?: emptyFlow()
    }

    fun close() {
        api?.close()
        api = null
    }
}
