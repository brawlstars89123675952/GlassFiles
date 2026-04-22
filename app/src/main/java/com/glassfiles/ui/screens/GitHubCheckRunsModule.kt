package com.glassfiles.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.glassfiles.data.github.GHCheckRun
import com.glassfiles.data.github.GitHubManager
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.launch

@Composable
internal fun CheckRunsScreen(
    repoOwner: String,
    repoName: String,
    ref: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checkRuns by remember { mutableStateOf<List<GHCheckRun>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(repoOwner, repoName, ref) {
        checkRuns = GitHubManager.getPullRequestCheckRuns(context, repoOwner, repoName, ref)
        loading = false
    }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        GHTopBar(
            title = "Check Runs",
            subtitle = "$repoOwner/$repoName · $ref",
            onBack = onBack
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
        } else if (checkRuns.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No check runs", fontSize = 14.sp, color = TextTertiary)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(checkRuns) { run ->
                    CheckRunCard(run)
                }
            }
        }
    }
}

@Composable
private fun CheckRunCard(run: GHCheckRun) {
    val statusColor = when (run.conclusion.lowercase()) {
        "success" -> Color(0xFF34C759)
        "failure" -> Color(0xFFFF3B30)
        "cancelled" -> Color(0xFFFF9500)
        "skipped" -> TextTertiary
        else -> when (run.status.lowercase()) {
            "in_progress" -> Blue
            "queued" -> Color(0xFFFFCC00)
            else -> TextSecondary
        }
    }

    val statusIcon = when (run.conclusion.lowercase()) {
        "success" -> Icons.Rounded.CheckCircle
        "failure" -> Icons.Rounded.Error
        "cancelled" -> Icons.Rounded.Cancel
        else -> when (run.status.lowercase()) {
            "in_progress" -> Icons.Rounded.HourglassTop
            "queued" -> Icons.Rounded.Schedule
            else -> Icons.Rounded.Help
        }
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(statusIcon, null, Modifier.size(20.dp), tint = statusColor)
            Text(run.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(run.status.replaceFirstChar { it.uppercase() }, fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.Medium)
        }

        if (run.outputTitle.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(run.outputTitle, fontSize = 12.sp, color = TextSecondary)
        }

        if (run.outputSummary.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(run.outputSummary, fontSize = 11.sp, color = TextTertiary, maxLines = 3)
        }

        if (run.conclusion.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("Conclusion: ${run.conclusion.replaceFirstChar { it.uppercase() }}", fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.Medium)
        }
    }
}
