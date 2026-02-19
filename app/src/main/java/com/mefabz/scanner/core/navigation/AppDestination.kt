package com.mefabz.scanner.core.navigation

sealed class AppDestination(val route: String) {
    data object Scanner : AppDestination("scanner")
    data object Result : AppDestination("result")
}
