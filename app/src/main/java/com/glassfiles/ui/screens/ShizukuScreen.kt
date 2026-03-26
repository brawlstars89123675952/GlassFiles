package com.glassfiles.ui.screens

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.glassfiles.data.ShizukuManager
import com.glassfiles.data.Strings
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ShizukuScreen(onBack: () -> Unit, onBrowseRestricted: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isInstalled by remember { mutableStateOf(ShizukuManager.isShizukuInstalled(context)) }
    var isRunning by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        isRunning = ShizukuManager.isShizukuRunning()
        hasPermission = ShizukuManager.hasShizukuPermission()
    }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        Row(Modifier.fillMaxWidth().background(SurfaceWhite).padding(top = 44.dp, start = 4.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Blue) }
            Text(Strings.shizuku, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp))
                .background(if (hasPermission) Color(0xFF4CAF50) else if (isRunning) Color(0xFFFF9800) else Color(0xFFFF5252)))
        }

        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (hasPermission) Icons.Rounded.CheckCircle else Icons.Rounded.Warning, null, Modifier.size(24.dp),
                        tint = if (hasPermission) Color(0xFF4CAF50) else Color(0xFFFF9800))
                    Text(when {
                        !isInstalled -> Strings.shizukuNotInstalled
                        !isRunning -> Strings.shizukuNotRunning
                        !hasPermission -> Strings.shizukuNoPermission
                        else -> Strings.shizukuConnected
                    }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                if (!hasPermission && isRunning) {
                    Button(onClick = {
                        ShizukuManager.requestPermission(100)
                        scope.launch { kotlinx.coroutines.delay(1000); hasPermission = ShizukuManager.hasShizukuPermission() }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(Strings.shizukuRequestPerm, color = Color.White)
                    }
                }
            }
        }

        if (!hasPermission) return@Column

        ScrollableTabRow(selectedTab, containerColor = SurfaceWhite, contentColor = Blue, edgePadding = 8.dp, indicator = {}, divider = {}) {
            listOf("Файлы", "Приложения", "Система", "Логи").forEachIndexed { i, title ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                    text = { Text(title, fontSize = 13.sp, fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == i) Blue else TextSecondary) })
            }
        }

        when (selectedTab) {
            0 -> FilesTab(context, scope, onBrowseRestricted)
            1 -> AppsTab(context, scope)
            2 -> SystemTab(context, scope)
            3 -> LogsTab(context, scope)
        }
    }
}

// ═══════════════════════════════════
// Files Tab
// ═══════════════════════════════════

@Composable
private fun FilesTab(context: Context, scope: kotlinx.coroutines.CoroutineScope, onBrowse: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SLabel("Ограниченные директории") }
        item { TCard(Icons.Rounded.Folder, Strings.androidData, "Android/data/", Color(0xFF2196F3)) { onBrowse("shizuku:///storage/emulated/0/Android/data") } }
        item { TCard(Icons.Rounded.Folder, Strings.androidObb, "Android/obb/", Color(0xFF9C27B0)) { onBrowse("shizuku:///storage/emulated/0/Android/obb") } }
        item { SLabel("Системные директории") }
        item { TCard(Icons.Rounded.Storage, "/data/local/tmp", "Временные файлы", Color(0xFF607D8B)) { onBrowse("shizuku:///data/local/tmp") } }
        item { TCard(Icons.Rounded.SettingsApplications, "/system/app", "Системные приложения", Color(0xFFFF9800)) { onBrowse("shizuku:///system/app") } }
        item { TCard(Icons.Rounded.FolderSpecial, "/data/data", "Данные приложений", Color(0xFFF44336)) { onBrowse("shizuku:///data/data") } }
        item { SLabel("Инструменты") }
        item {
            var show by remember { mutableStateOf(false) }
            TCard(Icons.Rounded.Lock, "chmod", "Изменить права файлов", Color(0xFF795548)) { show = true }
            if (show) ChmodDialog(scope) { show = false }
        }
        item {
            var show by remember { mutableStateOf(false) }
            TCard(Icons.Rounded.Link, "Symlink", "Символическая ссылка", Color(0xFF009688)) { show = true }
            if (show) SymlinkDialog(context, scope) { show = false }
        }
        item { TCard(Icons.Rounded.Info, "Точки монтирования", "mount", Color(0xFF455A64)) {
            scope.launch { val m = ShizukuManager.getMounts()
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("mounts", m))
                tst(context, true, "Скопировано (${m.lines().size} строк)") }
        }}
    }
}

