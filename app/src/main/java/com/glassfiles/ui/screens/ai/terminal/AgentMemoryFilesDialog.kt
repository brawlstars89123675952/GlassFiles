package com.glassfiles.ui.screens.ai.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.glassfiles.data.Strings
import com.glassfiles.data.ai.AiAgentMemoryStore
import com.glassfiles.ui.components.terminal.TerminalTabsRow
import com.glassfiles.ui.theme.JetBrainsMono

@Composable
fun AgentMemoryFilesDialog(
    files: List<AiAgentMemoryStore.MemoryFile>,
    onRebuildIndex: () -> Unit,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val editableFiles = remember(files) {
        files.filter { it.key in setOf("project", "preferences", "decisions") }
    }
    if (editableFiles.isEmpty()) return

    val colors = AgentTerminal.colors
    var selectedKey by remember(editableFiles) { mutableStateOf(editableFiles.first().key) }
    val selectedFile = editableFiles.firstOrNull { it.key == selectedKey } ?: editableFiles.first()
    var text by remember(selectedFile.key, selectedFile.content) { mutableStateOf(selectedFile.content) }

    AgentTerminalBottomSheet(
        title = Strings.aiAgentMemoryFilesTitle,
        onDismiss = onDismiss,
    ) {
        TerminalTabsRow(
            spacing = 8.dp,
            fadeColor = colors.background,
            chevronColor = colors.textMuted,
        ) {
            editableFiles.forEach { file ->
                val selected = file.key == selectedKey
                Text(
                    text = if (selected) "[\u25A3 ${file.label}]" else "[ ${file.label} ]",
                    color = if (selected) colors.accent else colors.textSecondary,
                    fontFamily = JetBrainsMono,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = AgentTerminal.type.toolCall,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            selectedKey = file.key
                            text = file.content
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }

        Text(
            text = selectedFile.path,
            color = colors.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = AgentTerminal.type.label,
            maxLines = 1,
        )

        MemoryTextEditor(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.heightIn(min = 300.dp, max = 520.dp),
        )

        AgentSheetActions {
            AgentTextButton(
                label = "[ ${Strings.aiKeySave.lowercase()} ]",
                color = colors.accent,
                enabled = true,
                onClick = { onSave(selectedFile.key, text) },
            )
            AgentTextButton(
                label = "[ ${Strings.aiAgentRebuildIndex} ]",
                color = colors.textSecondary,
                enabled = true,
                onClick = onRebuildIndex,
            )
            AgentTextButton(
                label = "[ ${Strings.done.lowercase()} ]",
                color = colors.textSecondary,
                enabled = true,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
fun AgentWorkingMemoryDialog(
    content: String,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AgentTerminal.colors
    var text by remember(content) { mutableStateOf(content) }

    AgentTerminalBottomSheet(
        title = Strings.aiAgentWorkingMemoryTitle,
        onDismiss = onDismiss,
    ) {
        Text(
            text = AiAgentMemoryStore.WORKING_MEMORY_FILE,
            color = colors.textMuted,
            fontFamily = JetBrainsMono,
            fontSize = AgentTerminal.type.label,
            maxLines = 1,
        )

        MemoryTextEditor(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.heightIn(min = 320.dp, max = 560.dp),
        )

        AgentSheetActions {
            AgentTextButton(
                label = "[ ${Strings.aiKeySave.lowercase()} ]",
                color = colors.accent,
                enabled = true,
                onClick = { onSave(text) },
            )
            AgentTextButton(
                label = "[ ${Strings.aiKeyClear.lowercase()} ]",
                color = colors.error,
                enabled = true,
                onClick = {
                    onClear()
                    text = ""
                },
            )
            AgentTextButton(
                label = "[ ${Strings.done.lowercase()} ]",
                color = colors.textSecondary,
                enabled = true,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun AgentTerminalBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AgentTerminal.colors
    var dragY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(top = 48.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(colors.background)
                    .border(
                        1.dp,
                        colors.accent,
                        RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .pointerInput(onDismiss) {
                            detectVerticalDragGestures(
                                onDragStart = { dragY = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    if (dragAmount > 0f) dragY += dragAmount
                                },
                                onDragEnd = {
                                    if (dragY > 96f) onDismiss()
                                    dragY = 0f
                                },
                                onDragCancel = { dragY = 0f },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .width(38.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.border),
                    )
                }
                Text(
                    text = title,
                    color = colors.accent,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = AgentTerminal.type.message,
                    lineHeight = 1.3.em,
                )
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                content()
            }
        }
    }
}

@Composable
private fun MemoryTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AgentTerminal.colors
    val scrollState = rememberScrollState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = colors.textPrimary,
            fontFamily = JetBrainsMono,
            fontSize = AgentTerminal.type.toolCall,
            lineHeight = 1.35.em,
        ),
        cursorBrush = SolidColor(colors.accent),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(6.dp))
            .verticalScroll(scrollState)
            .padding(10.dp),
    )
}

@Composable
private fun AgentSheetActions(content: @Composable () -> Unit) {
    val colors = AgentTerminal.colors
    TerminalTabsRow(
        spacing = 12.dp,
        fadeColor = colors.background,
        chevronColor = colors.textMuted,
    ) {
        content()
    }
}
