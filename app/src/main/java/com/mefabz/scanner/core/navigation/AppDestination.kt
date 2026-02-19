package com.mefabz.scanner.core.navigation

sealed class AppDestination(val route: String) {
    data object Scanner : AppDestination("scanner")
    data object Result : AppDestination("result")
    data object PdfReader : AppDestination("pdf_reader/{uri}") {
        fun createRoute(uri: String): String = "pdf_reader/${java.net.URLEncoder.encode(uri, "UTF-8")}"
    }
}
