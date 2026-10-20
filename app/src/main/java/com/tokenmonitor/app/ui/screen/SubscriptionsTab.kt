package com.tokenmonitor.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.tokenmonitor.app.R
import com.tokenmonitor.app.data.model.SubscriptionRecord
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun SubscriptionsTab(
    subscriptions: List<SubscriptionRecord>,
    updatedAt: String,
    isLoading: Boolean,
    onSave: (SubscriptionRecord) -> Unit,
    onDelete: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var editing by remember { mutableStateOf<SubscriptionRecord?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SubscriptionRecord?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabSummaryRow(
            text = stringResource(R.string.subscriptions_summary_format, subscriptions.size),
            trailing = {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.subscriptions_refresh))
                }
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.subscriptions_add))
                }
            }
        )

        if (isLoading && subscriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (subscriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                EmptyTabState(
                    Icons.Default.Payments,
                    stringResource(R.string.subscriptions_empty),
                    stringResource(R.string.subscriptions_empty_hint)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SubscriptionSummary(subscriptions) }
                items(subscriptions.sortedWith(compareBy({ it.provider }, { it.planName }))) { record ->
                    SubscriptionCard(
                        record = record,
                        onEdit = { editing = record },
                        onDelete = { pendingDelete = record }
                    )
                }
                if (updatedAt.isNotBlank()) {
                    item {
                        Text(
                            stringResource(R.string.subscriptions_updated_format, formatDateTimeShort(updatedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAdd || editing != null) {
        SubscriptionDialog(
            initial = editing,
            onDismiss = {
                showAdd = false
                editing = null
            },
            onSave = {
                onSave(it)
                showAdd = false
                editing = null
            }
        )
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.subscriptions_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.subscriptions_delete_message,
                        providerLabel(record.provider),
                        record.planName.takeIf { it.isNotBlank() } ?: "-"
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(record.id)
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.subscriptions_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.subscriptions_cancel))
                }
            }
        )
    }
}

