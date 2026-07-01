package com.tokenmonitor.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val ok: Boolean = false,
    val role: String = "",
    val runtime: String? = null,
    val version: Int = 0,
    val deviceCount: Int = 0,
    val secretRequired: Boolean = false,
    val now: String = ""
)

@Serializable
data class StatsResponse(
    val periods: Periods? = null,
    val limits: LimitsData? = null,
    val devices: List<DeviceRecord>? = null,
    val historyPreview: HistoryPreview? = null
)

@Serializable
data class Periods(
    val today: UsagePeriod? = null,
    val month: UsagePeriod? = null,
    val allTime: UsagePeriod? = null
)

@Serializable
data class UsagePeriod(
    val totalTokens: Long = 0,
    val costUsd: Double = 0.0,
    val cacheReadTokens: Long = 0,
    val cacheWriteTokens: Long = 0,
    val outputTokens: Long = 0,
    val clients: Map<String, Long>? = null,
    val clientCosts: Map<String, Double>? = null,
    val clientCacheReads: Map<String, Long>? = null,
    val clientCacheWrites: Map<String, Long>? = null,
    val clientOutputs: Map<String, Long>? = null,
    val models: Map<String, Long>? = null,
    val modelCosts: Map<String, Double>? = null,
    val modelCacheReads: Map<String, Long>? = null,
    val modelCacheWrites: Map<String, Long>? = null,
    val modelOutputs: Map<String, Long>? = null,
    val clientModels: Map<String, Map<String, Long>>? = null,
    val clientModelCosts: Map<String, Map<String, Double>>? = null,
    val sessions: Map<String, SessionInfo>? = null
)

@Serializable
data class SessionInfo(
    val client: String = "",
    val sessionId: String = "",
    val totalTokens: Long = 0,
    val costUsd: Double = 0.0,
    val messageCount: Int = 0,
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val cacheReadTokens: Long = 0,
    val cacheWriteTokens: Long = 0,
    val reasoningTokens: Long = 0,
    val startedAt: String? = null,
    val lastUsedAt: String? = null,
    val models: Map<String, Long>? = null,
    val modelCosts: Map<String, Double>? = null,
    val providers: Map<String, Long>? = null
)

@Serializable
data class LimitsData(
    val updatedAt: String? = null,
    val refreshMs: Long = 0,
    val providers: List<LimitProvider>? = null
)

@Serializable
data class LimitProvider(
    val provider: String = "",
    val accountKey: String? = null,
    val accountEmail: String? = null,
    val accountLabel: String? = null,
    val status: String = "ok",
    val updatedAt: String? = null,
    val windows: List<LimitWindow>? = null,
    val source: String? = null,
    val balanceUsd: Double? = null,
    val balance: BalanceInfo? = null
)

@Serializable
data class LimitWindow(
    val kind: String = "",
    val usedPercent: Double = 0.0,
    val remainingPercent: Double = 0.0,
    val resetsAt: String? = null
)

@Serializable
data class BalanceInfo(
    val amount: Double = 0.0,
    val currency: String = "USD",
    val todaySpend: Double? = null,
    val monthSpend: Double? = null,
    val monthSinceTracking: Boolean? = null
)

@Serializable
data class DevicePeriods(
    val today: UsagePeriod? = null,
    val month: UsagePeriod? = null,
    val allTime: UsagePeriod? = null
)

@Serializable
data class DeviceRecord(
    val deviceId: String = "",
    val hostname: String? = null,
    val platform: String? = null,
    val updatedAt: String? = null,
    val receivedAt: String? = null,
    val agentVersion: String? = null,
    val agentRuntime: String? = null,
    val trackedClients: List<String>? = null,
    val periods: DevicePeriods? = null,
    val today: UsagePeriod? = null,
    val month: UsagePeriod? = null,
    val allTime: UsagePeriod? = null,
    val limits: LimitsData? = null
)

@Serializable
data class DevicesResponse(
    val devices: List<DeviceRecord>? = null
)

@Serializable
data class HistoryPreview(
    val daily: List<HistoryDay>? = null,
    val monthly: List<HistoryMonth>? = null,
    val summary: HistorySummary? = null
)

@Serializable
data class HistoryResponse(
    val daily: List<HistoryDay>? = null,
    val monthly: List<HistoryMonth>? = null,
    val summary: HistorySummary? = null
)

@Serializable
data class HistoryDay(
    val date: String = "",
    val tokens: Long = 0,
    val cost: Double = 0.0,
    val intensity: Int = 0
)

@Serializable
data class HistoryMonth(
    val month: String = "",
    val tokens: Long = 0,
    val cost: Double = 0.0
)

@Serializable
data class HistorySummary(
    val totalTokens: Long = 0,
    val totalCost: Double = 0.0,
    val activeDays: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val peakDayTokens: Long = 0,
    val favoriteModel: String = "",
    val messages: Int = 0
)

@Serializable
data class SseStatsEvent(
    val type: String = "",
    val reason: String = "",
    val stats: StatsResponse? = null,
    val at: String? = null
)
