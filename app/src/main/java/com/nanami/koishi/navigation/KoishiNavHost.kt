package com.nanami.koishi.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nanami.koishi.R
import com.nanami.koishi.feature.home.HomeRoute
import com.nanami.koishi.feature.home.HomeViewModel
import com.nanami.koishi.feature.settings.SettingsViewModel
import com.nanami.koishi.feature.tools.grid_split.GridSplitRoute
import com.nanami.koishi.feature.tools.grid_split.GridSplitViewModel
import com.nanami.koishi.feature.tools.image_obfuscation.ImageObfuscationRoute
import com.nanami.koishi.feature.tools.image_obfuscation.ImageObfuscationViewModel
import com.nanami.koishi.feature.tools.qr_tool.QrToolRoute
import com.nanami.koishi.feature.tools.qr_tool.QrToolViewModel

@Composable
fun KoishiNavHost(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            val homeViewModel: HomeViewModel = viewModel()
            HomeRoute(
                viewModel = homeViewModel,
                settingsViewModel = settingsViewModel,
                onNavigateToTool = { tool ->
                    when (tool.id) {
                        "image_stitching" -> navController.navigate(ImageStitchingRoute)
                        "image_search" -> navController.navigate(ImageSearchRoute)
                        "grid_split" -> navController.navigate(GridSplitRoute)
                        "image_obfuscation" -> navController.navigate(ImageObfuscationRoute)
                        "qr_code" -> navController.navigate(QrToolRoute)
                        else -> {
                            navController.navigate(
                                ToolDetailRoute(
                                    toolId = tool.id,
                                    toolTitle = context.getString(tool.nameRes)
                                )
                            )
                        }
                    }
                }
            )
        }

        composable<ImageSearchRoute> {
            val imageSearchViewModel: com.nanami.koishi.feature.tools.image_search.ImageSearchViewModel = viewModel()
            com.nanami.koishi.feature.tools.image_search.ImageSearchRoute(
                viewModel = imageSearchViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<ImageStitchingRoute> {
            val stitchingViewModel: com.nanami.koishi.feature.tools.image_stitching.ImageStitchingViewModel = viewModel()
            com.nanami.koishi.feature.tools.image_stitching.ImageStitchingRoute(
                viewModel = stitchingViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<GridSplitRoute> {
            val gridSplitViewModel: GridSplitViewModel = viewModel()
            GridSplitRoute(
                viewModel = gridSplitViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<ImageObfuscationRoute> {
            val obfuscationViewModel: ImageObfuscationViewModel = viewModel()
            ImageObfuscationRoute(
                viewModel = obfuscationViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<QrToolRoute> {
            val qrToolViewModel: QrToolViewModel = viewModel()
            QrToolRoute(
                viewModel = qrToolViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<ToolDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ToolDetailRoute>()
            ToolPlaceholderScreen(
                toolId = route.toolId,
                toolTitle = route.toolTitle,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolPlaceholderScreen(
    toolId: String,
    toolTitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(toolTitle, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "[$toolTitle] feature/tools/$toolId",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
