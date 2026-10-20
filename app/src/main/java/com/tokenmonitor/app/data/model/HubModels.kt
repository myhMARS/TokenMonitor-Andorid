package com.tokenmonitor.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val ok: Boolean = false,
    val role: String = "",
    val runtime: String? = null,
    val version: Int = 0,
    val hubBuild: HubBuild? = null,
    val deviceCount: Int = 0,
    val secretRequired: Boolean = false,
    val now: String = ""
)

@Serializable
data class HubBuild(
    val schemaVersion: Int = 0,
    val runtime: String? = null,
    val coreRevision: Int = 0,
    val coreBuildId: String? = null,
    val runtimeRevision: Int = 0,
    val runtimeBuildId: String? = null
)

@Serializable
data class StatsResponse(
    val periods: Periods? = null,
    val limits: LimitsData? = null,
    val devices: List<DeviceRecord>? = null,
    val historyPreview: HistoryPreview? = null,
    val staleAfterMs: Long? = null,
    val periodWindows: PeriodWindows? = null,
    val historyRevision: String? = null,
    val deviceHistoryRevision: String? = null,
    val subscriptionsUpdatedAt: String? = null,
    val syncSettingsRevisions: Map<String, Int>? = null,
    val projectsIncomplete: Boolean = false
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
    val sessions: Map<String, SessionInfo>? = null,
    val projects: Map<String, ProjectRollup>? = null,
    val unclassifiedTokens: Long = 0,
    val unpricedTokens: Long = 0,
    val clientUnclassifiedTokens: Map<String, Long>? = null,
    val modelUnclassifiedTokens: Map<String, Long>? = null,
    val clientUnpricedTokens: Map<String, Long>? = null,
    val modelUnpricedTokens: Map<String, Long>? = null,
    val clientModelUnpricedTokens: Map<String, Map<String, Long>>? = null,
    val capabilities: PeriodCapabilities? = null
)

@Serializable
data class PeriodCapabilities(
    val tokenComponents: Boolean = false,
    val throughput: Boolean = false
)

@Serializable
data class ProjectRollup(
    val label: String = "",
    val tokens: Long = 0,
    val costUsd: Double = 0.0,
    val clients: Map<String, Long>? = null,
    val models: Map<String, Long>? = null,
    val unpricedTokens: Long = 0
)

@Serializable
data class SessionLiveActivity(
    val state: String = "unknown",
    val observedAt: String? = null
)

@Serializable
data class SessionPromptCache(
    val observedAt: String? = null,
    val ttlSeconds: Long = 0
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
    val providers: Map<String, Long>? = null,
    val projectId: String? = null,
    val projectLabel: String? = null,
    val title: String? = null,
    val contextTokens: Long = 0,
    val contextWindow: Long = 0,
    val turnEnded: Boolean? = null,
    val waitingForInput: Boolean? = null,
    val liveActivity: SessionLiveActivity? = null,
    val promptCache: SessionPromptCache? = null,
    val timedOutputTokens: Long = 0,
    val timedDurationMs: Long = 0,
    val sessionKind: String? = null,
    val parentSessionId: String? = null,
    val parentClient: String? = null,
    val nativeSessionId: String? = null,
    val t3ThreadId: String? = null,
    val usageSource: String? = null,
    val usageCoverage: String? = null,
    val unpricedTokens: Long = 0
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
    val accountName: String? = null,
    val accountLabel: String? = null,
    val planLabel: String? = null,
    val adapterId: String? = null,
    val status: String = "ok",
    val updatedAt: String? = null,
    val windows: List<LimitWindow>? = null,
    val source: String? = null,
    val actionRequired: String? = null,
    val usageSummary: UsageSummary? = null,
    val balanceUsd: Double? = null,
    val balance: BalanceInfo? = null
)

@Serializable
data class LimitWindow(
    val kind: String = "",
    val label: String? = null,
    val usedPercent: Double? = null,
    val remainingPercent: Double? = null,
    val resetsAt: String? = null,
    val boundaryKind: String? = null,
    val metric: String? = null,
    val currency: String? = null,
    val used: Double? = null,
    val limit: Double? = null,
    val remaining: Double? = null,
    val showMeter: Boolean = true,
    val detail: String? = null,
    val limitId: String? = null,
    val additional: Boolean = false,
    val source: String? = null
)

@Serializable
data class BalanceInfo(
    val amount: Double? = null,
    val currency: String? = null,
    val todaySpend: Double? = null,
    val weekSpend: Double? = null,
    val monthSpend: Double? = null,
    val allTimeSpend: Double? = null,
    val requestCount: Long? = null,
    val quotaGroup: String? = null,
    val expiresAt: String? = null,
    val trackingSince: String? = null,
    val monthSinceTracking: Boolean? = null,
    val giftBalance: Double? = null,
    val cashBalance: Double? = null,
    val planUsed: Double? = null,
    val planLimit: Double? = null,
    val planPercent: Double? = null,
    val planStatus: String? = null,
    val tranches: List<BalanceTranche>? = null
)

@Serializable
data class BalanceTranche(
    val amount: Double = 0.0,
    val currency: String? = null,
    val expiresAt: String? = null
)

