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
import com.glassfiles.data.github.GHDiscussion
import com.glassfiles.data.github.GHComment
import com.glassfiles.data.github.GitHubManager
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.launch

@Composable
internal fun DiscussionsScreen(
    repoOwner: String,
    repoName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var discussions by remember { mutableStateOf<List<GHDiscussion>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedDiscussion by remember { mutableStateOf<GHDiscussion?>(null) }

    LaunchedEffect(repoOwner, repoName) {
        discussions = GitHubManager.getDiscussions(context, repoOwner, repoName)
        loading = false
    }

    if (selectedDiscussion != null) {
        DiscussionDetailScreen(
            repoOwner = repoOwner,
            repoName = repoName,
            discussion = selectedDiscussion!!,
            onBack = { selectedDiscussion = null }
        )
        return
    }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        GHTopBar(
            title = "Discussions",
            subtitle = "$repoOwner/$repoName",
            onBack = onBack
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
        } else if (discussions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No discussions", fontSize = 14.sp, color = TextTertiary)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(discussions) { discussion ->
                    DiscussionCard(discussion) { selectedDiscussion = discussion }
                }
            }
        }
    }
}

@Composable
private fun DiscussionCard(discussion: GHDiscussion, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).clickable(onClick = onClick).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Forum, null, Modifier.size(20.dp), tint = Blue)
            Text(discussion.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 2)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(discussion.author, fontSize = 12.sp, color = Blue)
            Text(discussion.createdAt.take(10), fontSize = 11.sp, color = TextTertiary)
            Text("${discussion.comments} comments", fontSize = 11.sp, color = TextSecondary)
        }
        if (discussion.body.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(discussion.body.take(120), fontSize = 12.sp, color = TextTertiary, maxLines = 2)
        }
    }
}

@Composable
private fun DiscussionDetailScreen(
    repoOwner: String,
    repoName: String,
    discussion: GHDiscussion,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var comments by remember { mutableStateOf<List<GHComment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(discussion.number) {
        comments = GitHubManager.getDiscussionComments(context, repoOwner, repoName, discussion.number)
        loading = false
    }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        GHTopBar(
            title = discussion.title,
            subtitle = "#${discussion.number} · ${discussion.comments} comments",
            onBack = onBack
        )

        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Discussion body
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(discussion.author, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Blue)
                        Text(discussion.createdAt.take(10), fontSize = 11.sp, color = TextTertiary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(discussion.body, fontSize = 13.sp, color = TextPrimary, lineHeight = 20.sp)
                }
            }

            // Comments
            if (loading) {
                item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Blue, modifier = Modifier.size(24.dp), strokeWidth = 2.dp) } }
            } else {
                items(comments) { comment ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceWhite).padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(comment.author, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Blue)
                            Text(comment.createdAt.take(10), fontSize = 10.sp, color = TextTertiary)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(comment.body, fontSize = 13.sp, color = TextPrimary, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}
