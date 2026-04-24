package com.glassfiles.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.Strings
import com.glassfiles.data.github.GHCommit
import com.glassfiles.data.github.GHCompareResult
import com.glassfiles.data.github.GHDiffFile
import com.glassfiles.data.github.GitHubManager
import com.glassfiles.ui.theme.Blue
import com.glassfiles.ui.theme.SeparatorColor
import com.glassfiles.ui.theme.SurfaceLight
import com.glassfiles.ui.theme.SurfaceWhite
import com.glassfiles.ui.theme.TextPrimary
import com.glassfiles.ui.theme.TextSecondary
import com.glassfiles.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
internal fun CompareCommitsScreen(
    repoOwner: String,
    repoName: String,
    initialBase: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var baseBranch by remember { mutableStateOf(initialBase) }
    var headBranch by remember { mutableStateOf("") }
    var branches by remember { mutableStateOf<List<String>>(emptyList()) }
    var compareResult by remember { mutableStateOf<GHCompareResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showDiff by remember { mutableStateOf(false) }
    var showCreatePr by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        branches = GitHubManager.getBranches(context, repoOwner, repoName)
    }

    LaunchedEffect(branches, initialBase) {
        if (branches.isEmpty()) return@LaunchedEffect
        if (baseBranch.isBlank()) baseBranch = initialBase.takeIf { it in branches } ?: branches.first()
        if (headBranch.isBlank()) headBranch = branches.firstOrNull { it != baseBranch }.orEmpty()
    }

    val result = compareResult
    if (showDiff && result != null) {
        DiffViewerScreen(
            title = "$headBranch into $baseBranch",
            subtitle = "${result.files.size} files changed",
            files = result.files,
            totalAdditions = result.files.sumOf { it.additions },
            totalDeletions = result.files.sumOf { it.deletions },
            onBack = { showDiff = false }
        )
        return
    }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        GHTopBar(
            title = "Compare",
            subtitle = "$repoOwner/$repoName",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = {
                        val oldBase = baseBranch
                        baseBranch = headBranch
                        headBranch = oldBase
                        compareResult = null
                    },
                    enabled = baseBranch.isNotBlank() && headBranch.isNotBlank()
                ) {
                    Icon(Icons.Rounded.SwapHoriz, null, Modifier.size(20.dp), tint = Blue)
                }
            }
        )

        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CompareSelectorCard(
                branches = branches,
                baseBranch = baseBranch,
                headBranch = headBranch,
                loading = loading,
                onBaseChange = {
                    baseBranch = it
                    compareResult = null
                    if (headBranch == it) headBranch = branches.firstOrNull { branch -> branch != it }.orEmpty()
                },
                onHeadChange = {
                    headBranch = it
                    compareResult = null
                },
                onCompare = {
                    if (baseBranch.isNotBlank() && headBranch.isNotBlank() && baseBranch != headBranch) {
                        loading = true
                        scope.launch {
                            compareResult = GitHubManager.compareCommits(context, repoOwner, repoName, baseBranch, headBranch)
                            loading = false
                            if (compareResult == null) Toast.makeText(context, Strings.error, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            if (result == null) {
                CompareEmptyState()
            } else {
                CompareResultPanel(
                    result = result,
                    baseBranch = baseBranch,
                    headBranch = headBranch,
                    onOpenDiff = { showDiff = true },
                    onCreatePr = { showCreatePr = true },
                    onOpenWeb = {
                        if (result.htmlUrl.isNotBlank()) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.htmlUrl)))
                            } catch (_: Exception) {
                                Toast.makeText(context, Strings.error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }

    if (showCreatePr && result != null) {
        CreateComparePRDialog(
            repoOwner = repoOwner,
            repoName = repoName,
            baseBranch = baseBranch,
            headBranch = headBranch,
            result = result,
            onDismiss = { showCreatePr = false },
            onCreated = {
                showCreatePr = false
                Toast.makeText(context, Strings.done, Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun CompareSelectorCard(
    branches: List<String>,
    baseBranch: String,
    headBranch: String,
    loading: Boolean,
    onBaseChange: (String) -> Unit,
    onHeadChange: (String) -> Unit,
    onCompare: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Compare branches", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        BranchSelectorDropdown(branches, baseBranch, onBaseChange, "Base")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.ArrowForward, null, Modifier.size(18.dp), tint = TextTertiary)
        }
        BranchSelectorDropdown(branches, headBranch, onHeadChange, "Compare")
        Button(
            onClick = onCompare,
            enabled = branches.isNotEmpty() && baseBranch.isNotBlank() && headBranch.isNotBlank() && baseBranch != headBranch && !loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
            else {
                Icon(Icons.Rounded.CompareArrows, null, Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Compare")
            }
        }
    }
}

@Composable
private fun CompareEmptyState() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Choose two branches", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text("Use compare before creating a PR to see commits and changed files.", fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun CompareResultPanel(
    result: GHCompareResult,
    baseBranch: String,
    headBranch: String,
    onOpenDiff: () -> Unit,
    onCreatePr: () -> Unit,
    onOpenWeb: () -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompareMetric("${result.aheadBy}", "ahead", Color(0xFF34C759))
                CompareMetric("${result.behindBy}", "behind", Color(0xFFFF3B30))
                CompareMetric("${result.totalCommits}", "commits", Blue)
                CompareMetric("${result.files.size}", "files", TextSecondary)
            }
            Text(
                compareStatusText(result.status, baseBranch, headBranch),
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenDiff, enabled = result.files.isNotEmpty()) {
                    Icon(Icons.Rounded.Visibility, null, Modifier.size(16.dp), tint = Blue)
                    Spacer(Modifier.width(4.dp))
                    Text("View diff", color = Blue, fontSize = 12.sp)
                }
                TextButton(onClick = onCreatePr, enabled = result.aheadBy > 0) {
                    Icon(Icons.Rounded.MergeType, null, Modifier.size(16.dp), tint = Color(0xFF34C759))
                    Spacer(Modifier.width(4.dp))
                    Text("Create PR", color = Color(0xFF34C759), fontSize = 12.sp)
                }
                TextButton(onClick = onOpenWeb, enabled = result.htmlUrl.isNotBlank()) {
                    Icon(Icons.Rounded.OpenInNew, null, Modifier.size(16.dp), tint = TextSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text("GitHub", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (result.commits.isNotEmpty()) {
                item {
                    Text("Commits", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                items(result.commits.take(8)) { commit ->
                    CompareCommitRow(commit)
                }
                if (result.commits.size > 8) {
                    item {
                        Text("+${result.commits.size - 8} more commits", fontSize = 11.sp, color = TextTertiary, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }

            item {
                Text("Changed files", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            if (result.files.isEmpty()) {
                item {
                    Text("No file changes", fontSize = 13.sp, color = TextTertiary, modifier = Modifier.padding(4.dp))
                }
            } else {
                items(result.files) { file ->
                    CompareFileCard(file)
                }
            }
        }
    }
}

@Composable
private fun CompareMetric(value: String, label: String, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.1f)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = color, fontSize = 11.sp)
    }
}

@Composable
private fun CompareCommitRow(commit: GHCommit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceWhite).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(commit.message.lineSequence().firstOrNull().orEmpty(), fontSize = 13.sp, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(commit.sha.take(7), fontSize = 11.sp, color = Blue, fontWeight = FontWeight.Medium)
            if (commit.author.isNotBlank()) Text(commit.author, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun CompareFileCard(file: GHDiffFile) {
    val statusColor = diffStatusColor(file.status)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceWhite).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
            Text(file.filename, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(file.status.replaceFirstChar { it.uppercase() }, fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.Medium)
            Text("+${file.additions}", fontSize = 11.sp, color = Color(0xFF34C759))
            Text("-${file.deletions}", fontSize = 11.sp, color = Color(0xFFFF3B30))
            if (file.patch.isBlank()) {
                Icon(Icons.Rounded.Description, null, Modifier.size(12.dp), tint = TextTertiary)
                Text("No patch preview", fontSize = 11.sp, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun CreateComparePRDialog(
    repoOwner: String,
    repoName: String,
    baseBranch: String,
    headBranch: String,
    result: GHCompareResult,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("Merge $headBranch into $baseBranch") }
    var body by remember {
        mutableStateOf(
            buildString {
                appendLine("Compare: $headBranch into $baseBranch")
                appendLine()
                appendLine("${result.totalCommits} commits, ${result.files.size} files changed")
                result.commits.take(10).forEach { appendLine("- ${it.message.lineSequence().firstOrNull().orEmpty()} (${it.sha.take(7)})") }
            }.trim()
        )
    }
    var creating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        title = { Text("Create pull request") },
        text = {
            Column(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$headBranch -> $baseBranch", fontSize = 12.sp, color = TextSecondary)
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(body, { body = it }, label = { Text("Description") }, minLines = 6, maxLines = 10, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = !creating && title.isNotBlank(),
                onClick = {
                    creating = true
                    scope.launch {
                        val ok = GitHubManager.createPullRequest(context, repoOwner, repoName, title, body, headBranch, baseBranch)
                        creating = false
                        if (ok) onCreated() else Toast.makeText(context, Strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                if (creating) CircularProgressIndicator(Modifier.size(16.dp), color = Blue, strokeWidth = 2.dp)
                else {
                    Icon(Icons.Rounded.Add, null, Modifier.size(16.dp), tint = Blue)
                    Spacer(Modifier.width(4.dp))
                    Text("Create", color = Blue)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) { Text(Strings.cancel) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BranchSelectorDropdown(
    branches: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    placeholder: String
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Blue, unfocusedBorderColor = SeparatorColor)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(SurfaceWhite)) {
            branches.forEach { branch ->
                DropdownMenuItem(
                    text = { Text(branch, fontSize = 13.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onSelect(branch)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun diffStatusColor(status: String): Color = when (status.lowercase(Locale.US)) {
    "added" -> Color(0xFF34C759)
    "removed" -> Color(0xFFFF3B30)
    "modified" -> Color(0xFFFF9500)
    "renamed" -> Color(0xFF5856D6)
    else -> TextSecondary
}

private fun compareStatusText(status: String, baseBranch: String, headBranch: String): String = when (status) {
    "identical" -> "$baseBranch and $headBranch are identical."
    "ahead" -> "$headBranch has commits that can be merged into $baseBranch."
    "behind" -> "$headBranch is behind $baseBranch."
    "diverged" -> "$baseBranch and $headBranch have diverged."
    else -> status.ifBlank { "Comparison loaded." }
}
