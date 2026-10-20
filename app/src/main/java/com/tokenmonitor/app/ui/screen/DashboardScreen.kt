package com.tokenmonitor.app.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ColorFilter
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.tokenmonitor.app.R
import com.tokenmonitor.app.data.model.ClientHealthEntry
import com.tokenmonitor.app.data.model.DeviceRecord
import com.tokenmonitor.app.data.model.HistoryBucket
import com.tokenmonitor.app.data.model.HistoryDay
import com.tokenmonitor.app.data.model.HubBuild
import com.tokenmonitor.app.data.model.HistoryResponse
import com.tokenmonitor.app.data.model.LimitProvider
import com.tokenmonitor.app.data.model.LimitWindow
import com.tokenmonitor.app.data.model.PeriodWindows
import com.tokenmonitor.app.data.model.UsageSummary
import com.tokenmonitor.app.data.model.SessionInfo
import com.tokenmonitor.app.data.model.SubscriptionRecord
import com.tokenmonitor.app.data.model.UsagePeriod
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    today: UsagePeriod?,
    month: UsagePeriod?,
    allTime: UsagePeriod?,
    limits: List<LimitProvider>,
    devices: List<DeviceRecord>,
    sessions: List<SessionInfo>,
    history: HistoryResponse?,
    deviceCount: Int,
    subscriptions: List<SubscriptionRecord> = emptyList(),
    subscriptionsUpdatedAt: String = "",
    subscriptionsLoading: Boolean = false,
    projectsIncomplete: Boolean = false,
    modelAliases: Map<String, String> = emptyMap(),
    modelAliasGrouping: String = "off",
    isStreamConnected: Boolean = false,
    hubVersion: Int,
    hubNow: String,
    hubRole: String = "",
    hubRuntime: String? = null,
    hubBuild: HubBuild? = null,
    staleAfterMs: Long? = null,
    periodWindows: PeriodWindows? = null,
    selectedTab: Int,
    isRefreshing: Boolean = false,
    deletingDeviceId: String? = null,
    snackbarMessage: String? = null,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
    onDeleteDevice: (String) -> Unit,
    onSaveSubscription: (SubscriptionRecord) -> Unit = {},
    onDeleteSubscription: (String) -> Unit = {},
    onRefreshSubscriptions: () -> Unit = {},
    onClearSnackbar: () -> Unit = {},
    onSwitchLanguage: (() -> Unit)? = null,
    importedApiData: String? = null,
    onImportRequest: (() -> Unit)? = null,
    onExportData: ((String) -> Unit)? = null,
    localeKey: Int = 0,
) {
    val tabResIds = remember {
        listOf(R.string.tab_overview, R.string.tab_quota, R.string.tab_subscriptions,
            R.string.tab_projects, R.string.tab_devices, R.string.tab_models,
            R.string.tab_tools, R.string.tab_sessions, R.string.tab_trends,
            R.string.tab_api_keys)
    }

    val aliasResolver = remember(modelAliases, modelAliasGrouping, allTime) {
        buildModelAliasResolver(modelAliases, allTime?.models?.keys.orEmpty(), modelAliasGrouping)
    }
    SideEffect { ModelAliasRegistry.resolver = aliasResolver }

    var keyMenuExpanded by remember { mutableStateOf(false) }
    var keyAction by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when message changes
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearSnackbar()
        }
    }

    // Pre-resolve dropdown strings so popup doesn't depend on LocalContext
    val menuRefresh = stringResource(R.string.menu_refresh)
    val menuLanguage = stringResource(R.string.menu_language)
    val menuAddKey = stringResource(R.string.apikeys_add_btn)
    val menuImport = stringResource(R.string.apikeys_import_btn)
    val menuExport = stringResource(R.string.apikeys_export_desc)
    val menuDisconnect = stringResource(R.string.action_disconnect)

    var currentPage by remember { mutableIntStateOf(selectedTab) }
    val pagerState = rememberPagerState(
        initialPage = currentPage,
        pageCount = { tabResIds.size }
    )

    // Sync pager → currentPage on swipe settle
    LaunchedEffect(pagerState.currentPage) {
        currentPage = pagerState.currentPage
    }
    // Sync tab click → pager
    LaunchedEffect(currentPage) {
        if (currentPage != pagerState.currentPage) {
            pagerState.scrollToPage(currentPage)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.title_token_monitor))
                        if (deviceCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.devices_count, deviceCount),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                },
                actions = {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = stringResource(
                            if (isStreamConnected) R.string.stream_live else R.string.stream_reconnecting
                        ),
                        tint = if (isStreamConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box {
                        IconButton(onClick = { keyMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_more))
                        }
                        DropdownMenu(expanded = keyMenuExpanded, onDismissRequest = { keyMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(menuRefresh) },
                                onClick = { keyMenuExpanded = false; onRefresh() },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.height(40.dp)
                            )
                            onSwitchLanguage?.let { onLang ->
                                DropdownMenuItem(
                                    text = { Text(menuLanguage) },
                                    onClick = { keyMenuExpanded = false; onLang() },
                                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.height(40.dp)
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            DropdownMenuItem(
                                text = { Text(menuAddKey) },
                                onClick = { keyMenuExpanded = false; currentPage = 7; keyAction = "add" },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.height(40.dp)
                            )
                            DropdownMenuItem(
                                text = { Text(menuImport) },
                                onClick = { keyMenuExpanded = false; currentPage = 7; onImportRequest?.invoke() },
                                leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.height(40.dp)
                            )
                            DropdownMenuItem(
                                text = { Text(menuExport) },
                                onClick = { keyMenuExpanded = false; currentPage = 7; keyAction = "export" },
                                leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.height(40.dp)
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                            DropdownMenuItem(
                                text = { Text(menuDisconnect, color = MaterialTheme.colorScheme.error) },
                                onClick = { keyMenuExpanded = false; onDisconnect() },
                                leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.height(40.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)) {
            androidx.compose.animation.AnimatedVisibility(visible = isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            PrimaryScrollableTabRow(
                selectedTabIndex = currentPage,
                edgePadding = 8.dp
            ) {
                tabResIds.forEachIndexed { index, resId ->
                    Tab(
                        selected = currentPage == index,
                        onClick = { currentPage = index },
                        text = { Text(stringResource(resId), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> OverviewTab(today, month, allTime, hubVersion, hubNow, hubRuntime, hubBuild, staleAfterMs, periodWindows)
                    1 -> QuotaTab(limits, today)
                    2 -> SubscriptionsTab(
                        subscriptions = subscriptions,
                        updatedAt = subscriptionsUpdatedAt,
                        isLoading = subscriptionsLoading,
                        onSave = onSaveSubscription,
                        onDelete = onDeleteSubscription,
                        onRefresh = onRefreshSubscriptions
                    )
                    3 -> ProjectsTab(today, month, allTime, projectsIncomplete)
                    4 -> DevicesTab(devices, deletingDeviceId, onDeleteDevice)
                    5 -> ModelsTab(allTime)
                    6 -> ToolsTab(allTime)
                    7 -> SessionsTab(sessions)
                    8 -> TrendsTab(history)
                    9 -> ApiKeysTab(keyAction = keyAction, onKeyActionHandled = { keyAction = null }, importedData = importedApiData, onExportData = onExportData)
                }
            }
        }
    }
}

@Composable
fun OverviewTab(
    today: UsagePeriod?,
    month: UsagePeriod?,
    allTime: UsagePeriod?,
    hubVersion: Int,
    hubNow: String,
    hubRuntime: String? = null,
    hubBuild: HubBuild? = null,
    staleAfterMs: Long? = null,
    periodWindows: PeriodWindows? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PeriodCard(stringResource(R.string.period_today), today, MaterialTheme.colorScheme.primary) }
        item { PeriodCard(stringResource(R.string.period_month), month, MaterialTheme.colorScheme.tertiary) }
        item { PeriodCard(stringResource(R.string.period_all_time), allTime, MaterialTheme.colorScheme.secondary) }

        if (hubVersion > 0 || hubNow.isNotBlank() || hubBuild != null) {
            item {
                HubStatusCard(
                    hubVersion = hubVersion,
                    hubNow = hubNow,
                    hubRuntime = hubRuntime,
                    hubBuild = hubBuild,
                    staleAfterMs = staleAfterMs,
                    periodWindows = periodWindows
                )
            }
        }

        if (today != null && !today.models.isNullOrEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.overview_model_usage), style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        val groupedModels = ModelAliasRegistry.groupModels(today.models)
                        val groupedCosts = ModelAliasRegistry.groupCosts(today.models, today.modelCosts)
                        groupedModels.entries.sortedByDescending { it.value }.take(10).forEach { (model, tokens) ->
                            ModelUsageRow(model, tokens, groupedCosts[model])
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubStatusCard(
    hubVersion: Int,
    hubNow: String,
    hubRuntime: String?,
    hubBuild: HubBuild?,
    staleAfterMs: Long?,
    periodWindows: PeriodWindows?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    hubRuntimeLabel(hubRuntime, hubVersion),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                if (hubNow.isNotBlank()) {
                    Text(
                        formatDateTime(hubNow),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (hubBuild != null) {
                val core = shortBuildId(hubBuild.coreBuildId)
                val runtime = shortBuildId(hubBuild.runtimeBuildId)
                if (core != null || runtime != null) {
                    Text(
                        stringResource(R.string.overview_build_format, core ?: "-", runtime ?: "-"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            periodWindows?.today?.endsAt?.takeIf { it.isNotBlank() }?.let { endsAt ->
                val zone = periodWindows.timeZone?.takeIf { it.isNotBlank() }
                Text(
                    if (zone != null) stringResource(R.string.overview_today_ends_tz_format, formatDateTime(endsAt), zone)
                    else stringResource(R.string.overview_today_ends_format, formatDateTime(endsAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            staleAfterMs?.takeIf { it > 0 }?.let { threshold ->
                Text(
                    stringResource(R.string.overview_stale_after_format, threshold / 60000),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun hubRuntimeLabel(runtime: String?, version: Int): String = when (runtime?.lowercase()) {
    "node-hub" -> stringResource(R.string.overview_runtime_node)
    "cloudflare-worker" -> stringResource(R.string.overview_runtime_worker)
    null, "" -> if (version > 0) stringResource(R.string.overview_hub_version, version) else stringResource(R.string.overview_runtime_unknown)
    else -> runtime
}

private fun shortBuildId(buildId: String?): String? {
    val value = buildId?.removePrefix("sha256:")?.takeIf { it.isNotBlank() } ?: return null
    return value.take(8)
}

private fun formatDateTime(isoTime: String): String = try {
    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(java.time.ZoneId.systemDefault())
        .format(Instant.parse(isoTime))
} catch (_: Exception) {
    isoTime.take(16).replace("T", " ")
}

@Composable
fun PeriodCard(title: String, period: UsagePeriod?, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = color)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(stringResource(R.string.stat_tokens), formatNumber(period?.totalTokens ?: 0), Modifier.weight(1f))
                    StatItem(stringResource(R.string.stat_cost), formatCost(period?.costUsd ?: 0.0), Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(stringResource(R.string.stat_output), formatNumber(period?.outputTokens ?: 0), Modifier.weight(1f))
                    StatItem(stringResource(R.string.stat_cache), formatNumber(period?.cacheReadTokens ?: 0), Modifier.weight(1f))
                    StatItem(stringResource(R.string.stat_cache_write), formatNumber(period?.cacheWriteTokens ?: 0), Modifier.weight(1f))
                }
            }
            val unpriced = period?.unpricedTokens ?: 0
            val unclassified = period?.unclassifiedTokens ?: 0
            if (unpriced > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.overview_unpriced_hint, formatNumber(unpriced)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (unclassified > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    stringResource(R.string.overview_unclassified_hint, formatNumber(unclassified)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun ModelUsageRow(model: String, tokens: Long, cost: Double?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(model, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row {
            Text(formatNumber(tokens), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            if (cost != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(formatCost(cost), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
fun QuotaTab(limits: List<LimitProvider>, today: UsagePeriod?) {
    // Hubs report every registered provider, including ones this device has not
    // configured. Those rows carry no window and no balance, so rendering them
    // would bury the one account that actually has data under dozens of empty
    // cards. Errors and rate limits stay visible because they are diagnostics.
    val visibleLimits = limits.filter { provider ->
        !provider.windows.isNullOrEmpty() ||
            provider.balance != null ||
            provider.balanceUsd != null ||
            (provider.status != "notConfigured" && provider.status != "disabled")
    }
    val hasData = visibleLimits.isNotEmpty() || today != null

    if (!hasData) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.quota_no_data), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (today != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.quota_today_consumption), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.stat_tokens), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text(formatNumber(today.totalTokens), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.stat_cost), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text(formatCost(today.costUsd), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }

        items(visibleLimits) { provider ->
            QuotaProviderCard(provider)
        }
    }
}

@Composable
private fun ProgressBar(ratio: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val trackColor = Color.LightGray.copy(alpha = 0.3f)
        // Track background
        drawRoundRect(
            color = trackColor,
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Filled portion
        if (ratio > 0f) {
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(4f, 4f),
                size = Size(size.width * ratio, size.height)
            )
        }
    }
}

@Composable
fun QuotaProviderCard(provider: LimitProvider) {
    val windows = provider.windows.orEmpty()
    val primaryWindows = windows.filter { !it.additional }
    val additionalWindows = windows.filter { it.additional }
    val balance = provider.balance
    val hasBalance = provider.balanceUsd != null || balance != null

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = statusColor(provider.status)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        providerLabel(provider.provider),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val account = provider.accountName?.takeIf { it.isNotBlank() }
                        ?: provider.accountEmail?.takeIf { it.isNotBlank() }
                    if (account != null) {
                        Text(
                            account,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                val plan = provider.planLabel?.takeIf { it.isNotBlank() }
                    ?: provider.accountLabel?.takeIf { it.isNotBlank() }
                if (plan != null) PlanBadge(plan)
            }

            providerStatusLabel(provider.status)?.let { status ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(status, style = MaterialTheme.typography.labelSmall, color = statusColor(provider.status))
            }

            when (provider.actionRequired) {
                "accountVerification" -> QuotaHint(stringResource(R.string.quota_action_verification))
                "appSessionEncrypted" -> QuotaHint(stringResource(R.string.quota_action_encrypted))
            }

            if (hasBalance) BalanceSection(provider)

            if (primaryWindows.isNotEmpty()) {
                if (hasBalance) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.quota_rate_limits),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                primaryWindows.forEach { LimitWindowRow(it) }
            }

            if (additionalWindows.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.quota_additional_limits),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                additionalWindows.forEach { LimitWindowRow(it) }
            }

            provider.usageSummary?.let { UsageSummarySection(it) }

            provider.updatedAt?.takeIf { it.isNotBlank() }?.let { updated ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.quota_updated_format, formatTime(updated, LocalContext.current)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlanBadge(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun QuotaHint(text: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LimitWindowRow(window: LimitWindow) {
    val currency = window.currency
    val label = window.label?.takeIf { it.isNotBlank() } ?: windowKindLabel(window.kind)
    val usedPercent = window.usedPercent
    val remainingPercent = window.remainingPercent
    val consumedRatio = when {
        usedPercent != null -> (usedPercent / 100.0).coerceIn(0.0, 1.0)
        window.used != null && window.limit != null && window.limit > 0 ->
            (window.used / window.limit).coerceIn(0.0, 1.0)
        else -> null
    }
    val percentText = when {
        usedPercent != null && remainingPercent != null ->
            stringResource(R.string.quota_used_left_format, usedPercent, remainingPercent)
        usedPercent != null -> stringResource(R.string.quota_used_format, usedPercent)
        remainingPercent != null -> stringResource(R.string.quota_left_format, remainingPercent)
        else -> null
    }
    val amountText = when {
        window.used != null && window.limit != null ->
            stringResource(R.string.quota_used_of_limit_format, formatMoney(window.used, currency), formatMoney(window.limit, currency))
        window.remaining != null -> stringResource(R.string.quota_remaining_format, formatMoney(window.remaining, currency))
        window.used != null -> stringResource(R.string.quota_used_amount_format, formatMoney(window.used, currency))
        else -> null
    }

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                window.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                    Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                percentText?.let { text ->
                    Text(
                        text,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = consumedRatio?.let { ratioColor(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                amountText?.let { text ->
                    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (window.showMeter && consumedRatio != null) {
            Spacer(modifier = Modifier.height(6.dp))
            ProgressBar(
                ratio = consumedRatio.toFloat(),
                color = ratioColor(consumedRatio),
                modifier = Modifier.fillMaxWidth().height(12.dp)
            )
        }
        window.resetsAt?.takeIf { it.isNotBlank() }?.let { resetTime ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                boundaryText(window.boundaryKind, formatTime(resetTime, LocalContext.current)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BalanceSection(provider: LimitProvider) {
    val balance = provider.balance

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        stringResource(R.string.quota_balance),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    if (balance != null) {
        val currency = balance.currency
        balance.amount?.let { amount ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currencyLabel = currency?.takeIf { it.isNotBlank() }
                Text(
                    if (currencyLabel != null) stringResource(R.string.quota_balance_format, currencyLabel)
                    else stringResource(R.string.quota_balance),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    formatMoney(amount, currency),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (amount > 0) Color(0xFF4CAF50) else Color(0xFFEF5350)
                )
            }
            val monthSpend = balance.monthSpend
            if (monthSpend != null && monthSpend > 0 && amount >= 0) {
                // Display-only wallet meter: balance / (balance + month spend).
                val ratio = (amount / (amount + monthSpend)).coerceIn(0.0, 1.0)
                Spacer(modifier = Modifier.height(4.dp))
                ProgressBar(
                    ratio = ratio.toFloat(),
                    color = ratioColor(1.0 - ratio),
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
            }
        }

        if (balance.planStatus == "expired") {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(R.string.quota_plan_expired),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF5350)
            )
        }

        if (balance.planUsed != null && balance.planLimit != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(
                    R.string.quota_plan_usage_format,
                    formatMoney(balance.planUsed, currency),
                    formatMoney(balance.planLimit, currency)
                ),
                style = MaterialTheme.typography.bodySmall
            )
            balance.planPercent?.let { percent ->
                Spacer(modifier = Modifier.height(4.dp))
                val ratio = (percent / 100.0).coerceIn(0.0, 1.0)
                ProgressBar(
                    ratio = ratio.toFloat(),
                    color = ratioColor(ratio),
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
            }
        }

        balance.todaySpend?.let { SpendRow(stringResource(R.string.quota_today_spend), it, currency) }
        balance.weekSpend?.let { SpendRow(stringResource(R.string.quota_week_spend), it, currency) }
        balance.monthSpend?.let {
            val label = stringResource(R.string.quota_month_spend) +
                if (balance.monthSinceTracking == true) "" else "*"
            SpendRow(label, it, currency)
        }
        balance.allTimeSpend?.let { SpendRow(stringResource(R.string.quota_all_time_spend), it, currency) }
        balance.giftBalance?.let { SpendRow(stringResource(R.string.quota_gift_balance), it, currency) }
        balance.cashBalance?.let { SpendRow(stringResource(R.string.quota_cash_balance), it, currency) }
        balance.requestCount?.let { count ->
            Text(
                stringResource(R.string.quota_requests_format, count),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        balance.expiresAt?.takeIf { it.isNotBlank() }?.let { expires ->
            Text(
                stringResource(R.string.quota_expires_on_format, formatTime(expires, LocalContext.current)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        balance.tranches?.forEach { tranche ->
            val trancheCurrency = tranche.currency ?: currency
            val expiresText = tranche.expiresAt?.takeIf { it.isNotBlank() }?.let {
                stringResource(R.string.quota_expires_on_format, formatTime(it, LocalContext.current))
            }
            val text = buildString {
                append("+ ")
                append(formatMoney(tranche.amount, trancheCurrency))
                if (expiresText != null) {
                    append(" · ")
                    append(expiresText)
                }
            }
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    provider.balanceUsd?.let { usd ->
        SpendRow(stringResource(R.string.quota_prepaid_usd), usd, "USD")
    }
}

@Composable
private fun SpendRow(label: String, value: Double, currency: String?) {
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(formatMoney(value, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun UsageSummarySection(summary: UsageSummary) {
    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        stringResource(R.string.quota_account_usage),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    val periodLabel = when (summary.period) {
        "today" -> stringResource(R.string.period_today)
        "week" -> stringResource(R.string.period_week)
        "month" -> stringResource(R.string.period_month)
        "allTime" -> stringResource(R.string.period_all_time)
        else -> null
    }
    if (periodLabel != null) {
        Text(periodLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    summary.requests?.let { SummaryRow(stringResource(R.string.quota_requests_label), formatNumber(it)) }
    summary.totalTokens?.let { SummaryRow(stringResource(R.string.stat_tokens), formatNumber(it)) }
    summary.inputTokens?.let { SummaryRow(stringResource(R.string.session_input), formatNumber(it)) }
    summary.outputTokens?.let { SummaryRow(stringResource(R.string.session_output), formatNumber(it)) }
    summary.cacheReadTokens?.let { SummaryRow(stringResource(R.string.session_cache_read), formatNumber(it)) }
    summary.cacheCreationTokens?.let { SummaryRow(stringResource(R.string.session_cache_write), formatNumber(it)) }
    summary.standardCost?.let { SummaryRow(stringResource(R.string.quota_standard_cost), formatCost(it)) }
    summary.actualCost?.let { SummaryRow(stringResource(R.string.quota_actual_cost), formatCost(it)) }
    summary.averageDurationMs?.let {
        SummaryRow(stringResource(R.string.quota_avg_duration), stringResource(R.string.quota_ms_format, it))
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun boundaryText(boundaryKind: String?, time: String): String = when (boundaryKind) {
    "expiry" -> stringResource(R.string.quota_expires_format, time)
    "mixed" -> stringResource(R.string.quota_resets_expires_format, time)
    else -> stringResource(R.string.quota_resets_format, time)
}

@Composable
private fun windowKindLabel(kind: String): String = when (kind) {
    "session" -> stringResource(R.string.quota_window_session)
    "daily" -> stringResource(R.string.quota_window_daily)
    "weekly" -> stringResource(R.string.quota_window_weekly)
    "billing" -> stringResource(R.string.quota_window_billing)
    "credits" -> stringResource(R.string.quota_window_credits)
    else -> kind.replaceFirstChar { it.uppercase() }
}

@Composable
private fun providerStatusLabel(status: String): String? = when (status) {
    "", "ok" -> null
    "rateLimited" -> stringResource(R.string.quota_status_rate_limited)
    "sourceRateLimited" -> stringResource(R.string.quota_status_source_rate_limited)
    "unauthorized" -> stringResource(R.string.quota_status_unauthorized)
    "unavailable" -> stringResource(R.string.quota_status_unavailable)
    "error" -> stringResource(R.string.quota_status_error)
    "notConfigured" -> stringResource(R.string.quota_status_not_configured)
    "disabled" -> stringResource(R.string.quota_status_disabled)
    else -> status.replaceFirstChar { it.uppercase() }
}

private fun statusColor(status: String): Color = when (status) {
    "warning", "rateLimited", "sourceRateLimited" -> Color(0xFFFFA726)
    "error", "unauthorized", "unavailable" -> Color(0xFFEF5350)
    "notConfigured", "disabled" -> Color(0xFF9E9E9E)
    else -> Color(0xFF66BB6A)
}

private fun ratioColor(ratio: Double): Color = when {
    ratio >= 0.8 -> Color(0xFFEF5350)
    ratio >= 0.5 -> Color(0xFFFFA726)
    else -> Color(0xFF66BB6A)
}

internal fun providerLabel(id: String): String = PROVIDER_LABELS[id] ?: id.replaceFirstChar { it.uppercase() }

private val PROVIDER_LABELS = mapOf(
    "claude" to "Claude",
    "codex" to "Codex",
    "opencode" to "OpenCode",
    "cursor" to "Cursor",
    "antigravity" to "Antigravity",
    "cline" to "Cline",
    "factory" to "Factory Droid",
    "kimi" to "Kimi",
    "grok" to "Grok",
    "copilot" to "GitHub Copilot",
    "zed" to "Zed",
    "commandcode" to "Command Code",
    "mimo" to "Xiaomi MiMo",
    "zai" to "GLM",
    "zaiteam" to "GLM Team",
    "kiro" to "Kiro",
    "workbuddy" to "WorkBuddy",
    "qoder" to "Qoder",
    "deepseek" to "DeepSeek",
    "devin" to "Devin",
    "minimax" to "Minimax",
    "typesafe" to "TypeSafe",
    "openrouter" to "OpenRouter",
    "volcengine" to "Volcengine",
    "ollama" to "Ollama",
    "trae" to "Trae CN",
    "alibaba" to "Alibaba Cloud",
    "stepfun" to "StepFun",
    "notion" to "Notion AI",
    "thirdparty" to "Third-party APIs"
)

private fun formatMoney(value: Double, currency: String?): String {
    val code = currency?.trim()?.uppercase(Locale.US).orEmpty()
    return when {
        code.isEmpty() || code == "USD" -> formatCost(value)
        code == "CREDITS" -> "${formatAmount(value)} Credits"
        else -> "$code ${formatAmount(value)}"
    }
}

private fun formatAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format(Locale.US, "%.2f", value)

@Composable
fun DevicesTab(devices: List<DeviceRecord>, deletingDeviceId: String?, onDeleteDevice: (String) -> Unit) {
    if (devices.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.devices_no_devices), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(devices) { device ->
            DeviceCard(device, isDeleting = deletingDeviceId == device.deviceId, onDeleteDevice)
        }
    }
}

@Composable
fun DeviceCard(device: DeviceRecord, isDeleting: Boolean = false, onDeleteDevice: (String) -> Unit) {
    var showDetail by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val name = deviceDisplayName(device)
    val osLabel = deviceOsLabel(device)
    val secondary = listOfNotNull(
        device.hostname?.takeIf { it.isNotBlank() && it != name },
        osLabel.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
    val allTime = device.allTime ?: device.periods?.allTime

    Card(modifier = Modifier.fillMaxWidth().clickable { showDetail = true }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Computer,
                contentDescription = null,
                tint = if (device.stale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (device.stale) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StaleBadge()
                    }
                }
                if (secondary.isNotBlank()) {
                    Text(
                        secondary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                allTime?.let {
                    Text(
                        stringResource(R.string.devices_total_format, formatNumber(it.totalTokens), formatCost(it.costUsd)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                device.updatedAt?.let { updated ->
                    Text(
                        stringResource(R.string.devices_last_seen_format, formatTime(updated, LocalContext.current)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (device.stale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isDeleting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete_device), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDetail) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            title = { Text(name) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionTitle(stringResource(R.string.session_stats))
                    SummaryRow(stringResource(R.string.period_today), devicePeriodSummary(device.today ?: device.periods?.today))
                    SummaryRow(stringResource(R.string.period_month), devicePeriodSummary(device.month ?: device.periods?.month))
                    SummaryRow(stringResource(R.string.period_all_time), devicePeriodSummary(allTime))

                    val infoRows = buildList<Pair<String, String>> {
                        device.hostname?.takeIf { it.isNotBlank() && it != name }?.let {
                            add(stringResource(R.string.devices_host_label) to it)
                        }
                        osLabel.takeIf { it.isNotBlank() }?.let {
                            add(stringResource(R.string.devices_os_label) to it)
                        }
                        device.platform?.takeIf { it.isNotBlank() && it != osLabel }?.let {
                            add(stringResource(R.string.devices_platform_label) to it)
                        }
                        device.agentVersion?.takeIf { it.isNotBlank() }?.let { version ->
                            val runtime = device.agentRuntime?.takeIf { it.isNotBlank() }
                            add(stringResource(R.string.devices_agent_label) to listOfNotNull(version, runtime).joinToString(" · "))
                        }
                        device.syncUploadIntervalMs?.takeIf { it > 0 }?.let {
                            add(stringResource(R.string.devices_upload_interval_label) to stringResource(R.string.devices_upload_interval_format, it / 60000))
                        }
                        if (device.stale) add(stringResource(R.string.devices_status_label) to stringResource(R.string.devices_stale))
                    }
                    if (infoRows.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        SectionTitle(stringResource(R.string.devices_info_title))
                        infoRows.forEach { (label, value) -> SummaryRow(label, value) }
                    }

                    val clients = allTime?.clients
                    if (!clients.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        SectionTitle(stringResource(R.string.devices_top_tools))
                        clients.entries.sortedByDescending { it.value }.take(5).forEach { (client, tokens) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ToolAvatar(client, toolLabel(client), toolColor(client), 18.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    toolLabel(client),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(formatNumber(tokens), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    val models = allTime?.models
                    if (!models.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        SectionTitle(stringResource(R.string.devices_top_models))
                        models.entries.sortedByDescending { it.value }.take(5).forEach { (model, tokens) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    model,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(formatNumber(tokens), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    DeviceHealthSection(device)
                }
            },
            confirmButton = { TextButton(onClick = { showDetail = false }) { Text(stringResource(R.string.session_close)) } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.device_delete_title)) },
            text = { Text(stringResource(R.string.device_delete_message, name)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteDevice(device.deviceId)
                }) { Text(stringResource(R.string.device_delete_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.device_delete_cancel)) }
            }
        )
    }
}

@Composable
private fun StaleBadge() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(
            stringResource(R.string.devices_stale),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun DeviceHealthSection(device: DeviceRecord) {
    val health = device.clientHealth ?: return
    val entries = health.clients.orEmpty()
    if (entries.isEmpty()) return
    // Only attention/waiting rows are expanded. A device that tracks 35 tools
    // reports "source-missing" for most of them; listing every one turns a
    // diagnostic into a wall of noise. The counts still account for all of them.
    val problems = entries.filter {
        it.value.overall == "attention" || it.value.overall == "waiting"
    }.entries.sortedWith(compareBy({ healthRank(it.value.overall) }, { it.key }))
    val healthyCount = entries.count { it.value.overall == "healthy" }
    val unavailableCount = entries.count { it.value.overall == "unavailable" }
    val unknownCount = entries.count { it.value.overall == "unknown" }

    Spacer(modifier = Modifier.height(4.dp))
    SectionTitle(stringResource(R.string.devices_health_title))
    if (problems.isEmpty() && unavailableCount == 0 && unknownCount == 0) {
        Text(
            stringResource(R.string.health_all_healthy),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4CAF50)
        )
    } else {
        problems.take(6).forEach { (client, entry) -> HealthClientRow(client, entry) }
        if (problems.size > 6) {
            Text(
                stringResource(R.string.health_more_format, problems.size - 6),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val summary = buildList {
            if (healthyCount > 0) add(stringResource(R.string.health_healthy_count_format, healthyCount))
            if (unavailableCount > 0) add(stringResource(R.string.health_unavailable_count_format, unavailableCount))
            if (unknownCount > 0) add(stringResource(R.string.health_unknown_count_format, unknownCount))
        }
        if (summary.isNotEmpty()) {
            Text(
                summary.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    health.observedAt?.takeIf { it.isNotBlank() }?.let { observed ->
        Text(
            stringResource(R.string.quota_updated_format, formatTime(observed, LocalContext.current)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HealthClientRow(client: String, entry: ClientHealthEntry) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolAvatar(client, toolLabel(client), toolColor(client), 16.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                toolLabel(client),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                healthStateLabel(entry.overall),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = healthColor(entry.overall)
            )
        }
        entry.source?.let { source ->
            if (source.checkedCount > 0) {
                Text(
                    stringResource(R.string.health_source_format, source.detectedCount, source.checkedCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        entry.collection?.let { collection ->
            when (collection.state) {
                "failed" -> {
                    Text(
                        stringResource(R.string.health_collection_failed_format, collection.syncFailureStage ?: "unknown"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    collection.lastSuccessAt?.takeIf { it.isNotBlank() }?.let { success ->
                        Text(
                            stringResource(R.string.health_last_success_format, formatTime(success, LocalContext.current)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                "pending", "idle" -> Text(
                    stringResource(R.string.health_collection_state_format, collection.state),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        entry.data?.let { data ->
            val parts = mutableListOf<String>()
            if (data.liveTokens > 0) {
                parts += stringResource(R.string.health_live_tokens_format, formatNumber(data.liveTokens))
            }
            data.lastActivityDay?.takeIf { it.isNotBlank() }?.let {
                parts += stringResource(R.string.health_last_activity_format, it)
            }
            if (parts.isNotEmpty()) {
                Text(
                    parts.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        entry.diagnostics?.takeIf { it.isNotEmpty() }?.let { diagnostics ->
            Text(
                stringResource(R.string.health_diagnostics_format, diagnostics.joinToString(", ") { it.code }),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun healthStateLabel(state: String): String = when (state) {
    "healthy" -> stringResource(R.string.health_state_healthy)
    "waiting" -> stringResource(R.string.health_state_waiting)
    "attention" -> stringResource(R.string.health_state_attention)
    "unavailable" -> stringResource(R.string.health_state_unavailable)
    else -> stringResource(R.string.health_state_unknown)
}

private fun healthColor(state: String): Color = when (state) {
    "healthy" -> Color(0xFF4CAF50)
    "waiting" -> Color(0xFFFFA726)
    "attention" -> Color(0xFFEF5350)
    "unavailable" -> Color(0xFF9E9E9E)
    else -> Color(0xFF9E9E9E)
}

private fun healthRank(state: String): Int = when (state) {
    "attention" -> 0
    "unavailable" -> 1
    "waiting" -> 2
    "unknown" -> 3
    else -> 4
}

private fun deviceDisplayName(device: DeviceRecord): String =
    device.deviceId.takeIf { it.isNotBlank() }
        ?: device.hostname?.takeIf { it.isNotBlank() }
        ?: "device"

private fun deviceOsLabel(device: DeviceRecord): String {
    val os = listOfNotNull(
        device.osName?.takeIf { it.isNotBlank() },
        device.osVersion?.takeIf { it.isNotBlank() }
    ).joinToString(" ")
    return os.ifBlank { device.platform.orEmpty() }
}

@Composable
private fun devicePeriodSummary(period: UsagePeriod?): String =
    if (period == null) stringResource(R.string.session_na)
    else "${formatNumber(period.totalTokens)} · ${formatCost(period.costUsd)}"

@Composable
fun ModelsTab(allTime: UsagePeriod?) {
    // Use allTime only — periods are nested (allTime includes month, month includes today),
    // so merging them would double/triple-count token values.
    val rawModels = allTime?.models ?: emptyMap()
    val models = ModelAliasRegistry.groupModels(rawModels)
    val modelCosts = ModelAliasRegistry.groupCosts(rawModels, allTime?.modelCosts)
    val modelOutputs = ModelAliasRegistry.groupLongs(rawModels, allTime?.modelOutputs)
    val modelCacheReads = ModelAliasRegistry.groupLongs(rawModels, allTime?.modelCacheReads)

    if (models.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ModelTraining, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.models_no_data), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val totalTokens = models.values.sum().toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(models.entries.sortedByDescending { it.value }.toList()) { (model, tokens) ->
        val vendor = modelVendorFor(model)
        val vendorLabel = if (vendor != null) toolLabel(vendor) else model
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        ToolAvatar(tool = vendor ?: model, label = vendorLabel, color = toolColor(vendor ?: model), size = 28.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(model, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(formatNumber(tokens), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        modelCosts[model]?.let { cost ->
                            Text(formatCost(cost), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                        modelOutputs[model]?.let { output ->
                            Text(stringResource(R.string.models_output_format, formatNumber(output)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        modelCacheReads[model]?.let { cache ->
                            Text(stringResource(R.string.models_cache_format, formatNumber(cache)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ProgressBar(
                        ratio = tokens / totalTokens,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ToolsTab(allTime: UsagePeriod?) {
    // Use allTime only — periods are nested, so merging would double-count.
    val tools = allTime?.clients ?: emptyMap()
    val toolCosts = allTime?.clientCosts ?: emptyMap()
    val toolOutputs = allTime?.clientOutputs ?: emptyMap()
    val toolCacheReads = allTime?.clientCacheReads ?: emptyMap()

    if (tools.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.tools_no_data), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val totalTokens = tools.values.sum().toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tools.entries.sortedByDescending { it.value }.toList()) { (tool, tokens) ->
            val label = toolLabel(tool)
            val color = toolColor(tool)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            ToolAvatar(tool = tool, label = label, color = color, size = 32.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(formatNumber(tokens), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        toolCosts[tool]?.let { cost ->
                            Text(formatCost(cost), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                        toolOutputs[tool]?.let { output ->
                            Text(stringResource(R.string.tools_output_format, formatNumber(output)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        toolCacheReads[tool]?.let { cache ->
                            Text(stringResource(R.string.tools_cache_format, formatNumber(cache)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ProgressBar(
                        ratio = tokens / totalTokens,
                        color = color,
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                }
            }
        }
    }
}

private val TOOL_LABELS = mapOf(
    "claude" to "Claude Code", "codex" to "Codex", "opencode" to "OpenCode",
    "hermes" to "Hermes Agent", "openclaw" to "OpenClaw", "cursor" to "Cursor",
    "antigravity" to "Antigravity", "cline" to "Cline", "amp" to "Amp",
    "droid" to "Factory Droid", "kimi" to "Kimi", "qwen" to "Qwen",
    "grok" to "Grok Build", "copilot" to "GitHub Copilot", "pi" to "Pi",
    "omp" to "Oh My Pi", "goose" to "Goose", "zed" to "Zed",
    "kilo" to "Kilo", "kilocode" to "Kilo", "commandcode" to "Command Code",
    "mimo" to "Xiaomi MiMo", "micode" to "Xiaomi MiMo", "muse" to "Muse Code",
    "zcode" to "ZCode", "kiro" to "Kiro", "codebuddy" to "CodeBuddy",
    "workbuddy" to "WorkBuddy", "proma" to "Proma", "catpaw" to "CatPaw",
    "qodercn" to "Qoder CN", "reasonix" to "Reasonix", "dsh" to "DeepSeek Harness",
    "cherrystudio" to "Cherry Studio", "lmstudio" to "LM Studio",
    "unsloth" to "Unsloth", "devin" to "Devin", "fx" to "fx",
    "mcode" to "MiniMax Code", "gemini" to "Gemini",
    // Limit-provider and model-vendor ids shown by the API Keys tab.
    "openai" to "OpenAI", "anthropic" to "Anthropic", "google" to "Google",
    "bigmodel" to "BigModel", "zhipu" to "Zhipu GLM", "bailian" to "Ali Bailian",
    "xai" to "xAI", "mistral" to "Mistral", "cohere" to "Cohere",
    "openrouter" to "OpenRouter", "groq" to "Groq", "together" to "Together",
    "deepseek" to "DeepSeek", "meta" to "Meta", "moonshot" to "Moonshot",
    "zai" to "Z.AI", "xiaomi" to "Xiaomi", "minimax" to "MiniMax"
)

internal val TOOL_COLORS = mapOf(
    "claude" to Color(0xFFCC7C5E), "codex" to Color(0xFF49A3B0),
    "hermes" to Color(0xFFD4AF37), "gemini" to Color(0xFF4285F4),
    "antigravity" to Color(0xFF4285F4), "cline" to Color(0xFF323B43),
    "kimi" to Color(0xFF16191E), "grok" to Color(0xFF000000),
    "copilot" to Color(0xFF24292F), "deepseek" to Color(0xFF4D6BFE),
    "cursor" to Color(0xFF000000), "opencode" to Color(0xFF000000),
    "openclaw" to Color(0xFFFF4D4D), "xai" to Color(0xFF000000),
    "meta" to Color(0xFF1D65C1), "mistral" to Color(0xFFFA520F),
    "qwen" to Color(0xFF615CED), "pi" to Color(0xFF000000),
    "zed" to Color(0xFF4173E7), "kilo" to Color(0xFFE8B800),
    "kilocode" to Color(0xFFE8B800), "mimo" to Color(0xFFFF6700),
    "micode" to Color(0xFFFF6700), "zcode" to Color(0xFF000000),
    "amp" to Color(0xFFF97316), "droid" to Color(0xFF1F2937),
    "omp" to Color(0xFF111827), "goose" to Color(0xFFFF6B00),
    "commandcode" to Color(0xFF5B8DEF), "muse" to Color(0xFF7C3AED),
    "kiro" to Color(0xFF8B5CF6), "codebuddy" to Color(0xFF2F6BFF),
    "workbuddy" to Color(0xFF10B981), "proma" to Color(0xFF6366F1),
    "catpaw" to Color(0xFFF59E0B), "qodercn" to Color(0xFF00A3FF),
    "reasonix" to Color(0xFF0EA5E9), "dsh" to Color(0xFF4D6BFE),
    "cherrystudio" to Color(0xFFFF5A5F), "lmstudio" to Color(0xFF6D28D9),
    "unsloth" to Color(0xFF22C55E), "devin" to Color(0xFF2563EB),
    "fx" to Color(0xFF64748B), "mcode" to Color(0xFFF23F5D),
    "moonshot" to Color(0xFF16191E), "zai" to Color(0xFF000000),
    "cohere" to Color(0xFF39594D), "xiaomi" to Color(0xFFFF6700),
    "minimax" to Color(0xFFF23F5D),
    "openai" to Color(0xFF49A3B0), "anthropic" to Color(0xFFCC7C5E),
    "google" to Color(0xFF4285F4), "bigmodel" to Color(0xFF3B7CFF),
    "zhipu" to Color(0xFF3B7CFF), "bailian" to Color(0xFFFF6A00),
    "openrouter" to Color(0xFF6E40C9), "groq" to Color(0xFFF86624),
    "together" to Color(0xFF0F9AF0)
)

internal val DEFAULT_TOOL_COLOR = Color(0xFF6AB4F0)

internal fun toolLabel(id: String): String = TOOL_LABELS[id] ?: id.replaceFirstChar { it.uppercase() }

internal fun toolColor(id: String): Color = TOOL_COLORS[id] ?: DEFAULT_TOOL_COLOR

private fun modelVendorFor(model: String): String? {
    val name = model.lowercase()
    return when {
        Regex("^(?:cursor-)?auto$").matches(name) -> "cursor"
        Regex("claude|anthropic|sonnet|opus|haiku").containsMatchIn(name) -> "claude"
        Regex("""gpt|openai|codex|^o[134](?:-|$)|chatgpt""").containsMatchIn(name) -> "codex"
        Regex("gemini|gemma|google").containsMatchIn(name) -> "gemini"
        Regex("grok|xai").containsMatchIn(name) -> "xai"
        Regex("deepseek").containsMatchIn(name) -> "deepseek"
        Regex("llama|meta").containsMatchIn(name) -> "meta"
        Regex("mistral|mixtral|codestral").containsMatchIn(name) -> "mistral"
        Regex("qwen|qwq|qvq").containsMatchIn(name) -> "qwen"
        Regex("kimi|moonshot").containsMatchIn(name) -> "kimi"
        Regex("minimax").containsMatchIn(name) -> "minimax"
        Regex("""^pi(?:-|$)""").containsMatchIn(name) -> "pi"
        Regex("^copilot|github").containsMatchIn(name) -> "copilot"
        else -> null
    }
}

private val TOOL_ICONS = mapOf(
    "claude" to R.raw.ic_tool_claude, "codex" to R.raw.ic_tool_codex,
    "hermes" to R.raw.ic_tool_hermes, "gemini" to R.raw.ic_tool_gemini,
    "cursor" to R.raw.ic_tool_cursor, "opencode" to R.raw.ic_tool_opencode,
    "openclaw" to R.raw.ic_tool_openclaw, "antigravity" to R.raw.ic_tool_antigravity,
    "cline" to R.raw.ic_tool_cline, "kimi" to R.raw.ic_tool_moonshot,
    "qwen" to R.raw.ic_tool_qwen, "grok" to R.raw.ic_tool_grok,
    "copilot" to R.raw.ic_tool_copilot, "pi" to R.raw.ic_tool_pi,
    "zed" to R.raw.ic_tool_zed, "zcode" to R.raw.ic_tool_zai,
    "deepseek" to R.raw.ic_tool_deepseek, "xai" to R.raw.ic_tool_xai,
    "meta" to R.raw.ic_tool_meta, "mistral" to R.raw.ic_tool_mistral,
    "moonshot" to R.raw.ic_tool_moonshot, "zai" to R.raw.ic_tool_zai,
    "cohere" to R.raw.ic_tool_cohere, "xiaomi" to R.raw.ic_tool_xiaomi,
    "minimax" to R.raw.ic_tool_minimax,
    "openai" to R.raw.ic_tool_codex,
    "anthropic" to R.raw.ic_tool_claude, "google" to R.raw.ic_tool_gemini,
    "bigmodel" to R.raw.ic_tool_bigmodel,
    "zhipu" to R.raw.ic_tool_bigmodel
)

private val TOOL_RASTER_ICONS = mapOf(
    "amp" to R.drawable.ic_tool_amp,
    "droid" to R.drawable.ic_tool_droid,
    "omp" to R.drawable.ic_tool_omp,
    "goose" to R.drawable.ic_tool_goose,
    "kilo" to R.drawable.ic_tool_kilo,
    "kilocode" to R.drawable.ic_tool_kilo,
    "commandcode" to R.drawable.ic_tool_commandcode,
    "mimo" to R.drawable.ic_tool_mimo,
    "micode" to R.drawable.ic_tool_mimo,
    "muse" to R.drawable.ic_tool_muse,
    "kiro" to R.drawable.ic_tool_kiro,
    "codebuddy" to R.drawable.ic_tool_codebuddy,
    "workbuddy" to R.drawable.ic_tool_workbuddy,
    "proma" to R.drawable.ic_tool_proma,
    "catpaw" to R.drawable.ic_tool_catpaw,
    "qodercn" to R.drawable.ic_tool_qoder,
    "reasonix" to R.drawable.ic_tool_reasonix,
    "dsh" to R.drawable.ic_tool_dsh,
    "cherrystudio" to R.drawable.ic_tool_cherrystudio,
    "lmstudio" to R.drawable.ic_tool_lmstudio,
    "unsloth" to R.drawable.ic_tool_unsloth,
    "devin" to R.drawable.ic_tool_devin,
    "fx" to R.drawable.ic_tool_fx,
    "mcode" to R.drawable.ic_tool_mcode
)

private fun toolIconRes(id: String): Int? = TOOL_ICONS[id]

private fun toolRasterIconRes(id: String): Int? = TOOL_RASTER_ICONS[id]

@Composable
internal fun ToolAvatar(tool: String, label: String, color: Color, size: androidx.compose.ui.unit.Dp) {
    val rasterIconRes = toolRasterIconRes(tool)
    val iconRes = toolIconRes(tool)
    when {
        rasterIconRes != null -> {
            Image(
                painter = painterResource(id = rasterIconRes),
                contentDescription = label,
                modifier = Modifier.size(size)
            )
        }
        iconRes != null -> {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(iconRes)
                    .decoderFactory(SvgDecoder.Factory())
                    .build(),
                contentDescription = label,
                colorFilter = ColorFilter.tint(color),
                modifier = Modifier.size(size)
            )
        }
        else -> {
            // Fallback: colored circle with initials for unknown tools
            val initials = label.take(2)
            Canvas(modifier = Modifier.size(size)) {
                drawCircle(color = color)
                drawContext.canvas.nativeCanvas.drawText(
                    initials,
                    center.x,
                    center.y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        textSize = size.toPx() * 0.40f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                )
            }
        }
    }
}

@Composable
fun SessionsTab(sessions: List<SessionInfo>) {
    var selectedSession by remember { mutableStateOf<SessionInfo?>(null) }

    if (sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.sessions_no_sessions), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sessions) { session ->
            SessionCard(session, onClick = { selectedSession = session })
        }
    }

    selectedSession?.let { session ->
        SessionDetailDialog(session = session, onDismiss = { selectedSession = null })
    }
}

@Composable
fun SessionCard(session: SessionInfo, onClick: () -> Unit = {}) {
    val state = sessionState(session)
    val title = sessionTitle(session)
    val subtitle = listOfNotNull(
        session.projectLabel?.takeIf { it.isNotBlank() && it != title },
        session.sessionId.takeLast(40).takeIf { it != title }
    ).joinToString(" · ")
    val cacheHit = cacheHitRate(session)
    val speed = outputTokensPerSecond(session)
    val context = contextUsage(session)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    ToolAvatar(session.client, toolLabel(session.client), toolColor(session.client), 18.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        toolLabel(session.client),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        SessionStateBadge(state)
                    }
                }
                Text(formatNumber(session.totalTokens), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.sessions_msgs_cost_format, session.messageCount, formatCost(session.costUsd)),
                    style = MaterialTheme.typography.labelSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cacheHit?.let {
                        Text(
                            stringResource(R.string.sessions_metric_cache_format, it * 100),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    speed?.let {
                        Text(
                            stringResource(R.string.sessions_metric_tps_format, it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    context?.let {
                        Text(
                            stringResource(R.string.sessions_metric_context_format, it * 100),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    session.lastUsedAt?.let { last ->
                        Text(
                            formatTime(last, LocalContext.current),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            val flags = buildList {
                if (session.sessionKind == "background-review") {
                    add(stringResource(R.string.sessions_background_review))
                }
                if (!session.parentSessionId.isNullOrBlank()) {
                    add(stringResource(R.string.sessions_subagent))
                }
            }
            if (flags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    flags.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

private enum class SessionState { RUNNING, WAITING, IDLE }

@Composable
private fun SessionStateBadge(state: SessionState) {
    val labelRes = when (state) {
        SessionState.RUNNING -> R.string.sessions_running
        SessionState.WAITING -> R.string.sessions_waiting_input
        SessionState.IDLE -> R.string.sessions_idle
    }
    val color = when (state) {
        SessionState.RUNNING -> Color(0xFF4CAF50)
        SessionState.WAITING -> Color(0xFFFFA726)
        SessionState.IDLE -> Color(0xFF9E9E9E)
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Text(
            stringResource(labelRes),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun sessionState(session: SessionInfo): SessionState? {
    val recent = sessionIsRecent(session.lastUsedAt)
    // liveActivity is a 30-second observation; after that the transcript's own
    // turn boundary is the only evidence left.
    val liveState = sessionLiveState(session)
    return when {
        liveState == "running" -> SessionState.RUNNING
        liveState == "waiting" -> SessionState.WAITING
        liveState == "idle" -> SessionState.IDLE
        session.waitingForInput == true && recent -> SessionState.WAITING
        session.turnEnded == false && recent -> SessionState.RUNNING
        else -> null
    }
}

private fun sessionLiveState(session: SessionInfo): String? {
    val live = session.liveActivity ?: return null
    val observed = live.observedAt ?: return null
    return try {
        val ageSeconds = Duration.between(Instant.parse(observed), Instant.now()).seconds
        if (ageSeconds in 0..30) live.state else null
    } catch (_: Exception) {
        null
    }
}

private fun sessionIsRecent(iso: String?, minutes: Long = 10): Boolean {
    if (iso.isNullOrBlank()) return false
    return try {
        Duration.between(Instant.parse(iso), Instant.now()).toMinutes() in 0..minutes
    } catch (_: Exception) {
        false
    }
}

private fun sessionTitle(session: SessionInfo): String =
    session.title?.takeIf { it.isNotBlank() }
        ?: session.projectLabel?.takeIf { it.isNotBlank() }
        ?: session.sessionId.takeLast(40)

private fun cacheHitRate(session: SessionInfo): Double? {
    val denominator = session.inputTokens + session.cacheReadTokens + session.cacheWriteTokens
    if (denominator <= 0 || session.cacheReadTokens <= 0) return null
    return session.cacheReadTokens.toDouble() / denominator
}

private fun outputTokensPerSecond(session: SessionInfo): Double? {
    if (session.timedDurationMs <= 0 || session.timedOutputTokens <= 0) return null
    return session.timedOutputTokens * 1000.0 / session.timedDurationMs
}

private fun contextUsage(session: SessionInfo): Double? {
    if (session.contextWindow <= 0 || session.contextTokens <= 0) return null
    return (session.contextTokens.toDouble() / session.contextWindow).coerceIn(0.0, 1.0)
}

@Composable
fun SessionDetailDialog(session: SessionInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    sessionTitle(session),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    toolLabel(session.client) + " · " + session.sessionId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Token breakdown
                SectionBlock(stringResource(R.string.session_token_breakdown)) {
                    TokenRow(stringResource(R.string.session_input), session.inputTokens)
                    TokenRow(stringResource(R.string.session_output), session.outputTokens)
                    TokenRow(stringResource(R.string.session_cache_read), session.cacheReadTokens)
                    TokenRow(stringResource(R.string.session_cache_write), session.cacheWriteTokens)
                    if (session.reasoningTokens > 0) {
                        TokenRow(stringResource(R.string.session_reasoning), session.reasoningTokens)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    TokenRow(stringResource(R.string.session_total), session.totalTokens, bold = true)
                }

                // Quick stats
                SectionBlock(stringResource(R.string.session_stats)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(stringResource(R.string.stat_messages), session.messageCount.toString(), Modifier.weight(1f))
                        StatItem(stringResource(R.string.stat_cost), formatCost(session.costUsd), Modifier.weight(1f))
                    }
                }

                // Live activity, context headroom and throughput
                val state = sessionState(session)
                val cacheHit = cacheHitRate(session)
                val speed = outputTokensPerSecond(session)
                val context = contextUsage(session)
                val promptCache = session.promptCache
                if (state != null || session.projectLabel != null || session.contextWindow > 0 ||
                    cacheHit != null || speed != null || promptCache != null ||
                    !session.parentSessionId.isNullOrBlank() || session.unpricedTokens > 0
                ) {
                    SectionBlock(stringResource(R.string.session_context_title)) {
                        state?.let {
                            val label = when (it) {
                                SessionState.RUNNING -> stringResource(R.string.sessions_running)
                                SessionState.WAITING -> stringResource(R.string.sessions_waiting_input)
                                SessionState.IDLE -> stringResource(R.string.sessions_idle)
                            }
                            SummaryRow(stringResource(R.string.session_activity), label)
                        }
                        session.projectLabel?.takeIf { it.isNotBlank() }?.let {
                            SummaryRow(stringResource(R.string.session_project), it)
                        }
                        if (session.contextWindow > 0) {
                            val ratio = context ?: 0.0
                            SummaryRow(
                                stringResource(R.string.session_context_usage),
                                stringResource(
                                    R.string.session_context_usage_format,
                                    formatNumber(session.contextTokens),
                                    formatNumber(session.contextWindow),
                                    ratio * 100
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { ratio.toFloat() },
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                        }
                        cacheHit?.let {
                            SummaryRow(
                                stringResource(R.string.session_cache_hit),
                                stringResource(R.string.session_cache_hit_format, it * 100)
                            )
                        }
                        speed?.let {
                            SummaryRow(
                                stringResource(R.string.session_output_speed),
                                stringResource(R.string.session_output_speed_format, it)
                            )
                        }
                        promptCache?.let { cache ->
                            SummaryRow(
                                stringResource(R.string.session_prompt_cache),
                                stringResource(
                                    R.string.session_prompt_cache_format,
                                    cache.ttlSeconds / 60,
                                    cache.observedAt?.let { formatTime(it, LocalContext.current) }
                                        ?: stringResource(R.string.session_na)
                                )
                            )
                        }
                        session.parentSessionId?.takeIf { it.isNotBlank() }?.let { parent ->
                            val parentClient = session.parentClient?.takeIf { it.isNotBlank() } ?: session.client
                            SummaryRow(
                                stringResource(R.string.session_parent),
                                "$parentClient · $parent"
                            )
                        }
                        if (session.unpricedTokens > 0) {
                            Text(
                                stringResource(R.string.session_unpriced_hint, formatNumber(session.unpricedTokens)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Models
                if (!session.models.isNullOrEmpty()) {
                    SectionBlock(stringResource(R.string.session_models_used)) {
                        val groupedSessionModels = ModelAliasRegistry.groupModels(session.models)
                        val groupedSessionCosts = ModelAliasRegistry.groupCosts(session.models, session.modelCosts)
                        groupedSessionModels.entries.sortedByDescending { it.value }.forEach { (model, tokens) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(model, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(formatNumber(tokens), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    groupedSessionCosts[model]?.let { cost ->
                                        Text(formatCost(cost), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                            }
                        }
                    }
                }

                // Providers
                if (!session.providers.isNullOrEmpty()) {
                    SectionBlock(stringResource(R.string.session_providers)) {
                        session.providers.entries.forEach { (provider, tokens) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(provider.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall)
                                Text("${formatNumber(tokens)} tokens", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Time range
                SectionBlock(stringResource(R.string.session_time)) {
                    if (!session.startedAt.isNullOrBlank()) {
                        Text(stringResource(R.string.session_started_format, session.startedAt.replace("T", " ").take(19)), style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(stringResource(R.string.session_started_format, stringResource(R.string.session_na)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!session.lastUsedAt.isNullOrBlank()) {
                        Text(stringResource(R.string.session_last_used_format, session.lastUsedAt.replace("T", " ").take(19)), style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(stringResource(R.string.session_last_used_format, stringResource(R.string.session_na)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.session_close)) }
        }
    )
}

@Composable
private fun SectionBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun TokenRow(label: String, tokens: Long, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            formatNumber(tokens),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun TrendsTab(history: HistoryResponse?) {
    val allDaily = history?.daily.orEmpty()
    val monthly = history?.monthly
    val summary = history?.summary
    var range by remember { mutableIntStateOf(1) }

    val daily = remember(allDaily, range) {
        when (range) {
            0 -> {
                val cutoff = java.time.LocalDate.now().minusDays(6).toString()
                allDaily.filter { it.date >= cutoff }
            }
            1 -> {
                val cutoff = java.time.LocalDate.now().minusDays(29).toString()
                allDaily.filter { it.date >= cutoff }
            }
            else -> allDaily
        }
    }

    if (allDaily.isEmpty() && monthly.isNullOrEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.trends_no_data), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.trends_no_data_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val rangeTokens = daily.sumOf { it.tokens }
    val rangeCost = daily.sumOf { it.cost }
    val rangeActiveDays = daily.count { it.tokens > 0 || it.cost > 0 }
    val rangeMessages = daily.sumOf { it.messages }
    val rangeActiveTimeMs = daily.sumOf { it.activeTimeMs }
    val rangeUnpriced = daily.sumOf { it.unpricedTokens }
    val byClient = aggregateHistoryBuckets(daily) { it.perClient }
    val byModel = aggregateHistoryBuckets(daily, resolveAliases = true) { it.perModel }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.trends_range_7d, R.string.trends_range_30d, R.string.trends_range_all)
                .forEachIndexed { index, res ->
                    FilterChip(
                        selected = range == index,
                        onClick = { range = index },
                        label = { Text(stringResource(res)) }
                    )
                }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.trends_summary), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatItem(stringResource(R.string.trends_total_tokens), formatNumber(rangeTokens), Modifier.weight(1f))
                    StatItem(stringResource(R.string.trends_total_cost), formatCost(rangeCost), Modifier.weight(1f))
                    StatItem(stringResource(R.string.trends_active_days), rangeActiveDays.toString(), Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatItem(stringResource(R.string.trends_messages), formatNumber(rangeMessages.toLong()), Modifier.weight(1f))
                    StatItem(stringResource(R.string.trends_active_time), formatActiveTime(rangeActiveTimeMs), Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (rangeUnpriced > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.overview_unpriced_hint, formatNumber(rangeUnpriced)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        if (summary != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.trends_lifetime_summary), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(stringResource(R.string.trends_total_tokens), formatNumber(summary.totalTokens), Modifier.weight(1f))
                        StatItem(stringResource(R.string.trends_total_cost), formatCost(summary.totalCost), Modifier.weight(1f))
                        StatItem(stringResource(R.string.trends_active_days), summary.activeDays.toString(), Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(stringResource(R.string.trends_current_streak), stringResource(R.string.trends_streak_format, summary.currentStreak), Modifier.weight(1f))
                        StatItem(stringResource(R.string.trends_longest_streak), stringResource(R.string.trends_streak_format, summary.longestStreak), Modifier.weight(1f))
                        StatItem(stringResource(R.string.trends_peak_day), formatNumber(summary.peakDayTokens), Modifier.weight(1f))
                    }
                    if (summary.activeTimeMs > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.trends_active_time_format, formatActiveTime(summary.activeTimeMs)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (allDaily.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.trends_activity), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    ActivityGrid(
                        days = allDaily,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (daily.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.trends_daily_tokens), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    DailyBarChart(
                        days = daily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.trends_daily_cost), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    DailyCostChart(
                        days = daily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
        }

        if (byClient.isNotEmpty() && rangeTokens > 0) {
            BreakdownCard(
                title = stringResource(R.string.trends_by_tool),
                entries = byClient,
                totalTokens = rangeTokens,
                showAvatar = true
            )
        }

        if (byModel.isNotEmpty() && rangeTokens > 0) {
            BreakdownCard(
                title = stringResource(R.string.trends_by_model),
                entries = byModel,
                totalTokens = rangeTokens,
                showAvatar = false
            )
        }

        if (!monthly.isNullOrEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.trends_monthly), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    monthly.takeLast(12).reversed().forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(m.month, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(
                                "${formatNumber(m.tokens)} tokens",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(120.dp)
                            )
                            Text(
                                formatCost(m.cost),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.width(80.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownCard(
    title: String,
    entries: Map<String, Long>,
    totalTokens: Long,
    showAvatar: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            entries.entries.sortedByDescending { it.value }.take(8).forEach { (key, tokens) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showAvatar) {
                        ToolAvatar(key, toolLabel(key), toolColor(key), 16.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        if (showAvatar) toolLabel(key) else key,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(
                            R.string.trends_breakdown_format,
                            formatNumber(tokens),
                            tokens * 100.0 / totalTokens
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { (tokens.toFloat() / totalTokens).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
        }
    }
}

private fun aggregateHistoryBuckets(
    days: List<HistoryDay>,
    resolveAliases: Boolean = false,
    selector: (HistoryDay) -> Map<String, HistoryBucket>?
): Map<String, Long> {
    val totals = mutableMapOf<String, Long>()
    days.forEach { day ->
        selector(day)?.forEach { (rawKey, bucket) ->
            val key = if (resolveAliases) ModelAliasRegistry.label(rawKey) else rawKey
            totals[key] = (totals[key] ?: 0L) + bucket.tokens
        }
    }
    return totals
}

@Composable
private fun formatActiveTime(milliseconds: Long): String {
    val totalMinutes = milliseconds / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.trends_hours_minutes_format, hours, minutes)
    } else {
        stringResource(R.string.trends_minutes_format, minutes)
    }
}

@Composable
fun ActivityGrid(
    days: List<HistoryDay>,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return

    val intensityMap = remember(days) { days.associate { it.date to it.intensity } }
    val today = remember { java.time.LocalDate.now() }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val emptyColor = if (isDark) Color(0x1AFFFFFF) else Color(0x33000000)
    val colors = if (isDark) listOf(
        emptyColor,
        Color(0xFF0E4429),
        Color(0xFF006D32),
        Color(0xFF26A641),
        Color(0xFF39D353),
    ) else listOf(
        emptyColor,
        Color(0xFF9BE9A8),
        Color(0xFF40C463),
        Color(0xFF30A14E),
        Color(0xFF216E39),
    )

    val totalWeeks = 53
    val endDate = today
    val startDate = endDate.minusWeeks((totalWeeks - 1).toLong()).with(java.time.DayOfWeek.SUNDAY)
    val cellGap = 3.dp
    val cellSize = 10.dp
    val topPad = 14.dp
    val gridWidth = cellSize * totalWeeks + cellGap * (totalWeeks - 1)
    val gridHeight = cellSize * 7 + cellGap * 6 + topPad + 6.dp
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { scrollState.scrollTo(scrollState.maxValue) }

    Row(modifier = modifier) {
        // Fixed day labels on the left
        Canvas(modifier = Modifier.width(16.dp).height(gridHeight)) {
            val cellPx = cellSize.toPx()
            val stepPx = cellPx + cellGap.toPx()
            val topPx = topPad.toPx()
            val fontSize = (cellPx * 0.75f).coerceIn(8f, 14f)
            val labelColor = if (isDark) 0xFF999999.toInt() else 0xFF777777.toInt()

            val dayPaint = android.graphics.Paint().apply {
                textSize = fontSize; color = labelColor; isAntiAlias = true
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            val labelRows = intArrayOf(1, 3, 5)
            val labelTexts = arrayOf("Mon", "Wed", "Fri")
            for (i in labelRows.indices) {
                drawContext.canvas.nativeCanvas.drawText(labelTexts[i], size.width - 4.dp.toPx(), topPx + labelRows[i] * stepPx + cellPx * 0.7f, dayPaint)
            }
        }

        // Scrollable grid
        Row(modifier = Modifier.nestedScroll(rememberBlockPagerScroll()).horizontalScroll(scrollState).weight(1f)) {
            Canvas(modifier = Modifier.width(gridWidth).height(gridHeight)) {
                val cellPx = cellSize.toPx()
                val stepPx = cellPx + cellGap.toPx()
                val topPx = topPad.toPx()
                val fontSize = (cellPx * 0.75f).coerceIn(8f, 14f)
                val labelColor = if (isDark) 0xFF999999.toInt() else 0xFF777777.toInt()

                // Month labels on top
                val monthPaint = android.graphics.Paint().apply {
                    textSize = fontSize; color = labelColor; isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.LEFT
                }
                var lastMonth = ""
                for (col in 0 until totalWeeks) {
                    val monDate = startDate.plusDays(col * 7L + 1)
                    if (monDate > today) continue
                    val month = monDate.month.toString().take(3)
                    if (month != lastMonth) {
                        lastMonth = month
                        drawContext.canvas.nativeCanvas.drawText(month, col * stepPx, topPx - 4.dp.toPx(), monthPaint)
                    }
                }

                // Draw cells
                for (col in 0 until totalWeeks) {
                    for (row in 0 until 7) {
                        val date = startDate.plusDays(col * 7L + row.toLong())
                        if (date > today) continue
                        val intensity = (intensityMap[date.toString()] ?: 0).coerceIn(0, 4)
                        drawRoundRect(
                            color = colors[intensity],
                            topLeft = Offset(col * stepPx, topPx + row * stepPx),
                            size = Size(cellPx, cellPx),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyBarChart(
    days: List<HistoryDay>,
    modifier: Modifier = Modifier
) {
    val recentDays = remember(days) { days.takeLast(30) }
    if (recentDays.isEmpty()) return

    val maxTokens = recentDays.maxOf { it.tokens }.toFloat().coerceAtLeast(1f)
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val barWidthDp = 10.dp
    val barGapDp = 2.dp
    val chartHeightDp = 200.dp
    val yAxisWidthDp = 14.dp
    val contentPaddingTop = 16f
    val contentPaddingBottom = 28f

    val barCount = recentDays.size
    val chartWidthDp = barWidthDp * barCount + barGapDp * (barCount - 1)

    val scrollState = rememberScrollState()
    LaunchedEffect(recentDays) { scrollState.scrollTo(scrollState.maxValue) }

    val labelColorInt = if (isDark) 0xFFBBBBBB.toInt() else 0xFF666666.toInt()
    val gridLineColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    Row(modifier = modifier) {
        // Fixed Y-axis
        Canvas(modifier = Modifier.width(yAxisWidthDp).height(chartHeightDp)) {
            val canvasHeight = size.height
            val chartHeightPx = canvasHeight - contentPaddingTop - contentPaddingBottom
            val ySteps = 3
            for (i in 0..ySteps) {
                val y = contentPaddingTop + chartHeightPx * (1f - i.toFloat() / ySteps)
                val value = (maxTokens * i / ySteps).toLong()
                drawContext.canvas.nativeCanvas.drawText(
                    formatCompactNumber(value),
                    size.width - 1f, y + 2f,
                    android.graphics.Paint().apply {
                        textSize = 11f; color = labelColorInt
                        textAlign = android.graphics.Paint.Align.RIGHT; isAntiAlias = true
                    }
                )
            }
        }

        // Scrollable chart
        Box(modifier = Modifier.weight(1f).nestedScroll(rememberBlockPagerScroll()).horizontalScroll(scrollState)) {
            Canvas(modifier = Modifier.width(chartWidthDp).height(chartHeightDp)) {
                val canvasHeight = size.height
                val chartHeightPx = canvasHeight - contentPaddingTop - contentPaddingBottom
                val barW = barWidthDp.toPx()
                val barG = barGapDp.toPx()

                val ySteps = 3
                for (i in 0..ySteps) {
                    val y = contentPaddingTop + chartHeightPx * (1f - i.toFloat() / ySteps)
                    if (i > 0) {
                        drawLine(color = gridLineColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                    }
                }

                recentDays.forEachIndexed { index, day ->
                    val barHeight = (day.tokens / maxTokens) * chartHeightPx
                    val x = index * (barW + barG)
                    val y = contentPaddingTop + chartHeightPx - barHeight
                    val color = when {
                        day.intensity >= 4 -> Color(0xFF1565C0)
                        day.intensity >= 3 -> Color(0xFF42A5F5)
                        day.intensity >= 2 -> Color(0xFF90CAF9)
                        day.intensity >= 1 -> Color(0xFFBBDEFB)
                        else -> Color(0xFFE3F2FD)
                    }
                    drawRect(color = color, topLeft = Offset(x, y), size = Size(barW, barHeight.coerceAtLeast(1f)))
                }

                val labelPaint = android.graphics.Paint().apply {
                    textSize = 18f; color = labelColorInt
                    textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
                }
                recentDays.forEachIndexed { i, day ->
                    if (i % 7 == 0 || i == recentDays.size - 1) {
                        val x = i * (barW + barG) + barW / 2f
                        drawContext.canvas.nativeCanvas.drawText(day.date.takeLast(5), x, canvasHeight - 4f, labelPaint)
                    }
                }
            }
        }
    }
}

@Composable
fun DailyCostChart(
    days: List<HistoryDay>,
    modifier: Modifier = Modifier
) {
    val recentDays = remember(days) { days.takeLast(30) }
    if (recentDays.isEmpty()) return

    val maxCost = recentDays.maxOf { it.cost }.toFloat().coerceAtLeast(1f)
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val barWidthDp = 10.dp
    val barGapDp = 2.dp
    val chartHeightDp = 200.dp
    val yAxisWidthDp = 20.dp
    val contentPaddingTop = 16f
    val contentPaddingBottom = 28f

    val barCount = recentDays.size
    val chartWidthDp = barWidthDp * barCount + barGapDp * (barCount - 1)

    val scrollState = rememberScrollState()
    LaunchedEffect(recentDays) { scrollState.scrollTo(scrollState.maxValue) }

    val labelColorInt = if (isDark) 0xFFBBBBBB.toInt() else 0xFF666666.toInt()
    val gridLineColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    Row(modifier = modifier) {
        // Fixed Y-axis
        Canvas(modifier = Modifier.width(yAxisWidthDp).height(chartHeightDp)) {
            val canvasHeight = size.height
            val chartHeightPx = canvasHeight - contentPaddingTop - contentPaddingBottom
            val ySteps = 3
            for (i in 0..ySteps) {
                val y = contentPaddingTop + chartHeightPx * (1f - i.toFloat() / ySteps)
                val value = maxCost * i / ySteps
                drawContext.canvas.nativeCanvas.drawText(
                    formatCost(value.toDouble()),
                    size.width - 1f, y + 2f,
                    android.graphics.Paint().apply {
                        textSize = 11f; color = labelColorInt
                        textAlign = android.graphics.Paint.Align.RIGHT; isAntiAlias = true
                    }
                )
            }
        }

        // Scrollable chart
        Box(modifier = Modifier.weight(1f).nestedScroll(rememberBlockPagerScroll()).horizontalScroll(scrollState)) {
            Canvas(modifier = Modifier.width(chartWidthDp).height(chartHeightDp)) {
                val canvasHeight = size.height
                val chartHeightPx = canvasHeight - contentPaddingTop - contentPaddingBottom
                val barW = barWidthDp.toPx()
                val barG = barGapDp.toPx()

                val ySteps = 3
                for (i in 0..ySteps) {
                    val y = contentPaddingTop + chartHeightPx * (1f - i.toFloat() / ySteps)
                    if (i > 0) {
                        drawLine(color = gridLineColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                    }
                }

                recentDays.forEachIndexed { index, day ->
                    val barHeight = (day.cost.toFloat() / maxCost) * chartHeightPx
                    val x = index * (barW + barG)
                    val y = contentPaddingTop + chartHeightPx - barHeight
                    drawRect(color = Color(0xFF4CAF50), topLeft = Offset(x, y), size = Size(barW, barHeight.coerceAtLeast(1f)))
                }

                val labelPaint = android.graphics.Paint().apply {
                    textSize = 18f; color = labelColorInt
                    textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
                }
                recentDays.forEachIndexed { i, day ->
                    if (i % 7 == 0 || i == recentDays.size - 1) {
                        val x = i * (barW + barG) + barW / 2f
                        drawContext.canvas.nativeCanvas.drawText(day.date.takeLast(5), x, canvasHeight - 4f, labelPaint)
                    }
                }
            }
        }
    }
}


private fun formatNumber(n: Long): String = NumberFormat.getNumberInstance(Locale.US).format(n)

private fun formatCompactNumber(n: Long): String {
    return when {
        n >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", n / 1_000_000_000.0)
        n >= 1_000_000 -> String.format(Locale.US, "%.1fM", n / 1_000_000.0)
        n >= 1_000 -> String.format(Locale.US, "%.1fK", n / 1_000.0)
        else -> n.toString()
    }
}

private fun formatCost(cost: Double): String {
    return if (cost < 0.01) """<$0.01""" else """$${String.format(Locale.US, "%.2f", cost)}"""
}

@Composable
private fun rememberBlockPagerScroll() = remember {
    object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            // Consume remaining horizontal delta so the outer pager doesn't switch tabs
            return if (source == NestedScrollSource.Drag && available.x != 0f) Offset(available.x, 0f) else Offset.Zero
        }
    }
}

private fun formatTime(isoTime: String, context: android.content.Context? = null): String {
    return try {
        val instant = Instant.parse(isoTime)
        val now = Instant.now()
        val duration = Duration.between(instant, now)
        when {
            duration.toMinutes() < 1 -> context?.getString(R.string.time_just_now) ?: "just now"
            duration.toMinutes() < 60 -> context?.getString(R.string.time_minutes_ago, duration.toMinutes()) ?: "${duration.toMinutes()}m ago"
            duration.toHours() < 24 -> context?.getString(R.string.time_hours_ago, duration.toHours()) ?: "${duration.toHours()}h ago"
            duration.toDays() < 7 -> context?.getString(R.string.time_days_ago, duration.toDays()) ?: "${duration.toDays()}d ago"
            else -> isoTime.take(10)
        }
    } catch (_: Exception) {
        isoTime.take(16).replace("T", " ")
    }
}
