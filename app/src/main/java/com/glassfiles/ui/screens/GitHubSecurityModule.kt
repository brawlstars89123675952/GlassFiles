package com.glassfiles.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.github.GHDependabotAlert
import com.glassfiles.data.github.GHRuleset
import com.glassfiles.data.github.GitHubManager
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.launch

private val RULESET_FILTERS = listOf("all", "active", "evaluate", "disabled")
private val ALERT_SEVERITIES = listOf("all", "critical", "high", "medium", "low")
private val ALERT_STATES = listOf("open", "fixed", "dismissed", "all")

@Composable
internal fun RulesetsScreen(
    repoOwner: String,
    repoName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rulesets by remember { mutableStateOf<List<GHRuleset>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var enforcementFilter by remember { mutableStateOf("all") }

    fun loadRulesets() {
        loading = true
        scope.launch {
            rulesets = GitHubManager.getRulesets(context, repoOwner, repoName)
            loading = false
        }
    }

    LaunchedEffect(repoOwner, repoName) { loadRulesets() }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        GHTopBar(
            title = "Rulesets",
            subtitle = "$repoOwner/$repoName",
            onBack = onBack,
            actions = {
                IconButton(onClick = { loadRulesets() }, enabled = !loading) {
                    Icon(Icons.Rounded.Refresh, null, Modifier.size(20.dp), tint = Blue)
                }
            }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
        } else {
            val visibleRulesets = rulesets.filter { ruleset ->
                (enforcementFilter == "all" || ruleset.enforcement.equals(enforcementFilter, ignoreCase = true)) &&
                    (query.isBlank() ||
                        ruleset.name.contains(query, ignoreCase = true) ||
                        ruleset.target.contains(query, ignoreCase = true) ||
                        ruleset.sourceType.contains(query, ignoreCase = true))
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { RulesetsSummaryCard(rulesets) }
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search rulesets") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = TextSecondary) }
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        RULESET_FILTERS.forEach { filter ->
                            GitHubSmallChoice(label = filter.replaceFirstChar { it.uppercase() }, selected = enforcementFilter == filter) {
                                enforcementFilter = filter
                            }
                        }
                    }
                }
                items(visibleRulesets, key = { it.id }) { ruleset ->
                    RulesetCard(ruleset) {
                        openGitHubSecurityUrl(context, ruleset.htmlUrl)
                    }
                }
                if (visibleRulesets.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(if (rulesets.isEmpty()) "No rulesets configured" else "No matching rulesets", fontSize = 14.sp, color = TextTertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RulesetsSummaryCard(rulesets: List<GHRuleset>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Rule, null, Modifier.size(20.dp), tint = Blue)
            Text("Repository rules", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            Text("${rulesets.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            SecurityPill("Active ${rulesets.count { it.enforcement.equals("active", true) }}", Color(0xFF34C759))
            SecurityPill("Evaluate ${rulesets.count { it.enforcement.equals("evaluate", true) }}", Color(0xFFFF9500))
            SecurityPill("Disabled ${rulesets.count { it.enforcement.equals("disabled", true) }}", TextTertiary)
        }
    }
}

@Composable
private fun RulesetCard(ruleset: GHRuleset, onOpen: () -> Unit) {
    val enforcementColor = rulesetColor(ruleset.enforcement)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Rule, null, Modifier.size(20.dp), tint = enforcementColor)
            Column(Modifier.weight(1f)) {
                Text(ruleset.name.ifBlank { "Ruleset #${ruleset.id}" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(cleanJoin(listOf(ruleset.target, ruleset.sourceType)), fontSize = 11.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onOpen, enabled = ruleset.htmlUrl.isNotBlank()) {
                Icon(Icons.Rounded.OpenInNew, null, Modifier.size(18.dp), tint = Blue)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            SecurityPill(ruleset.enforcement.ifBlank { "unknown" }, enforcementColor)
            SecurityPill("${ruleset.rulesCount} rules", TextSecondary)
            if (ruleset.updatedAt.isNotBlank()) SecurityPill("Updated ${ruleset.updatedAt.take(10)}", TextTertiary)
        }
    }
}

@Composable
internal fun SecurityScreen(
    repoOwner: String,
    repoName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf<List<GHDependabotAlert>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var severityFilter by remember { mutableStateOf("all") }
    var stateFilter by remember { mutableStateOf("open") }

    fun loadAlerts() {
        loading = true
        scope.launch {
            alerts = GitHubManager.getDependabotAlerts(context, repoOwner, repoName)
            loading = false
        }
    }

    LaunchedEffect(repoOwner, repoName) { loadAlerts() }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        GHTopBar(
            title = "Security",
            subtitle = "$repoOwner/$repoName · Dependabot",
            onBack = onBack,
            actions = {
                IconButton(onClick = { loadAlerts() }, enabled = !loading) {
                    Icon(Icons.Rounded.Refresh, null, Modifier.size(20.dp), tint = Blue)
                }
            }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
        } else {
            val visibleAlerts = alerts.filter { alert ->
                (severityFilter == "all" || alert.severity.equals(severityFilter, ignoreCase = true)) &&
                    (stateFilter == "all" || alert.state.equals(stateFilter, ignoreCase = true)) &&
                    (query.isBlank() ||
                        alert.packageName.contains(query, ignoreCase = true) ||
                        alert.summary.contains(query, ignoreCase = true) ||
                        alert.ecosystem.contains(query, ignoreCase = true) ||
                        alert.manifestPath.contains(query, ignoreCase = true) ||
                        alert.ghsaId.contains(query, ignoreCase = true) ||
                        alert.cveId.contains(query, ignoreCase = true))
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { SecuritySummaryCard(alerts) }
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search package, advisory or manifest") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = TextSecondary) }
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        ALERT_STATES.forEach { state ->
                            GitHubSmallChoice(label = state.replaceFirstChar { it.uppercase() }, selected = stateFilter == state) {
                                stateFilter = state
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        ALERT_SEVERITIES.forEach { severity ->
                            GitHubSmallChoice(label = severity.replaceFirstChar { it.uppercase() }, selected = severityFilter == severity) {
                                severityFilter = severity
                            }
                        }
                    }
                }
                items(visibleAlerts, key = { it.number }) { alert ->
                    AlertCard(alert) {
                        openGitHubSecurityUrl(context, alert.htmlUrl)
                    }
                }
                if (visibleAlerts.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(if (alerts.isEmpty()) "No Dependabot alerts" else "No matching alerts", fontSize = 14.sp, color = TextTertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecuritySummaryCard(alerts: List<GHDependabotAlert>) {
    val open = alerts.count { it.state.equals("open", true) }
    val criticalHigh = alerts.count { it.severity.equals("critical", true) || it.severity.equals("high", true) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Security, null, Modifier.size(20.dp), tint = if (criticalHigh > 0) Color(0xFFFF3B30) else Blue)
            Text("Dependabot alerts", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            Text("$open open", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            SecurityPill("Critical ${alerts.count { it.severity.equals("critical", true) }}", Color(0xFFFF3B30))
            SecurityPill("High ${alerts.count { it.severity.equals("high", true) }}", Color(0xFFFF3B30))
            SecurityPill("Medium ${alerts.count { it.severity.equals("medium", true) }}", Color(0xFFFF9500))
            SecurityPill("Low ${alerts.count { it.severity.equals("low", true) }}", Color(0xFF34C759))
        }
    }
}

@Composable
private fun AlertCard(alert: GHDependabotAlert, onOpen: () -> Unit) {
    val severityColor = alertSeverityColor(alert.severity)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Security, null, Modifier.size(20.dp), tint = severityColor)
            Column(Modifier.weight(1f)) {
                Text(alert.packageName.ifBlank { "Dependency alert #${alert.number}" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(cleanJoin(listOf(alert.ecosystem, alert.manifestPath)), fontSize = 11.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onOpen, enabled = alert.htmlUrl.isNotBlank()) {
                Icon(Icons.Rounded.OpenInNew, null, Modifier.size(18.dp), tint = Blue)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            SecurityPill(alert.severity.ifBlank { "unknown" }, severityColor)
            SecurityPill(alert.state.ifBlank { "unknown" }, alertStateColor(alert.state))
            alert.ghsaId.takeIf { it.isNotBlank() }?.let { SecurityPill(it, TextSecondary) }
            alert.cveId.takeIf { it.isNotBlank() }?.let { SecurityPill(it, TextSecondary) }
        }
        if (alert.summary.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(alert.summary, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        val detailLines = listOfNotNull(
            alert.vulnerableRequirements.takeIf { it.isNotBlank() }?.let { "Requires $it" },
            alert.fixedIn.takeIf { it.isNotEmpty() }?.joinToString(", ")?.let { "Fixed in $it" },
            alert.updatedAt.takeIf { it.isNotBlank() }?.take(10)?.let { "Updated $it" }
        )
        if (detailLines.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(detailLines.joinToString(" · "), fontSize = 11.sp, color = TextTertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun GitHubSmallChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (selected) Blue.copy(alpha = 0.14f) else SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, color = if (selected) Blue else TextSecondary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun SecurityPill(label: String, color: Color) {
    Text(
        label,
        fontSize = 11.sp,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private fun rulesetColor(enforcement: String): Color = when (enforcement.lowercase()) {
    "active" -> Color(0xFF34C759)
    "evaluate" -> Color(0xFFFF9500)
    "disabled" -> TextTertiary
    else -> TextSecondary
}

private fun alertSeverityColor(severity: String): Color = when (severity.lowercase()) {
    "critical", "high" -> Color(0xFFFF3B30)
    "medium" -> Color(0xFFFF9500)
    "low" -> Color(0xFF34C759)
    else -> TextSecondary
}

private fun alertStateColor(state: String): Color = when (state.lowercase()) {
    "open" -> Color(0xFFFF3B30)
    "fixed" -> Color(0xFF34C759)
    "dismissed" -> TextTertiary
    else -> TextSecondary
}

private fun cleanJoin(values: List<String>): String =
    values.filter { it.isNotBlank() && it != "null" }.joinToString(" · ").ifBlank { "Repository" }

private fun openGitHubSecurityUrl(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    } catch (_: Exception) {
    }
}
