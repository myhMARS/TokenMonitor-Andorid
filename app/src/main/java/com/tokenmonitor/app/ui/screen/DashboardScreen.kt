package com.tokenmonitor.app.ui.screen

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ColorFilter
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.tokenmonitor.app.R
import com.tokenmonitor.app.data.model.DeviceRecord
import com.tokenmonitor.app.data.model.HistoryDay
import com.tokenmonitor.app.data.model.HistoryResponse
import com.tokenmonitor.app.data.model.LimitProvider
import com.tokenmonitor.app.data.model.SessionInfo
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
    hubVersion: Int,
    hubNow: String,
    selectedTab: Int,
    isRefreshing: Boolean = false,
    deletingDeviceId: String? = null,
    snackbarMessage: String? = null,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
    onDeleteDevice: (String) -> Unit,
    onClearSnackbar: () -> Unit = {},
    onSwitchLanguage: (() -> Unit)? = null,
    importedApiData: String? = null,
    onImportRequest: (() -> Unit)? = null,
    onExportData: ((String) -> Unit)? = null,
    localeKey: Int = 0,
) {
    val tabResIds = remember {
        listOf(R.string.tab_overview, R.string.tab_quota, R.string.tab_devices,
            R.string.tab_models, R.string.tab_tools, R.string.tab_sessions,
            R.string.tab_trends, R.string.tab_api_keys)
    }

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
                    0 -> OverviewTab(today, month, allTime, hubVersion, hubNow)
                    1 -> QuotaTab(limits, today)
                    2 -> DevicesTab(devices, deletingDeviceId, onDeleteDevice)
                    3 -> ModelsTab(allTime)
                    4 -> ToolsTab(allTime)
                    5 -> SessionsTab(sessions)
                    6 -> TrendsTab(history)
                    7 -> ApiKeysTab(keyAction = keyAction, onKeyActionHandled = { keyAction = null }, importedData = importedApiData, onExportData = onExportData)
                }
            }
        }
    }
}

