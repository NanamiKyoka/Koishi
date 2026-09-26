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
import com.nanami.koishi.feature.home.poetry.PoetryViewModel
import com.nanami.koishi.feature.settings.SettingsViewModel
import com.nanami.koishi.feature.tools.decision_maker.DecisionMakerRoute
import com.nanami.koishi.feature.tools.decision_maker.DecisionMakerViewModel
import com.nanami.koishi.feature.tools.grid_split.GridSplitRoute
import com.nanami.koishi.feature.tools.grid_split.GridSplitViewModel
import com.nanami.koishi.feature.tools.image_obfuscation.ImageObfuscationRoute
import com.nanami.koishi.feature.tools.image_obfuscation.ImageObfuscationViewModel
import com.nanami.koishi.feature.tools.image_sketch.ImageSketchRoute
import com.nanami.koishi.feature.tools.image_sketch.ImageSketchViewModel
import com.nanami.koishi.feature.tools.mirage_tank.MirageTankRoute
import com.nanami.koishi.feature.tools.mirage_tank.MirageTankViewModel
import com.nanami.koishi.feature.tools.mini_apps.MiniAppsRoute
import com.nanami.koishi.feature.tools.mini_apps.MiniAppsViewModel
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
            val poetryViewModel: PoetryViewModel = viewModel()
            HomeRoute(
                viewModel = homeViewModel,
                poetryViewModel = poetryViewModel,
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
                        "decision_maker" -> navController.navigate(DecisionMakerRoute)
                        "ruler" -> navController.navigate(RulerRoute)
                        "currency_converter" -> navController.navigate(CurrencyConverterRoute)
                        "bmi_calculator" -> navController.navigate(BmiCalculatorRoute)
                        "video_to_gif" -> navController.navigate(VideoToGifRoute)
                        "bili_cover" -> navController.navigate(BiliCoverRoute)
                        "meme_maker" -> navController.navigate(MemeMakerRoute)
                        "mini_apps" -> navController.navigate(MiniAppsRoute)
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

        composable<DecisionMakerRoute> {
            val decisionViewModel: DecisionMakerViewModel = viewModel()
            DecisionMakerRoute(
                viewModel = decisionViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<RulerRoute> {
            val context = androidx.compose.ui.platform.LocalContext.current
            com.nanami.koishi.feature.tools.ruler.RulerSelectionScreen(
                onBack = { navController.popBackStack() },
                onSelectMode = { mode ->
                    val intent = com.nanami.koishi.feature.tools.ruler.RulerActivity.createIntent(context, mode)
                    context.startActivity(intent)
                }
            )
        }

        composable<CurrencyConverterRoute> {
            val currencyConverterViewModel: com.nanami.koishi.feature.tools.currency_converter.CurrencyConverterViewModel = viewModel()
            com.nanami.koishi.feature.tools.currency_converter.CurrencyConverterRoute(
                viewModel = currencyConverterViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<BmiCalculatorRoute> {
            val bmiCalculatorViewModel: com.nanami.koishi.feature.tools.bmi_calculator.BmiCalculatorViewModel = viewModel()
            com.nanami.koishi.feature.tools.bmi_calculator.BmiCalculatorRoute(
                viewModel = bmiCalculatorViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<VideoToGifRoute> {
            val videoToGifViewModel: com.nanami.koishi.feature.tools.video_to_gif.VideoToGifViewModel = viewModel()
            com.nanami.koishi.feature.tools.video_to_gif.VideoToGifRoute(
                viewModel = videoToGifViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<BiliCoverRoute> {
            val biliCoverViewModel: com.nanami.koishi.feature.tools.bili_cover.BiliCoverViewModel = viewModel()
            com.nanami.koishi.feature.tools.bili_cover.BiliCoverRoute(
                viewModel = biliCoverViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<MemeMakerRoute> {
            val memeMakerViewModel: com.nanami.koishi.feature.tools.meme_maker.MemeMakerViewModel = viewModel()
            com.nanami.koishi.feature.tools.meme_maker.MemeMakerRoute(
                viewModel = memeMakerViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<MiniAppsRoute> {
            val miniAppsViewModel: MiniAppsViewModel = viewModel()
            MiniAppsRoute(
                viewModel = miniAppsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