@Serializable
data class UsageSummary(
    val period: String = "",
    val requests: Long? = null,
    val todayTokens: Long? = null,
    val weekTokens: Long? = null,
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
    val cacheReadTokens: Long? = null,
    val cacheCreationTokens: Long? = null,
    val totalTokens: Long? = null,
    val standardCost: Double? = null,
    val actualCost: Double? = null,
    val averageDurationMs: Double? = null
)


@Serializable
data class ModelAliasesResponse(
    val ok: Boolean = false,
    val version: Int = 0,
    val revision: Int = 0,
    val updatedAt: String = "",
    val value: ModelAliasesValue? = null
)

@Serializable
data class ModelAliasesValue(
    val modelAliases: Map<String, String>? = null,
    val modelAliasGrouping: String = "off"
)

@Serializable
data class SubscriptionsResponse(
    val ok: Boolean = false,
    val version: Int = 0,
    val updatedAt: String = "",
    val subscriptions: List<SubscriptionRecord>? = null
)

@Serializable
data class SubscriptionsWriteRequest(
    val subscriptions: List<SubscriptionRecord>,
    val baseUpdatedAt: String
)

@Serializable
data class SubscriptionRecord(
    val id: String = "",
    val provider: String = "",
    val kind: String = "subscription",
    val binding: SubscriptionBinding? = null,
    val planName: String = "",
    val amountMinor: Long = 0,
    val currency: String = "USD",
    val interval: String = "month",
    val intervalCount: Int = 1,
    val startDate: String = "",
    val topUps: List<SubscriptionTopUp>? = null,
    val autoRenew: Boolean = true,
    val nextRenewalOverride: String? = null,
    val endDate: String? = null,
    val note: String = "",
    val updatedAt: String? = null
)

@Serializable
data class SubscriptionBinding(
    val profileName: String = "",
    val accountKey: String? = null,
    val accountEmail: String? = null
)

@Serializable
data class SubscriptionTopUp(
    val id: String = "",
    val date: String = "",
    val amountMinor: Long = 0
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
    val limits: LimitsData? = null,
    val osName: String? = null,
    val osVersion: String? = null,
    val ageMs: Long? = null,
    val stale: Boolean = false,
    val clientStatus: Map<String, String>? = null,
    val clientHealth: ClientHealth? = null,
    val projectsEnabled: Boolean? = null,
    val syncUploadIntervalMs: Long? = null,
    val periodWindows: PeriodWindows? = null,
    val historyAvailable: Boolean? = null
)

@Serializable
data class ClientHealth(
    val version: Int = 0,
    val observedAt: String? = null,
    val clients: Map<String, ClientHealthEntry>? = null
)

@Serializable
data class ClientHealthEntry(
    val source: ClientHealthSource? = null,
    val collection: ClientHealthCollection? = null,
    val data: ClientHealthData? = null,
    val diagnostics: List<ClientHealthDiagnostic>? = null,
    val overall: String = "unknown"
)

@Serializable
data class ClientHealthSource(
    val state: String = "",
    val detectedCount: Int = 0,
    val checkedCount: Int = 0,
    val checks: List<ClientHealthCheck>? = null
)

@Serializable
data class ClientHealthCheck(
    val id: String = "",
    val exists: Boolean = false
)

@Serializable
data class ClientHealthCollection(
    val state: String = "",
    val syncFailureStage: String? = null,
    val syncDetailCode: String? = null,
    val lastAttemptAt: String? = null,
    val lastSuccessAt: String? = null
)

@Serializable
data class ClientHealthData(
    val liveTokens: Long = 0,
    val lastActivityDay: String? = null
)

@Serializable
data class ClientHealthDiagnostic(
    val code: String = ""
)

@Serializable
data class PeriodWindows(
    val timeZone: String? = null,
    val today: PeriodWindow? = null,
    val month: PeriodWindow? = null
)

@Serializable
data class PeriodWindow(
    val key: String = "",
    val endsAt: String? = null
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
    val intensity: Int = 0,
    val tokenIntensity: Int = 0,
    val costIntensity: Int = 0,
    val messages: Int = 0,
    val activeTimeMs: Long = 0,
    val cacheReadTokens: Long = 0,
    val cacheWriteTokens: Long = 0,
    val outputTokens: Long = 0,
    val unclassifiedTokens: Long = 0,
    val unpricedTokens: Long = 0,
    val perClient: Map<String, HistoryBucket>? = null,
    val perModel: Map<String, HistoryBucket>? = null
)

@Serializable
data class HistoryBucket(
    val tokens: Long = 0,
    val cost: Double = 0.0,
    val messages: Int = 0,
    val cacheReadTokens: Long = 0,
    val cacheWriteTokens: Long = 0,
    val outputTokens: Long = 0,
    val unclassifiedTokens: Long = 0,
    val unpricedTokens: Long = 0
)

@Serializable
data class HistoryMonth(
    val month: String = "",
    val tokens: Long = 0,
    val cost: Double = 0.0,
    val activeTimeMs: Long = 0,
    val messages: Int = 0,
    val unpricedTokens: Long = 0,
    val perClient: Map<String, HistoryBucket>? = null,
    val perModel: Map<String, HistoryBucket>? = null
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
    val messages: Int = 0,
    val activeTimeMs: Long = 0,
    val unpricedTokens: Long = 0
)

@Serializable
data class SseStatsEvent(
    val type: String = "",
    val reason: String = "",
    val stats: StatsResponse? = null,
    val at: String? = null
)