@Composable
private fun SubscriptionSummary(subscriptions: List<SubscriptionRecord>) {
    val monthKey = LocalDate.now().toString().take(7)
    val monthly = subscriptions
        .filter { it.kind != "topup" && it.amountMinor > 0 }
        .groupBy { it.currency.uppercase(Locale.US) }
        .mapValues { (_, records) -> records.sumOf { monthlyMinor(it) } }
        .filterValues { it > 0 }
    val topUps = subscriptions
        .flatMap { record ->
            record.topUps.orEmpty().map { record.currency.uppercase(Locale.US) to it }
        }
        .filter { (_, topUp) -> topUp.date.startsWith(monthKey) }
        .groupBy({ it.first }, { it.second.amountMinor })
        .mapValues { (_, amounts) -> amounts.sum() }
        .filterValues { it > 0 }

    if (monthly.isEmpty() && topUps.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (monthly.isNotEmpty()) {
                Text(
                    stringResource(R.string.subscriptions_monthly_total),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                monthly.entries.sortedBy { it.key }.forEach { (currency, minor) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(currencyLabel(currency), style = MaterialTheme.typography.bodySmall)
                        Text(formatMinor(minor, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (topUps.isNotEmpty()) {
                if (monthly.isNotEmpty()) Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.subscriptions_month_topups),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                topUps.entries.sortedBy { it.key }.forEach { (currency, minor) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(currencyLabel(currency), style = MaterialTheme.typography.bodySmall)
                        Text(formatMinor(minor, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    record: SubscriptionRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isTopUp = record.kind == "topup"
    val renewal = if (isTopUp) null else nextRenewalDate(record, LocalDate.now())
    val topUps = record.topUps.orEmpty()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ToolAvatar(
                    tool = record.provider,
                    label = providerLabel(record.provider),
                    color = toolColor(record.provider),
                    size = 28.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        providerLabel(record.provider),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val subtitle = listOfNotNull(
                        record.planName.takeIf { it.isNotBlank() },
                        record.binding?.accountEmail?.takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!isTopUp) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.subscriptions_edit_title),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.subscriptions_delete_confirm),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(amountLabel(record), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (!isTopUp) {
                    val renewLabel = if (record.autoRenew && renewal != null) {
                        stringResource(R.string.subscriptions_renews_format, renewal.toString())
                    } else {
                        stringResource(R.string.subscriptions_cancelled)
                    }
                    Text(
                        renewLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (record.autoRenew) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                }
            }

            val dates = buildList {
                if (record.startDate.isNotBlank()) {
                    add(stringResource(R.string.subscriptions_starts_format, record.startDate))
                }
                record.endDate?.takeIf { it.isNotBlank() }?.let {
                    add(stringResource(R.string.subscriptions_ends_format, it))
                }
            }
            if (dates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    dates.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (record.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(record.note, style = MaterialTheme.typography.bodySmall)
            }

            if (topUps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.subscriptions_topups_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                topUps.take(6).forEach { topUp ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(topUp.date, style = MaterialTheme.typography.bodySmall)
                        Text(formatMinor(topUp.amountMinor, record.currency), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.subscriptions_topup_total),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        formatMinor(topUps.sumOf { it.amountMinor }, record.currency),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isTopUp) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.subscriptions_topup_view_only),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun amountLabel(record: SubscriptionRecord): String {
    if (record.kind == "topup") {
        return stringResource(R.string.subscriptions_topup_label)
    }
    val amount = formatMinor(record.amountMinor, record.currency)
    val unit = if (record.interval == "year") {
        stringResource(R.string.subscriptions_year)
    } else {
        stringResource(R.string.subscriptions_month)
    }
    val count = record.intervalCount.coerceAtLeast(1)
    return if (count == 1) {
        stringResource(R.string.subscriptions_amount_format, amount, unit)
    } else {
        stringResource(R.string.subscriptions_amount_count_format, amount, count, unit)
    }
}

@Composable
private fun SubscriptionDialog(
    initial: SubscriptionRecord?,
    onDismiss: () -> Unit,
    onSave: (SubscriptionRecord) -> Unit
) {
    var provider by remember { mutableStateOf(initial?.provider ?: "") }
    var planName by remember { mutableStateOf(initial?.planName ?: "") }
    var amount by remember { mutableStateOf(initial?.let { formatPlainAmount(it.amountMinor) } ?: "") }
    var currency by remember { mutableStateOf(initial?.currency?.uppercase(Locale.US) ?: "USD") }
    var interval by remember { mutableStateOf(initial?.interval ?: "month") }
    var intervalCount by remember { mutableStateOf((initial?.intervalCount ?: 1).toString()) }
    var startDate by remember {
        mutableStateOf(initial?.startDate?.takeIf { it.isNotBlank() } ?: LocalDate.now().toString())
    }
    var autoRenew by remember { mutableStateOf(initial?.autoRenew ?: true) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var currencyMenu by remember { mutableStateOf(false) }
    var intervalMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val currencies = listOf("USD", "HKD", "CNY", "TWD")
    val invalidAmount = stringResource(R.string.subscriptions_invalid_amount)
    val invalidDate = stringResource(R.string.subscriptions_invalid_date)
    val providerRequired = stringResource(R.string.subscriptions_provider_required)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.subscriptions_add_title else R.string.subscriptions_edit_title
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text(stringResource(R.string.subscriptions_provider)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it },
                    label = { Text(stringResource(R.string.subscriptions_plan)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.subscriptions_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { currencyMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.subscriptions_currency_format, currency))
                    }
                    DropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                        currencies.forEach { code ->
                            DropdownMenuItem(
                                text = { Text(code) },
                                onClick = {
                                    currency = code
                                    currencyMenu = false
                                }
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { intervalMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(
                                R.string.subscriptions_interval_format,
                                if (interval == "year") stringResource(R.string.subscriptions_year)
                                else stringResource(R.string.subscriptions_month)
                            )
                        )
                    }
                    DropdownMenu(expanded = intervalMenu, onDismissRequest = { intervalMenu = false }) {
                        listOf("month", "year").forEach { value ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (value == "year") stringResource(R.string.subscriptions_year)
                                        else stringResource(R.string.subscriptions_month)
                                    )
                                },
                                onClick = {
                                    interval = value
                                    intervalMenu = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = intervalCount,
                    onValueChange = { intervalCount = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.subscriptions_interval_count)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text(stringResource(R.string.subscriptions_start_date)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.subscriptions_auto_renew),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = autoRenew, onCheckedChange = { autoRenew = it })
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.subscriptions_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minor = parseAmountMinor(amount)
                if (minor == null) {
                    error = invalidAmount
                    return@TextButton
                }
                if (!isValidDate(startDate)) {
                    error = invalidDate
                    return@TextButton
                }
                if (provider.isBlank()) {
                    error = providerRequired
                    return@TextButton
                }
                val base = initial ?: SubscriptionRecord(id = newSubscriptionId(), kind = "subscription")
                onSave(
                    base.copy(
                        provider = provider.trim().lowercase(Locale.US),
                        planName = planName.trim(),
                        amountMinor = minor,
                        currency = currency,
                        interval = interval,
                        intervalCount = intervalCount.toIntOrNull()?.coerceIn(1, 24) ?: 1,
                        startDate = startDate.trim(),
                        autoRenew = autoRenew,
                        note = note.trim(),
                        updatedAt = Instant.now().toString()
                    )
                )
            }) {
                Text(stringResource(R.string.subscriptions_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.subscriptions_cancel))
            }
        }
    )
}

private fun monthlyMinor(record: SubscriptionRecord): Long {
    val months = if (record.interval == "year") {
        record.intervalCount.coerceAtLeast(1) * 12
    } else {
        record.intervalCount.coerceAtLeast(1)
    }
    return record.amountMinor / months
}

private fun nextRenewalDate(record: SubscriptionRecord, today: LocalDate): LocalDate? {
    if (record.kind == "topup" || !record.autoRenew) return null
    val override = record.nextRenewalOverride?.takeIf { it.isNotBlank() }?.let {
        try {
            LocalDate.parse(it)
        } catch (_: DateTimeParseException) {
            null
        }
    }
    if (override != null && !override.isBefore(today)) return override
    val anchor = try {
        LocalDate.parse(record.startDate)
    } catch (_: DateTimeParseException) {
        return null
    }
    val step = if (record.interval == "year") {
        record.intervalCount.coerceAtLeast(1) * 12L
    } else {
        record.intervalCount.coerceAtLeast(1).toLong()
    }
    var periods = 0L
    var candidate = anchor
    while (candidate.isBefore(today) && periods < 2400) {
        periods += 1
        candidate = anchor.plusMonths(periods * step)
    }
    if (candidate.isBefore(today)) return null
    return candidate
}

private fun parseAmountMinor(text: String): Long? {
    val normalized = text.trim().replace(",", ".")
    if (normalized.isEmpty()) return null
    val value = normalized.toDoubleOrNull() ?: return null
    if (value < 0 || value.isNaN() || value.isInfinite()) return null
    return (value * 100).roundToLong()
}

private fun formatPlainAmount(minor: Long): String = String.format(Locale.US, "%.2f", minor / 100.0)

private fun isValidDate(text: String): Boolean = try {
    LocalDate.parse(text.trim())
    true
} catch (_: DateTimeParseException) {
    false
}

private fun newSubscriptionId(): String =
    "sub_" + System.currentTimeMillis() + "_" + (1000..9999).random()

private fun formatMinor(minor: Long, currency: String): String {
    val code = currency.trim().uppercase(Locale.US)
    val number = String.format(Locale.US, "%.2f", minor / 100.0)
    return when (code) {
        "USD" -> "\$$number"
        "HKD" -> "HK\$$number"
        "CNY" -> "¥$number"
        "TWD" -> "NT\$$number"
        else -> "$code $number"
    }
}

private fun currencyLabel(currency: String): String = when (currency.uppercase(Locale.US)) {
    "USD" -> "USD"
    "HKD" -> "HKD"
    "CNY" -> "CNY"
    "TWD" -> "TWD"
    else -> currency
}

private fun formatDateTimeShort(iso: String): String = try {
    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(java.time.ZoneId.systemDefault())
        .format(Instant.parse(iso))
} catch (_: Exception) {
    iso.take(16).replace("T", " ")
}
