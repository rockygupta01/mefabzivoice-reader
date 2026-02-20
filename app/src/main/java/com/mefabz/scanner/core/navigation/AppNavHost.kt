package com.mefabz.scanner.core.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mefabz.scanner.feature.scanner.presentation.ResultScreen
import com.mefabz.scanner.feature.scanner.presentation.ScannerEvent
import com.mefabz.scanner.feature.scanner.presentation.ScannerScreen
import com.mefabz.scanner.feature.scanner.presentation.ScannerViewModel
import com.mefabz.scanner.feature.settings.presentation.SettingsScreen
import com.mefabz.scanner.ui.theme.NeonCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCachingPdf by remember { mutableStateOf(false) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { contentUri ->
            isCachingPdf = true
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(contentUri)
                    if (inputStream != null) {
                        val fileName = "cached_pdf_${System.currentTimeMillis()}.pdf"
                        val file = File(context.cacheDir, fileName)
                        val outputStream = java.io.FileOutputStream(file)
                        inputStream.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            navController.navigate(AppDestination.PdfReader.createRoute(file.toUri().toString())) {
                                launchSingleTop = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isCachingPdf = false
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            // Only show bottom bar on main screens, not on result or pdf reader
            val showBottomBar = currentRoute in listOf(
                AppDestination.Scanner.route,
                AppDestination.Settings.route
            )
            
            if (showBottomBar) {
                NavigationBar(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = currentRoute == AppDestination.Scanner.route,
                        onClick = {
                            navController.navigate(AppDestination.Scanner.route) {
                                popUpTo(AppDestination.Scanner.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Scanner") },
                        label = { Text("Scanner") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan.copy(alpha = 0.2f)
                        )
                    )
                    
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            pdfLauncher.launch(arrayOf("application/pdf"))
                        },
                        icon = { Icon(Icons.Filled.Add, contentDescription = "PDF") },
                        label = { Text("PDF") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan.copy(alpha = 0.2f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute == AppDestination.Settings.route,
                        onClick = {
                            navController.navigate(AppDestination.Settings.route) {
                                popUpTo(AppDestination.Scanner.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.Scanner.route
            ) {
                composable(AppDestination.Scanner.route) {
                    // PDF button is no longer needed in the screen itself, it's in the bottom bar 
                    ScannerScreen(
                        viewModel = viewModel,
                        onOpenPdf = { /* No-op or keep for legacy call if needed */ }
                    )
                }

                composable(AppDestination.Result.route) {
                    ResultScreen(
                        viewModel = viewModel,
                        onScanAnother = {
                            viewModel.resetForRescan()
                            navController.popBackStack(AppDestination.Scanner.route, inclusive = false)
                        }
                    )
                }

                composable(AppDestination.PdfReader.route) { backStackEntry ->
                    val uriArg = backStackEntry.arguments?.getString("uri") ?: return@composable
                    val uri = java.net.URLDecoder.decode(uriArg, "UTF-8")
                    com.mefabz.scanner.feature.pdfreader.presentation.PdfReaderScreen(
                        uri = uri,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                
                composable(AppDestination.Settings.route) {
                    SettingsScreen()
                }
            }

            if (isCachingPdf) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            }
        }
    }
}
