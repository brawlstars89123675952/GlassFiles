package com.glassfiles.ui.screens
import com.glassfiles.data.Strings

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.ai.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════
// Main entry — shows history or chat
// ═══════════════════════════════════

@Composable
fun AiChatScreen(onBack: () -> Unit, initialPrompt: String? = null, initialImageBase64: String? = null) {
    val context = LocalContext.current
    val historyMgr = remember { ChatHistoryManager(context) }
    var activeSessionId by remember { mutableStateOf<String?>(null) }
    var sessions by remember { mutableStateOf(historyMgr.getSessions()) }

    var consumedPrompt by remember { mutableStateOf(false) }
    val effectivePrompt = if (!consumedPrompt) initialPrompt else null
    val effectiveImage = if (!consumedPrompt) initialImageBase64 else null

    // Если передан initialPrompt — сразу создаём чат
    LaunchedEffect(initialPrompt, initialImageBase64) {
        if (initialPrompt != null && activeSessionId == null && !consumedPrompt) {
            val s = historyMgr.createSession(AiProvider.GEMINI_FLASH)
            historyMgr.saveSession(s)
            activeSessionId = s.id
            consumedPrompt = true
        }
    }

    fun refresh() { sessions = historyMgr.getSessions() }

    if (activeSessionId != null) {
        ChatView(
            sessionId = activeSessionId!!,
            historyMgr = historyMgr,
            onBack = { activeSessionId = null; refresh() },
            initialPrompt = effectivePrompt,
            initialImageBase64 = effectiveImage
        )
    } else {
        ChatHistoryList(
            sessions = sessions,
            onNewChat = {
                val s = historyMgr.createSession(AiProvider.AUTO)
                historyMgr.saveSession(s)
                activeSessionId = s.id
            },
            onOpenChat = { activeSessionId = it.id },
            onDeleteChat = { historyMgr.deleteSession(it.id); refresh() },
            onDeleteAll = { historyMgr.deleteAll(); refresh() },
            onBack = onBack
        )
    }
}

// ═══════════════════════════════════
// Chat History List
// ═══════════════════════════════════

