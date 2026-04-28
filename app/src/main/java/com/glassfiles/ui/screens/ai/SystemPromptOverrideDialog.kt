package com.glassfiles.ui.screens.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.Strings

/**
 * Dialog for editing the per-repo system-prompt override and the
 * plan-then-execute toggle. Both options are scoped to the same repo
 * and persisted via `AiAgentPrefs`. Empty submit clears the override
 * so the user can wipe a prior value without a separate menu item.
 *
 * @param repoFullName       shown in the title — the user needs to
 *                           know which repo is being configured.
 * @param initialPrompt      prefilled override pulled from prefs.
 * @param initialPlanFirst   prefilled plan-then-execute toggle.
 * @param onSave             receives the trimmed prompt and the new
 *                           toggle value.
 * @param onDismiss          close without persisting.
 */
@Composable
fun SystemPromptOverrideDialog(
    repoFullName: String,
    initialPrompt: String,
    initialPlanFirst: Boolean,
    onSave: (prompt: String, planFirst: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var text by remember { mutableStateOf(initialPrompt) }
    var planFirst by remember { mutableStateOf(initialPlanFirst) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    Strings.aiAgentSystemPromptTitle,
                    fontSize = 16.sp,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    repoFullName,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                )
            }
        },
        text = {
            Column {
                Text(
                    Strings.aiAgentSystemPromptHint,
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            Strings.aiAgentSystemPromptPlaceholder,
                            fontSize = 14.sp,
                            color = colors.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 240.dp),
                    minLines = 5,
                )
                Spacer(Modifier.height(12.dp))
                // C3 — plan-first toggle. Lives in the same dialog as
                // the system-prompt override because both are per-repo
                // workflow tweaks, and bundling them avoids adding yet
                // another icon to the agent's already-crowded top bar.
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            Strings.aiAgentPlanFirstLabel,
                            fontSize = 14.sp,
                            color = colors.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            Strings.aiAgentPlanFirstHint,
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = planFirst,
                        onCheckedChange = { planFirst = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim(), planFirst) }) {
                Text(Strings.aiAgentSystemPromptSave, color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.aiAgentSystemPromptCancel, color = colors.onSurfaceVariant)
            }
        },
    )
}
