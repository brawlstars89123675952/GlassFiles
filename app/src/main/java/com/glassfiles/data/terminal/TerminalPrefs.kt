package com.glassfiles.data.terminal

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class TerminalPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("terminal_prefs", Context.MODE_PRIVATE)

    var fontSize by mutableFloatStateOf(prefs.getFloat("font_size", 13f))
        private set

    var colorSchemeIndex by mutableIntStateOf(prefs.getInt("color_scheme", 0))
        private set

    var vibrateOnDone by mutableStateOf(prefs.getBoolean("vibrate_done", true))
        private set

    var bellOnDone by mutableStateOf(prefs.getBoolean("bell_done", true))
        private set

    var extraKeysRow1 by mutableStateOf(
        prefs.getString(
            "extra_keys_1",
            "ESC|MENU|CTRL|ALT|HOME|↑|END|PGUP"
        ) ?: "ESC|MENU|CTRL|ALT|HOME|↑|END|PGUP"
    )
        private set

    var extraKeysRow2 by mutableStateOf(
        prefs.getString(
            "extra_keys_2",
            "TAB|BKSP|←|↓|→|PGDN|ENTER"
        ) ?: "TAB|BKSP|←|↓|→|PGDN|ENTER"
    )
        private set

    fun changeFontSize(size: Float) {
        fontSize = size.coerceIn(9f, 22f)
        prefs.edit().putFloat("font_size", fontSize).apply()
    }

    fun saveThemeIndex(idx: Int) {
        prefs.edit().putInt("xterm_theme", idx).apply()
    }

    fun loadThemeIndex(): Int {
        return prefs.getInt("xterm_theme", 0)
    }

    fun changeColorScheme(index: Int) {
        colorSchemeIndex = index.coerceIn(0, colorSchemes.lastIndex)
        prefs.edit().putInt("color_scheme", colorSchemeIndex).apply()
    }

    fun changeVibrateOnDone(v: Boolean) {
        vibrateOnDone = v
        prefs.edit().putBoolean("vibrate_done", v).apply()
    }

    fun changeBellOnDone(v: Boolean) {
        bellOnDone = v
        prefs.edit().putBoolean("bell_done", v).apply()
    }

    fun changeExtraKeys1(keys: String) {
        extraKeysRow1 = keys
        prefs.edit().putString("extra_keys_1", keys).apply()
    }

    fun changeExtraKeys2(keys: String) {
        extraKeysRow2 = keys
        prefs.edit().putString("extra_keys_2", keys).apply()
    }

    val scheme: TermColorScheme
        get() = colorSchemes[colorSchemeIndex]

    companion object {
        val colorSchemes = listOf(
            TermColorScheme(
                "Dark",
                Color(0xFF1A1A2E),
                Color(0xFF16213E),
                Color(0xFFE0E0E0),
                Color(0xFF00E676),
                Color(0xFF64B5F6),
                Color(0xFFEF5350),
                Color(0xFFFFD54F),
                Color(0xFF757575),
                Color(0xFF0F3460)
            ),
            TermColorScheme(
                "Monokai",
                Color(0xFF272822),
                Color(0xFF1E1F1C),
                Color(0xFFF8F8F2),
                Color(0xFFA6E22E),
                Color(0xFF66D9EF),
                Color(0xFFF92672),
                Color(0xFFE6DB74),
                Color(0xFF75715E),
                Color(0xFF3E3D32)
            ),
            TermColorScheme(
                "Solarized",
                Color(0xFF002B36),
                Color(0xFF073642),
                Color(0xFF839496),
                Color(0xFF859900),
                Color(0xFF268BD2),
                Color(0xFFDC322F),
                Color(0xFFB58900),
                Color(0xFF586E75),
                Color(0xFF073642)
            ),
            TermColorScheme(
                "Dracula",
                Color(0xFF282A36),
                Color(0xFF21222C),
                Color(0xFFF8F8F2),
                Color(0xFF50FA7B),
                Color(0xFF8BE9FD),
                Color(0xFFFF5555),
                Color(0xFFF1FA8C),
                Color(0xFF6272A4),
                Color(0xFF44475A)
            ),
            TermColorScheme(
                "Nord",
                Color(0xFF2E3440),
                Color(0xFF3B4252),
                Color(0xFFD8DEE9),
                Color(0xFFA3BE8C),
                Color(0xFF88C0D0),
                Color(0xFFBF616A),
                Color(0xFFEBCB8B),
                Color(0xFF4C566A),
                Color(0xFF434C5E)
            ),
            TermColorScheme(
                "Black",
                Color(0xFF000000),
                Color(0xFF111111),
                Color(0xFFCCCCCC),
                Color(0xFF00FF00),
                Color(0xFF00AAFF),
                Color(0xFFFF4444),
                Color(0xFFFFFF00),
                Color(0xFF666666),
                Color(0xFF1A1A1A)
            ),
        )
    }
}

data class TermColorScheme(
    val name: String,
    val bg: Color,
    val surface: Color,
    val text: Color,
    val green: Color,
    val blue: Color,
    val red: Color,
    val yellow: Color,
    val gray: Color,
    val inputBg: Color
)