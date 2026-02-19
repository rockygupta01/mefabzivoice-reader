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
            ScannerScreen(viewModel = viewModel)
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
    }
}
