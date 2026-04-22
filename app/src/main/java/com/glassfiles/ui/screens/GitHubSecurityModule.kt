package com.glassfiles.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.github.GHRuleset
import com.glassfiles.data.github.GHDependabotAlert
import com.glassfiles.data.github.GitHubManager
import com.glassfiles.ui.theme.*

@Composable
internal fun RulesetsScreen(
    repoOwner: String,
    repoName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var rulesets by remember { mutableStateOf<List<GHRuleset>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(repoOwner, repoName) {
        rulesets = GitHubManager.getRulesets(context, repoOwner, repoName)
        loading = false
    }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        GHTopBar(
            title = "Rulesets",
            subtitle = "$repoOwner/$repoName",
            onBack = onBack
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
        } else if (rulesets.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No rulesets configured", fontSize = 14.sp, color = TextTertiary)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(rulesets) { ruleset ->
                    RulesetCard(ruleset)
                }
            }
        }
    }
}

@Composable
private fun RulesetCard(ruleset: GHRuleset) {
    val enforcementColor = when (ruleset.enforcement.lowercase()) {
        "active" -> Color(0xFF34C759)
        "evaluate" -> Color(0xFFFF9500)
        "disabled" -> TextTertiary
        else -> TextSecondary
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Rule, null, Modifier.size(20.dp), tint = enforcementColor)
            Text(ruleset.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Enforcement: ${ruleset.enforcement}", fontSize = 12.sp, color = enforcementColor, fontWeight = FontWeight.Medium)
            Text("${ruleset.rulesCount} rules", fontSize = 12.sp, color = TextSecondary)
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
    var alerts by remember { mutableStateOf<List<GHDependabotAlert>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(repoOwner, repoName) {
        alerts = GitHubManager.getDependabotAlerts(context, repoOwner, repoName)
        loading = false
    }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        GHTopBar(
            title = "Security",
            subtitle = "$repoOwner/$repoName · Dependabot",
            onBack = onBack
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
        } else if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No Dependabot alerts", fontSize = 14.sp, color = TextTertiary)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alerts) { alert ->
                    AlertCard(alert)
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: GHDependabotAlert) {
    val severityColor = when (alert.severity.lowercase()) {
        "critical" -> Color(0xFFFF3B30)
        "high" -> Color(0xFFFF3B30)
        "medium" -> Color(0xFFFF9500)
        "low" -> Color(0xFF34C759)
        else -> TextSecondary
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Security, null, Modifier.size(20.dp), tint = severityColor)
            Text(alert.packageName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(alert.severity.uppercase(), fontSize = 11.sp, color = severityColor, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(severityColor.copy(0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(alert.summary, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        if (alert.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(alert.description, fontSize = 12.sp, color = TextSecondary, maxLines = 3)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("State: ${alert.state}", fontSize = 11.sp, color = TextTertiary)
            Text(alert.createdAt.take(10), fontSize = 11.sp, color = TextTertiary)
        }
    }
}
