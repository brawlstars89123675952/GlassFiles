package com.glassfiles.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.glassfiles.data.terminal.*
import com.glassfiles.data.Strings
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.abs

private class TermSession(
    val id: Int, var name: String,
    var webView: WebView? = null, var ptyProcess: PtyProcess? = null,
    var readerJob: Job? = null, var isReady: Boolean = false,
    val recorder: SessionRecorder = SessionRecorder()
) {
    fun destroy() {
        readerJob?.cancel()
        readerJob = null
        try { ptyProcess?.destroy() } catch (_: Exception) {}
        ptyProcess = null
        recorder.stop()
        try {
            webView?.removeJavascriptInterface("AndroidPty")
            webView?.stopLoading()
            webView?.loadUrl("about:blank")
            webView?.destroy()
        } catch (_: Exception) {}
        webView = null
        isReady = false
    }
}

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TerminalScreen(
    initialDir: String? = null, onBackClick: () -> Unit = {},
    onOpenFile: ((String) -> Unit)? = null, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bootstrap = remember { TermuxBootstrap(context) }
    val bsStatus by bootstrap.status.collectAsState()
    val prefs = remember { TerminalPrefs(context) }

    var showSettings by remember { mutableStateOf(false) }
    var showCommands by remember { mutableStateOf(false) }
    var showSSH by remember { mutableStateOf(false) }
    var showQuickActions by remember { mutableStateOf(false) }
    var ctrlDown by remember { mutableStateOf(false) }
    var altDown by remember { mutableStateOf(false) }

    var sessions by remember { mutableStateOf(listOf<TermSession>()) }
    var activeId by remember { mutableIntStateOf(0) }
    var nextId by remember { mutableIntStateOf(1) }
    var renamingSession by remember { mutableStateOf<TermSession?>(null) }
    var renameText by remember { mutableStateOf("") }
    var selectedThemeIdx by remember { mutableIntStateOf(prefs.loadThemeIndex()) }
    var pendingCloseSession by remember { mutableStateOf<TermSession?>(null) }

    // Handle deferred session close — runs AFTER recomposition
    LaunchedEffect(pendingCloseSession) {
        val closing = pendingCloseSession ?: return@LaunchedEffect
        pendingCloseSession = null
        val remaining = sessions.filter { it.id != closing.id }
        if (remaining.isEmpty()) {
            val newSession = TermSession(nextId, "Shell ${nextId + 1}")
            activeId = nextId; nextId++
            sessions = listOf(newSession)
        } else {
            if (activeId == closing.id) activeId = remaining.first().id
            sessions = remaining
        }
        // Destroy on IO thread to avoid blocking UI
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            delay(100) // let Compose drop the WebView from tree first
            closing.destroy()
        }
    }

    LaunchedEffect(Unit) {
        if (bootstrap.isInstalled() && bootstrap.hasProot()) bootstrap.updateStatus(BootstrapStatus.Ready)
        else bootstrap.updateStatus(BootstrapStatus.NotInstalled)
        if (sessions.isEmpty()) sessions = listOf(TermSession(0, "Shell 1"))
    }

    val activeSession = sessions.find { it.id == activeId }

    fun vibrate(ms: Long = 50) {
        try {
            if (Build.VERSION.SDK_INT >= 31) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                .vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    fun onBell() { vibrate(100); try { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50).startTone(ToneGenerator.TONE_PROP_BEEP, 100) } catch (_: Exception) {} }

    fun pasteClipboard() {
        val text = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.getItemAt(0)?.text?.toString() ?: return
        // Escape for JS and use xterm bracket paste mode
        val escaped = text.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r")
        activeSession?.webView?.evaluateJavascript("term.paste('$escaped')", null)
    }

    fun sendCommand(cmd: String) {
        activeSession?.ptyProcess?.write("$cmd\r")
    }

    fun startPty(session: TermSession, cols: Int, rows: Int) {
        session.ptyProcess?.destroy(); session.readerJob?.cancel()
        val canLinux = bootstrap.isInstalled() && bootstrap.hasProot()
        val env = mutableListOf("HOME=/storage/emulated/0", "TERM=xterm-256color", "LANG=en_US.UTF-8", "LC_ALL=en_US.UTF-8", "PATH=/system/bin:/system/xbin", "PROOT_TMP_DIR=${bootstrap.tmpDir.absolutePath}")
        val cmd: String; val args: Array<String>
        if (canLinux) {
            cmd = bootstrap.prootBin.absolutePath
            val shell = if (File(bootstrap.rootfsDir, "bin/bash").exists()) "/bin/bash" else "/bin/sh"
            args = arrayOf("--link2symlink", "--rootfs=${bootstrap.rootfsDir.absolutePath}", "-0", "-w", "/root", "-b", "/dev", "-b", "/proc", "-b", "/sys",
                "-b", "${bootstrap.homeDir.absolutePath}:/root", "-b", "${bootstrap.tmpDir.absolutePath}:/tmp", "-b", "/storage/emulated/0:/sdcard", "-b", "/storage",
                "/usr/bin/env", "-i", "HOME=/root", "USER=root", "LOGNAME=root",
                "PATH=/root/.bun/bin:/root/.opencode/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                "TERM=xterm-256color", "LANG=C.UTF-8", "TMPDIR=/tmp", shell, "--login")
        } else { cmd = "/system/bin/sh"; args = arrayOf("-l"); env.add("PWD=${initialDir ?: "/storage/emulated/0"}") }

        val pty = PtyProcess.start(cmd, args, env.toTypedArray(), rows, cols) ?: return
        session.ptyProcess = pty
        session.readerJob = scope.launch(Dispatchers.IO) {
            val buf = ByteArray(8192)
            try { while (isActive && pty.isAlive && session.webView != null) {
                val n = pty.read(buf); if (n <= 0) break
                val data = buf.copyOf(n)
                val b64 = Base64.encodeToString(data, Base64.NO_WRAP)
                if (session.recorder.isRecording) { try { session.recorder.write(String(data, Charsets.UTF_8)) } catch (_: Exception) {} }
                val wv = session.webView ?: break
                try { withContext(Dispatchers.Main) { wv.evaluateJavascript("termWrite('$b64')", null) } } catch (_: Exception) { break }
            } } catch (_: Exception) {}
            try { withContext(Dispatchers.Main) { vibrate(200) } } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) { onDispose { sessions.forEach { it.destroy() } } }

    fun switchSession(delta: Int) {
        val idx = sessions.indexOfFirst { it.id == activeId }; if (idx < 0) return
        val ni = (idx + delta).coerceIn(0, sessions.lastIndex)
        if (ni != idx) { activeId = sessions[ni].id; vibrate(30) }
    }

    fun sendKey(key: String) {
        val s = activeSession ?: return
        if (key == "CTRL") { ctrlDown = !ctrlDown; return }
        if (key == "ALT") { altDown = !altDown; return }
        val data: String? = when {
            ctrlDown && key.length == 1 && key[0].uppercaseChar() in 'A'..'Z' -> String(byteArrayOf((key[0].uppercaseChar().code - 64).toByte()))
            altDown && key.length == 1 -> "\u001b$key"
            else -> when (key) {
                "ESC"->"\u001b";"TAB"->"\t";"ENTER"->"\r";"BKSP"->"\u007f";"DEL"->"\u001b[3~"
                "↑"->"\u001b[A";"↓"->"\u001b[B";"→"->"\u001b[C";"←"->"\u001b[D"
                "HOME"->"\u001b[H";"END"->"\u001b[F";"PGUP"->"\u001b[5~";"PGDN"->"\u001b[6~"
                else -> if (key.length <= 2) key else null
            }
        }
        if (data != null) s.ptyProcess?.write(data)
        ctrlDown = false; altDown = false
    }

    // Full screen overlays
    if (showCommands) { CommandsReferenceScreen(onBack = { showCommands = false }); return }
    if (showSSH) { SSHScreen(onBack = { showSSH = false }, onConnect = { cmd -> showSSH = false; sendCommand(cmd) }); return }

    Box(modifier.fillMaxSize().background(Color.Black).imePadding()
        .onKeyEvent { e -> if (e.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) when (e.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> { activeSession?.ptyProcess?.write(byteArrayOf(3)); true }
            KeyEvent.KEYCODE_VOLUME_DOWN -> { activeSession?.ptyProcess?.write(byteArrayOf(26)); true }
            else -> false } else false }
    ) {
        Column(Modifier.fillMaxSize()) {
            // Tabs
            Row(Modifier.fillMaxWidth().background(Color(0xFF0A0A0A)).padding(top = 32.dp).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick, Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(16.dp), tint = Color(0xFF888888)) }
                sessions.forEach { session ->
                    val isActive = session.id == activeId
                    Box(Modifier.padding(horizontal = 2.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(if (isActive) Color(0xFF1A1A1A) else Color.Transparent)
                        .combinedClickable(onClick = { activeId = session.id }, onLongClick = { vibrate(30); renamingSession = session; renameText = session.name })
                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val ptyAlive = session.ptyProcess?.isAlive == true
                            if (session.isReady) Box(Modifier.size(6.dp).clip(CircleShape).background(if (ptyAlive) Color(0xFF00E676) else Color(0xFF666666)))
                            else if (session.ptyProcess != null) Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF00E676)))
                            if (session.recorder.isRecording) Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF5350)))
                            Text(session.name, color = if (isActive) Color.White else Color(0xFF666666), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (sessions.size > 1) Icon(Icons.Rounded.Close, null, Modifier.size(14.dp).clickable {
                                pendingCloseSession = session
                            }, tint = Color(0xFF555555))
                        }
                    }
                }
                IconButton(onClick = { val s = TermSession(nextId, "Shell ${nextId + 1}"); sessions = sessions + s; activeId = nextId; nextId++ }, Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(16.dp), tint = Color(0xFF666666)) }
                Spacer(Modifier.weight(1f))
                // Record toggle
                activeSession?.let { s ->
                    IconButton(onClick = {
                        if (s.recorder.isRecording) { s.recorder.stop(); Toast.makeText(context, "Recording: ${s.recorder.getFilePath()}", Toast.LENGTH_LONG).show() }
                        else { val p = s.recorder.start(File(context.filesDir, "recordings")); Toast.makeText(context, "Recording: $p", Toast.LENGTH_SHORT).show() }
                        sessions = sessions.toList()
                    }, Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.FiberManualRecord, null, Modifier.size(16.dp),
                            tint = if (s.recorder.isRecording) Color(0xFFEF5350) else Color(0xFF444444))
                    }
                }
                IconButton(onClick = { showSettings = !showSettings }, Modifier.size(36.dp)) { Icon(Icons.Rounded.Settings, null, Modifier.size(16.dp), tint = Color(0xFF555555)) }
            }

            // WebView
            Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black)
                .pointerInput(sessions.size) {
                    var dx = 0f; var dy = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dx = 0f },
                        onDragEnd = { if (abs(dx) > 150) switchSession(if (dx < 0) 1 else -1) },
                        onHorizontalDrag = { _, d -> dx += d })
                }
            ) {
                sessions.forEach { session ->
                    if (session.id == activeId) { key(session.id) {
                        val js = remember(session.id) { object {
                            @JavascriptInterface fun onInput(d: String) { session.ptyProcess?.write(d) }
                            @JavascriptInterface fun onResize(c: Int, r: Int) { session.ptyProcess?.resize(r, c) }
                            @JavascriptInterface fun onReady(c: Int, r: Int) {
                                session.isReady = true; scope.launch { startPty(session, c, r)
                                    // Auto-cd to initial directory if set
                                    if (initialDir != null && session.id == 0) {
                                        delay(500)
                                        session.ptyProcess?.write("cd /sdcard/${initialDir.removePrefix("/storage/emulated/0/")}\r")
                                    }
                                }
                                // Apply saved theme and font size on load
                                val themeIdx = prefs.loadThemeIndex()
                                if (themeIdx in TermuxThemes.themes.indices) {
                                    val t = TermuxThemes.themes[themeIdx]
                                    scope.launch(Dispatchers.Main) {
                                        session.webView?.evaluateJavascript(TermuxThemes.toJsTheme(t), null)
                                        session.webView?.evaluateJavascript("setFontSize(${prefs.fontSize.toInt()})", null)
                                    }
                                }
                            }
                            @JavascriptInterface fun onBell() { scope.launch(Dispatchers.Main) { onBell() } }
                            @JavascriptInterface fun onUrlClick(u: String) { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) } catch (_: Exception) {} }
                        } }
                        AndroidView(factory = { ctx -> (session.webView ?: WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            settings.javaScriptEnabled = true; settings.domStorageEnabled = true
                            settings.setNeedInitialFocus(true); settings.textZoom = 100
                            webViewClient = WebViewClient(); webChromeClient = WebChromeClient()
                            setBackgroundColor(android.graphics.Color.BLACK)
                            isFocusable = true; isFocusableInTouchMode = true
                            addJavascriptInterface(js, "AndroidPty")
                            setOnLongClickListener { pasteClipboard(); vibrate(30); true }
                            loadUrl("file:///android_asset/terminal.html"); session.webView = this
                        }) }, Modifier.fillMaxSize())
                        // Dead pty overlay — reconnect
                        if (session.isReady && session.ptyProcess?.isAlive != true) {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xCC1A1A1A))
                                    .clickable { scope.launch { session.webView?.let { wv ->
                                        val dims = kotlinx.coroutines.withContext(Dispatchers.Main) { wv.evaluateJavascript("getDimensions()") { } }
                                        startPty(session, 80, 24)
                                    } } }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)) {
                                    Text("⟳ Процесс завершён — нажми чтобы перезапустить", color = Color(0xFFFF9800), fontSize = 12.sp)
                                }
                            }
                        }
                    } }
                }
            }

            // Quick actions bar (swipe up area)
            AnimatedVisibility(showQuickActions, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                QuickActionsPanel(onCommand = { sendCommand(it); showQuickActions = false }, onDismiss = { showQuickActions = false })
            }

            // Extra keys
            Column(Modifier.fillMaxWidth().background(Color(0xFF1A1A1A))
                .pointerInput(Unit) { var dy = 0f
                    detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragEnd = { if (dy < -80) showQuickActions = true },
                        onVerticalDrag = { _, d -> dy += d })
                }
            ) {
                Row(Modifier.fillMaxWidth().height(38.dp)) {
                    listOf("ESC", "TAB", "CTRL", "ALT", "HOME", "↑", "END").forEach { k ->
                        val a = (k == "CTRL" && ctrlDown) || (k == "ALT" && altDown)
                        Box(Modifier.weight(1f).fillMaxHeight().background(if (a) Color(0xFF333333) else Color.Transparent).clickable { sendKey(k) }, contentAlignment = Alignment.Center) {
                            Text(k, color = if (a) Color(0xFF00E676) else Color(0xFFAAAAAA), fontSize = 13.sp, fontFamily = FontFamily.Monospace) }
                    }
                }
                Row(Modifier.fillMaxWidth().height(38.dp)) {
                    listOf("BKSP", "-", "/", "|", "←", "↓", "→").forEach { k ->
                        Box(Modifier.weight(1f).fillMaxHeight().clickable { sendKey(k) }, contentAlignment = Alignment.Center) {
                            Text(k, color = Color(0xFFAAAAAA), fontSize = 13.sp, fontFamily = FontFamily.Monospace) }
                    }
                }
                // Swipe hint
                Box(Modifier.fillMaxWidth().height(3.dp).padding(horizontal = 140.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF333333)))
            }
        }

        // Rename dialog
        if (renamingSession != null) AlertDialog(onDismissRequest = { renamingSession = null },
            title = { Text(Strings.rename) },
            text = { OutlinedTextField(renameText, { renameText = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { renamingSession?.name = renameText; sessions = sessions.toList(); renamingSession = null }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { renamingSession = null }) { Text(Strings.cancel) } })

        // Settings
        if (showSettings) SettingsPanel(bootstrap, bsStatus, prefs, activeSession?.webView, selectedThemeIdx,
            onClose = { showSettings = false }, onShowCommands = { showSettings = false; showCommands = true },
            onShowSSH = { showSettings = false; showSSH = true },
            onThemeSelect = { idx ->
                selectedThemeIdx = idx; prefs.saveThemeIndex(idx)
                val t = TermuxThemes.themes[idx]
                activeSession?.webView?.evaluateJavascript(TermuxThemes.toJsTheme(t), null)
            },
            onInstall = { scope.launch { bootstrap.install(LinuxDistro.UBUNTU) } },
            onUninstall = { sessions.forEach { it.destroy() }; bootstrap.uninstall() })
    }
}

