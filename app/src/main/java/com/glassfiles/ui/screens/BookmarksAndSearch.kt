package com.glassfiles.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.Strings
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ═══════════════════════════════════
// Bookmark Manager
// ═══════════════════════════════════

object BookmarkStore {
    private const val PREF_KEY = "bookmarks"
    fun load(context: Context): List<Pair<String, String>> {
        val prefs = context.getSharedPreferences("bookmarks", Context.MODE_PRIVATE)
        val raw = prefs.getString(PREF_KEY, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("|", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }
    }
    fun save(context: Context, bookmarks: List<Pair<String, String>>) {
        val prefs = context.getSharedPreferences("bookmarks", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY, bookmarks.joinToString("\n") { "${it.first}|${it.second}" }).apply()
    }
    fun add(context: Context, name: String, path: String) {
        val list = load(context).toMutableList()
        list.add(name to path)
        save(context, list)
    }
    fun remove(context: Context, index: Int) {
        val list = load(context).toMutableList()
        if (index in list.indices) { list.removeAt(index); save(context, list) }
    }
}

@Composable
fun BookmarksScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var bookmarks by remember { mutableStateOf(BookmarkStore.load(context)) }
    var showAdd by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        Row(Modifier.fillMaxWidth().background(SurfaceWhite).padding(top = 44.dp, start = 4.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Blue) }
            Text(Strings.bookmarks, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, null, Modifier.size(22.dp), tint = Blue) }
        }

        if (bookmarks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.BookmarkBorder, null, Modifier.size(48.dp), tint = TextTertiary)
                    Spacer(Modifier.height(12.dp))
                    Text(Strings.noBookmarks, color = TextSecondary, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bookmarks.size) { i ->
                    val (name, path) = bookmarks[i]
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite)
                        .clickable { onNavigate(path) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(36.dp).background(Blue.copy(0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Bookmark, null, Modifier.size(20.dp), tint = Blue)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(path, fontSize = 11.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace)
                        }
                        IconButton(onClick = { BookmarkStore.remove(context, i); bookmarks = BookmarkStore.load(context)
                            Toast.makeText(context, Strings.bookmarkRemoved, Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Rounded.Close, null, Modifier.size(18.dp), tint = TextTertiary)
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var path by remember { mutableStateOf("/storage/emulated/0/") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = SurfaceWhite,
            title = { Text(Strings.addBookmark, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(Strings.bookmarkName) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(path, { path = it }, label = { Text(Strings.bookmarkPath) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp))
                }
            },
            confirmButton = { TextButton(onClick = {
                if (name.isNotBlank() && path.isNotBlank()) {
                    BookmarkStore.add(context, name, path); bookmarks = BookmarkStore.load(context)
                    Toast.makeText(context, Strings.bookmarkAdded, Toast.LENGTH_SHORT).show(); showAdd = false
                }
            }) { Text(Strings.create, color = Blue) } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(Strings.cancel, color = TextSecondary) } }
        )
    }
}

// ═══════════════════════════════════
// Content Search (grep)
// ═══════════════════════════════════

data class GrepMatch(val file: String, val line: Int, val text: String)

@Composable
fun ContentSearchScreen(onBack: () -> Unit, onFileClick: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("/storage/emulated/0/") }
    var results by remember { mutableStateOf<List<GrepMatch>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        Row(Modifier.fillMaxWidth().background(SurfaceWhite).padding(top = 44.dp, start = 4.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Blue) }
            Text(Strings.contentSearch, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text(Strings.searchInFiles) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(20.dp)) })
            OutlinedTextField(folder, { folder = it }, label = { Text(Strings.searchFolder) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp))

            Button(onClick = {
                if (query.length >= 2) {
                    searching = true; searched = true
                    scope.launch {
                        results = withContext(Dispatchers.IO) { grepFiles(folder, query) }
                        searching = false
                    }
                }
            }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue), enabled = query.length >= 2 && !searching) {
                if (searching) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else { Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = Color.White); Spacer(Modifier.width(6.dp))
                    Text(Strings.search, color = Color.White, fontWeight = FontWeight.SemiBold) }
            }

            if (searched && !searching) {
                Text("${Strings.matchesFound}: ${results.size}", fontSize = 13.sp, color = TextSecondary)
            }
        }

        if (results.isEmpty() && searched && !searching) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Strings.noMatches, color = TextSecondary)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(results.take(200)) { match ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onFileClick(match.file) }
                        .padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(File(match.file).name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Blue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${Strings.lineNumber} ${match.line}: ${match.text.trim()}", fontSize = 12.sp, color = TextPrimary,
                                fontFamily = FontFamily.Monospace, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(match.file, fontSize = 10.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(SeparatorColor))
                }
            }
        }
    }
}

private fun grepFiles(dirPath: String, query: String, maxResults: Int = 500): List<GrepMatch> {
    val results = mutableListOf<GrepMatch>()
    val dir = File(dirPath)
    if (!dir.exists()) return results
    val textExts = setOf("txt", "kt", "java", "py", "js", "ts", "json", "xml", "html", "css", "md", "yaml", "yml",
        "toml", "gradle", "properties", "cfg", "conf", "ini", "sh", "bash", "log", "csv", "sql", "rs", "go", "c", "h", "cpp", "swift")

    fun scan(file: File) {
        if (results.size >= maxResults) return
        if (file.isDirectory) {
            if (file.name.startsWith(".") || file.name == "node_modules" || file.name == "build") return
            file.listFiles()?.forEach { scan(it) }
        } else if (file.isFile && file.extension.lowercase() in textExts && file.length() < 2_000_000) {
            try {
                file.bufferedReader().useLines { lines ->
                    lines.forEachIndexed { i, line ->
                        if (results.size >= maxResults) return@useLines
                        if (line.contains(query, ignoreCase = true)) {
                            results.add(GrepMatch(file.absolutePath, i + 1, line.take(200)))
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
    scan(dir)
    return results
}
