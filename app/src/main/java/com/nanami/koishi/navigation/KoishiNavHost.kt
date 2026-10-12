package com.nanami.koishi.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.compose.koinViewModel
import com.nanami.koishi.feature.home.HomeRoute
import com.nanami.koishi.feature.home.HomeViewModel
import com.nanami.koishi.feature.home.poetry.PoetryViewModel
import com.nanami.koishi.feature.settings.SettingsViewModel
import com.nanami.koishi.feature.tools.color_picker.ColorPickerRoute
import com.nanami.koishi.feature.tools.color_picker.ColorPickerViewModel
import com.nanami.koishi.feature.tools.date_time_calculator.DateTimeCalculatorRoute
import com.nanami.koishi.feature.tools.date_time_calculator.DateTimeCalculatorViewModel
import com.nanami.koishi.feature.tools.decision_maker.DecisionMakerRoute
import com.nanami.koishi.feature.tools.decision_maker.DecisionMakerViewModel
import com.nanami.koishi.feature.tools.grid_split.GridSplitRoute
import com.nanami.koishi.feature.tools.grid_split.GridSplitViewModel
import com.nanami.koishi.feature.tools.image_obfuscation.ImageObfuscationRoute
import com.nanami.koishi.feature.tools.image_obfuscation.ImageObfuscationViewModel
import com.nanami.koishi.feature.tools.image_sketch.ImageSketchRoute
import com.nanami.koishi.feature.tools.image_sketch.ImageSketchViewModel
import com.nanami.koishi.feature.tools.lan_transfer.LanTransferEngine
import com.nanami.koishi.feature.tools.lan_transfer.LanTransferRoute
import com.nanami.koishi.feature.tools.lan_transfer.LanTransferShareStore
import com.nanami.koishi.feature.tools.lan_transfer.LanTransferViewModel
import com.nanami.koishi.feature.tools.mirage_tank.MirageTankRoute
import com.nanami.koishi.feature.tools.mirage_tank.MirageTankViewModel
import com.nanami.koishi.feature.tools.mini_apps.MiniAppsRoute
import com.nanami.koishi.feature.tools.mini_apps.MiniAppsViewModel
import com.nanami.koishi.feature.tools.postal_code.PostalCodeRoute
import com.nanami.koishi.feature.tools.prism_tank.PrismTankRoute
import com.nanami.koishi.feature.tools.prism_tank.PrismTankViewModel
import com.nanami.koishi.feature.tools.postal_code.PostalCodeViewModel
import com.nanami.koishi.feature.tools.qr_tool.QrToolRoute
import com.nanami.koishi.feature.tools.qr_tool.QrToolViewModel
import com.nanami.koishi.feature.tools.text_apps.encoding.EncodingRoute
import com.nanami.koishi.feature.tools.text_apps.hash.HashRoute
import com.nanami.koishi.feature.tools.text_apps.regex.RegexRoute
import com.nanami.koishi.feature.tools.text_apps.text_process.TextProcessRoute
import com.nanami.koishi.feature.tools.text_apps.diff.DiffRoute
import com.nanami.koishi.feature.tools.text_apps.json.JsonFormatterRoute
import com.nanami.koishi.feature.tools.today_in_history.TodayInHistoryRoute
import com.nanami.koishi.feature.tools.today_in_history.TodayInHistoryViewModel
import com.nanami.koishi.feature.tools.unit_converter.UnitConverterRoute
import com.nanami.koishi.feature.tools.unit_converter.UnitConverterViewModel
import com.nanami.koishi.feature.tools.watermark.WatermarkRoute
import com.nanami.koishi.feature.tools.watermark.WatermarkViewModel

