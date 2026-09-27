package com.nanami.koishi.core.di

import com.nanami.koishi.feature.home.HomeViewModel
import com.nanami.koishi.feature.home.poetry.PoetryViewModel
import com.nanami.koishi.feature.settings.SettingsViewModel
import com.nanami.koishi.feature.tools.bili_cover.BiliCoverViewModel
import com.nanami.koishi.feature.tools.bmi_calculator.BmiCalculatorViewModel
import com.nanami.koishi.feature.tools.currency_converter.CurrencyConverterViewModel
import com.nanami.koishi.feature.tools.decision_maker.DecisionMakerViewModel
import com.nanami.koishi.feature.tools.grid_split.GridSplitViewModel
import com.nanami.koishi.feature.tools.image_obfuscation.ImageObfuscationViewModel
import com.nanami.koishi.feature.tools.image_search.ImageSearchViewModel
import com.nanami.koishi.feature.tools.image_sketch.ImageSketchViewModel
import com.nanami.koishi.feature.tools.image_stitching.ImageStitchingViewModel
import com.nanami.koishi.feature.tools.meme_maker.MemeMakerViewModel
import com.nanami.koishi.feature.tools.mini_apps.MiniAppsViewModel
import com.nanami.koishi.feature.tools.mirage_tank.MirageTankViewModel
import com.nanami.koishi.feature.tools.qr_tool.QrToolViewModel
import com.nanami.koishi.feature.tools.today_in_history.TodayInHistoryViewModel
import com.nanami.koishi.feature.tools.video_to_gif.VideoToGifViewModel
import com.nanami.koishi.feature.tools.watermark.WatermarkViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { PoetryViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }

    viewModel { BiliCoverViewModel(get(), get()) }
    viewModel { BmiCalculatorViewModel(get(), get()) }
    viewModel { CurrencyConverterViewModel(get(), get()) }
    viewModel { DecisionMakerViewModel(get(), get(), get()) }
    viewModel { GridSplitViewModel(get()) }
    viewModel { ImageObfuscationViewModel(get()) }
    viewModel { ImageSearchViewModel(get(), get()) }
    viewModel { ImageSketchViewModel(get()) }
    viewModel { ImageStitchingViewModel(get()) }
    viewModel { MemeMakerViewModel(get(), get(), get()) }
    viewModel { MiniAppsViewModel(get(), get()) }
    viewModel { MirageTankViewModel(get()) }
    viewModel { QrToolViewModel(get()) }
    viewModel { TodayInHistoryViewModel(get(), get(), get()) }
    viewModel { VideoToGifViewModel(get(), get()) }
    viewModel { WatermarkViewModel(get()) }
}
