package com.glassfiles.ui.screens

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.glassfiles.data.*
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

// ═══════════════════════════════════
// Batch Rename Dialog
// ═══════════════════════════════════

@Composable
fun BatchRenameDialog(files: List<File>, onDismiss: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(BatchRenamer.RenameMode.REPLACE) }
    var param1 by remember { mutableStateOf("") }
    var param2 by remember { mutableStateOf("") }
    val preview = remember(mode, param1, param2) { BatchRenamer.preview(files, mode, param1, param2).take(5) }
    val menuBg = if (ThemeState.isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)

    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(menuBg).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(Strings.batchRename, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextPrimary)
            Text("${files.size} файлов", color = TextSecondary, fontSize = 13.sp)

            // Mode selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BatchRenamer.RenameMode.entries.forEach { m ->
                    val sel = m == mode
                    val chipBg = if (sel) Blue.copy(0.15f) else if (ThemeState.isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(chipBg)
                        .clickable { mode = m }.padding(10.dp)) {
                        Text(m.label, color = if (sel) Blue else TextPrimary, fontSize = 14.sp)
                    }
                }
            }

            // Params
            when (mode) {
                BatchRenamer.RenameMode.REPLACE -> {
                    OutlinedTextField(param1, { param1 = it }, label = { Text(Strings.find) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(param2, { param2 = it }, label = { Text(Strings.replaceWith) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                BatchRenamer.RenameMode.PREFIX, BatchRenamer.RenameMode.SUFFIX -> {
                    OutlinedTextField(param1, { param1 = it }, label = { Text(Strings.text) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                BatchRenamer.RenameMode.SEQUENCE -> {
                    OutlinedTextField(param1, { param1 = it }, label = { Text(Strings.prefixOpt) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                else -> {}
            }

            // Preview
            if (preview.isNotEmpty()) {
                Text(Strings.preview + ":", color = TextSecondary, fontSize = 12.sp)
                preview.forEach { (old, new) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(old, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("→", color = TextTertiary, fontSize = 11.sp)
                        Text(new, color = Blue, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(Strings.cancel) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val r = BatchRenamer.execute(files, mode, param1, param2)
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            Toast.makeText(context, "${Strings.renamed}: ${r.renamed}, ${Strings.errors}: ${r.errors}", Toast.LENGTH_SHORT).show()
                            onDone()
                        }
                    }
                }) { Text(Strings.renameBtn) }
            }
        }
    }
}

// ═══════════════════════════════════
// Encrypt/Decrypt Dialog
// ═══════════════════════════════════

@Composable
fun EncryptDialog(file: File, onDismiss: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var processing by remember { mutableStateOf(false) }
    val isEncrypted = file.name.endsWith(".enc")
    val menuBg = if (ThemeState.isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)

    Dialog(onDismissRequest = { if (!processing) onDismiss() }) {
        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(menuBg).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (isEncrypted) Icons.Rounded.LockOpen else Icons.Rounded.Lock, null, Modifier.size(24.dp), tint = Blue)
                Text(if (isEncrypted) Strings.decrypt else Strings.encrypt, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextPrimary)
            }
            Text(file.name, color = TextSecondary, fontSize = 13.sp)

            OutlinedTextField(password, { password = it }, label = { Text(Strings.password) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())

            if (!isEncrypted) {
                OutlinedTextField(confirmPassword, { confirmPassword = it }, label = { Text(Strings.confirmPassword) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
            }

            if (processing) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Blue)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss, enabled = !processing) { Text(Strings.cancel) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (password.length < 4) { Toast.makeText(context, Strings.minChars, Toast.LENGTH_SHORT).show(); return@Button }
                    if (!isEncrypted && password != confirmPassword) { Toast.makeText(context, Strings.passwordsMismatch, Toast.LENGTH_SHORT).show(); return@Button }
                    processing = true
                    scope.launch(Dispatchers.IO) {
                        val result = if (isEncrypted) FileEncryptor.decrypt(file, password) else FileEncryptor.encrypt(file, password)
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            processing = false
                            result.fold(
                                onSuccess = { Toast.makeText(context, if (isEncrypted) "${Strings.decrypted}: ${it.name}" else "${Strings.encrypted}: ${it.name}", Toast.LENGTH_SHORT).show(); onDone() },
                                onFailure = { Toast.makeText(context, "${Strings.error}: ${it.message}", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }, enabled = !processing && password.isNotEmpty()) {
                    Text(if (isEncrypted) Strings.decrypt else Strings.encrypt)
                }
            }
        }
    }
}

// ═══════════════════════════════════
// Create File/Folder Dialog
// ═══════════════════════════════════

@Composable
fun CreateItemDialog(currentPath: String, onDismiss: () -> Unit, onCreated: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var isFolder by remember { mutableStateOf(true) }
    val menuBg = if (ThemeState.isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)

    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(menuBg).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(Strings.create, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = TextPrimary)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val selBg = Blue.copy(0.15f)
                val unselBg = if (ThemeState.isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
                Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (isFolder) selBg else unselBg)
                    .clickable { isFolder = true }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(20.dp), tint = if (isFolder) Blue else TextSecondary)
                        Text(Strings.createFolder, color = if (isFolder) Blue else TextPrimary, fontSize = 14.sp)
                    }
                }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (!isFolder) selBg else unselBg)
                    .clickable { isFolder = false }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.NoteAdd, null, Modifier.size(20.dp), tint = if (!isFolder) Blue else TextSecondary)
                        Text(Strings.createFile, color = if (!isFolder) Blue else TextPrimary, fontSize = 14.sp)
                    }
                }
            }

            OutlinedTextField(name, { name = it },
                label = { Text(if (isFolder) Strings.folderName else Strings.fileName) },
                singleLine = true, modifier = Modifier.fillMaxWidth())

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(Strings.cancel) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (name.isBlank()) return@Button
                    val f = File(currentPath, name)
                    try {
                        if (isFolder) f.mkdirs() else { f.parentFile?.mkdirs(); f.createNewFile() }
                        Toast.makeText(context, "${Strings.created}: $name", Toast.LENGTH_SHORT).show()
                        onCreated()
                    } catch (e: Exception) {
                        Toast.makeText(context, "${Strings.error}: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }, enabled = name.isNotBlank()) { Text(Strings.create) }
            }
        }
    }
}

