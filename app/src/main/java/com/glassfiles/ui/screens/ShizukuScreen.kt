package com.glassfiles.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0=tools, 1=apps

    LaunchedEffect(Unit) {
        isRunning = ShizukuManager.isShizukuRunning()
        hasPermission = ShizukuManager.hasShizukuPermission()
    }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        // Top bar
        Row(Modifier.fillMaxWidth().background(SurfaceWhite).padding(top = 44.dp, start = 4.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Blue) }
            Text(Strings.shizuku, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp, modifier = Modifier.weight(1f))
            // Status indicator
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp))
                .background(if (hasPermission) Color(0xFF4CAF50) else if (isRunning) Color(0xFFFF9800) else Color(0xFFFF5252)))
        }

        // Status card
        Box(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (hasPermission) Icons.Rounded.CheckCircle else Icons.Rounded.Warning, null, Modifier.size(24.dp),
                        tint = if (hasPermission) Color(0xFF4CAF50) else Color(0xFFFF9800))
                    Text(
                        when {
                            !isInstalled -> Strings.shizukuNotInstalled
                            !isRunning -> Strings.shizukuNotRunning
                            !hasPermission -> Strings.shizukuNoPermission
                            else -> Strings.shizukuConnected
                        },
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                    )
                }
                if (!hasPermission && isRunning) {
                    Button(
                        onClick = {
                            ShizukuManager.requestPermission(100)
                            // Re-check after a moment
                            scope.launch {
                                kotlinx.coroutines.delay(1000)
                                hasPermission = ShizukuManager.hasShizukuPermission()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.shizukuRequestPerm, color = Color.White)
                    }
                }
            }
        }

        if (!hasPermission) return@Column

        // Tab row
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selectedTab == 0, Strings.tools) { selectedTab = 0 }
            FilterChip(selectedTab == 1, Strings.appManager) { selectedTab = 1 }
        }

        Spacer(Modifier.height(8.dp))

        when (selectedTab) {
            0 -> ShizukuToolsList(context, scope, onBrowseRestricted)
            1 -> ShizukuAppsList(context, scope)
        }
    }
}

@Composable
private fun FilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (selected) Blue.copy(0.15f) else SurfaceWhite)
        .border(1.dp, if (selected) Blue.copy(0.4f) else SeparatorColor, RoundedCornerShape(8.dp))
        .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, fontSize = 13.sp, color = if (selected) Blue else TextSecondary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun ShizukuToolsList(context: Context, scope: kotlinx.coroutines.CoroutineScope, onBrowse: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Android/data access
        item {
            ToolCard(Icons.Rounded.Folder, Strings.androidData, "Android/data/", Color(0xFF2196F3)) {
                onBrowse("/storage/emulated/0/Android/data")
            }
        }
        item {
            ToolCard(Icons.Rounded.Folder, Strings.androidObb, "Android/obb/", Color(0xFF9C27B0)) {
                onBrowse("/storage/emulated/0/Android/obb")
            }
        }
        // Silent install
        item {
            var showInstall by remember { mutableStateOf(false) }
            ToolCard(Icons.Rounded.InstallMobile, Strings.silentInstall, Strings.shizukuSub, Color(0xFF4CAF50)) {
                showInstall = true
            }
            if (showInstall) {
                // TODO: File picker for APK then ShizukuManager.silentInstall()
            }
        }
    }
}

@Composable
private fun ShizukuAppsList(context: Context, scope: kotlinx.coroutines.CoroutineScope) {
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            apps = loadApps(context)
            loading = false
        }
    }

    val filtered = remember(apps, query) {
        val list = apps.filter { !it.isSystem }
        if (query.isNotBlank()) list.filter { it.name.contains(query, true) || it.packageName.contains(query, true) }
        else list.sortedBy { it.name.lowercase() }
    }

    Column(Modifier.fillMaxSize()) {
        // Search
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceWhite).padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (query.isEmpty()) Text(Strings.searchApps, color = TextTertiary, fontSize = 14.sp)
            androidx.compose.foundation.text.BasicTextField(query, { query = it },
                textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp), singleLine = true, modifier = Modifier.fillMaxWidth())
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue, modifier = Modifier.size(32.dp))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(filtered, key = { it.packageName }) { app ->
                    ShizukuAppRow(app, context, scope)
                }
            }
        }
    }
}

@Composable
private fun ShizukuAppRow(app: AppItem, context: Context, scope: kotlinx.coroutines.CoroutineScope) {
    var expanded by remember { mutableStateOf(false) }
    var isFrozen by remember { mutableStateOf(false) }

    LaunchedEffect(app.packageName) {
        isFrozen = ShizukuManager.isAppFrozen(app.packageName)
    }

    Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (app.icon != null) {
                val bmp = remember(app.packageName) { app.icon.toBitmap(96, 96).asImageBitmap() }
                Image(bmp, app.name, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
            } else {
                Box(Modifier.size(40.dp).background(Blue.copy(0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Android, null, Modifier.size(22.dp), tint = Blue)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(app.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, fontSize = 11.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isFrozen) {
                Text(Strings.frozen, fontSize = 11.sp, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
            }
        }

        // Expanded actions
        AnimatedVisibility(expanded) {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Force stop
                ShizukuActionBtn(Icons.Rounded.Stop, Strings.forceStop, Color(0xFFFF5252)) {
                    scope.launch {
                        val ok = ShizukuManager.forceStop(app.packageName)
                        Toast.makeText(context, if (ok) Strings.forceStopped else Strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
                // Freeze/Unfreeze
                ShizukuActionBtn(
                    if (isFrozen) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    if (isFrozen) Strings.unfreezeApp else Strings.freezeApp,
                    Color(0xFF2196F3)
                ) {
                    scope.launch {
                        val ok = if (isFrozen) ShizukuManager.unfreezeApp(app.packageName) else ShizukuManager.freezeApp(app.packageName)
                        if (ok) isFrozen = !isFrozen
                        Toast.makeText(context, if (ok) (if (isFrozen) Strings.frozen else Strings.unfrozen) else Strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
                // Clear cache
                ShizukuActionBtn(Icons.Rounded.CleaningServices, Strings.clearCacheDone, Color(0xFF4CAF50)) {
                    scope.launch {
                        val ok = ShizukuManager.clearCache(app.packageName)
                        Toast.makeText(context, if (ok) Strings.clearCacheDone else Strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(0.5.dp).background(SeparatorColor))
    }
}

@Composable
private fun ShizukuActionBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(0.1f)).clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(20.dp), tint = color)
        Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun ToolCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).background(color.copy(0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(22.dp), tint = color)
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp), tint = TextTertiary)
    }
}
