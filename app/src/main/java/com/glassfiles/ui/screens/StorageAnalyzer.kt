package com.glassfiles.ui.screens
import com.glassfiles.data.Strings

import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.ui.components.*
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class StorageCategory(val name: String, val size: Long, val color: Color, val count: Int)

@Composable
fun StorageAnalyzerScreen(onBack: () -> Unit) {
    var categories by remember { mutableStateOf(listOf<StorageCategory>()) }
    var largestFiles by remember { mutableStateOf(listOf<Pair<File, Long>>()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalUsed by remember { mutableStateOf(0L) }; var totalFree by remember { mutableStateOf(0L) }; var totalSize by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) { withContext(Dispatchers.IO) {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        totalSize = stat.totalBytes; totalFree = stat.availableBytes; totalUsed = totalSize - totalFree
        val byType = mutableMapOf<String, Pair<Long, Int>>(); val large = mutableListOf<Pair<File, Long>>()
        Environment.getExternalStorageDirectory().walkTopDown().filter { it.isFile }.forEach { f ->
            val cat = when (f.extension.lowercase()) {
                "jpg","jpeg","png","gif","webp","heic" -> "Photos"; "mp4","avi","mkv","mov" -> "Video"
                "mp3","wav","ogg","flac","aac" -> "Music"; "apk" -> "Apps"
                "zip","rar","7z","tar","gz" -> "Archives"; "pdf","doc","docx","xls","txt" -> "Documents"; else -> "Other"
            }
            val (s, c) = byType.getOrDefault(cat, Pair(0L, 0)); byType[cat] = Pair(s + f.length(), c + 1)
            if (f.length() > 10_000_000) large.add(Pair(f, f.length()))
        }
        val colors = listOf(Color(0xFF4CAF50), Color(0xFFE91E63), Color(0xFFFF9800), Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFF009688), Color(0xFF607D8B))
        withContext(Dispatchers.Main) {
            categories = byType.entries.sortedByDescending { it.value.first }.mapIndexed { i, (n, p) -> StorageCategory(n, p.first, colors[i % colors.size], p.second) }
            largestFiles = large.sortedByDescending { it.second }.take(20); isLoading = false
        }
    } }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        GlassTopBar {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Blue) }
            Text(Strings.storageAnalyzer, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Blue) }
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                GlassCard(cornerRadius = 14.dp) {
                    Column(Modifier.padding(16.dp)) {
                    Text("${fmtG(totalUsed)} ${Strings.of} ${fmtG(totalSize)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("${fmtG(totalFree)} ${Strings.freeSpace}", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { (totalUsed.toFloat() / totalSize).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Blue, trackColor = Color(0xFFE0E0E0))
                    }
                }
            }
            item { Text("By category", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary) }
            items(categories) { cat ->
                Row(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(12.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).background(cat.color.copy(0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(cat.color)) }
                    Column(Modifier.weight(1f)) { Text(cat.name, fontSize = 15.sp, color = TextPrimary); Text("${cat.count} ${Strings.filesCount}", fontSize = 12.sp, color = TextSecondary) }
                    Text(fmtG(cat.size), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
            if (largestFiles.isNotEmpty()) {
                item { Text("Large files", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary) }
                items(largestFiles) { (f, s) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.InsertDriveFile, null, Modifier.size(24.dp), tint = TextSecondary)
                        Column(Modifier.weight(1f)) { Text(f.name, fontSize = 13.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        Text(fmtG(s), fontSize = 13.sp, color = Red, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

private fun fmtG(b: Long): String = when { b < 1024 -> "$b B"; b < 1024*1024 -> "${"%.1f".format(b/1024.0)} KB"
    b < 1024L*1024*1024 -> "${"%.1f".format(b/(1024.0*1024))} MB"; else -> "${"%.2f".format(b/(1024.0*1024*1024))} GB" }
