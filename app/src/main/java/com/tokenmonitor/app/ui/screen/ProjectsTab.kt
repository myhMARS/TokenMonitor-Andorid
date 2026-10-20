@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tokenmonitor.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tokenmonitor.app.R
import com.tokenmonitor.app.data.model.ProjectRollup
import com.tokenmonitor.app.data.model.UsagePeriod
import java.util.Locale

@Composable
fun ProjectsTab(
    today: UsagePeriod?,
    month: UsagePeriod?,
    allTime: UsagePeriod?,
    projectsIncomplete: Boolean = false
) {
    var selected by remember { mutableIntStateOf(2) }
    val period = when (selected) {
        0 -> today
        1 -> month
        else -> allTime
    }
    val projects = period?.projects.orEmpty()
    val totalTokens = period?.totalTokens ?: 0L

    Column(modifier = Modifier.fillMaxSize()) {
        if (projects.isNotEmpty()) {
            TabSummary(
                stringResource(
                    R.string.projects_summary_format,
                    projects.size,
                    formatCompactNumber(totalTokens),
                    projectFormatCost(projects.values.sumOf { it.costUsd })
                )
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(R.string.period_today, R.string.period_month, R.string.period_all_time)
                .forEachIndexed { index, res ->
                    FilterChip(
                        selected = selected == index,
                        onClick = { selected = index },
                        label = { Text(stringResource(res)) }
                    )
                }
        }

        if (projectsIncomplete) {
            Text(
                stringResource(R.string.projects_incomplete),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                EmptyTabState(
                    Icons.Default.Folder,
                    stringResource(R.string.projects_empty),
                    stringResource(R.string.projects_empty_hint)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects.entries.sortedByDescending { it.value.tokens }.toList()) { (key, project) ->
                    ProjectCard(project = project, fallbackLabel = key, totalTokens = totalTokens)
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: ProjectRollup, fallbackLabel: String, totalTokens: Long) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    project.label.ifBlank { fallbackLabel }.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    projectFormatCost(project.costUsd),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatCompactNumber(project.tokens) + " tokens",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (totalTokens > 0) {
                    Text(
                        stringResource(
                            R.string.projects_share_format,
                            project.tokens * 100.0 / totalTokens
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (totalTokens > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (project.tokens.toFloat() / totalTokens).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    strokeCap = StrokeCap.Butt,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }

            val clients = project.clients.orEmpty()
            if (clients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    stringResource(R.string.projects_tools_title),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                clients.entries.sortedByDescending { it.value }.take(4).forEach { (client, tokens) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToolAvatar(client, toolLabel(client), toolColor(client), 16.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            toolLabel(client),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            formatCompactNumber(tokens),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            val models = ModelAliasRegistry.groupModels(project.models)
            if (models.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.projects_models_title),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                models.entries.sortedByDescending { it.value }.take(5).forEach { (model, tokens) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            model,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            formatCompactNumber(tokens),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (project.unpricedTokens > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    stringResource(R.string.projects_unpriced_hint, formatCompactNumber(project.unpricedTokens)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}


private fun projectFormatCost(value: Double): String = "$" + String.format(Locale.US, "%.2f", value)
