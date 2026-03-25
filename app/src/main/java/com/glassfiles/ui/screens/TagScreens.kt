package com.glassfiles.ui.screens

import android.os.Environment
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
import androidx.compose.ui.window.Dialog
import com.glassfiles.data.*
import com.glassfiles.ui.theme.*
import java.io.File

/** Dialog to select tags for a file */
@Composable
fun TagSelectorDialog(filePath: String, tagManager: TagManager, onDismiss: () -> Unit) {
    val currentTags = remember(filePath) { tagManager.getTags(filePath) }
    var selected by remember { mutableStateOf(currentTags.map { it.name }.toSet()) }
    val menuBg = if (ThemeState.isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)
    val divColor = if (ThemeState.isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)

    Dialog(onDismissRequest = {
        tagManager.setTags(filePath, selected.mapNotNull { FileTags.byName(it) })
        onDismiss()
    }) {
        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(menuBg).padding(vertical = 8.dp)) {
            // Header
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.tags, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, null, Modifier.size(24.dp).clickable {
                    tagManager.setTags(filePath, selected.mapNotNull { FileTags.byName(it) })
                    onDismiss()
                }, tint = TextSecondary)
            }

            // Tags list
            FileTags.builtIn.forEach { tag ->
                val isSelected = tag.name in selected
                Row(
                    Modifier.fillMaxWidth().clickable {
                        selected = if (isSelected) selected - tag.name else selected + tag.name
                    }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(
                        if (isSelected) tag.color else Color.Transparent
                    ).border(2.dp, if (isSelected) tag.color else TextTertiary, CircleShape),
                        contentAlignment = Alignment.Center) {
                        if (isSelected) Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Color.White)
                    }
                    Text(tag.name, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                    Box(Modifier.size(16.dp).background(tag.color, CircleShape))
                }
                Box(Modifier.fillMaxWidth().padding(start = 52.dp).height(0.5.dp).background(divColor))
            }
        }
    }
}

/** Screen showing files with a specific tag */
@Composable
fun TaggedFilesScreen(tagName: String, onBack: () -> Unit, onFileClick: (String) -> Unit) {
    val context = LocalContext.current
    val tagManager = remember { TagManager(context) }
    val tag = FileTags.byName(tagName)
    val files = remember(tagName) {
        tagManager.getFilesByTag(tagName).mapNotNull { path ->
            val f = File(path)
            if (f.exists()) FileItem(
                name = f.name, path = f.absolutePath,
                size = if (f.isFile) f.length() else 0,
                lastModified = f.lastModified(),
                type = if (f.isDirectory) FileType.FOLDER else getFileType(f.extension),
                isDirectory = f.isDirectory, extension = f.extension, isDownloaded = true
            ) else null
        }
    }

    val topBg = if (ThemeState.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val circleBg = if (ThemeState.isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        Row(Modifier.fillMaxWidth().background(topBg).padding(top = 52.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(circleBg).clickable(onClick = onBack),
                contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(18.dp),
                    tint = if (ThemeState.isDark) Color.White else TextPrimary)
            }
            Spacer(Modifier.weight(1f))
            Text(tagName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.weight(1f)); Spacer(Modifier.size(36.dp))
        }

        if (files.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (tag != null) Box(Modifier.size(48.dp).background(tag.color.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(24.dp).background(tag.color, CircleShape))
                }
                Spacer(Modifier.height(16.dp))
                Text(Strings.noFilesWithTag, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(Strings.filesWithTag, color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                items(files) { file ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onFileClick(file.path) }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        com.glassfiles.ui.components.FileTypeIcon(file.type, file.extension, size = 40)
                        Column(Modifier.weight(1f)) {
                            Text(file.name, color = TextPrimary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(file.path.replace(Environment.getExternalStorageDirectory().absolutePath, ""), color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Box(Modifier.fillMaxWidth().padding(start = 68.dp).height(0.5.dp).background(SeparatorColor))
                }
            }
        }
    }
}
