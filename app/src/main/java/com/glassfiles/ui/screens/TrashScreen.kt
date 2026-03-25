package com.glassfiles.ui.screens

import android.widget.Toast
import com.glassfiles.data.Strings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.*
import com.glassfiles.data.TrashManager
import com.glassfiles.ui.components.*
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrashScreen(trashManager: TrashManager, onBack: () -> Unit) {
    var items by remember { mutableStateOf(trashManager.getTrashItems()) }
    var trashSize by remember { mutableStateOf(trashManager.getTrashSize()) }
    val scope = rememberCoroutineScope(); val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()) }
    fun refresh() { items = trashManager.getTrashItems(); trashSize = trashManager.getTrashSize() }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        GlassTopBar {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Blue) }
            Text(Strings.trashTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
            if (items.isNotEmpty()) TextButton(onClick = { scope.launch { trashManager.emptyTrash(); refresh(); Toast.makeText(context, "Emptied", Toast.LENGTH_SHORT).show() } }) {
                Text(Strings.emptyTrash, color = Red, fontWeight = FontWeight.SemiBold) }
        }
        if (items.isNotEmpty()) Text("${items.size} ${Strings.objects}", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 16.dp))
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(64.dp), tint = TextTertiary)
                    Spacer(Modifier.height(12.dp)); Text(Strings.trashEmpty, fontSize = 17.sp, color = TextSecondary)
                }
            }
        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                Row(Modifier.fillMaxWidth().background(SurfaceWhite, RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (item.isDirectory) Icons.Rounded.Folder else Icons.Rounded.InsertDriveFile, null, Modifier.size(32.dp),
                        tint = if (item.isDirectory) FolderBlue else TextSecondary)
                    Column(Modifier.weight(1f)) {
                        Text(item.name, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Deleted ${sdf.format(Date(item.trashedAt))}", fontSize = 11.sp, color = TextSecondary)
                    }
                    IconButton(onClick = { scope.launch { trashManager.restore(item); refresh(); Toast.makeText(context, Strings.restored, Toast.LENGTH_SHORT).show() } }) {
                        Icon(Icons.Rounded.Restore, null, Modifier.size(22.dp), tint = Blue) }
                    IconButton(onClick = { scope.launch { trashManager.deletePermanently(item); refresh() } }) {
                        Icon(Icons.Rounded.DeleteForever, null, Modifier.size(22.dp), tint = Red) }
                }
            }
        }
    }
}
