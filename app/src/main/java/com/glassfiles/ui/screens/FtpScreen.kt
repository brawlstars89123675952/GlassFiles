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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.Strings
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class FtpServer(val id: String, val name: String, val host: String, val port: Int, val user: String, val pass: String, val isSftp: Boolean)

data class FtpFileItem(val name: String, val size: Long, val isDirectory: Boolean, val modified: Long)

object FtpStore {
    private const val PREFS = "ftp_servers"
    private const val KEY = "servers_json"

    fun load(context: Context): List<FtpServer> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                FtpServer(o.getString("id"), o.getString("name"), o.getString("host"), o.getInt("port"),
                    o.getString("user"), o.getString("pass"), o.optBoolean("sftp", false))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun save(context: Context, servers: List<FtpServer>) {
        val arr = JSONArray()
        servers.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id); put("name", s.name); put("host", s.host); put("port", s.port)
                put("user", s.user); put("pass", s.pass); put("sftp", s.isSftp)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    fun add(context: Context, server: FtpServer) {
        val list = load(context).toMutableList()
        list.add(server)
        save(context, list)
    }

    fun remove(context: Context, id: String) {
        save(context, load(context).filter { it.id != id })
    }
}

@Composable
fun FtpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var servers by remember { mutableStateOf(FtpStore.load(context)) }
    var showAdd by remember { mutableStateOf(false) }
    var connectedServer by remember { mutableStateOf<FtpServer?>(null) }
    var ftpClient by remember { mutableStateOf<FTPClient?>(null) }
    var currentPath by remember { mutableStateOf("/") }
    var files by remember { mutableStateOf<List<FtpFileItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }

    fun refresh() { servers = FtpStore.load(context) }

    fun disconnect() {
        scope.launch(Dispatchers.IO) {
            try { ftpClient?.disconnect() } catch (_: Exception) {}
            ftpClient = null
        }
        connectedServer = null; files = emptyList(); currentPath = "/"; statusMsg = ""
    }

    fun loadDir(client: FTPClient, path: String) {
        loading = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    client.changeWorkingDirectory(path)
                    currentPath = client.printWorkingDirectory() ?: path
                    client.listFiles()?.map { f ->
                        FtpFileItem(f.name, f.size, f.isDirectory, f.timestamp?.timeInMillis ?: 0L)
                    }?.filter { it.name != "." && it.name != ".." }
                        ?.sortedWith(compareByDescending<FtpFileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
                        ?: emptyList()
                } catch (e: Exception) { statusMsg = "${Strings.error}: ${e.message}"; emptyList() }
            }
            files = result; loading = false
        }
    }

    fun connect(server: FtpServer) {
        loading = true; statusMsg = Strings.ftpConnecting
        scope.launch {
            val client = withContext(Dispatchers.IO) {
                try {
                    val c = FTPClient().apply {
                        connectTimeout = 10000
                        connect(server.host, server.port)
                        login(server.user, server.pass)
                        enterLocalPassiveMode()
                        setFileType(FTP.BINARY_FILE_TYPE)
                    }
                    c
                } catch (e: Exception) {
                    statusMsg = "${Strings.ftpError}: ${e.message}"; null
                }
            }
            if (client != null) {
                ftpClient = client; connectedServer = server
                statusMsg = Strings.ftpConnected
                loadDir(client, "/")
            }
            loading = false
        }
    }

    // Connected — browse mode
    if (connectedServer != null && ftpClient != null) {
        FtpBrowseView(
            server = connectedServer!!,
            currentPath = currentPath,
            files = files,
            loading = loading,
            statusMsg = statusMsg,
            onBack = {
                if (currentPath == "/") disconnect()
                else {
                    val parent = currentPath.substringBeforeLast("/", "/").ifEmpty { "/" }
                    loadDir(ftpClient!!, parent)
                }
            },
            onDisconnect = { disconnect() },
            onNavigate = { item ->
                if (item.isDirectory) loadDir(ftpClient!!, "$currentPath/${item.name}")
            },
            onDownload = { item ->
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            val destDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "GlassFiles_FTP")
                            destDir.mkdirs()
                            val dest = File(destDir, item.name)
                            FileOutputStream(dest).use { fos ->
                                ftpClient!!.retrieveFile("$currentPath/${item.name}", fos)
                            }
                            true
                        } catch (_: Exception) { false }
                    }
                    Toast.makeText(context, if (ok) Strings.ftpDownloaded else Strings.error, Toast.LENGTH_SHORT).show()
                }
            }
        )
        return
    }

    // Server list
    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        Row(Modifier.fillMaxWidth().background(SurfaceWhite).padding(top = 44.dp, start = 4.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Blue) }
            Text(Strings.ftpClient, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, null, Modifier.size(22.dp), tint = Blue) }
        }

        if (statusMsg.isNotBlank()) {
            Text(statusMsg, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        if (servers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Cloud, null, Modifier.size(48.dp), tint = TextTertiary)
                    Spacer(Modifier.height(12.dp))
                    Text(Strings.ftpNoFiles, color = TextSecondary, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(Strings.ftpSaved, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
                items(servers) { server ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite)
                        .clickable { connect(server) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).background(Color(0xFF2196F3).copy(0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Dns, null, Modifier.size(22.dp), tint = Color(0xFF2196F3))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(server.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("${if (server.isSftp) "SFTP" else "FTP"} • ${server.host}:${server.port}", fontSize = 12.sp, color = TextSecondary,
                                fontFamily = FontFamily.Monospace)
                        }
                        IconButton(onClick = { FtpStore.remove(context, server.id); refresh() }) {
                            Icon(Icons.Rounded.Close, null, Modifier.size(18.dp), tint = TextTertiary)
                        }
                    }
                }
            }
        }
    }

    // Add server dialog
    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var host by remember { mutableStateOf("") }
        var port by remember { mutableStateOf("21") }
        var user by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = SurfaceWhite,
            title = { Text(Strings.ftpConnect, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(host, { host = it }, label = { Text(Strings.ftpHost) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(port, { port = it }, label = { Text(Strings.ftpPort) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(user, { user = it }, label = { Text(Strings.ftpUser) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(pass, { pass = it }, label = { Text(Strings.ftpPassword) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (host.isNotBlank()) {
                        FtpStore.add(context, FtpServer(UUID.randomUUID().toString(), name.ifBlank { host },
                            host, port.toIntOrNull() ?: 21, user, pass, false))
                        refresh(); showAdd = false
                    }
                }) { Text(Strings.create, color = Blue) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(Strings.cancel, color = TextSecondary) } }
        )
    }
}

@Composable
private fun FtpBrowseView(
    server: FtpServer, currentPath: String, files: List<FtpFileItem>, loading: Boolean, statusMsg: String,
    onBack: () -> Unit, onDisconnect: () -> Unit, onNavigate: (FtpFileItem) -> Unit, onDownload: (FtpFileItem) -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        Row(Modifier.fillMaxWidth().background(SurfaceWhite).padding(top = 44.dp, start = 4.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Blue) }
            Column(Modifier.weight(1f)) {
                Text(server.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(currentPath, fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onDisconnect) {
                Icon(Icons.Rounded.LinkOff, null, Modifier.size(20.dp), tint = Red)
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue, modifier = Modifier.size(32.dp))
            }
        } else if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Strings.ftpNoFiles, color = TextSecondary, fontSize = 15.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(files) { item ->
                    Row(Modifier.fillMaxWidth().clickable { if (item.isDirectory) onNavigate(item) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            if (item.isDirectory) Icons.Rounded.Folder else Icons.Rounded.InsertDriveFile,
                            null, Modifier.size(28.dp),
                            tint = if (item.isDirectory) FolderBlue else TextSecondary
                        )
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!item.isDirectory) Text(fmtSize(item.size), fontSize = 11.sp, color = TextSecondary)
                                if (item.modified > 0) Text(sdf.format(Date(item.modified)), fontSize = 11.sp, color = TextTertiary)
                            }
                        }
                        if (!item.isDirectory) {
                            IconButton(onClick = { onDownload(item) }) {
                                Icon(Icons.Rounded.Download, null, Modifier.size(20.dp), tint = Blue)
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().padding(start = 56.dp).height(0.5.dp).background(SeparatorColor))
                }
            }
        }
    }
}

private fun fmtSize(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024L * 1024 -> "%.1f KB".format(b / 1024.0)
    b < 1024L * 1024 * 1024 -> "%.1f MB".format(b / (1024.0 * 1024))
    else -> "%.2f GB".format(b / (1024.0 * 1024 * 1024))
}
