package com.glassfiles.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.Strings
import com.glassfiles.data.ai.AiKeyStore
import com.glassfiles.data.ai.models.AiProviderId

/**
 * Screen for entering / managing the API key for each [AiProviderId].
 *
 * Layout: collapsed by default, one compact row per provider showing
 * `[status dot]  Provider name        masked-key  ▾`. Tapping the row
 * expands an inline editor (input + reveal toggle + Save / Remove / Get-key
 * link). Designed to fit all 7 providers above-the-fold without scrolling on
 * a typical phone.
 */
@Composable
fun AiKeysScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    val keyValues = remember { mutableStateMapOf<AiProviderId, String>() }
    val showKey = remember { mutableStateMapOf<AiProviderId, Boolean>() }
    val expanded = remember { mutableStateMapOf<AiProviderId, Boolean>() }
    val savedFlash = remember { mutableStateMapOf<AiProviderId, Boolean>() }
    var savedRefreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(savedRefreshTick) {
        AiProviderId.entries.forEach { keyValues[it] = AiKeyStore.getKey(context, it) }
    }

    Column(Modifier.fillMaxSize().background(colors.surface)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 44.dp, start = 4.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = colors.onSurface)
            }
            Text(
                Strings.aiKeys,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
        }

        // Hairline under the header.
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant.copy(alpha = 0.12f)))

        LazyColumn(
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(AiProviderId.entries.toList(), key = { it.name }) { provider ->
                val value = keyValues[provider].orEmpty()
                ProviderRow(
                    provider = provider,
                    value = value,
                    revealed = showKey[provider] == true,
                    expanded = expanded[provider] == true,
                    saved = savedFlash[provider] == true,
                    onValueChange = {
                        keyValues[provider] = it
                        savedFlash[provider] = false
                    },
                    onToggleReveal = { showKey[provider] = !(showKey[provider] ?: false) },
                    onToggleExpanded = {
                        expanded[provider] = !(expanded[provider] ?: false)
                    },
                    onSave = {
                        AiKeyStore.saveKey(context, provider, value)
                        savedFlash[provider] = true
                        savedRefreshTick++
                    },
                    onClear = {
                        AiKeyStore.saveKey(context, provider, "")
                        keyValues[provider] = ""
                        savedFlash[provider] = false
                    },
                    onOpenConsole = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(provider.consoleUrl))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    },
                )

                // Hairline divider between rows.
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant.copy(alpha = 0.10f)))
            }
        }
    }
}

@Composable
private fun ProviderRow(
    provider: AiProviderId,
    value: String,
    revealed: Boolean,
    expanded: Boolean,
    saved: Boolean,
    onValueChange: (String) -> Unit,
    onToggleReveal: () -> Unit,
    onToggleExpanded: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onOpenConsole: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val hasKey = value.isNotBlank()

    Column(Modifier.fillMaxWidth()) {
        // Collapsed/header row — always visible.
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status dot — `tertiary` when key set, `outline` when missing.
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (hasKey) colors.tertiary else colors.outline),
            )
            Spacer(Modifier.size(12.dp))
            Text(
                provider.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (hasKey) maskKey(value) else "—",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = colors.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.size(6.dp))
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                null,
                Modifier.size(18.dp),
                tint = colors.onSurfaceVariant,
            )
        }

        // Expanded inline editor.
        AnimatedVisibility(visible = expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Input
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.merge(
                            TextStyle(
                                color = colors.onSurface,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        decorationBox = { inner ->
                            if (value.isEmpty()) {
                                Text(
                                    Strings.aiKeyHint,
                                    fontSize = 14.sp,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            inner()
                        },
                    )
                    if (value.isNotEmpty()) {
                        IconButton(
                            onClick = onToggleReveal,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                null,
                                Modifier.size(18.dp),
                                tint = colors.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Action row: Get-key link on the left, Remove + Save on the right.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GetKeyLink(onClick = onOpenConsole)
                    Spacer(Modifier.weight(1f))
                    if (hasKey) {
                        TextPill(
                            text = Strings.aiKeyClear,
                            primary = false,
                            onClick = onClear,
                        )
                        Spacer(Modifier.size(6.dp))
                    }
                    SavePill(
                        saved = saved,
                        enabled = value.isNotBlank() && !saved,
                        onClick = onSave,
                    )
                }
            }
        }
    }
}

@Composable
private fun GetKeyLink(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.OpenInNew,
            null,
            Modifier.size(13.dp),
            tint = colors.primary,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            Strings.aiKeyGetHere,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.primary,
        )
    }
}

@Composable
private fun TextPill(text: String, primary: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val bg = if (primary) colors.primary else colors.surface
    val fg = if (primary) colors.onPrimary else colors.onSurfaceVariant
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun SavePill(saved: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val bg = when {
        saved -> colors.tertiary
        enabled -> colors.primary
        else -> colors.surfaceVariant.copy(alpha = 0.5f)
    }
    val fg = when {
        saved -> colors.onTertiary
        enabled -> colors.onPrimary
        else -> colors.onSurfaceVariant
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(enabled = enabled || saved) { if (!saved) onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (saved) {
            Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = fg)
            Spacer(Modifier.size(4.dp))
        }
        Text(
            if (saved) Strings.aiKeySaved else Strings.aiKeySave,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
        )
    }
}

/**
 * Mask a secret key for display: show first 3 and last 4 characters with a
 * fixed-width middle (avoids leaking length). Falls back to `"•"` only when
 * the value is too short to safely show parts.
 */
private fun maskKey(value: String): String {
    val s = value.trim()
    if (s.length <= 8) return "••••••••"
    val head = s.take(3)
    val tail = s.takeLast(4)
    return "$head••••$tail"
}
