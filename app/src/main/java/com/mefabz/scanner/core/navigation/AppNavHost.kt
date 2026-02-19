package com.mefabz.scanner.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mefabz.scanner.feature.scanner.presentation.ResultScreen
import com.mefabz.scanner.feature.scanner.presentation.ScannerEvent
import com.mefabz.scanner.feature.scanner.presentation.ScannerScreen
import com.mefabz.scanner.feature.scanner.presentation.ScannerViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppNavHost(
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                ScannerEvent.NavigateToResult -> {
                    if (navController.currentDestination?.route != AppDestination.Result.route) {
                        navController.navigate(AppDestination.Result.route)
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestination.Scanner.route
    ) {
        composable(AppDestination.Scanner.route) {
            ScannerScreen(
                viewModel = viewModel,
                onOpenPdf = { uri ->
                    navController.navigate(AppDestination.PdfReader.createRoute(uri))
                }
            )
        }

        composable(AppDestination.Result.route) {
            ResultScreen(
                viewModel = viewModel,
                onScanAnother = {
                    viewModel.resetForRescan()
                    navController.popBackStack()
                }
            )
        }

        composable(AppDestination.PdfReader.route) { backStackEntry ->
            val uriArg = backStackEntry.arguments?.getString("uri") ?: return@composable
            val uri = java.net.URLDecoder.decode(uriArg, "UTF-8")
            // We need a separate ViewModel for PDF Reader, or we can use hiltViewModel() inside the screen
            // The screen already uses hiltViewModel(), so we just pass the uri
            com.mefabz.scanner.feature.pdfreader.presentation.PdfReaderScreen(
                uri = uri,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
