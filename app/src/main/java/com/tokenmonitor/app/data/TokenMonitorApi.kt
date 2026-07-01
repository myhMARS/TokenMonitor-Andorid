package com.tokenmonitor.app.data

import com.tokenmonitor.app.data.model.DevicesResponse
import com.tokenmonitor.app.data.model.HealthResponse
import com.tokenmonitor.app.data.model.HistoryResponse
import com.tokenmonitor.app.data.model.SseStatsEvent
import com.tokenmonitor.app.data.model.StatsResponse
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class TokenMonitorApi(
    private val baseUrl: String,
    private val secret: String
) {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun addAuth(builder: io.ktor.client.request.HttpRequestBuilder) {
        if (secret.isNotBlank()) {
            builder.header("Authorization", "Bearer $secret")
        }
    }

    suspend fun health(): Result<HealthResponse> = runCatching {
        httpClient.get("$baseUrl/api/health").let { response ->
            response.bodyAsText().let { json.decodeFromString<HealthResponse>(it) }
        }
    }

    suspend fun getStats(): Result<StatsResponse> = runCatching {
        httpClient.get("$baseUrl/api/stats") {
            addAuth(this)
        }.bodyAsText().let { json.decodeFromString<StatsResponse>(it) }
    }

    suspend fun getDevices(): Result<DevicesResponse> = runCatching {
        httpClient.get("$baseUrl/api/devices") {
            addAuth(this)
        }.bodyAsText().let { json.decodeFromString<DevicesResponse>(it) }
    }

    suspend fun getHistory(): Result<HistoryResponse> = runCatching {
        httpClient.get("$baseUrl/api/history") {
            addAuth(this)
        }.bodyAsText().let { json.decodeFromString<HistoryResponse>(it) }
    }

    suspend fun deleteDevice(deviceId: String): Result<Unit> = runCatching {
        httpClient.delete("$baseUrl/api/devices/${java.net.URLEncoder.encode(deviceId, "UTF-8")}") {
            addAuth(this)
        }
    }

    fun statsStream(): Flow<SseStatsEvent> = flow {
        try {
            httpClient.get("$baseUrl/api/stats/stream") {
                addAuth(this)
            }.let { response ->
                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    when {
                        line.startsWith("event: ") -> { /* track event type */ }
                        line.startsWith("data: ") -> {
                            val data = line.removePrefix("data: ")
                            try {
                                val event = json.decodeFromString<SseStatsEvent>(data)
                                emit(event)
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Stream ended or connection error
        }
    }

    fun close() {
        httpClient.close()
    }
}