// ═══════════════════════════════════
// Quick Actions Panel
// ═══════════════════════════════════
@Composable
private fun QuickActionsPanel(onCommand: (String) -> Unit, onDismiss: () -> Unit) {
    val quickCmds = listOf(
        "📦" to "apt update" to "Update packages",
        "📥" to "apt upgrade -y" to "Install updates",
        "📊" to "htop" to "Process monitor",
        "💾" to "df -h" to "Disk space",
        "📁" to "ls -la" to "List files",
        "🌐" to "curl ifconfig.me" to "My IP",
        "🧹" to "apt autoremove -y" to "Cleanup",
        "📝" to "nano" to "Text editor",
        "🔀" to "git status" to "Git статус",
        "🐍" to "python3 --version" to "Python version",
        "📡" to "ping -c 3 google.com" to "Network check",
        "🥟" to "bun --version" to "Bun version",
    )

    Column(Modifier.fillMaxWidth().background(Color(0xFF111111), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(Strings.quickCommands, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onDismiss, Modifier.size(28.dp)) { Icon(Icons.Rounded.Close, null, Modifier.size(16.dp), tint = Color(0xFF666666)) }
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.heightIn(max = 200.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(quickCmds) { (iconCmd, desc) ->
                val (icon, cmd) = iconCmd
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A)).clickable { onCommand(cmd) }.padding(8.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(icon, fontSize = 18.sp)
                        Text(desc, color = Color(0xFF999999), fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════
// Settings Panel
// ═══════════════════════════════════
@Composable
private fun SettingsPanel(
    bootstrap: TermuxBootstrap, bsStatus: BootstrapStatus, prefs: TerminalPrefs,
    webView: WebView?, selectedTheme: Int,
    onClose: () -> Unit, onShowCommands: () -> Unit, onShowSSH: () -> Unit,
    onThemeSelect: (Int) -> Unit, onInstall: () -> Unit, onUninstall: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.7f)).clickable(onClick = onClose)) {
        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter)
            .background(Color(0xFF111111), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clickable(enabled = false, onClick = {}).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.termSettings, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, null, tint = Color(0xFF666666)) }
            }

            // Font
            Text("${Strings.fontLabel}: ${prefs.fontSize.toInt()}px", color = Color(0xFF888888), fontSize = 12.sp)
            Slider(prefs.fontSize, { prefs.changeFontSize(it); webView?.evaluateJavascript("setFontSize(${it.toInt()})", null) },
                valueRange = 9f..22f, colors = SliderDefaults.colors(thumbColor = Color(0xFF00E676), activeTrackColor = Color(0xFF00E676)))

            // Themes
            Text(Strings.themeLabel, color = Color(0xFF888888), fontSize = 12.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TermuxThemes.themes.forEachIndexed { i, t ->
                    Box(Modifier.size(44.dp, 32.dp).clip(RoundedCornerShape(6.dp))
                        .background(Color(android.graphics.Color.parseColor(t.bg)))
                        .border(2.dp, if (i == selectedTheme) Color(0xFF00E676) else Color(0xFF333333), RoundedCornerShape(6.dp))
                        .clickable { onThemeSelect(i) }, contentAlignment = Alignment.Center) {
                        Text(t.name.take(2), color = Color(android.graphics.Color.parseColor(t.fg)), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Actions
            SettingsButton("📖", Strings.commandRef, Strings.commandsCount) { onShowCommands() }
            SettingsButton("🔗", Strings.sshTitle, Strings.savedServers) { onShowSSH() }

            // Gestures
            Column(Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(Strings.gesturesTitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                listOf("Swipe ← → — switch tabs", "Swipe ↑ on keys — quick commands",
                    "Long press — paste from clipboard", "Long press tab — rename",
                    "Volume ↑ = Ctrl+C, ↓ = Ctrl+Z", "🔴 Button — record session to file"
                ).forEach { Text("• $it", color = Color(0xFF888888), fontSize = 12.sp) }
            }

            // Linux
            val distroName = bootstrap.getDistro().label
            Column(Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Linux ($distroName)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(when (val s = bsStatus) {
                    is BootstrapStatus.Ready -> "✅ Установлен • bun, git, python3"; is BootstrapStatus.NotInstalled -> "❌ Не установлен"
                    is BootstrapStatus.Downloading -> "⬇️ ${(s.progress * 100).toInt()}%"; is BootstrapStatus.Extracting -> "📦 Распаковка..."
                    is BootstrapStatus.Configuring -> "⚙️ Настройка..."; is BootstrapStatus.Error -> "❌ ${s.message.take(300)}"; else -> "..."
                }, color = Color(0xFF888888), fontSize = 12.sp)
            }
            when (bsStatus) {
                is BootstrapStatus.NotInstalled, is BootstrapStatus.Error ->
                    Box(Modifier.fillMaxWidth().clickable(onClick = onInstall).background(Color(0xFF00E676).copy(0.15f), RoundedCornerShape(10.dp)).padding(14.dp), contentAlignment = Alignment.Center) {
                        Text(Strings.install + " Ubuntu", color = Color(0xFF00E676), fontWeight = FontWeight.SemiBold) }
                is BootstrapStatus.Ready ->
                    Box(Modifier.fillMaxWidth().clickable(onClick = onUninstall).background(Color(0xFFEF5350).copy(0.1f), RoundedCornerShape(10.dp)).padding(14.dp), contentAlignment = Alignment.Center) {
                        Text(Strings.delete + " $distroName", color = Color(0xFFEF5350)) }
                else -> {
                    Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF00E676),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsButton(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A1A1A)).clickable(onClick = onClick).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(icon, fontSize = 18.sp)
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color(0xFF888888), fontSize = 12.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp), tint = Color(0xFF555555))
        }
    }
}
