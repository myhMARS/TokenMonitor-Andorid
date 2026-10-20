package com.tokenmonitor.app.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tokenmonitor.app.R
import com.tokenmonitor.app.data.ApiKeyStorage
import com.tokenmonitor.app.data.model.ApiKeyEntry
import com.tokenmonitor.app.data.model.PlatformKeys
import com.tokenmonitor.app.data.model.PlatformsData
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.time.LocalDate

// One shared client: validations run concurrently, so creating and closing an
// HttpClient per key would add connection setup cost and churn TLS state.
private val validationClient by lazy {
    HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 20_000
        }
    }
}

private data class PendingKeyDelete(
    val platform: String,
    val index: Int,
    val name: String,
    val key: String
)

private var autoValidated = false
private val persistedResults = mutableMapOf<String, String>()
private val persistedValidating = mutableSetOf<String>()
private var validationVersion by mutableIntStateOf(0)

@Composable
private fun KeyStatusChip(
    result: String?,
    validating: Boolean,
    onClick: (() -> Unit)?
) {
    val label = when {
        validating -> stringResource(R.string.apikeys_status_checking)
        result == "valid" -> stringResource(R.string.apikeys_status_valid)
        result?.startsWith("invalid") == true -> stringResource(R.string.apikeys_status_invalid)
        result?.startsWith("error") == true -> stringResource(R.string.apikeys_status_error)
        else -> stringResource(R.string.apikeys_status_unverified)
    }
    val color = when {
        validating -> MaterialTheme.colorScheme.onSurfaceVariant
        result == "valid" -> Color(0xFF4CAF50)
        result?.startsWith("invalid") == true -> MaterialTheme.colorScheme.error
        result?.startsWith("error") == true -> Color(0xFFFFA726)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (validating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = color
                )
            } else if (result == "valid" || result?.startsWith("invalid") == true || result?.startsWith("error") == true) {
                Icon(
                    if (result == "valid") Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
            }
            if (validating || result == "valid" || result?.startsWith("invalid") == true || result?.startsWith("error") == true) {
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ApiKeysTab(keyAction: String? = null, onKeyActionHandled: () -> Unit = {}, importedData: String? = null, onExportData: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    val storage = remember { ApiKeyStorage(context) }
    var data by remember {
        mutableStateOf(try { storage.load() } catch (_: Exception) { PlatformsData() })
    }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<PendingKeyDelete?>(null) }
    var expandedPlatforms by remember { mutableStateOf(data.platforms.keys) }
    var showImportConfirm by remember { mutableStateOf<String?>(null) }

    fun persist(d: PlatformsData) { data = d; storage.save(d) }

    val invalidJsonMsg = stringResource(R.string.apikeys_invalid_json)
    val copiedMsg = stringResource(R.string.apikeys_copied)
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    // Read version counter to subscribe to validation state changes from coroutines
    @Suppress("UNUSED_EXPRESSION")
    val _v = validationVersion

    data class PlatformDef(
        val id: String, val name: String,
        val baseUrl: String, val verifyUrl: String = "", val balanceUrl: String = "",
        val authHeader: String = "Bearer {api_key}",
        val authHeaderName: String = "Authorization",
        val extraHeaders: List<Pair<String, String>> = emptyList(),
        val usePost: Boolean = false,
    ) {
        val validationUrl: String get() = when {
            balanceUrl.isNotBlank() -> baseUrl + balanceUrl
            verifyUrl.isNotBlank() -> baseUrl + verifyUrl
            else -> ""
        }
    }

    val knownPlatforms = listOf(
        PlatformDef("deepseek", "DeepSeek", "https://api.deepseek.com", balanceUrl = "/user/balance"),
        PlatformDef("openai", "OpenAI", "https://api.openai.com", verifyUrl = "/v1/models"),
        PlatformDef("bailian", "Ali Bailian", "https://dashscope.aliyuncs.com/compatible-mode", verifyUrl = "/v1/models"),
        PlatformDef("mimo", "Xiaomi Mimo", "https://api.xiaomimimo.com", verifyUrl = "/v1/models"),
        PlatformDef("moonshot", "Moonshot", "https://api.moonshot.cn", balanceUrl = "/v1/users/me/balance"),
        PlatformDef("zhipu", "Zhipu GLM", "https://open.bigmodel.cn/api/paas/v4", verifyUrl = "/models", authHeader = "{api_key}"),
        PlatformDef("minimax", "MiniMax", "https://api.minimaxi.com", verifyUrl = "/v1/models"),
        PlatformDef("anthropic", "Anthropic", "https://api.anthropic.com", verifyUrl = "/v1/models",
            authHeaderName = "x-api-key", authHeader = "{api_key}",
            extraHeaders = listOf("anthropic-version" to "2023-06-01")),
        PlatformDef("google", "Google Gemini", "https://generativelanguage.googleapis.com", verifyUrl = "/v1beta/models",
            authHeaderName = "x-goog-api-key", authHeader = "{api_key}"),
        PlatformDef("qwen", "Qwen", "https://dashscope.aliyuncs.com/compatible-mode", verifyUrl = "/v1/models"),
        PlatformDef("xai", "xAI", "https://api.x.ai", verifyUrl = "/v1/models"),
        PlatformDef("mistral", "Mistral", "https://api.mistral.ai", verifyUrl = "/v1/models"),
        PlatformDef("cohere", "Cohere", "https://api.cohere.ai", verifyUrl = "/v1/models"),
        PlatformDef("groq", "Groq", "https://api.groq.com/openai", verifyUrl = "/v1/models"),
        PlatformDef("together", "Together", "https://api.together.xyz", verifyUrl = "/v1/models"),
        PlatformDef("openrouter", "OpenRouter", "https://openrouter.ai/api", verifyUrl = "/v1/models"),
    )

    suspend fun validateKeySuspend(platform: PlatformDef, key: String, name: String, force: Boolean = false) {
        val tag = "${platform.id}:${key.take(16)}"
        if (!force && (tag in persistedValidating || tag in persistedResults)) return
        if (platform.validationUrl.isEmpty()) return
        // Clear previous result on retry
        if (force) { persistedResults.remove(tag); validationVersion++ }
        persistedValidating += tag
        validationVersion++
        val requestHeaders = buildList {
            add(platform.authHeaderName to platform.authHeader.replace("{api_key}", key))
            platform.extraHeaders.forEach { (name, value) ->
                add(name to value.replace("{api_key}", key))
            }
        }
        val result = try {
            val resp = withContext(Dispatchers.IO) {
                if (platform.usePost) {
                    validationClient.post(platform.validationUrl) {
                        requestHeaders.forEach { (name, value) -> header(name, value) }
                        contentType(ContentType.Application.Json)
                        setBody("""{"model":"gpt-3.5-turbo","messages":[{"role":"user","content":"hi"}],"max_tokens":1}""")
                    }
                } else {
                    validationClient.get(platform.validationUrl) {
                        requestHeaders.forEach { (name, value) -> header(name, value) }
                    }
                }
            }
            when {
                resp.status.value == 200 -> "valid"
                resp.status.value == 401 -> "invalid: 401 Unauthorized"
                resp.status.value == 403 -> "invalid: 403 Forbidden"
                resp.status.value == 404 -> "error: 404 Not Found"
                else -> "error: HTTP ${resp.status.value}"
            }
        } catch (e: Exception) {
            when {
                e.message?.contains("timeout", true) == true -> "error: timeout"
                e.message?.contains("resolve", true) == true -> "error: DNS"
                e.message?.contains("connect", true) == true -> "error: connection refused"
                else -> "error: ${e.message?.take(40) ?: "unknown"}"
            }
        }
        persistedResults[tag] = result
        persistedValidating -= tag
        validationVersion++
    }

    // Non-suspend wrapper for button click handlers (force = true for retry)
    fun validateKey(platform: PlatformDef, key: String, name: String) {
        scope.launch { validateKeySuspend(platform, key, name, force = true) }
    }

    // Import data from external file picker (DashboardScreen handles ActivityResult)
    LaunchedEffect(importedData) {
        val raw = importedData ?: return@LaunchedEffect
        showImportConfirm = raw
    }

    // React to keyAction from parent dropdown
    LaunchedEffect(keyAction) {
        when (keyAction) {
            "add" -> showAddDialog = true
            "export" -> onExportData?.invoke(storage.export())
        }
        if (keyAction != null) onKeyActionHandled()
    }

    // Auto-validate all known-platform keys once per session
    if (!autoValidated) {
        autoValidated = true
        LaunchedEffect(Unit) {
            scope.launch {
                // Different providers are independent, so validate up to four at
                // a time instead of serialising every request behind the previous
                // one. The cap keeps a batch from turning into a burst.
                val limiter = Semaphore(4)
                coroutineScope {
                    data.platforms.flatMap { (platformId, pkeys) ->
                        val pDef = knownPlatforms.find { it.id == platformId.lowercase() }
                            ?: return@flatMap emptyList<Pair<PlatformDef, ApiKeyEntry>>()
                        if (pDef.validationUrl.isEmpty()) {
                            return@flatMap emptyList<Pair<PlatformDef, ApiKeyEntry>>()
                        }
                        pkeys.keys.map { entry -> pDef to entry }
                    }.map { (pDef, entry) ->
                        async {
                            limiter.withPermit {
                                validateKeySuspend(pDef, entry.key, entry.name)
                            }
                        }
                    }.awaitAll()
                }
            }
        }
    }

    val filteredPlatforms = data.platforms.entries.filter { (platform, pkeys) ->
        if (searchQuery.isBlank()) true
        else platform.contains(searchQuery, true) || pkeys.keys.any { it.name.contains(searchQuery, true) }
    }

    // Dialogs
    showImportConfirm?.let { jsonText ->
        AlertDialog(
            onDismissRequest = { showImportConfirm = null },
            title = { Text(stringResource(R.string.apikeys_import_title)) },
            text = { Text(stringResource(R.string.apikeys_import_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    try { persist(storage.import(jsonText)) } catch (_: Exception) { Toast.makeText(context, invalidJsonMsg, Toast.LENGTH_SHORT).show() }
                    showImportConfirm = null
                }) { Text(stringResource(R.string.apikeys_import_btn)) }
            },
            dismissButton = { TextButton(onClick = { showImportConfirm = null }) { Text(stringResource(R.string.apikeys_cancel)) } }
        )
    }

    showDeleteConfirm?.let { pending ->
        val keyPreview = "${pending.key.take(8)}…${pending.key.takeLast(4)}"
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(stringResource(R.string.apikeys_delete_title)) },
            text = { Text(stringResource(R.string.apikeys_delete_msg, "${pending.name} · $keyPreview", pending.platform)) },
            confirmButton = {
                TextButton(onClick = {
                    val updated = data.platforms.toMutableMap()
                    val pkeys = updated[pending.platform]
                    if (pkeys != null) {
                        val remaining = pkeys.keys.toMutableList()
                        // Entries can share a name; remove exactly the clicked one.
                        // Fall back to the stored key if the list moved while the
                        // dialog was open.
                        val index = if (pending.index in remaining.indices &&
                            remaining[pending.index].key == pending.key
                        ) {
                            pending.index
                        } else {
                            remaining.indexOfFirst { it.key == pending.key && it.name == pending.name }
                        }
                        if (index >= 0) remaining.removeAt(index)
                        if (remaining.isEmpty()) {
                            updated.remove(pending.platform)
                        } else {
                            updated[pending.platform] = PlatformKeys(remaining)
                        }
                    }
                    persist(PlatformsData(updated))
                    showDeleteConfirm = null
                }) { Text(stringResource(R.string.apikeys_delete_btn), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text(stringResource(R.string.apikeys_cancel)) } }
        )
    }

    val totalKeys = data.platforms.values.sumOf { it.keys.size }
    var searchExpanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(searchExpanded) {
        if (searchExpanded) searchFocus.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabSummary(stringResource(R.string.apikeys_summary_format, data.platforms.size, totalKeys))
        if (data.platforms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Key, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.apikeys_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.apikeys_empty_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (filteredPlatforms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.apikeys_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPlatforms) { (platform, pkeys) ->
                    val isExpanded = platform in expandedPlatforms
                    val validCount = pkeys.keys.count {
                        persistedResults["$platform:${it.key.take(16)}"] == "valid"
                    }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedPlatforms = if (isExpanded) expandedPlatforms - platform else expandedPlatforms + platform
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ToolAvatar(tool = platform, label = toolLabel(platform), color = toolColor(platform), size = 28.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        toolLabel(platform),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        if (validCount > 0) {
                                            stringResource(R.string.apikeys_keys_valid_format, pkeys.keys.size, validCount)
                                        } else {
                                            stringResource(R.string.apikeys_keys_format, pkeys.keys.size)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (isExpanded) {
                                pkeys.keys.forEachIndexed { entryIndex, entry ->
                                    if (entryIndex > 0) {
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))
                                    }
                                    val vTag = "$platform:${entry.key.take(16)}"
                                    val vResult = persistedResults[vTag]
                                    val validating = vTag in persistedValidating
                                    val pDef = knownPlatforms.find { it.id == platform.lowercase() }
                                        ?.takeIf { it.validationUrl.isNotEmpty() }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                entry.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                entry.key.take(12) + "\u2026" + entry.key.takeLast(4),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!vResult.isNullOrBlank() && vResult != "valid") {
                                                Text(
                                                    vResult,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (vResult.startsWith("error")) Color(0xFFFFA726) else MaterialTheme.colorScheme.error,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        KeyStatusChip(
                                            result = vResult,
                                            validating = validating,
                                            onClick = pDef?.let { def ->
                                                { validateKey(def, entry.key, entry.name) }
                                            }
                                        )
                                        IconButton(onClick = {
                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cb.setPrimaryClip(ClipData.newPlainText("API Key", entry.key))
                                            Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.ContentCopy, stringResource(R.string.apikeys_copy_desc), modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = {
                                            showDeleteConfirm = PendingKeyDelete(platform, entryIndex, entry.name, entry.key)
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, stringResource(R.string.apikeys_delete_desc), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .imePadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = searchExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 3.dp,
                        shadowElevation = 6.dp
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocus),
                            placeholder = { Text(stringResource(R.string.apikeys_search_hint)) },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, stringResource(R.string.apikeys_clear_search))
                                    }
                                }
                            } else {
                                null
                            },
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = menuExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            menuExpanded = false
                            searchExpanded = true
                        },
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        text = { Text(stringResource(R.string.apikeys_action_search)) },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    ExtendedFloatingActionButton(
                        onClick = {
                            menuExpanded = false
                            showAddDialog = true
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.apikeys_action_add)) },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                }
            }
            FloatingActionButton(onClick = {
                if (searchExpanded) {
                    searchExpanded = false
                    searchQuery = ""
                    focusManager.clearFocus()
                } else {
                    menuExpanded = !menuExpanded
                }
            }) {
                Icon(
                    if (menuExpanded || searchExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = stringResource(
                        if (menuExpanded || searchExpanded) R.string.apikeys_search_close else R.string.apikeys_actions_open
                    )
                )
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        var addPlatform by remember { mutableStateOf(knownPlatforms[0]) }
        var addName by remember { mutableStateOf("") }
        var addKey by remember { mutableStateOf("") }
        var keyValid by remember { mutableStateOf(true) }
        var platformDropdown by remember { mutableStateOf(false) }
        var isCustomPlatform by remember { mutableStateOf(false) }
        var customPlatform by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.apikeys_add_title)) },
            text = {
                Column {
                    // Platform dropdown / custom input
                    if (isCustomPlatform) {
                        OutlinedTextField(customPlatform, { customPlatform = it }, label = { Text(stringResource(R.string.apikeys_platform_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = { isCustomPlatform = false }) { Text("← Select from list") }
                    } else {
                        Box {
                            OutlinedTextField(
                                value = addPlatform.name,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.apikeys_platform_label)) },
                                trailingIcon = { Text("▾") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { platformDropdown = true })
                            DropdownMenu(expanded = platformDropdown, onDismissRequest = { platformDropdown = false }) {
                                Column(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                                    knownPlatforms.forEach { p ->
                                        DropdownMenuItem(text = { Text(p.name) }, onClick = { addPlatform = p; platformDropdown = false })
                                    }
                                    DropdownMenuItem(text = { Text("Other…", color = MaterialTheme.colorScheme.primary) }, onClick = { platformDropdown = false; isCustomPlatform = true })
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(addName, { addName = it }, label = { Text(stringResource(R.string.apikeys_name_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(addKey, { addKey = it; keyValid = addKey.length >= 8 || addKey.isEmpty() }, label = { Text(stringResource(R.string.apikeys_key_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (!keyValid) Text(stringResource(R.string.apikeys_key_too_short), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val platformId = if (isCustomPlatform) customPlatform.lowercase().replace(" ", "") else addPlatform.id
                    if (platformId.isBlank() || addName.isBlank() || addKey.length < 8) return@TextButton
                    val updated = data.platforms.toMutableMap()
                    val existing = updated[platformId]
                    val entry = ApiKeyEntry(name = addName, created_at = LocalDate.now().toString(), key = addKey)
                    updated[platformId] = if (existing != null) PlatformKeys(existing.keys + entry) else PlatformKeys(listOf(entry))
                    persist(PlatformsData(updated))
                    expandedPlatforms = expandedPlatforms + platformId
                    showAddDialog = false
                    // Check the new key immediately so the row shows a result
                    // without waiting for the next session's batch validation.
                    knownPlatforms.find { it.id == platformId.lowercase() }
                        ?.takeIf { it.validationUrl.isNotEmpty() }
                        ?.let { pDef ->
                            scope.launch {
                                validateKeySuspend(pDef, entry.key, entry.name, force = true)
                            }
                        }
                }) { Text(stringResource(R.string.apikeys_add_btn)) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.apikeys_cancel)) } }
        )
    }
}
