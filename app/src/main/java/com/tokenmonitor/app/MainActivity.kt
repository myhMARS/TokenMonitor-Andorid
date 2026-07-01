package com.tokenmonitor.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tokenmonitor.app.data.LocaleManager
import com.tokenmonitor.app.ui.DashboardViewModel
import com.tokenmonitor.app.ui.screen.DashboardScreen
import com.tokenmonitor.app.ui.screen.SetupScreen
import com.tokenmonitor.app.ui.theme.TokenMointorTheme

class MainActivity : ComponentActivity() {

    private val localeManager by lazy { LocaleManager(this) }

    override fun attachBaseContext(newBase: Context) {
        val savedLocale = LocaleManager.getSavedLocale(newBase)
        val ctx = if (savedLocale.isNotEmpty()) {
            LocaleManager.wrap(newBase, savedLocale)
        } else {
            newBase
        }
        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TokenMointorTheme {
                TokenMonitorApp(localeManager = localeManager)
            }
        }
    }
}

@Composable
fun TokenMonitorApp(
    localeManager: LocaleManager,
    viewModel: DashboardViewModel = viewModel()
) {
    var localeKey by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()

    // API key import/export (must be at this level for ActivityResultRegistryOwner)
    var importedApiData by remember { mutableStateOf<String?>(null) }
    var pendingExportData by remember { mutableStateOf<String?>(null) }
    val appCtx = androidx.compose.ui.platform.LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { appCtx.contentResolver.openInputStream(it)?.use { s -> importedApiData = s.bufferedReader().readText() } }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val data = pendingExportData
        if (uri != null && data != null) {
            appCtx.contentResolver.openOutputStream(uri)?.use { it.write(data.toByteArray()) }
        }
        pendingExportData = null
    }

    // Force fresh context on locale change by wrapping with localeKey
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val wrappedCtx = remember(localeKey) {
        val saved = LocaleManager.getSavedLocale(ctx)
        if (saved.isNotEmpty()) LocaleManager.wrap(ctx, saved) else ctx
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalContext provides wrappedCtx
    ) {
        if (!uiState.isConnected) {
            SetupScreen(
                hubUrl = uiState.hubUrl,
                hubSecret = uiState.hubSecret,
                isConnecting = uiState.isConnecting,
                connectionError = uiState.connectionError,
                onUrlChange = { viewModel.updateHubUrl(it) },
                onSecretChange = { viewModel.updateHubSecret(it) },
                onConnect = { viewModel.connect() }
            )
        } else {
            DashboardScreen(
                today = uiState.today,
                month = uiState.month,
                allTime = uiState.allTime,
                limits = uiState.limits,
                devices = uiState.devices,
                sessions = uiState.sessions,
                history = uiState.history,
                deviceCount = uiState.deviceCount,
                hubVersion = uiState.hubVersion,
                hubNow = uiState.hubNow,
                selectedTab = uiState.selectedTab,
                isRefreshing = uiState.isRefreshing,
                deletingDeviceId = uiState.deletingDeviceId,
                snackbarMessage = uiState.snackbarMessage,
                onRefresh = { viewModel.refresh() },
                onDisconnect = { viewModel.disconnect() },
                onDeleteDevice = { viewModel.deleteDevice(it) },
                onClearSnackbar = { viewModel.clearSnackbar() },
                onSwitchLanguage = {
                    val current = LocaleManager.getSavedLocale(ctx)
                    val next = if (current == LocaleManager.LOCALE_ZH) LocaleManager.LOCALE_EN else LocaleManager.LOCALE_ZH
                    localeManager.setLocale(next)
                    localeKey++
                },
                importedApiData = importedApiData,
                onImportRequest = { importLauncher.launch(arrayOf("application/json")) },
                onExportData = { data -> pendingExportData = data; exportLauncher.launch("api_keys.json") },
                localeKey = localeKey
            )
        }
    }
}
