package com.glassfiles.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.glassfiles.R

/**
 * Shared JetBrains Mono family used by the terminal-style AI surfaces.
 * Keep the definition in one place so AgentTerminalTheme and the wider
 * AiModuleTheme cannot drift apart.
 */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)
