package com.tokenmonitor.app.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private var autoValidated = false
private val persistedResults = mutableMapOf<String, String>()
private val persistedValidating = mutableSetOf<String>()
private var validationVersion by mutableIntStateOf(0)

@Composable
fun ApiKeysTab(keyAction: String? = null, onKeyActionHandled: () -> Unit = {}, importedData: String? = null, onExportData: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    val storage = remember { ApiKeyStorage(context) }
    var data by remember {
        mutableStateOf(try { storage.load() } catch (_: Exception) { PlatformsData() })
    }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Pair<String, String>?>(null) }
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
        PlatformDef("anthropic", "Anthropic", "https://api.anthropic.com", verifyUrl = "/v1/models"),
        PlatformDef("google", "Google Gemini", "https://generativelanguage.googleapis.com", verifyUrl = "/v1beta/models"),
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
        val authValue = platform.authHeader.replace("{api_key}", key)
        val client = HttpClient()
        val result = try {
            val resp = withContext(Dispatchers.IO) {
                if (platform.usePost) {
                    client.post(platform.validationUrl) {
                        header("Authorization", authValue)
                        contentType(ContentType.Application.Json)
                        setBody("""{"model":"gpt-3.5-turbo","messages":[{"role":"user","content":"hi"}],"max_tokens":1}""")
                    }
                } else {
                    client.get(platform.validationUrl) { header("Authorization", authValue) }
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
        }.also { client.close() }
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
            data.platforms.forEach { (platformId, pkeys) ->
                val pDef = knownPlatforms.find { it.id == platformId.lowercase() } ?: return@forEach
                if (pDef.validationUrl.isEmpty()) return@forEach
                pkeys.keys.forEach { entry ->
                    validateKeySuspend(pDef, entry.key, entry.name)
                }
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

    showDeleteConfirm?.let { (platform, keyName) ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(stringResource(R.string.apikeys_delete_title)) },
            text = { Text(stringResource(R.string.apikeys_delete_msg, keyName, platform)) },
            confirmButton = {
                TextButton(onClick = {
                    val updated = data.platforms.toMutableMap()
                    val pkeys = updated[platform]
                    if (pkeys != null) {
                        val f = pkeys.keys.filter { it.name != keyName }
                        if (f.isEmpty()) updated.remove(platform) else updated[platform] = PlatformKeys(f)
                    }
                    persist(PlatformsData(updated))
                    showDeleteConfirm = null
                }) { Text(stringResource(R.string.apikeys_delete_btn), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text(stringResource(R.string.apikeys_cancel)) } }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.apikeys_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )

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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPlatforms) { (platform, pkeys) ->
                    val isExpanded = platform in expandedPlatforms
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    expandedPlatforms = if (isExpanded) expandedPlatforms - platform else expandedPlatforms + platform
                                }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ToolAvatar(tool = platform, label = platform, color = toolColor(platform), size = 24.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(platform, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(stringResource(R.string.apikeys_keys_format, pkeys.keys.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(if (isExpanded) "▾" else "▸", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (isExpanded) {
                                pkeys.keys.forEach { entry ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(entry.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                            Text(entry.key.take(12) + "…" + entry.key.takeLast(4), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            val vTag = "$platform:${entry.key.take(16)}"
                                            val vResult = persistedResults[vTag]
                                            if (vResult != null && vResult != "valid") {
                                                Text(vResult, style = MaterialTheme.typography.labelSmall, color = when {
                                                    vResult.startsWith("error") -> Color(0xFFFFA726)
                                                    else -> MaterialTheme.colorScheme.error
                                                }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        IconButton(onClick = {
                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cb.setPrimaryClip(ClipData.newPlainText("API Key", entry.key))
                                            Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.ContentCopy, stringResource(R.string.apikeys_copy_desc), modifier = Modifier.size(16.dp))
                                        }
                                        // Validate button — only for known platforms
                                        val pDef = knownPlatforms.find { it.id == platform.lowercase() }
                                        if (pDef != null && pDef.validationUrl.isNotEmpty()) {
                                            val vTag = "$platform:${entry.key.take(16)}"
                                            val vResult = persistedResults[vTag]
                                            IconButton(onClick = { validateKey(pDef, entry.key, entry.name) }, modifier = Modifier.size(32.dp)) {
                                                if (vTag in persistedValidating) {
                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                } else when {
                                                    vResult == "valid" -> Icon(Icons.Default.CheckCircle, vResult, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                                    vResult != null && vResult.startsWith("error") -> Icon(Icons.Default.Error, vResult, tint = Color(0xFFFFA726), modifier = Modifier.size(16.dp))
                                                    vResult != null -> Icon(Icons.Default.Error, vResult, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                    else -> Icon(Icons.Default.CheckCircle, "Validate", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        IconButton(onClick = { showDeleteConfirm = Pair(platform, entry.name) }, modifier = Modifier.size(32.dp)) {
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
                    showAddDialog = false
                }) { Text(stringResource(R.string.apikeys_add_btn)) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.apikeys_cancel)) } }
        )
    }
}