@Composable
fun KoishiNavHost(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
 ) {
    val homeViewModel: HomeViewModel = koinViewModel()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val isHomeDestination = currentBackStackEntry?.destination?.hasRoute<HomeRoute>() != false

    LaunchedEffect(Unit) {
        if (LanTransferShareStore.hasPending()) {
            org.koin.core.context.GlobalContext.get().get<LanTransferEngine>()
                .addSharedFiles(LanTransferShareStore.consume())
            navController.navigate(LanTransferRoute) { launchSingleTop = true }
        }
        LanTransferShareStore.events.collect {
            org.koin.core.context.GlobalContext.get().get<LanTransferEngine>()
                .addSharedFiles(LanTransferShareStore.consume())
            navController.navigate(LanTransferRoute) { launchSingleTop = true }
        }
    }

    KoishiAdaptiveShell(
        selectedTab = homeUiState.currentTab,
        showBottomNavigation = isHomeDestination,
        onSelectTab = { tab ->
            homeViewModel.onEvent(com.nanami.koishi.feature.home.HomeUiEvent.OnSelectTab(tab))
            if (!isHomeDestination) {
                navController.navigate(HomeRoute) {
                    launchSingleTop = true
                    popUpTo<HomeRoute> { inclusive = false }
                }
            }
        },
        modifier = modifier,
    ) {
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<HomeRoute> {
                val poetryViewModel: PoetryViewModel = koinViewModel()
                HomeRoute(
                    viewModel = homeViewModel,
                    poetryViewModel = poetryViewModel,
                    settingsViewModel = settingsViewModel,
                    onNavigateToTool = { tool ->
                        homeViewModel.recordToolUsage(tool.id)
                        when (tool.id) {
                            "image_stitching" -> navController.navigate(ImageStitchingRoute)
                            "image_search" -> navController.navigate(ImageSearchRoute)
                            "grid_split" -> navController.navigate(GridSplitRoute)
                            "image_obfuscation" -> navController.navigate(ImageObfuscationRoute)
                            "mirage_tank" -> navController.navigate(MirageTankRoute)
                            "prism_tank" -> navController.navigate(PrismTankRoute)
                            "qr_code" -> navController.navigate(QrToolRoute)
                            "watermark" -> navController.navigate(WatermarkRoute)
                            "image_sketch" -> navController.navigate(ImageSketchRoute)
                            "lan_transfer" -> navController.navigate(LanTransferRoute)
                            "today_in_history" -> navController.navigate(TodayInHistoryRoute)
                            "decision_maker" -> navController.navigate(DecisionMakerRoute)
                            "ruler" -> navController.navigate(RulerRoute)
                            "currency_converter" -> navController.navigate(CurrencyConverterRoute)
                            "unit_converter" -> navController.navigate(UnitConverterRoute)
                            "date_time_calculator" -> navController.navigate(DateTimeCalculatorRoute)
                            "bmi_calculator" -> navController.navigate(BmiCalculatorRoute)
                            "worth_calculator" -> navController.navigate(WorthCalculatorRoute)
                            "postal_code" -> navController.navigate(PostalCodeRoute)
                            "video_to_gif" -> navController.navigate(VideoToGifRoute)
                            "bili_cover" -> navController.navigate(BiliCoverRoute)
                            "meme_maker" -> navController.navigate(MemeMakerRoute)
                            "color_picker" -> navController.navigate(ColorPickerRoute)
                            "mini_apps" -> navController.navigate(MiniAppsRoute)
                            "json_formatter" -> navController.navigate(JsonFormatterRoute)
                            "text_encoding" -> navController.navigate(EncodingRoute)
                            "text_hash" -> navController.navigate(HashRoute)
                            "regex_tool" -> navController.navigate(RegexRoute)
                            "text_process" -> navController.navigate(TextProcessRoute)
                            "text_diff" -> navController.navigate(TextDiffRoute)
                            "noise_meter" -> navController.navigate(NoiseMeterRoute)
                        }
                    }
                )
            }

            composable<ImageSearchRoute> {
                val imageSearchViewModel: com.nanami.koishi.feature.tools.image_search.ImageSearchViewModel = koinViewModel()
                com.nanami.koishi.feature.tools.image_search.ImageSearchRoute(
                    viewModel = imageSearchViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<ImageStitchingRoute> {
                val stitchingViewModel: com.nanami.koishi.feature.tools.image_stitching.ImageStitchingViewModel = koinViewModel()
                com.nanami.koishi.feature.tools.image_stitching.ImageStitchingRoute(
                    viewModel = stitchingViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<GridSplitRoute> {
                val gridSplitViewModel: GridSplitViewModel = koinViewModel()
                GridSplitRoute(
                    viewModel = gridSplitViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<ImageObfuscationRoute> {
                val obfuscationViewModel: ImageObfuscationViewModel = koinViewModel()
                ImageObfuscationRoute(
                    viewModel = obfuscationViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<MirageTankRoute> {
                val mirageTankViewModel: MirageTankViewModel = koinViewModel()
                MirageTankRoute(
                    viewModel = mirageTankViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<PrismTankRoute> {
                val prismTankViewModel: PrismTankViewModel = koinViewModel()
                PrismTankRoute(
                    viewModel = prismTankViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<QrToolRoute> {
                val qrToolViewModel: QrToolViewModel = koinViewModel()
                QrToolRoute(
                    viewModel = qrToolViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<WatermarkRoute> {
                val watermarkViewModel: WatermarkViewModel = koinViewModel()
                WatermarkRoute(
                    viewModel = watermarkViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<ImageSketchRoute> {
                val imageSketchViewModel: ImageSketchViewModel = koinViewModel()
                ImageSketchRoute(
                    viewModel = imageSketchViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<LanTransferRoute> {
                val lanTransferViewModel: LanTransferViewModel = koinViewModel()
                LanTransferRoute(
                    viewModel = lanTransferViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<TodayInHistoryRoute> {
                val historyViewModel: TodayInHistoryViewModel = koinViewModel()
                TodayInHistoryRoute(
                    viewModel = historyViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<DecisionMakerRoute> {
                val decisionViewModel: DecisionMakerViewModel = koinViewModel()
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
                val currencyConverterViewModel: com.nanami.koishi.feature.tools.currency_converter.CurrencyConverterViewModel = koinViewModel()
                com.nanami.koishi.feature.tools.currency_converter.CurrencyConverterRoute(
                    viewModel = currencyConverterViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<UnitConverterRoute> {
                val unitConverterViewModel: UnitConverterViewModel = koinViewModel()
                UnitConverterRoute(
                    viewModel = unitConverterViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<DateTimeCalculatorRoute> {
                val dateTimeCalculatorViewModel: DateTimeCalculatorViewModel = koinViewModel()
                DateTimeCalculatorRoute(
                    viewModel = dateTimeCalculatorViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<BmiCalculatorRoute> {
                val bmiCalculatorViewModel: com.nanami.koishi.feature.tools.bmi_calculator.BmiCalculatorViewModel = koinViewModel()
                com.nanami.koishi.feature.tools.bmi_calculator.BmiCalculatorRoute(
                    viewModel = bmiCalculatorViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<PostalCodeRoute> {
                val postalCodeViewModel: PostalCodeViewModel = koinViewModel()
                PostalCodeRoute(
                    viewModel = postalCodeViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<VideoToGifRoute> {
                val videoToGifViewModel: com.nanami.koishi.feature.tools.video_to_gif.VideoToGifViewModel = koinViewModel()
                com.nanami.koishi.feature.tools.video_to_gif.VideoToGifRoute(
                    viewModel = videoToGifViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<BiliCoverRoute> {
                val biliCoverViewModel: com.nanami.koishi.feature.tools.bili_cover.BiliCoverViewModel = koinViewModel()
                com.nanami.koishi.feature.tools.bili_cover.BiliCoverRoute(
                    viewModel = biliCoverViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<MemeMakerRoute> {
                val memeMakerViewModel: com.nanami.koishi.feature.tools.meme_maker.MemeMakerViewModel = koinViewModel()
                com.nanami.koishi.feature.tools.meme_maker.MemeMakerRoute(
                    viewModel = memeMakerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<MiniAppsRoute> {
                val miniAppsViewModel: MiniAppsViewModel = koinViewModel()
                MiniAppsRoute(
                    viewModel = miniAppsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<ColorPickerRoute> {
                val colorPickerViewModel: ColorPickerViewModel = koinViewModel()
                ColorPickerRoute(
                    viewModel = colorPickerViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<WorthCalculatorRoute> {
                val worthCalculatorViewModel: com.nanami.koishi.feature.tools.worth_calculator.WorthCalculatorViewModel = koinViewModel()
                com.nanami.koishi.feature.tools.worth_calculator.WorthCalculatorRoute(
                    viewModel = worthCalculatorViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable<JsonFormatterRoute> {
                JsonFormatterRoute(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<EncodingRoute> {
                EncodingRoute(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<HashRoute> {
                HashRoute(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<RegexRoute> {
                RegexRoute(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<TextProcessRoute> {
                TextProcessRoute(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<TextDiffRoute> {
                DiffRoute(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<NoiseMeterRoute> {
                val noiseMeterViewModel: com.nanami.koishi.feature.tools.noise_meter.NoiseMeterViewModel = koinViewModel()
                com.nanami.koishi.feature.tools.noise_meter.NoiseMeterRoute(
                    viewModel = noiseMeterViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
