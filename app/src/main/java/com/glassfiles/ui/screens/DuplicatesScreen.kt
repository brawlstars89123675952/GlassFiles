package com.glassfiles.ui.screens

import android.os.Environment
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.*
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DuplicatesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var duplicates by remember { mutableStateOf<List<DuplicateGroup>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var scanned by remember { mutableIntStateOf(0) }
    var found by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        scanning = true
        withContext(Dispatchers.IO) {
            val root = Environment.getExternalStorageDirectory().absolutePath
            duplicates = DuplicateFinder.findDuplicates(root) { s, f ->
                scanned = s; found = f
            }
        }
        scanning = false; done = true
    }

    val topBg = if (ThemeState.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val circleBg = if (ThemeState.isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    val cardBg = if (ThemeState.isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)
    val totalWaste = duplicates.sumOf { it.size * (it.files.size - 1) }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        // Top bar
        Row(Modifier.fillMaxWidth().background(topBg).padding(top = 52.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(circleBg).clickable(onClick = onBack),
                contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(18.dp),
                    tint = if (ThemeState.isDark) Color.White else TextPrimary)
            }
            Spacer(Modifier.weight(1f))
            Text(Strings.duplicates, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            // Delete selected
            if (selectedFiles.isNotEmpty()) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(Red.copy(0.15f))
                    .clickable {
                        scope.launch(Dispatchers.IO) {
                            selectedFiles.forEach { path -> try { File(path).delete() } catch (_: Exception) {} }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Deleted ${selectedFiles.size} files", Toast.LENGTH_SHORT).show()
                                selectedFiles = emptySet()
                                // Re-scan
                                scanning = true
                                val root = Environment.getExternalStorageDirectory().absolutePath
                                duplicates = DuplicateFinder.findDuplicates(root) { s, f -> scanned = s; found = f }
                                scanning = false
                            }
                        }
                    }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Delete, null, Modifier.size(20.dp), tint = Red)
                }
            } else Spacer(Modifier.size(36.dp))
        }

        if (scanning) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = Blue)
                Spacer(Modifier.height(16.dp))
                Text(Strings.scanning, color = TextPrimary, fontSize = 16.sp)
                Text("${Strings.files}: $scanned • ${Strings.duplicatesFound}: $found", color = TextSecondary, fontSize = 13.sp)
            }
        } else if (duplicates.isEmpty() && done) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(64.dp), tint = Green)
                Spacer(Modifier.height(16.dp))
                Text(Strings.noDuplicates, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(Strings.storageClean, color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            // Summary
            Box(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(12.dp)).background(cardBg).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.FileCopy, null, Modifier.size(32.dp), tint = Orange)
                    Column {
                        Text("${duplicates.size} групп дубликатов", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(Strings.canFree + "  ${formatSize(totalWaste)}", color = TextSecondary, fontSize = 13.sp)
                        if (selectedFiles.isNotEmpty()) Text("${Strings.selected}: ${selectedFiles.size}", color = Blue, fontSize = 13.sp)
                    }
                }
            }

            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                duplicates.forEachIndexed { gi, group ->
                    item(key = "header_$gi") {
                        Text("${formatSize(group.size)} × ${group.files.size} файлов",
                            color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
                    }
                    items(group.files, key = { it.absolutePath }) { file ->
                        val isSelected = file.absolutePath in selectedFiles
                        Row(
                            Modifier.fillMaxWidth().background(if (isSelected) Blue.copy(0.08f) else Color.Transparent)
                                .clickable {
                                    selectedFiles = if (isSelected) selectedFiles - file.absolutePath
                                    else selectedFiles + file.absolutePath
                                }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(isSelected, { checked ->
                                selectedFiles = if (checked) selectedFiles + file.absolutePath else selectedFiles - file.absolutePath
                            }, colors = CheckboxDefaults.colors(checkedColor = Blue))
                            Column(Modifier.weight(1f)) {
                                Text(file.name, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(file.parent?.replace(Environment.getExternalStorageDirectory().absolutePath, Strings.storage) ?: "",
                                    color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.1f ГБ".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f МБ".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.1f КБ".format(bytes / 1024.0)
        else -> "$bytes Б"
    }
}