// ═══════════════════════════════════
// Apps Tab
// ═══════════════════════════════════

@Composable
private fun AppsTab(context: Context, scope: kotlinx.coroutines.CoroutineScope) {
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { apps = loadApps(context); loading = false } }

    val filtered = remember(apps, query) {
        val list = apps.filter { !it.isSystem }
        if (query.isNotBlank()) list.filter { it.name.contains(query, true) || it.packageName.contains(query, true) }
        else list.sortedBy { it.name.lowercase() }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceWhite).padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (query.isEmpty()) Text(Strings.searchApps, color = TextTertiary, fontSize = 14.sp)
            BasicTextField(query, { query = it }, textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp), singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Blue, modifier = Modifier.size(32.dp)) }
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
            items(filtered, key = { it.packageName }) { app -> AppRow(app, context, scope) }
        }
    }
}

@Composable
private fun AppRow(app: AppItem, context: Context, scope: kotlinx.coroutines.CoroutineScope) {
    var expanded by remember { mutableStateOf(false) }
    var isFrozen by remember { mutableStateOf(false) }
    var dataSize by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(app.packageName) { isFrozen = ShizukuManager.isAppFrozen(app.packageName) }

    Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (app.icon != null) {
                val bmp = remember(app.packageName) { app.icon.toBitmap(96, 96).asImageBitmap() }
                Image(bmp, app.name, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
            } else Box(Modifier.size(40.dp).background(Blue.copy(0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Android, null, Modifier.size(22.dp), tint = Blue)
            }
            Column(Modifier.weight(1f)) {
                Text(app.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(app.packageName, fontSize = 11.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    if (dataSize != null) Text(fmtSz(dataSize!!), fontSize = 11.sp, color = Blue, fontWeight = FontWeight.Medium)
                }
            }
            if (isFrozen) Text("❄", fontSize = 14.sp)
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, Modifier.size(18.dp), tint = TextTertiary)
        }

        AnimatedVisibility(expanded) {
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AChip(Icons.Rounded.Stop, Strings.forceStop, Color(0xFFFF5252), Modifier.weight(1f)) {
                        scope.launch { tst(context, ShizukuManager.forceStop(app.packageName), Strings.forceStopped) } }
                    AChip(if (isFrozen) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, if (isFrozen) Strings.unfreezeApp else Strings.freezeApp, Color(0xFF2196F3), Modifier.weight(1f)) {
                        scope.launch { val ok = if (isFrozen) ShizukuManager.unfreezeApp(app.packageName) else ShizukuManager.freezeApp(app.packageName)
                            if (ok) isFrozen = !isFrozen; tst(context, ok, if (isFrozen) Strings.frozen else Strings.unfrozen) } }
                    AChip(Icons.Rounded.CleaningServices, "Кэш", Color(0xFF4CAF50), Modifier.weight(1f)) {
                        scope.launch { tst(context, ShizukuManager.clearCache(app.packageName), Strings.clearCacheDone) } }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AChip(Icons.Rounded.DeleteForever, "Все данные", Color(0xFFFF9800), Modifier.weight(1f)) {
                        scope.launch { tst(context, ShizukuManager.clearAllData(app.packageName), "Данные очищены") } }
                    AChip(Icons.Rounded.Delete, "Удалить", Red, Modifier.weight(1f)) {
                        scope.launch { tst(context, ShizukuManager.uninstallApp(app.packageName), "Удалено") } }
                    AChip(Icons.Rounded.DataUsage, "Размер", Color(0xFF607D8B), Modifier.weight(1f)) {
                        scope.launch { dataSize = ShizukuManager.getAppDataSize(app.packageName) } }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AChip(Icons.Rounded.Block, "Огр. фон", Color(0xFF795548), Modifier.weight(1f)) {
                        scope.launch { tst(context, ShizukuManager.restrictBackground(app.packageName, true), "Фон ограничен") } }
                    AChip(Icons.Rounded.CheckCircle, "Разр. фон", Color(0xFF009688), Modifier.weight(1f)) {
                        scope.launch { tst(context, ShizukuManager.restrictBackground(app.packageName, false), "Фон разрешён") } }
                    AChip(Icons.Rounded.Backup, "Бэкап", Color(0xFF3F51B5), Modifier.weight(1f)) {
                        scope.launch {
                            val dir = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "GlassFiles_Backup"); dir.mkdirs()
                            val p = "${dir.absolutePath}/${app.packageName}_data.tar.gz"
                            tst(context, ShizukuManager.backupAppData(app.packageName, p), "Бэкап: ${java.io.File(p).name}")
                        } }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(SeparatorColor))
    }
}

