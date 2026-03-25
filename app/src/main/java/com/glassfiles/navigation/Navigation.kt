package com.glassfiles.navigation

sealed class Screen(val route: String) {
    data object Recents : Screen("recents")
    data object Shared : Screen("shared")
    data object Browse : Screen("browse")
}
