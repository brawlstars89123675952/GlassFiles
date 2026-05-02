package com.glassfiles.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.glassfiles.ui.components.CodeColors

/**
 * Shared terminal-style palette for the wider AI module: Hub, Chat,
 * Models, Keys, Usage, Settings, Image generation and Video generation.
 *
 * Do not depend on AgentTerminalTheme here: Agent/Coding have their own
 * namespace, while this theme intentionally duplicates the same visual
 * values so the whole AI area looks unified without cross-package leaks.
 */
@Immutable
data class AiModuleColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val accent: Color,
    val accentDim: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val warning: Color,
    val error: Color,
    val syntaxKeyword: Color,
    val syntaxFlag: Color,
    val syntaxString: Color,
    val syntaxArg: Color,
    val syntaxComment: Color,
    val syntaxNumber: Color,
)

val AiModuleDarkColors = AiModuleColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceElevated = Color(0xFF141414),
    border = Color(0xFF1F1F1F),
    accent = Color(0xFFA8D982),
    accentDim = Color(0xFF6B8C54),
    textPrimary = Color(0xFFE0E0E0),
    textSecondary = Color(0xFF999999),
    textMuted = Color(0xFF5C5C5C),
    warning = Color(0xFFE5C07B),
    error = Color(0xFFE06C75),
    syntaxKeyword = Color(0xFFA8D982),
    syntaxFlag = Color(0xFFE5C07B),
    syntaxString = Color(0xFF98C379),
    syntaxArg = Color(0xFFABB2BF),
    syntaxComment = Color(0xFF5C6370),
    syntaxNumber = Color(0xFFD19A66),
)

@Immutable
data class AiModuleTypography(
    val topBarTitle: TextUnit = 16.sp,
    val message: TextUnit = 14.sp,
    val code: TextUnit = 13.sp,
    val input: TextUnit = 14.sp,
    val toolCall: TextUnit = 13.sp,
    val costChip: TextUnit = 12.sp,
    val label: TextUnit = 12.sp,
)

val AiModuleDefaultTypography = AiModuleTypography()

// Default to AiModuleDarkColors instead of throwing. The AI module always
// wraps its screens in AiModuleSurface, but the GitHub module reuses these
// primitives in deeply-nested contexts (RepoCard called from Profile / Explore
// / AdvancedSearch, BranchPickerDialog opened from RepoDetailScreen, etc.)
// where forcing every call site to wrap is brittle and a missed wrap turns
// into a hard crash on entry. Falling back to the dark palette keeps the
// terminal-styled widgets visually consistent in any host composition.
val LocalAiModuleColors = compositionLocalOf<AiModuleColors> { AiModuleDarkColors }

val LocalAiModuleTypography = compositionLocalOf { AiModuleDefaultTypography }

object AiModuleTheme {
    val colors: AiModuleColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAiModuleColors.current

    val type: AiModuleTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAiModuleTypography.current
}

@Composable
fun AiModuleSurface(
    modifier: Modifier = Modifier,
    colors: AiModuleColors = AiModuleDarkColors,
    typography: AiModuleTypography = AiModuleDefaultTypography,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAiModuleColors provides colors,
        LocalAiModuleTypography provides typography,
    ) {
        AiModuleContentBridge {
            Box(modifier.fillMaxSize().background(colors.background)) {
                content()
            }
        }
    }
}

@Composable
fun AiModuleContentBridge(content: @Composable () -> Unit) {
    content()
}

fun AiModuleColors.toCodeColors(): CodeColors = CodeColors(
    plain = textPrimary,
    keyword = syntaxKeyword,
    string = syntaxString,
    number = syntaxNumber,
    comment = syntaxComment,
    annotation = syntaxFlag,
)
