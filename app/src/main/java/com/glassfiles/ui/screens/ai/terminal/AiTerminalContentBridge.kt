package com.glassfiles.ui.screens.ai.terminal

import androidx.compose.runtime.Composable

/**
 * Compatibility wrapper kept for older call sites. It intentionally no
 * longer installs an external UI theme: AI-module screens render from
 * [AgentTerminalSurface] and terminal primitives directly.
 */
@Composable
fun AiTerminalContentBridge(content: @Composable () -> Unit) {
    content()
}
