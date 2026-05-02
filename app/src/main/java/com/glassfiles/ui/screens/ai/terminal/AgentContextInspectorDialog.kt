package com.glassfiles.ui.screens.ai.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.glassfiles.ui.theme.JetBrainsMono

data class AgentContextInspectorState(
    val sections: List<AgentContextInspectorSection>,
)

data class AgentContextInspectorSection(
    val title: String,
    val rows: List<Pair<String, String>> = emptyList(),
    val body: String? = null,
)

@Composable
fun AgentContextInspectorDialog(
    state: AgentContextInspectorState,
    onDismiss: () -> Unit,
) {
    val colors = AgentTerminal.colors
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
                    .widthIn(max = 720.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(colors.surfaceElevated)
                    .border(
                        1.dp,
                        colors.border,
                        RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                    )
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AgentContextHeader("CONTEXT INSPECTOR")
                state.sections.forEach { section ->
                    AgentContextSection(section)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AgentContextCommand("[ done ]", colors.accent, onDismiss)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AgentContextHeader(text: String) {
    val colors = AgentTerminal.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border),
        )
        Text(
            text = text,
            color = colors.textPrimary,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = AgentTerminal.type.message,
            lineHeight = 1.4.em,
        )
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border),
        )
    }
}

@Composable
private fun AgentContextSection(section: AgentContextInspectorSection) {
    val colors = AgentTerminal.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = section.title.uppercase(),
            color = colors.accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = AgentTerminal.type.label,
            lineHeight = 1.3.em,
        )
        section.rows.forEach { (key, value) ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = key,
                    color = colors.textMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = AgentTerminal.type.label,
                    modifier = Modifier.weight(0.42f),
                    lineHeight = 1.35.em,
                )
                Text(
                    text = value,
                    color = colors.textPrimary,
                    fontFamily = JetBrainsMono,
                    fontSize = AgentTerminal.type.label,
                    modifier = Modifier.weight(0.58f),
                    lineHeight = 1.35.em,
                )
            }
        }
        section.body?.takeIf { it.isNotBlank() }?.let { body ->
            Text(
                text = body,
                color = colors.textSecondary,
                fontFamily = JetBrainsMono,
                fontSize = AgentTerminal.type.label,
                lineHeight = 1.35.em,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                    .padding(10.dp),
            )
        }
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border),
        )
    }
}

@Composable
private fun AgentContextCommand(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = color,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = AgentTerminal.type.message,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}