// ═══════════════════════════════════
// System Tab
// ═══════════════════════════════════

@Composable
private fun SystemTab(context: Context, scope: kotlinx.coroutines.CoroutineScope) {
    var screenDpi by remember { mutableStateOf("") }
    var screenRes by remember { mutableStateOf("") }
    var batteryInfo by remember { mutableStateOf("") }
    var processes by remember { mutableStateOf<List<ShizukuManager.ProcessInfo>>(emptyList()) }
    var showDpi by remember { mutableStateOf(false) }
    var showRes by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { screenDpi = ShizukuManager.getScreenDpi().toString(); screenRes = ShizukuManager.getScreenResolution(); batteryInfo = ShizukuManager.getBatteryStats() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SLabel("Экран") }
        item { TCard(Icons.Rounded.Smartphone, "DPI: $screenDpi", "Изменить плотность", Color(0xFF2196F3)) { showDpi = true } }
        item { TCard(Icons.Rounded.AspectRatio, screenRes.ifBlank { "Разрешение" }, "Изменить разрешение", Color(0xFF9C27B0)) { showRes = true } }
        item { TCard(Icons.Rounded.Refresh, "Сбросить экран", "DPI + разрешение по умолчанию", Color(0xFF607D8B)) {
            scope.launch { ShizukuManager.resetScreenDpi(); ShizukuManager.resetScreenResolution()
                screenDpi = ShizukuManager.getScreenDpi().toString(); screenRes = ShizukuManager.getScreenResolution(); tst(context, true, "Сброшено") } } }

        item { SLabel("Захват экрана") }
        item { TCard(Icons.Rounded.Screenshot, "Скриншот", "Сохранить снимок", Color(0xFF4CAF50)) {
            scope.launch { val p = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/scr_${System.currentTimeMillis()}.png"
                tst(context, ShizukuManager.takeScreenshot(p), "Скриншот сохранён") } } }
        item { TCard(Icons.Rounded.Videocam, "Запись экрана 30с", "screenrecord", Color(0xFFE91E63)) {
            scope.launch { val p = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)}/rec_${System.currentTimeMillis()}.mp4"
                tst(context, ShizukuManager.startScreenRecord(p, 30), "Запись начата") } } }
        item { TCard(Icons.Rounded.StopCircle, "Остановить запись", "", Color(0xFF795548)) {
            scope.launch { ShizukuManager.stopScreenRecord(); tst(context, true, "Запись остановлена") } } }

        item { SLabel("Соединение") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TCard(Icons.Rounded.Wifi, "Wi-Fi ON", "", Color(0xFF2196F3), Modifier.weight(1f)) { scope.launch { ShizukuManager.setWifi(true); tst(context, true, "ON") } }
            TCard(Icons.Rounded.WifiOff, "Wi-Fi OFF", "", Color(0xFFFF5252), Modifier.weight(1f)) { scope.launch { ShizukuManager.setWifi(false); tst(context, true, "OFF") } }
        } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TCard(Icons.Rounded.Bluetooth, "BT ON", "", Color(0xFF2196F3), Modifier.weight(1f)) { scope.launch { ShizukuManager.setBluetooth(true); tst(context, true, "ON") } }
            TCard(Icons.Rounded.BluetoothDisabled, "BT OFF", "", Color(0xFFFF5252), Modifier.weight(1f)) { scope.launch { ShizukuManager.setBluetooth(false); tst(context, true, "OFF") } }
        } }

        item { SLabel("Батарея") }
        item { if (batteryInfo.isNotBlank()) Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceWhite).padding(12.dp)) {
            Text(batteryInfo.take(500), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary, lineHeight = 16.sp) } }

        item { SLabel("Процессы (RAM)") }
        item { Button(onClick = { scope.launch { processes = ShizukuManager.getRunningProcesses() } },
            colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) { Text("Загрузить", color = Color.White) } }
        items(processes.take(30)) { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${p.pid}", fontSize = 11.sp, color = TextTertiary, fontFamily = FontFamily.Monospace, modifier = Modifier.width(48.dp))
                Text(fmtSz(p.memKb * 1024), fontSize = 11.sp, color = Blue, fontFamily = FontFamily.Monospace, modifier = Modifier.width(56.dp))
                Text(p.name, fontSize = 11.sp, color = TextPrimary, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        item { SLabel("Перезагрузка") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TCard(Icons.Rounded.RestartAlt, "Reboot", "", Color(0xFFFF5252), Modifier.weight(1f)) { scope.launch { ShizukuManager.reboot() } }
            TCard(Icons.Rounded.PhoneAndroid, "Recovery", "", Color(0xFFFF9800), Modifier.weight(1f)) { scope.launch { ShizukuManager.reboot("recovery") } }
            TCard(Icons.Rounded.DeveloperMode, "Bootloader", "", Color(0xFF795548), Modifier.weight(1f)) { scope.launch { ShizukuManager.reboot("bootloader") } }
        } }
        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showDpi) DpiDlg(context, scope, screenDpi, { showDpi = false }) { screenDpi = it }
    if (showRes) ResDlg(context, scope, { showRes = false }) { screenRes = it }
}