@Composable
fun OverviewTab(today: UsagePeriod?, month: UsagePeriod?, allTime: UsagePeriod?, hubVersion: Int, hubNow: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PeriodCard(stringResource(R.string.period_today), today, MaterialTheme.colorScheme.primary) }
        item { PeriodCard(stringResource(R.string.period_month), month, MaterialTheme.colorScheme.tertiary) }
        item { PeriodCard(stringResource(R.string.period_all_time), allTime, MaterialTheme.colorScheme.secondary) }

        if (hubVersion > 0 || hubNow.isNotBlank()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        if (hubVersion > 0) {
                            Text(
                                text = stringResource(R.string.overview_hub_version, hubVersion),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = hubNow.take(16).replace("T", " "),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (today != null && !today.models.isNullOrEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.overview_model_usage), style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        today.models.entries.sortedByDescending { it.value }.take(10).forEach { (model, tokens) ->
                            ModelUsageRow(model, tokens, today.modelCosts?.get(model))
                        }
                    }
                }
            }
        }
    }
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
            // 2x2 grid to avoid cramped layout with large numbers
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
                }
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
    val hasData = limits.isNotEmpty() || today != null

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
        // Today's consumption summary
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

        // Provider balance and limits
        items(limits) { provider ->
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
    val hasWindows = !provider.windows.isNullOrEmpty()
    val hasBalance = provider.balanceUsd != null || provider.balance != null

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = when (provider.status) {
                        "warning" -> Color(0xFFFFA726)
                        "error" -> Color(0xFFEF5350)
                        else -> Color(0xFF66BB6A)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    provider.provider.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                provider.accountLabel?.let { label ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Balance progress bars
            if (hasBalance) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.quota_balance), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                provider.balanceUsd?.let { usd ->
                    val estimatedMonthly = usd.coerceAtLeast(0.01) // prevent divide by zero for ratio
                    val ratio = ((usd / estimatedMonthly)).toFloat().coerceIn(0f, 1f)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.quota_prepaid_usd), style = MaterialTheme.typography.bodySmall)
                            Text(formatCost(usd), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                                color = if (usd > 0) Color(0xFF4CAF50) else Color(0xFFEF5350))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        ProgressBar(
                            ratio = ratio,
                            color = if (usd > 0) Color(0xFF4CAF50) else Color(0xFFEF5350),
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                    }
                }

                provider.balance?.let { bal ->
                    Spacer(modifier = Modifier.height(8.dp))
                    // Show balance amount + today/month spend as progress
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.quota_balance_format, bal.currency), style = MaterialTheme.typography.bodySmall)
                            Text(String.format(Locale.US, "%.2f", bal.amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        ProgressBar(
                            ratio = 1f,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                    }
                    bal.todaySpend?.let { todaySpend ->
                        Spacer(modifier = Modifier.height(6.dp))
                        val spendRatio = (todaySpend / bal.amount.coerceAtLeast(0.001)).toFloat().coerceIn(0f, 1f)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.quota_today_spend), style = MaterialTheme.typography.bodySmall)
                                Text("${String.format(Locale.US, "%.2f", todaySpend)} ${bal.currency}", style = MaterialTheme.typography.bodySmall,
                                    color = if (spendRatio > 0.8f) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            ProgressBar(
                                ratio = spendRatio,
                                color = when {
                                    spendRatio > 0.8f -> Color(0xFFEF5350)
                                    spendRatio > 0.5f -> Color(0xFFFFA726)
                                    else -> Color(0xFF42A5F5)
                                },
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                        }
                    }
                    bal.monthSpend?.let { monthSpend ->
                        Spacer(modifier = Modifier.height(6.dp))
                        val monthRatio = (monthSpend / bal.amount.coerceAtLeast(0.01)).toFloat().coerceIn(0f, 1f)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stringResource(R.string.quota_month_spend) + if (bal.monthSinceTracking == true) "" else "*",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text("${String.format(Locale.US, "%.2f", monthSpend)} ${bal.currency}", style = MaterialTheme.typography.bodySmall,
                                    color = if (monthRatio > 0.8f) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            ProgressBar(
                                ratio = monthRatio,
                                color = when {
                                    monthRatio > 0.8f -> Color(0xFFEF5350)
                                    monthRatio > 0.5f -> Color(0xFFFFA726)
                                    else -> Color(0xFF42A5F5)
                                },
                                modifier = Modifier.fillMaxWidth().height(6.dp)
                            )
                        }
                    }
                }
            }

            // Rate limit windows
            if (hasWindows) {
                if (hasBalance) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.quota_rate_limits), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.height(8.dp))

                provider.windows.forEach { window ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                window.kind.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${stringResource(R.string.quota_used_format, window.usedPercent)}  ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        window.usedPercent > 80 -> Color(0xFFEF5350)
                                        window.usedPercent > 50 -> Color(0xFFFFA726)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    stringResource(R.string.quota_left_format, window.remainingPercent),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        window.remainingPercent < 20 -> Color(0xFFEF5350)
                                        window.remainingPercent < 50 -> Color(0xFFFFA726)
                                        else -> Color(0xFF4CAF50)
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ProgressBar(
                            ratio = (window.usedPercent / 100.0).toFloat().coerceIn(0f, 1f),
                            color = when {
                                window.usedPercent > 80 -> Color(0xFFEF5350)
                                window.usedPercent > 50 -> Color(0xFFFFA726)
                                else -> Color(0xFF66BB6A)
                            },
                            modifier = Modifier.fillMaxWidth().height(12.dp)
                        )
                        window.resetsAt?.let { resetTime ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                stringResource(R.string.quota_resets_format, formatTime(resetTime, LocalContext.current)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

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

    Card(modifier = Modifier.fillMaxWidth().clickable { showDetail = true }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.deviceId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                device.platform?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                (device.allTime ?: device.periods?.allTime)?.let {
                    Text(stringResource(R.string.devices_total_format, formatNumber(it.totalTokens), formatCost(it.costUsd)), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                }
                device.updatedAt?.let { updated ->
                    Text(stringResource(R.string.devices_last_seen_format, formatTime(updated, LocalContext.current)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    // Detail dialog
    if (showDetail) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            title = { Text(device.deviceId) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val today = device.today ?: device.periods?.today
                    val month = device.month ?: device.periods?.month
                    val allTime = device.allTime ?: device.periods?.allTime
                    today?.let {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.period_today), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text("${formatNumber(it.totalTokens)} · ${formatCost(it.costUsd)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    month?.let {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.period_month), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text("${formatNumber(it.totalTokens)} · ${formatCost(it.costUsd)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    allTime?.let {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.period_all_time), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text("${formatNumber(it.totalTokens)} · ${formatCost(it.costUsd)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDetail = false }) { Text(stringResource(R.string.session_close)) } }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.device_delete_title)) },
            text = { Text(stringResource(R.string.device_delete_message, device.deviceId)) },
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
fun ModelsTab(allTime: UsagePeriod?) {
    // Use allTime only — periods are nested (allTime includes month, month includes today),
    // so merging them would double/triple-count token values.
    val models = allTime?.models ?: emptyMap()
    val modelCosts = allTime?.modelCosts ?: emptyMap()
    val modelOutputs = allTime?.modelOutputs ?: emptyMap()
    val modelCacheReads = allTime?.modelCacheReads ?: emptyMap()

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
    "claude" to "Claude Code", "codex" to "Codex", "hermes" to "Hermes",
    "gemini" to "Gemini", "cursor" to "Cursor", "opencode" to "OpenCode",
    "openclaw" to "OpenClaw", "antigravity" to "Antigravity", "cline" to "Cline",
    "kimi" to "Kimi", "qwen" to "Qwen", "grok" to "Grok Build",
    "copilot" to "GitHub Copilot", "pi" to "Pi", "zed" to "Zed",
    "kilocode" to "Kilo Code", "micode" to "MiMo Code", "zcode" to "ZCode",
    "openai" to "OpenAI", "anthropic" to "Anthropic", "google" to "Google",
    "bigmodel" to "BigModel", "zhipu" to "Zhipu GLM",
    "bailian" to "Ali Bailian", "mimo" to "Xiaomi Mimo", "qwen" to "Qwen",
    "xai" to "xAI", "mistral" to "Mistral", "cohere" to "Cohere",
    "openrouter" to "OpenRouter", "groq" to "Groq", "together" to "Together",
    "deepseek" to "DeepSeek", "xai" to "xAI", "meta" to "Meta",
    "mistral" to "Mistral", "moonshot" to "Moonshot", "zai" to "Z.AI",
    "cohere" to "Cohere", "xiaomi" to "Xiaomi", "minimax" to "MiniMax"
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
    "zed" to Color(0xFF4173E7), "kilocode" to Color(0xFFF8F676),
    "micode" to Color(0xFF000000), "zcode" to Color(0xFF000000),
    "moonshot" to Color(0xFF16191E), "zai" to Color(0xFF000000),
    "cohere" to Color(0xFF39594D), "xiaomi" to Color(0xFFFF6700),
    "minimax" to Color(0xFFF23F5D),
    "openai" to Color(0xFF49A3B0), "anthropic" to Color(0xFFCC7C5E),
    "google" to Color(0xFF4285F4), "bigmodel" to Color(0xFF3B7CFF), "zhipu" to Color(0xFF3B7CFF),
    "bailian" to Color(0xFFFF6A00), "mimo" to Color(0xFFFF6700),
    "qwen" to Color(0xFF615CED), "openrouter" to Color(0xFF6E40C9),
    "groq" to Color(0xFFF86624), "together" to Color(0xFF0F9AF0),
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
    "zed" to R.raw.ic_tool_zed, "kilocode" to R.raw.ic_tool_kilocode,
    "micode" to R.raw.ic_tool_xiaomi, "zcode" to R.raw.ic_tool_zai,
    "deepseek" to R.raw.ic_tool_deepseek, "xai" to R.raw.ic_tool_xai,
    "meta" to R.raw.ic_tool_meta, "mistral" to R.raw.ic_tool_mistral,
    "moonshot" to R.raw.ic_tool_moonshot, "zai" to R.raw.ic_tool_zai,
    "cohere" to R.raw.ic_tool_cohere, "xiaomi" to R.raw.ic_tool_xiaomi,
    "minimax" to R.raw.ic_tool_minimax,
    "openai" to R.raw.ic_tool_codex,
    "anthropic" to R.raw.ic_tool_claude, "google" to R.raw.ic_tool_gemini,
    "qwen" to R.raw.ic_tool_qwen, "xai" to R.raw.ic_tool_xai,
    "mistral" to R.raw.ic_tool_mistral, "cohere" to R.raw.ic_tool_cohere,
    "bigmodel" to R.raw.ic_tool_bigmodel,
    "zhipu" to R.raw.ic_tool_bigmodel
)

private fun toolIconRes(id: String): Int? = TOOL_ICONS[id]

@Composable
internal fun ToolAvatar(tool: String, label: String, color: Color, size: androidx.compose.ui.unit.Dp) {
    val iconRes = toolIconRes(tool)
    if (iconRes != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(iconRes)
                .decoderFactory(SvgDecoder.Factory())
                .build(),
            contentDescription = label,
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier.size(size)
        )
    } else {
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
                    Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(session.client.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                Text(formatNumber(session.totalTokens), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                session.sessionId.takeLast(40),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.sessions_msgs_cost_format, session.messageCount, formatCost(session.costUsd)),
                    style = MaterialTheme.typography.labelSmall
                )
                session.lastUsedAt?.let { last ->
                    Text(formatTime(last, LocalContext.current), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SessionDetailDialog(session: SessionInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    session.client.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    session.sessionId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

                // Models
                if (!session.models.isNullOrEmpty()) {
                    SectionBlock(stringResource(R.string.session_models_used)) {
                        session.models.entries.sortedByDescending { it.value }.forEach { (model, tokens) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(model, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(formatNumber(tokens), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    session.modelCosts?.get(model)?.let { cost ->
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
    val daily = history?.daily
    val monthly = history?.monthly
    val summary = history?.summary

    if (daily.isNullOrEmpty() && monthly.isNullOrEmpty()) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (summary != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.trends_summary), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(stringResource(R.string.trends_total_tokens), formatNumber(summary.totalTokens), Modifier.weight(1f))
                        StatItem(stringResource(R.string.trends_total_cost), formatCost(summary.totalCost), Modifier.weight(1f))
                        StatItem(stringResource(R.string.trends_active_days), summary.activeDays.toString(), Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItem(stringResource(R.string.trends_current_streak), "${summary.currentStreak}d", Modifier.weight(1f))
                        StatItem(stringResource(R.string.trends_longest_streak), "${summary.longestStreak}d", Modifier.weight(1f))
                        StatItem(stringResource(R.string.trends_peak_day), formatNumber(summary.peakDayTokens), Modifier.weight(1f))
                    }
                }
            }
        }

        if (!daily.isNullOrEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.trends_activity), style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    ActivityGrid(
                        days = daily,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

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