@Composable
private fun ChatHistoryList(
    sessions: List<ChatSession>,
    onNewChat: () -> Unit,
    onOpenChat: (ChatSession) -> Unit,
    onDeleteChat: (ChatSession) -> Unit,
    onDeleteAll: () -> Unit,
    onBack: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().background(Color(0xFF0D1117))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF161B22)).padding(top = 36.dp, start = 4.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Color.White) }
            Text(Strings.aiChat, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            if (sessions.isNotEmpty()) {
                IconButton(onClick = onDeleteAll) { Icon(Icons.Rounded.DeleteSweep, null, Modifier.size(22.dp), tint = Color(0xFF8B949E)) }
            }
        }

        if (sessions.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(56.dp), tint = Color(0xFF58A6FF))
                    Text(Strings.noChats, color = Color(0xFF8B949E), fontSize = 16.sp)
                    Text(Strings.startNewChat, color = Color(0xFF484F58), fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sessions) { session ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF161B22))
                        .clickable { onOpenChat(session) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).background(Color(0xFF21262D), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Chat, null, Modifier.size(20.dp), tint = Color(0xFF58A6FF))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(if (session.title == "Новый чат" || session.title == "New chat") Strings.newChat else session.title, color = Color(0xFFE6EDF3), fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(sdf.format(Date(session.updatedAt)), color = Color(0xFF484F58), fontSize = 11.sp)
                                Text("${session.messages.size} ${Strings.messages}", color = Color(0xFF484F58), fontSize = 11.sp)
                                Text(try { AiProvider.valueOf(session.provider).label } catch (_: Exception) { session.provider }, color = Color(0xFF58A6FF), fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = { onDeleteChat(session) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, null, Modifier.size(16.dp), tint = Color(0xFF484F58))
                        }
                    }
                }
            }
        }

        // New chat button
        Box(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF238636)).clickable(onClick = onNewChat).padding(14.dp),
            contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Add, null, Modifier.size(20.dp), tint = Color.White)
                Text(Strings.newChat, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ═══════════════════════════════════
// Chat View
// ═══════════════════════════════════

@Composable
private fun ChatView(sessionId: String, historyMgr: ChatHistoryManager, onBack: () -> Unit,
    initialPrompt: String? = null, initialImageBase64: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember { historyMgr.getSession(sessionId) }

    var messages by remember { mutableStateOf(session?.messages ?: emptyList()) }
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var currentResponse by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf(try { AiProvider.valueOf(session?.provider ?: "GEMINI_FLASH") } catch (_: Exception) { AiProvider.GEMINI_FLASH }) }
    var showProviderMenu by remember { mutableStateOf(false) }
    var attachedImage by remember { mutableStateOf<String?>(null) }
    var attachedFile by remember { mutableStateOf<Pair<String, String>?>(null) }
    val listState = rememberLazyListState()
    var geminiKey by remember { mutableStateOf(GeminiKeyStore.getKey(context)) }
    var showKeyDialog by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf(geminiKey) }
    var openRouterKey by remember { mutableStateOf(GeminiKeyStore.getOpenRouterKey(context)) }
    var orKeyInput by remember { mutableStateOf(openRouterKey) }
    var proxyUrl by remember { mutableStateOf(GeminiKeyStore.getProxy(context)) }
    var proxyInput by remember { mutableStateOf(proxyUrl) }
    var autoSent by remember { mutableStateOf(false) }

    // File picker
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val mime = context.contentResolver.getType(uri) ?: ""
            val isImage = mime.startsWith("image/")

            // Получаем имя файла
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val name = cursor?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            } ?: uri.lastPathSegment ?: "file"

            if (isImage) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val file = File(context.cacheDir, "ai_img.jpg")
                    file.outputStream().use { out -> stream.copyTo(out) }
                    attachedImage = AiManager.encodeImage(file)
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    val truncated = if (text.length > 8000) text.take(8000) + "\n...[обрезано]" else text
                    attachedFile = Pair(name, truncated)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "${Strings.error}: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun save(msgs: List<ChatMessage>) {
        val title = historyMgr.generateTitle(msgs)
        historyMgr.saveSession(ChatSession(
            id = sessionId, title = title, provider = provider.name,
            messages = msgs, createdAt = session?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ))
    }

    LaunchedEffect(messages.size, currentResponse) {
        val total = messages.size + if (currentResponse.isNotEmpty()) 1 else 0
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    // Авто-отправка начального промпта (из контекстного меню файлов)
    LaunchedEffect(initialPrompt) {
        if (initialPrompt != null && !autoSent && messages.isEmpty()) {
            autoSent = true
            val userMsg = ChatMessage("user", initialPrompt, initialImageBase64)
            messages = messages + userMsg
            isLoading = true
            try {
                val response = AiManager.chat(provider, listOf(userMsg), geminiKey, openRouterKey, proxyUrl) { chunk -> currentResponse += chunk }
                messages = messages + ChatMessage("assistant", response)
                currentResponse = ""
                val title = historyMgr.generateTitle(messages)
                historyMgr.saveSession(ChatSession(id = sessionId, title = title, provider = provider.name, messages = messages, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) {
                messages = messages + ChatMessage("assistant", "❌ ${e.message}")
            }
            isLoading = false
        }
    }

    fun send() {
        var text = input.trim()
        if (text.isEmpty() && attachedImage == null && attachedFile == null) return
        if (isLoading) return

        // Prepend file content
        if (attachedFile != null) {
            text = "File: ${attachedFile!!.first}\n```\n${attachedFile!!.second}\n```\n\n$text"
        }
        if (text.isEmpty()) text = "What is in this image?"

        val userMsg = ChatMessage("user", text, attachedImage)
        input = ""; currentResponse = ""
        attachedImage = null; attachedFile = null
        messages = messages + userMsg
        isLoading = true

        scope.launch {
            try {
                val response = AiManager.chat(provider, messages, geminiKey, openRouterKey, proxyUrl) { chunk -> currentResponse += chunk }
                messages = messages + ChatMessage("assistant", response)
                currentResponse = ""
                save(messages)
            } catch (e: Exception) {
                messages = messages + ChatMessage("assistant", "❌ ${e.message}")
                currentResponse = ""; save(messages)
            }
            isLoading = false
        }
    }

    fun copyText(text: String) {
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("ai", text))
        Toast.makeText(context, Strings.copied, Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF0D1117)).imePadding()) {
        // Top bar
        Row(Modifier.fillMaxWidth().background(Color(0xFF161B22)).padding(top = 36.dp, start = 4.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { save(messages); onBack() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Color.White)
            }
            Text("AI", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f))

            // Provider
            Box {
                Row(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF21262D))
                    .clickable { showProviderMenu = true }.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(
                        if (provider.isGemini) Color(0xFFD29922) else if (provider.supportsVision) Color(0xFF00D084) else Color(0xFF58A6FF)))
                    Text(provider.label, color = Color(0xFFC9D1D9), fontSize = 12.sp)
                    Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(16.dp), tint = Color(0xFF8B949E))
                }
                DropdownMenu(expanded = showProviderMenu, onDismissRequest = { showProviderMenu = false }) {
                    // Разделитель OpenRouter / Gemini
                    AiProvider.entries.forEach { p ->
                        // Разделитель перед первым Gemini
                        if (p == AiProvider.GEMINI_FLASH) {
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text("Gemini", fontSize = 11.sp, color = Color(0xFF484F58), fontWeight = FontWeight.SemiBold)
                                if (geminiKey.isBlank()) Text(" • no key", fontSize = 11.sp, color = Color(0xFFDA3633))
                            }
                        }
                        DropdownMenuItem(
                            text = { Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(p.label, fontSize = 14.sp)
                                if (p.supportsVision) Text("📷", fontSize = 12.sp)
                                if (p.isGemini) Text("🔑", fontSize = 12.sp)
                            } },
                            onClick = {
                                if (p.isGemini && geminiKey.isBlank()) {
                                    showProviderMenu = false; showKeyDialog = true
                                } else {
                                    provider = p; showProviderMenu = false
                                }
                            },
                            trailingIcon = { if (p == provider) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = Color(0xFF58A6FF)) }
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    // Кнопка настроек API
                    DropdownMenuItem(
                        text = { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Settings, null, Modifier.size(16.dp), tint = Color(0xFFD29922))
                            Text(Strings.apiSettings, fontSize = 14.sp, color = Color(0xFFD29922))
                        } },
                        onClick = { showProviderMenu = false; showKeyDialog = true }
                    )
                }
            }
        }

        // Messages
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            if (messages.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(48.dp), tint = Color(0xFF58A6FF))
                        Text(Strings.newChat, color = Color(0xFFC9D1D9), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("📷 = ${Strings.modelsWithPhoto}", color = Color(0xFF484F58), fontSize = 13.sp)
                        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Explain what Kotlin is", "Analyze this code", "Help with git").forEach { q ->
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF21262D))
                                    .clickable { input = q }.padding(12.dp)) {
                                    Text(q, color = Color(0xFF8B949E), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            items(messages) { msg -> Bubble(msg) { copyText(msg.content) } }

            if (currentResponse.isNotEmpty()) {
                item { Bubble(ChatMessage("assistant", currentResponse + "▊")) {} }
            }

            if (isLoading && currentResponse.isEmpty()) {
                item {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = Color(0xFF58A6FF), strokeWidth = 2.dp)
                        Text("${provider.label} думает...", color = Color(0xFF8B949E), fontSize = 13.sp)
                    }
                }
            }
        }

        // Attachments preview
        if (attachedImage != null || attachedFile != null) {
            Row(Modifier.fillMaxWidth().background(Color(0xFF161B22)).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (attachedImage != null) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF21262D)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Image, null, Modifier.size(20.dp), tint = Color(0xFF00D084))
                    }
                    Text(Strings.photoAttached, color = Color(0xFF8B949E), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { attachedImage = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, null, Modifier.size(14.dp), tint = Color(0xFF8B949E))
                    }
                }
                if (attachedFile != null) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF21262D)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Description, null, Modifier.size(20.dp), tint = Color(0xFF58A6FF))
                    }
                    Text(attachedFile!!.first, color = Color(0xFF8B949E), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { attachedFile = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, null, Modifier.size(14.dp), tint = Color(0xFF8B949E))
                    }
                }
            }
        }

        // Input bar
        Row(Modifier.fillMaxWidth().background(Color(0xFF161B22)).padding(8.dp),
            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {

            // Attach button
            IconButton(onClick = { filePicker.launch("*/*") }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.AttachFile, null, Modifier.size(22.dp), tint = Color(0xFF8B949E))
            }

            BasicTextField(input, { input = it },
                Modifier.weight(1f).background(Color(0xFF21262D), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = TextStyle(Color(0xFFC9D1D9), 15.sp), cursorBrush = SolidColor(Color(0xFF58A6FF)),
                decorationBox = { inner -> if (input.isEmpty()) Text(Strings.message, color = Color(0xFF484F58), fontSize = 15.sp); inner() })

            Box(Modifier.size(44.dp).clip(CircleShape)
                .background(if ((input.isNotBlank() || attachedImage != null || attachedFile != null) && !isLoading) Color(0xFF238636) else Color(0xFF21262D))
                .clickable(enabled = (input.isNotBlank() || attachedImage != null || attachedFile != null) && !isLoading) { send() },
                contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(20.dp),
                    tint = if ((input.isNotBlank() || attachedImage != null || attachedFile != null) && !isLoading) Color.White else Color(0xFF484F58))
            }
        }
    }

    // Диалог настроек API
    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            containerColor = Color(0xFF161B22),
            title = { Text(Strings.apiSettings, color = Color(0xFFE6EDF3)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    // ── Gemini ──
                    Text("🔑 Gemini API key", color = Color(0xFFD29922), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("aistudio.google.com/apikey", color = Color(0xFF8B949E), fontSize = 12.sp)
                    OutlinedTextField(
                        value = keyInput, onValueChange = { keyInput = it }, singleLine = true,
                        placeholder = { Text("AIza...", color = Color(0xFF484F58)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE6EDF3), unfocusedTextColor = Color(0xFFC9D1D9),
                            focusedBorderColor = Color(0xFF58A6FF), unfocusedBorderColor = Color(0xFF30363D),
                            cursorColor = Color(0xFF58A6FF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── Прокси для Gemini ──
                    Text("🌐 Gemini Proxy (if blocked)", color = Color(0xFF58A6FF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Proxy URL, replaces googleapis.com", color = Color(0xFF8B949E), fontSize = 12.sp)
                    OutlinedTextField(
                        value = proxyInput, onValueChange = { proxyInput = it }, singleLine = true,
                        placeholder = { Text("https://my-proxy.com/v1beta/models", color = Color(0xFF484F58), fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE6EDF3), unfocusedTextColor = Color(0xFFC9D1D9),
                            focusedBorderColor = Color(0xFF58A6FF), unfocusedBorderColor = Color(0xFF30363D),
                            cursorColor = Color(0xFF58A6FF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (proxyUrl.isNotBlank()) {
                        Text("✅ Proxy: ${proxyUrl.take(40)}...", color = Color(0xFF3FB950), fontSize = 11.sp)
                    }

                    HorizontalDivider(color = Color(0xFF30363D))

                    // ── OpenRouter ──
                    Text("🔑 OpenRouter API key", color = Color(0xFF8B5CF6), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("openrouter.ai/keys", color = Color(0xFF8B949E), fontSize = 12.sp)
                    OutlinedTextField(
                        value = orKeyInput, onValueChange = { orKeyInput = it }, singleLine = true,
                        placeholder = { Text("sk-or-v1-...", color = Color(0xFF484F58)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFE6EDF3), unfocusedTextColor = Color(0xFFC9D1D9),
                            focusedBorderColor = Color(0xFF8B5CF6), unfocusedBorderColor = Color(0xFF30363D),
                            cursorColor = Color(0xFF8B5CF6)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    GeminiKeyStore.saveKey(context, keyInput)
                    geminiKey = keyInput.trim()
                    GeminiKeyStore.saveOpenRouterKey(context, orKeyInput)
                    openRouterKey = orKeyInput.trim()
                    GeminiKeyStore.saveProxy(context, proxyInput)
                    proxyUrl = proxyInput.trim()
                    showKeyDialog = false
                    if (geminiKey.isNotBlank() && provider.isGemini.not()) {
                        provider = AiProvider.GEMINI_FLASH
                    }
                    Toast.makeText(context, Strings.settingsSaved, Toast.LENGTH_SHORT).show()
                }) { Text(Strings.save, color = Color(0xFF58A6FF)) }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) { Text(Strings.cancel, color = Color(0xFF8B949E)) }
            }
        )
    }
}

// ═══════════════════════════════════
// Message Bubble
// ═══════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Bubble(msg: ChatMessage, onCopy: () -> Unit) {
    val isUser = msg.role == "user"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        // Image preview
        if (msg.imageBase64 != null) {
            val bmp = remember(msg.imageBase64) {
                try {
                    val bytes = android.util.Base64.decode(msg.imageBase64, android.util.Base64.NO_WRAP)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (_: Exception) { null }
            }
            if (bmp != null) {
                androidx.compose.foundation.Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Photo",
                    modifier = Modifier.widthIn(max = 240.dp).heightIn(max = 180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(4.dp))
            } else {
                Box(Modifier.padding(bottom = 4.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF21262D)).padding(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Image, null, Modifier.size(14.dp), tint = Color(0xFF00D084))
                        Text(Strings.photo, color = Color(0xFF8B949E), fontSize = 11.sp)
                    }
                }
            }
        }

        Box(Modifier.widthIn(max = 320.dp)
            .clip(RoundedCornerShape(16.dp, 16.dp, if (isUser) 4.dp else 16.dp, if (isUser) 16.dp else 4.dp))
            .background(if (isUser) Color(0xFF238636) else Color(0xFF21262D))
            .combinedClickable(onClick = {}, onLongClick = onCopy).padding(12.dp)) {
            val content = msg.content
            if (content.contains("```")) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    content.split("```").forEachIndexed { i, part ->
                        if (i % 2 == 0) { if (part.isNotBlank()) Text(part.trim(), color = Color(0xFFE6EDF3), fontSize = 14.sp, lineHeight = 20.sp) }
                        else {
                            val code = part.lines().let { l -> if (l.isNotEmpty() && l.first().matches(Regex("^[a-z]+$"))) l.drop(1) else l }.joinToString("\n")
                            Box(Modifier.fillMaxWidth().background(Color(0xFF0D1117), RoundedCornerShape(6.dp)).padding(8.dp)) {
                                Text(code, color = Color(0xFFA5D6FF), fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            } else Text(content, color = if (isUser) Color.White else Color(0xFFE6EDF3), fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