// ═══════════════════════════════════
// Logcat Tab
// ═══════════════════════════════════

@Composable
private fun LogsTab(context: Context, scope: kotlinx.coroutines.CoroutineScope) {
    var logText by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(SurfaceWhite).padding(horizontal = 12.dp, vertical = 10.dp)) {
                if (filter.isEmpty()) Text("Фильтр (grep)...", color = TextTertiary, fontSize = 14.sp)
                BasicTextField(filter, { filter = it }, textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            IconButton(onClick = { loading = true; scope.launch { logText = ShizukuManager.getLogcat(300, filter); loading = false } }) {
                Icon(Icons.Rounded.Refresh, null, Modifier.size(22.dp), tint = Blue) }
            IconButton(onClick = { scope.launch { ShizukuManager.clearLogcat(); logText = ""; tst(context, true, "Очищено") } }) {
                Icon(Icons.Rounded.DeleteSweep, null, Modifier.size(22.dp), tint = Red) }
        }
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Blue, modifier = Modifier.size(32.dp)) }
        else if (logText.isBlank()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Terminal, null, Modifier.size(48.dp), tint = TextTertiary)
                Spacer(Modifier.height(8.dp)); Text("Нажмите ↻ для загрузки логов", color = TextSecondary, fontSize = 14.sp) }
        } else Box(Modifier.fillMaxSize().padding(horizontal = 8.dp).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
            Text(logText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary, lineHeight = 14.sp, modifier = Modifier.padding(8.dp))
        }
    }
}

// ═══════════════════════════════════
// Dialogs
// ═══════════════════════════════════

@Composable
private fun DpiDlg(context: Context, scope: kotlinx.coroutines.CoroutineScope, cur: String, onDismiss: () -> Unit, onApplied: (String) -> Unit) {
    var dpi by remember { mutableStateOf(cur) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = SurfaceWhite,
        title = { Text("Изменить DPI", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Текущий: $cur", fontSize = 13.sp, color = TextSecondary)
            OutlinedTextField(dpi, { dpi = it }, label = { Text("DPI") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("320", "360", "400", "420", "480", "560").forEach { v ->
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (dpi == v) Blue.copy(0.15f) else SurfaceLight).clickable { dpi = v }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(v, fontSize = 12.sp, color = if (dpi == v) Blue else TextSecondary) } } }
        } },
        confirmButton = { TextButton(onClick = { scope.launch { ShizukuManager.setScreenDpi(dpi.toIntOrNull() ?: 420); onApplied(dpi); tst(context, true, "DPI: $dpi") }; onDismiss() }) { Text("OK", color = Blue) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel, color = TextSecondary) } })
}

