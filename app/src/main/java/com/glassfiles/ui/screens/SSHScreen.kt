package com.glassfiles.ui.screens

import android.widget.Toast
import com.glassfiles.data.Strings
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
import com.glassfiles.data.*
import com.glassfiles.data.terminal.SSHConnection
import com.glassfiles.data.*
import com.glassfiles.data.terminal.SSHManager

@Composable
fun SSHScreen(
    onBack: () -> Unit,
    onConnect: (String) -> Unit // passes ssh command to terminal
) {
    val context = LocalContext.current
    val mgr = remember { SSHManager(context) }
    var connections by remember { mutableStateOf(mgr.getAll()) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SSHConnection?>(null) }

    fun refresh() { connections = mgr.getAll() }

    Column(Modifier.fillMaxSize().background(Color(0xFF0D0D0D))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(top = 36.dp, start = 4.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Color.White) }
            Text(Strings.sshTitle, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { editing = null; showAdd = true }) {
                Icon(Icons.Rounded.Add, null, Modifier.size(22.dp), tint = Color(0xFF00E676))
            }
        }

        if (connections.isEmpty() && !showAdd) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.Dns, null, Modifier.size(56.dp), tint = Color(0xFF333333))
                    Text("No saved servers", color = Color(0xFF666666), fontSize = 15.sp)
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF00E676).copy(0.15f))
                        .clickable { showAdd = true }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text("Add server", color = Color(0xFF00E676), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(connections) { conn ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A))
                        .clickable { onConnect(conn.toCommand()) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).background(Color(0xFF00E676).copy(0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Dns, null, Modifier.size(20.dp), tint = Color(0xFF00E676))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(conn.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("${conn.user}@${conn.host}:${conn.port}", color = Color(0xFF00E676),
                                fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            if (conn.useKey) Text("🔑 Ключ", color = Color(0xFF666666), fontSize = 11.sp)
                        }
                        IconButton(onClick = { editing = conn; showAdd = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp), tint = Color(0xFF555555))
                        }
                        IconButton(onClick = { mgr.delete(conn.id); refresh() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Delete, null, Modifier.size(16.dp), tint = Color(0xFF555555))
                        }
                    }
                }
            }
        }

        if (showAdd) {
            SSHEditDialog(
                initial = editing,
                onSave = { conn -> mgr.save(conn); refresh(); showAdd = false; editing = null },
                onDismiss = { showAdd = false; editing = null }
            )
        }
    }
}

@Composable
private fun SSHEditDialog(initial: SSHConnection?, onSave: (SSHConnection) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var port by remember { mutableStateOf(initial?.port?.toString() ?: "22") }
    var user by remember { mutableStateOf(initial?.user ?: "root") }
    var password by remember { mutableStateOf(initial?.password ?: "") }
    var keyPath by remember { mutableStateOf(initial?.keyPath ?: "") }
    var useKey by remember { mutableStateOf(initial?.useKey ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text(if (initial != null) Strings.edit else Strings.newServer, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(Strings.serverName, name) { name = it }
                Field("Host (IP or domain)", host) { host = it }
                Field(Strings.port, port, KeyboardType.Number) { port = it }
                Field(Strings.user, user) { user = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Key authentication", color = Color(0xFF888888), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(checked = useKey, onCheckedChange = { useKey = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF00E676)))
                }
                if (useKey) Field("Key path", keyPath) { keyPath = it }
                else Field(Strings.password, password, isPassword = true) { password = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (host.isNotBlank() && user.isNotBlank()) {
                    onSave(SSHConnection(
                        id = initial?.id ?: System.currentTimeMillis().toString(),
                        name = name.ifBlank { "$user@$host" }, host = host,
                        port = port.toIntOrNull() ?: 22, user = user,
                        password = password, keyPath = keyPath, useKey = useKey
                    ))
                }
            }) { Text(Strings.save, color = Color(0xFF00E676)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel, color = Color(0xFF888888)) } }
    )
}

@Composable
private fun Field(label: String, value: String, keyboardType: KeyboardType = KeyboardType.Text,
                  isPassword: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label, fontSize = 12.sp) },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00E676), unfocusedBorderColor = Color(0xFF333333),
            cursorColor = Color(0xFF00E676), focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedLabelColor = Color(0xFF00E676), unfocusedLabelColor = Color(0xFF666666)
        )
    )
}
