package com.glassfiles.ui.screens.ai.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import com.glassfiles.data.Strings
import com.glassfiles.ui.theme.JetBrainsMono

@Composable
fun YoloModeConfirmDialog(
    previouslyConfirmed: Boolean,
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(
            LocalAgentTerminalColors provides AgentTerminalDarkColors,
            LocalAgentTerminalTypography provides AgentTerminalDefaultTypography,
        ) {
            YoloModeConfirmBlock(
                previouslyConfirmed = previouslyConfirmed,
                onEnable = onEnable,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun YoloModeConfirmBlock(
    previouslyConfirmed: Boolean,
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AgentTerminal.colors
    Column(
        Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.warning, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = if (previouslyConfirmed) "\u26A0 ${Strings.aiAgentYoloMode}?" else "\u26A0 ${Strings.aiAgentYoloReadCarefully}",
            color = colors.warning,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = AgentTerminal.type.message,
            lineHeight = 1.3.em,
        )
        if (previouslyConfirmed) {
            YoloBodyLine(Strings.aiAgentYoloRememberedLine1)
            YoloBodyLine(Strings.aiAgentYoloRememberedLine2)
        } else {
            YoloSection(Strings.aiAgentYoloWillExecute)
            YoloListLine("\u2713", Strings.aiAgentYoloReadFiles, colors.accent)
            YoloListLine("\u2713", Strings.aiAgentYoloEditFiles, colors.accent)
            YoloListLine("\u2713", Strings.aiAgentYoloCreateFiles, colors.accent)
            YoloListLine("\u2713", Strings.aiAgentYoloFeatureCommits, colors.accent)
            Spacer(Modifier.height(2.dp))
            YoloSection(Strings.aiAgentYoloStillAsk)
            YoloListLine("\u2022", Strings.aiAgentYoloMainCommits, colors.textSecondary)
            YoloListLine("\u2022", Strings.aiAgentYoloDestructiveOps, colors.textSecondary)
            YoloListLine("\u2022", Strings.aiAgentYoloProtectedFiles, colors.textSecondary)
            Spacer(Modifier.height(2.dp))
            YoloSection(Strings.aiAgentYoloRisks)
            YoloListLine("\u2022", Strings.aiAgentYoloRiskUnexpected, colors.textSecondary)
            YoloListLine("\u2022", Strings.aiAgentYoloRiskCost, colors.textSecondary)
            YoloListLine("\u2022", Strings.aiAgentYoloRiskMistakes, colors.textSecondary)
            YoloListLine("\u2022", Strings.aiAgentYoloRiskReview, colors.textSecondary)
            Spacer(Modifier.height(2.dp))
            YoloSection(Strings.aiAgentYoloRecommended)
            YoloListLine("\u2022", Strings.aiAgentYoloBackup, colors.textSecondary)
            YoloListLine("\u2022", Strings.aiAgentYoloUnderstandTools, colors.textSecondary)
            YoloListLine("\u2022", Strings.aiAgentYoloReviewChanges, colors.textSecondary)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            YoloDialogButton(
                label = if (previouslyConfirmed) "[ yes ]" else "[ yes \u00B7 ${Strings.aiAgentPermissionYolo} ]",
                color = colors.warning,
                onClick = onEnable,
            )
            YoloDialogButton(
                label = "[ no \u00B7 ${Strings.cancel.lowercase()} ]",
                color = colors.textSecondary,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun YoloSection(text: String) {
    Text(
        text = text,
        color = AgentTerminal.colors.textPrimary,
        fontFamily = JetBrainsMono,
        fontSize = AgentTerminal.type.toolCall,
        lineHeight = 1.35.em,
    )
}

@Composable
private fun YoloBodyLine(text: String) {
    Text(
        text = text,
        color = AgentTerminal.colors.textSecondary,
        fontFamily = JetBrainsMono,
        fontSize = AgentTerminal.type.toolCall,
        lineHeight = 1.35.em,
    )
}

@Composable
private fun YoloListLine(mark: String, text: String, markColor: Color) {
    Row {
        Text(
            text = mark.padEnd(2),
            color = markColor,
            fontFamily = JetBrainsMono,
            fontSize = AgentTerminal.type.toolCall,
            lineHeight = 1.35.em,
        )
        Text(
            text = text,
            color = AgentTerminal.colors.textSecondary,
            fontFamily = JetBrainsMono,
            fontSize = AgentTerminal.type.toolCall,
            lineHeight = 1.35.em,
        )
    }
}

@Composable
private fun YoloDialogButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = color,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = AgentTerminal.type.toolCall,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}
