package com.glassfiles.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.Strings
import com.glassfiles.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class NoteItem(val id: String, val title: String, val content: String, val modified: Long)

object NotesStore {
    private const val PREFS = "markdown_notes"
    private const val KEY = "notes_json"

    fun load(context: Context): List<NoteItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                NoteItem(o.getString("id"), o.getString("title"), o.getString("content"), o.getLong("modified"))
            }.sortedByDescending { it.modified }
        } catch (_: Exception) { emptyList() }
    }

    fun save(context: Context, notes: List<NoteItem>) {
        val arr = JSONArray()
        notes.forEach { n ->
            arr.put(JSONObject().apply { put("id", n.id); put("title", n.title); put("content", n.content); put("modified", n.modified) })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    fun addOrUpdate(context: Context, note: NoteItem) {
        val list = load(context).toMutableList()
        val idx = list.indexOfFirst { it.id == note.id }
        if (idx >= 0) list[idx] = note else list.add(0, note)
        save(context, list)
    }

    fun delete(context: Context, id: String) {
        save(context, load(context).filter { it.id != id })
    }
}

@Composable
fun NotesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(NotesStore.load(context)) }
    var editingNote by remember { mutableStateOf<NoteItem?>(null) }
    val sdf = remember { SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()) }

    fun refresh() { notes = NotesStore.load(context) }

    if (editingNote != null) {
        NoteEditorScreen(
            note = editingNote!!,
            onBack = { editingNote = null; refresh() },
            onSave = { updated ->
                NotesStore.addOrUpdate(context, updated)
                Toast.makeText(context, Strings.noteSaved, Toast.LENGTH_SHORT).show()
                editingNote = null; refresh()
            },
            onDelete = {
                NotesStore.delete(context, editingNote!!.id)
                Toast.makeText(context, Strings.noteDeleted, Toast.LENGTH_SHORT).show()
                editingNote = null; refresh()
            }
        )
        return
    }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        Row(
            Modifier.fillMaxWidth().background(SurfaceWhite).padding(top = 44.dp, start = 4.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Blue) }
            Text(Strings.quickNotes, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                editingNote = NoteItem(UUID.randomUUID().toString(), "", "", System.currentTimeMillis())
            }) { Icon(Icons.Rounded.Add, null, Modifier.size(22.dp), tint = Blue) }
        }

        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.StickyNote2, null, Modifier.size(48.dp), tint = TextTertiary)
                    Spacer(Modifier.height(12.dp))
                    Text(Strings.noNotes, color = TextSecondary, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite)
                            .clickable { editingNote = note }.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.size(36.dp).background(Blue.copy(0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Description, null, Modifier.size(20.dp), tint = Blue) }
                        Column(Modifier.weight(1f)) {
                            Text(
                                note.title.ifBlank { Strings.untitled },
                                fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            if (note.content.isNotBlank()) {
                                Text(
                                    note.content.take(80).replace("\n", " "),
                                    fontSize = 12.sp, color = TextSecondary,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(sdf.format(Date(note.modified)), fontSize = 11.sp, color = TextTertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditorScreen(note: NoteItem, onBack: () -> Unit, onSave: (NoteItem) -> Unit, onDelete: () -> Unit) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var isPreview by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().background(SurfaceWhite).padding(top = 44.dp, start = 4.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Blue) }
            Spacer(Modifier.weight(1f))
            // Preview toggle
            IconButton(onClick = { isPreview = !isPreview }) {
                Icon(
                    if (isPreview) Icons.Rounded.Edit else Icons.Rounded.Visibility,
                    null, Modifier.size(20.dp), tint = Blue
                )
            }
            // Delete
            if (note.content.isNotBlank() || note.title.isNotBlank()) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Rounded.Delete, null, Modifier.size(20.dp), tint = Red)
                }
            }
            // Save
            IconButton(onClick = {
                onSave(note.copy(title = title, content = content, modified = System.currentTimeMillis()))
            }) { Icon(Icons.Rounded.Check, null, Modifier.size(22.dp), tint = Color(0xFF4CAF50)) }
        }

        // Markdown toolbar (only in edit mode)
        if (!isPreview) {
            Row(
                Modifier.fillMaxWidth().background(SurfaceWhite).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                MdButton("H1") { content = insertMd(content, "# ") }
                MdButton("H2") { content = insertMd(content, "## ") }
                MdButton("B") { content = wrapMd(content, "**") }
                MdButton("I") { content = wrapMd(content, "_") }
                MdButton("~") { content = wrapMd(content, "~~") }
                MdButton("`") { content = wrapMd(content, "`") }
                MdButton("•") { content = insertMd(content, "- ") }
                MdButton("1.") { content = insertMd(content, "1. ") }
                MdButton("[ ]") { content = insertMd(content, "- [ ] ") }
                MdButton(">") { content = insertMd(content, "> ") }
                MdButton("---") { content += "\n---\n" }
                MdButton("```") { content += "\n```\n\n```\n" }
            }
        }

        if (isPreview) {
            // Markdown preview
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (title.isNotBlank()) {
                    Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                }
                MarkdownPreview(content)
            }
        } else {
            // Editor
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicTextField(
                    value = title, onValueChange = { title = it },
                    textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
                    cursorBrush = SolidColor(Blue),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (title.isEmpty()) Text(Strings.noteTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
                        inner()
                    }
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(SeparatorColor))
                BasicTextField(
                    value = content, onValueChange = { content = it },
                    textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary, fontFamily = FontFamily.Monospace, lineHeight = 22.sp),
                    cursorBrush = SolidColor(Blue),
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { inner ->
                        if (content.isEmpty()) Text(Strings.noteContent, fontSize = 15.sp, color = TextTertiary, fontFamily = FontFamily.Monospace)
                        inner()
                    }
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = SurfaceWhite,
            title = { Text(Strings.delete, fontWeight = FontWeight.Bold, color = TextPrimary) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text(Strings.delete, color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(Strings.cancel, color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun MdButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Blue, fontFamily = FontFamily.Monospace)
    }
}

private fun insertMd(text: String, prefix: String): String = "$text\n$prefix"
private fun wrapMd(text: String, wrap: String): String = "$text$wrap$wrap"

@Composable
private fun MarkdownPreview(markdown: String) {
    val lines = markdown.lines()
    var inCodeBlock = false

    for (line in lines) {
        if (line.trimStart().startsWith("```")) {
            inCodeBlock = !inCodeBlock
            continue
        }
        if (inCodeBlock) {
            Text(line, fontSize = 13.sp, color = Color(0xFF4EC9B0), fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
            continue
        }
        when {
            line.startsWith("### ") -> Text(line.drop(4), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            line.startsWith("## ") -> Text(line.drop(3), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            line.startsWith("# ") -> Text(line.drop(2), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            line.startsWith("> ") -> Box(
                Modifier.fillMaxWidth().padding(vertical = 2.dp).border(width = 3.dp, color = Blue.copy(0.4f), shape = RoundedCornerShape(2.dp))
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
            ) { Text(line.drop(2), fontSize = 14.sp, color = TextSecondary, fontStyle = FontStyle.Italic) }
            line.startsWith("---") -> Box(Modifier.fillMaxWidth().height(1.dp).background(SeparatorColor).padding(vertical = 8.dp))
            line.startsWith("- [x] ") -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckBox, null, Modifier.size(18.dp), tint = Color(0xFF4CAF50))
                Spacer(Modifier.width(6.dp))
                Text(line.drop(6), fontSize = 14.sp, color = TextPrimary, textDecoration = TextDecoration.LineThrough)
            }
            line.startsWith("- [ ] ") -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckBoxOutlineBlank, null, Modifier.size(18.dp), tint = TextTertiary)
                Spacer(Modifier.width(6.dp))
                Text(line.drop(6), fontSize = 14.sp, color = TextPrimary)
            }
            line.startsWith("- ") || line.startsWith("* ") -> Row(verticalAlignment = Alignment.Top) {
                Text("•", fontSize = 14.sp, color = Blue, modifier = Modifier.padding(end = 8.dp, top = 1.dp))
                RichText(line.drop(2))
            }
            line.matches(Regex("^\\d+\\.\\s.*")) -> {
                val num = line.substringBefore(". ")
                val rest = line.substringAfter(". ")
                Row(verticalAlignment = Alignment.Top) {
                    Text("$num.", fontSize = 14.sp, color = Blue, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 6.dp))
                    RichText(rest)
                }
            }
            line.isBlank() -> Spacer(Modifier.height(8.dp))
            else -> RichText(line)
        }
    }
}

@Composable
private fun RichText(text: String) {
    val annotated = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                        i = end + 2
                    } else { append(text[i]); i++ }
                }
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end > i) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(text.substring(i + 2, end)) }
                        i = end + 2
                    } else { append(text[i]); i++ }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22007AFF), color = Blue)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }
                text[i] == '_' && (i + 1 < text.length && text[i + 1] != ' ') -> {
                    val end = text.indexOf('_', i + 1)
                    if (end > i) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }
                else -> { append(text[i]); i++ }
            }
        }
    }
    Text(annotated, fontSize = 14.sp, color = TextPrimary, lineHeight = 21.sp)
}
