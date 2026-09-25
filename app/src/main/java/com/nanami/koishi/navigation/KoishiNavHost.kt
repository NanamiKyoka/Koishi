package com.nanami.koishi.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nanami.koishi.feature.home.HomeRoute
import com.nanami.koishi.feature.home.HomeViewModel
import com.nanami.koishi.feature.settings.SettingsViewModel
import com.nanami.koishi.feature.tools.grid_split.GridSplitRoute
import com.nanami.koishi.feature.tools.grid_split.GridSplitViewModel
import com.nanami.koishi.feature.tools.image_obfuscation.ImageObfuscationRoute
import com.nanami.koishi.feature.tools.image_obfuscation.ImageObfuscationViewModel
import com.nanami.koishi.feature.tools.image_sketch.ImageSketchRoute
import com.nanami.koishi.feature.tools.image_sketch.ImageSketchViewModel
import com.nanami.koishi.feature.tools.mirage_tank.MirageTankRoute
import com.nanami.koishi.feature.tools.mirage_tank.MirageTankViewModel
import com.nanami.koishi.feature.tools.qr_tool.QrToolRoute
import com.nanami.koishi.feature.tools.qr_tool.QrToolViewModel
import com.nanami.koishi.feature.tools.today_in_history.TodayInHistoryRoute
import com.nanami.koishi.feature.tools.today_in_history.TodayInHistoryViewModel
import com.nanami.koishi.feature.tools.watermark.WatermarkRoute
import com.nanami.koishi.feature.tools.watermark.WatermarkViewModel

@Composable
fun KoishiNavHost(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
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
                        "mirage_tank" -> navController.navigate(MirageTankRoute)
                        "qr_code" -> navController.navigate(QrToolRoute)
                        "watermark" -> navController.navigate(WatermarkRoute)
                        "image_sketch" -> navController.navigate(ImageSketchRoute)
                        "today_in_history" -> navController.navigate(TodayInHistoryRoute)
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

        composable<MirageTankRoute> {
            val mirageTankViewModel: MirageTankViewModel = viewModel()
            MirageTankRoute(
                viewModel = mirageTankViewModel,
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

        composable<WatermarkRoute> {
            val watermarkViewModel: WatermarkViewModel = viewModel()
            WatermarkRoute(
                viewModel = watermarkViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<ImageSketchRoute> {
            val imageSketchViewModel: ImageSketchViewModel = viewModel()
            ImageSketchRoute(
                viewModel = imageSketchViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<TodayInHistoryRoute> {
            val historyViewModel: TodayInHistoryViewModel = viewModel()
            TodayInHistoryRoute(
                viewModel = historyViewModel,
                onBack = { navController.popBackStack() }
            )
        }

    }
}
