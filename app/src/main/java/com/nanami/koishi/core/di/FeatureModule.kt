package com.nanami.koishi.core.di

import com.nanami.koishi.feature.home.poetry.HitokotoCache
import com.nanami.koishi.feature.home.poetry.HitokotoRepository
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliCoverSettingsRepository
import com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiRepository
import com.nanami.koishi.feature.tools.color_picker.engine.ColorPickerRepository
import com.nanami.koishi.feature.tools.currency_converter.engine.CurrencyConverterRepository
import com.nanami.koishi.feature.tools.decision_maker.engine.BuiltInPresets
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionArchiveStore
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionRepository
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionStorageRepository
import com.nanami.koishi.feature.tools.image_search.engine.ImageSearchSettingsRepository
import com.nanami.koishi.feature.tools.meme_maker.data.MemeLocalStickerRepository
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeAssetRepository
import com.nanami.koishi.feature.tools.mini_apps.engine.MiniAppStore
import com.nanami.koishi.feature.tools.postal_code.engine.PostalDatasetRepository
import com.nanami.koishi.feature.tools.postal_code.engine.PostalRepository
import com.nanami.koishi.feature.tools.postal_code.engine.PostalSettingsRepository
import com.nanami.koishi.feature.tools.today_in_history.engine.HistoryCache
import com.nanami.koishi.feature.tools.today_in_history.engine.HistoryRepository
import com.nanami.koishi.feature.tools.today_in_history.engine.HistorySettingsRepository
import com.nanami.koishi.feature.tools.video_to_gif.engine.VideoToGifEngine
import org.koin.dsl.module

val featureModule = module {

    single { HitokotoCache(get()) }
    single { HitokotoRepository(get()) }

    single { BmiRepository(get()) }
    single { ColorPickerRepository(get()) }
    single { CurrencyConverterRepository(get()) }

    single { BiliCoverSettingsRepository(get()) }
    single { ImageSearchSettingsRepository(get()) }

    single { HistorySettingsRepository(get()) }
    single { HistoryCache(get()) }
    single { HistoryRepository(get(), get()) }

    single { MiniAppStore(get()) }

    single { PostalSettingsRepository(get()) }
    single { PostalDatasetRepository(get()) }
    single { PostalRepository(get()) }

    single { DecisionStorageRepository(get()) }
    single { DecisionArchiveStore(get()) }
    single {
        DecisionRepository(
            builtInTopics = { BuiltInPresets.build(get()) },
            storage = get()
        )
    }

    single { MemeAssetRepository(get()) }
    single { MemeLocalStickerRepository(get()) }

    single { VideoToGifEngine(get()) }
}