@Composable
private fun ResDlg(context: Context, scope: kotlinx.coroutines.CoroutineScope, onDismiss: () -> Unit, onApplied: (String) -> Unit) {
    var w by remember { mutableStateOf("1080") }; var h by remember { mutableStateOf("2400") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = SurfaceWhite,
        title = { Text("Разрешение", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(w, { w = it }, label = { Text("W") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("×", fontSize = 20.sp, color = TextSecondary, modifier = Modifier.align(Alignment.CenterVertically))
            OutlinedTextField(h, { h = it }, label = { Text("H") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        } },
        confirmButton = { TextButton(onClick = { val ww = w.toIntOrNull() ?: 1080; val hh = h.toIntOrNull() ?: 2400
            scope.launch { ShizukuManager.setScreenResolution(ww, hh); onApplied("${ww}x${hh}"); tst(context, true, "${ww}x${hh}") }; onDismiss()
        }) { Text("OK", color = Blue) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel, color = TextSecondary) } })
}

@Composable
private fun ChmodDialog(scope: kotlinx.coroutines.CoroutineScope, onDismiss: () -> Unit) {
    var path by remember { mutableStateOf("") }; var mode by remember { mutableStateOf("755") }; val ctx = LocalContext.current
    AlertDialog(onDismissRequest = onDismiss, containerColor = SurfaceWhite,
        title = { Text("chmod", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(path, { path = it }, label = { Text("Путь") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp))
            OutlinedTextField(mode, { mode = it }, label = { Text("Права") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("644", "755", "777", "600").forEach { v ->
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (mode == v) Blue.copy(0.15f) else SurfaceLight).clickable { mode = v }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(v, fontSize = 12.sp, color = if (mode == v) Blue else TextSecondary, fontFamily = FontFamily.Monospace) } } }
        } },
        confirmButton = { TextButton(onClick = { if (path.isNotBlank()) scope.launch { tst(ctx, ShizukuManager.chmod(path, mode), "chmod $mode") }; onDismiss() }) { Text("OK", color = Blue) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel, color = TextSecondary) } })
}

@Composable
private fun SymlinkDialog(context: Context, scope: kotlinx.coroutines.CoroutineScope, onDismiss: () -> Unit) {
    var target by remember { mutableStateOf("") }; var link by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = SurfaceWhite,
        title = { Text("Symlink", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(target, { target = it }, label = { Text("Цель") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp))
            OutlinedTextField(link, { link = it }, label = { Text("Ссылка") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp))
        } },
        confirmButton = { TextButton(onClick = { if (target.isNotBlank() && link.isNotBlank()) scope.launch { tst(context, ShizukuManager.symlink(target, link), "Создано") }; onDismiss() }) { Text(Strings.create, color = Blue) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel, color = TextSecondary) } })
}

// ═══════════════════════════════════
// Shared UI
// ═══════════════════════════════════

@Composable
private fun SLabel(text: String) { Text(text, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) }

@Composable
private fun TCard(icon: ImageVector, title: String, subtitle: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(36.dp).background(color.copy(0.12f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = color) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = TextTertiary)
    }
}

@Composable
private fun AChip(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(0.1f)).clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(18.dp), tint = color); Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 9.sp, color = color, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun tst(ctx: Context, ok: Boolean, msg: String) { Toast.makeText(ctx, if (ok) msg else "Ошибка", Toast.LENGTH_SHORT).show() }
private fun fmtSz(b: Long): String = when { b < 1024 -> "$b B"; b < 1024L * 1024 -> "%.1f KB".format(b / 1024.0); b < 1024L * 1024 * 1024 -> "%.1f MB".format(b / (1024.0 * 1024)); else -> "%.2f GB".format(b / (1024.0 * 1024 * 1024)) }
