package com.glassfiles.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact picker chip used across the AI module to replace the legacy
 * `DropdownMenu`. Looks like a labelled card showing the current value
 * with an expand-more glyph; tapping opens [AiPickerSheet] (search +
 * scrollable list) which keeps long model lists usable.
 *
 * Single source of truth so coding / image-gen / video-gen / models
 * screens all share the same affordance and behaviour.
 */
@Composable
fun <T> AiPickerChip(
    label: String,
    value: String,
    title: String,
    options: List<T>,
    optionLabel: (T) -> String,
    optionSubtitle: (T) -> String? = { null },
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }

    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.5f))
            .clickable(enabled = enabled && options.isNotEmpty()) { open = true }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = colors.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
            )
        }
        Icon(
            Icons.Rounded.ExpandMore,
            null,
            Modifier.size(16.dp),
            tint = colors.onSurfaceVariant,
        )
    }

    if (open) {
        AiPickerSheet(
            title = title,
            options = options,
            optionLabel = optionLabel,
            optionSubtitle = optionSubtitle,
            selected = selected,
            onDismiss = { open = false },
            onSelect = onSelect,
        )
    }
}
