package com.glassfiles.data.terminal

data class TermuxTheme(
    val name: String,
    val bg: String,
    val fg: String,
    val cursor: String,
    val black: String = "#000000",
    val red: String = "#ef5350",
    val green: String = "#00e676",
    val yellow: String = "#ffd54f",
    val blue: String = "#64b5f6",
    val magenta: String = "#ce93d8",
    val cyan: String = "#4dd0e1",
    val white: String = "#e0e0e0"
)

object TermuxThemes {
    val themes = listOf(
        TermuxTheme("Default Dark", "#000000", "#e0e0e0", "#00e676"),
        TermuxTheme("Monokai", "#272822", "#f8f8f2", "#f8f8f0",
            "#272822", "#f92672", "#a6e22e", "#f4bf75", "#66d9ef", "#ae81ff", "#a1efe4", "#f8f8f2"),
        TermuxTheme("Solarized Dark", "#002b36", "#839496", "#93a1a1",
            "#073642", "#dc322f", "#859900", "#b58900", "#268bd2", "#d33682", "#2aa198", "#eee8d5"),
        TermuxTheme("Solarized Light", "#fdf6e3", "#657b83", "#586e75",
            "#073642", "#dc322f", "#859900", "#b58900", "#268bd2", "#d33682", "#2aa198", "#002b36"),
        TermuxTheme("Dracula", "#282a36", "#f8f8f2", "#f8f8f2",
            "#21222c", "#ff5555", "#50fa7b", "#f1fa8c", "#bd93f9", "#ff79c6", "#8be9fd", "#f8f8f2"),
        TermuxTheme("Nord", "#2e3440", "#d8dee9", "#d8dee9",
            "#3b4252", "#bf616a", "#a3be8c", "#ebcb8b", "#81a1c1", "#b48ead", "#88c0d0", "#e5e9f0"),
        TermuxTheme("Gruvbox Dark", "#282828", "#ebdbb2", "#ebdbb2",
            "#282828", "#cc241d", "#98971a", "#d79921", "#458588", "#b16286", "#689d6a", "#a89984"),
        TermuxTheme("One Dark", "#282c34", "#abb2bf", "#528bff",
            "#282c34", "#e06c75", "#98c379", "#e5c07b", "#61afef", "#c678dd", "#56b6c2", "#abb2bf"),
        TermuxTheme("Tokyo Night", "#1a1b26", "#a9b1d6", "#c0caf5",
            "#15161e", "#f7768e", "#9ece6a", "#e0af68", "#7aa2f7", "#bb9af7", "#7dcfff", "#a9b1d6"),
        TermuxTheme("Catppuccin Mocha", "#1e1e2e", "#cdd6f4", "#f5e0dc",
            "#181825", "#f38ba8", "#a6e3a1", "#f9e2af", "#89b4fa", "#cba6f7", "#94e2d5", "#bac2de"),
        TermuxTheme("Ubuntu", "#300a24", "#eeeeec", "#eeeeec",
            "#2e3436", "#cc0000", "#4e9a06", "#c4a000", "#3465a4", "#75507b", "#06989a", "#d3d7cf"),
        TermuxTheme("Ayu Dark", "#0a0e14", "#b3b1ad", "#e6b450",
            "#01060e", "#ea6c73", "#91b362", "#f9af4f", "#53bdfa", "#fae994", "#90e1c6", "#c7c7c7"),
        TermuxTheme("Cyberpunk", "#000b1e", "#0abdc6", "#ff00ff",
            "#000b1e", "#ff0055", "#00ff9c", "#fcee09", "#00bfff", "#ff00ff", "#00ffff", "#ffffff"),
        TermuxTheme("Matrix", "#000000", "#00ff00", "#00ff00",
            "#000000", "#ff0000", "#00ff00", "#ffff00", "#0000ff", "#ff00ff", "#00ffff", "#00ff00"),
        TermuxTheme("AMOLED Black", "#000000", "#ffffff", "#ffffff",
            "#000000", "#ff4444", "#44ff44", "#ffff44", "#4444ff", "#ff44ff", "#44ffff", "#ffffff"),
    )

    fun toJsTheme(theme: TermuxTheme): String {
        return """setFullTheme('${theme.bg}','${theme.fg}','${theme.cursor}','${theme.black}','${theme.red}','${theme.green}','${theme.yellow}','${theme.blue}','${theme.magenta}','${theme.cyan}','${theme.white}')"""
    }
}
