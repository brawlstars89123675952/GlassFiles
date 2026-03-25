package com.glassfiles.ui.screens

import android.os.Environment
import com.glassfiles.data.Strings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.*
import com.glassfiles.data.FileType
import com.glassfiles.ui.components.FileTypeIcon
import com.glassfiles.ui.components.*
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.*
import java.io.File

enum class SearchFilter { ALL, FOLDERS, IMAGES, VIDEOS, AUDIO, DOCUMENTS, ARCHIVES }

@Composable
fun GlobalSearchScreen(onBack: () -> Unit, onFileClick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<File>()) }
    var isSearching by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(SearchFilter.ALL) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(query, filter) {
        delay(300); searchJob?.cancel()
        if (query.length < 2) { results = emptyList(); return@LaunchedEffect }
        searchJob = scope.launch(Dispatchers.IO) {
            isSearching = true
            val found = mutableListOf<File>()
            Environment.getExternalStorageDirectory().walkTopDown().forEach { f ->
                if (f.name.contains(query, true)) {
                    val ok = when (filter) {
                        SearchFilter.ALL -> true; SearchFilter.FOLDERS -> f.isDirectory
                        SearchFilter.IMAGES -> f.extension.lowercase() in listOf("jpg","jpeg","png","gif","webp")
                        SearchFilter.VIDEOS -> f.extension.lowercase() in listOf("mp4","avi","mkv","mov")
                        SearchFilter.AUDIO -> f.extension.lowercase() in listOf("mp3","wav","ogg","flac")
                        SearchFilter.DOCUMENTS -> f.extension.lowercase() in listOf("pdf","doc","docx","txt","xls")
                        SearchFilter.ARCHIVES -> f.extension.lowercase() in listOf("zip","rar","7z","tar","gz")
                    }
                    if (ok) found.add(f)
                }
            }
            withContext(Dispatchers.Main) { results = found.sortedByDescending { it.lastModified() }; isSearching = false }
        }
    }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        GlassTopBar {
            IconButton(onClick = { searchJob?.cancel(); onBack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Blue) }
            Text(Strings.search, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        OutlinedTextField(query, { query = it }, placeholder = { Text(Strings.searchFiles) }, leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Clear, null) } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true, shape = RoundedCornerShape(12.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(SearchFilter.ALL to "All", SearchFilter.FOLDERS to "Folders", SearchFilter.IMAGES to "Photos", SearchFilter.DOCUMENTS to "Documents", SearchFilter.ARCHIVES to "Archives").forEach { (f, l) ->
                FilterChip(filter == f, { filter = f }, label = { Text(l, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Blue.copy(0.12f), selectedLabelColor = Blue))
            }
        }
        if (isSearching) Row(Modifier.padding(16.dp, 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), Blue, strokeWidth = 2.dp); Text(Strings.searching, fontSize = 12.sp, color = TextSecondary)
        } else if (query.length >= 2) Text("Found: ${results.size}", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(16.dp, 4.dp))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(results.take(200)) { file ->
                Row(Modifier.fillMaxWidth().clickable { onFileClick(file.absolutePath) }.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (file.isDirectory) FolderIcon(FolderBlue, size = 36.dp) else FileTypeIcon(FileType.UNKNOWN, file.extension, size = 36)
                    Column(Modifier.weight(1f)) {
                        Text(file.name, fontSize = 14.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        Text(file.parent?.replace("/storage/emulated/0", "~") ?: "", fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                    }
                }
                HorizontalDivider(Modifier.padding(start = 64.dp), thickness = 0.5.dp, color = Color(0x0F000000))
            }
        }
    }
}