// ═══════════════════════════════════
// Audio Player Screen
// ═══════════════════════════════════

@Composable
fun AudioPlayerDialog(filePath: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val file = remember { File(filePath) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableIntStateOf(0) }
    var current by remember { mutableIntStateOf(0) }

    val player = remember {
        MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
        }
    }

    LaunchedEffect(player) {
        duration = player.duration
        while (true) {
            if (player.isPlaying) {
                current = player.currentPosition
                progress = current.toFloat() / duration.coerceAtLeast(1)
            }
            delay(200)
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    val menuBg = if (ThemeState.isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)

    Dialog(onDismissRequest = { player.release(); onDismiss() }) {
        Column(Modifier.clip(RoundedCornerShape(14.dp)).background(menuBg).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Album art placeholder
            Box(Modifier.size(120.dp).background(if (ThemeState.isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MusicNote, null, Modifier.size(48.dp), tint = Blue)
            }

            Text(file.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

            // Progress
            Column(Modifier.fillMaxWidth()) {
                Slider(progress, { progress = it; player.seekTo((it * duration).toInt()) },
                    colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(current), color = TextSecondary, fontSize = 12.sp)
                    Text(formatTime(duration), color = TextSecondary, fontSize = 12.sp)
                }
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { player.seekTo((player.currentPosition - 10000).coerceAtLeast(0)) }) {
                    Icon(Icons.Rounded.Replay10, null, Modifier.size(32.dp), tint = TextPrimary)
                }
                Box(Modifier.size(56.dp).background(Blue, CircleShape).clickable {
                    if (isPlaying) player.pause() else player.start()
                    isPlaying = !isPlaying
                }, contentAlignment = Alignment.Center) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, Modifier.size(32.dp), tint = Color.White)
                }
                IconButton(onClick = { player.seekTo((player.currentPosition + 10000).coerceAtMost(duration)) }) {
                    Icon(Icons.Rounded.Forward10, null, Modifier.size(32.dp), tint = TextPrimary)
                }
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val s = ms / 1000; val m = s / 60; val sec = s % 60
    return "%d:%02d".format(m, sec)
}
