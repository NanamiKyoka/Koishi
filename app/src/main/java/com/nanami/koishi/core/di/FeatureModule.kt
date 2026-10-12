package com.nanami.koishi.core.di

import com.nanami.koishi.feature.home.poetry.HitokotoCache
import com.nanami.koishi.feature.home.poetry.HitokotoRepository
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliCoverSettingsRepository
import com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiRepository
import com.nanami.koishi.feature.tools.color_picker.engine.ColorPickerRepository
import com.nanami.koishi.feature.tools.currency_converter.engine.CurrencyConverterRepository
import com.nanami.koishi.feature.tools.date_time_calculator.engine.DateTimeCalculatorRepository
import com.nanami.koishi.feature.tools.unit_converter.engine.UnitConverterRepository
import com.nanami.koishi.feature.tools.decision_maker.engine.BuiltInPresets
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionArchiveStore
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionRepository
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionStorageRepository
import com.nanami.koishi.feature.tools.image_search.engine.ImageSearchSettingsRepository
import com.nanami.koishi.feature.tools.lan_transfer.engine.discovery.DeviceDiscovery
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalDeviceInfoProvider
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendClient
import com.nanami.koishi.feature.tools.lan_transfer.engine.protocol.LocalSendRegisterClient
import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendIdentityStore
import com.nanami.koishi.feature.tools.lan_transfer.LanTransferEngine
import com.nanami.koishi.feature.tools.lan_transfer.engine.settings.LanTransferSettingsRepository
import com.nanami.koishi.feature.tools.meme_maker.data.MemeLocalStickerRepository
import com.nanami.koishi.feature.tools.meme_maker.engine.MemeAssetRepository
import com.nanami.koishi.feature.tools.mini_apps.engine.MiniAppStore
import com.nanami.koishi.feature.tools.noise_meter.engine.NoiseMeterEngine
import com.nanami.koishi.feature.tools.noise_meter.engine.NoiseMeterRepository
import com.nanami.koishi.feature.tools.postal_code.engine.PostalDatasetRepository
import com.nanami.koishi.feature.tools.postal_code.engine.PostalRepository
import com.nanami.koishi.feature.tools.postal_code.engine.PostalSettingsRepository
import com.nanami.koishi.feature.tools.mirage_tank.engine.MirageTankSettingsRepository
import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankSettingsRepository
import com.nanami.koishi.feature.tools.today_in_history.engine.HistoryCache
import com.nanami.koishi.feature.tools.today_in_history.engine.HistoryRepository
import com.nanami.koishi.feature.tools.today_in_history.engine.HistorySettingsRepository
import com.nanami.koishi.feature.tools.video_to_gif.engine.VideoToGifEngine
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthRepository
import org.koin.dsl.module

val featureModule = module {

    single { HitokotoCache(get()) }
    single { HitokotoRepository(get()) }

    single { BmiRepository(get()) }
    single { ColorPickerRepository(get()) }
    single { CurrencyConverterRepository(get()) }
    single { UnitConverterRepository(get()) }
    single { DateTimeCalculatorRepository(get()) }
    single { WorthRepository(get()) }

    single { BiliCoverSettingsRepository(get()) }
    single { ImageSearchSettingsRepository(get()) }
    single { MirageTankSettingsRepository(get()) }
    single { PrismTankSettingsRepository(get()) }

    single { HistorySettingsRepository(get()) }
    single { HistoryCache(get()) }
    single { HistoryRepository(get(), get()) }

    single { MiniAppStore(get()) }

    single { NoiseMeterEngine() }
    single { NoiseMeterRepository(get()) }

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

    single { LanTransferSettingsRepository(get()) }
    single { LocalSendIdentityStore(get()) }
    single { LocalDeviceInfoProvider(get()) }
    single { LocalSendClient(get<LocalSendIdentityStore>().get()) }
    single<LocalSendRegisterClient> { get<LocalSendClient>() }
    single { DeviceDiscovery(get(), get(), get()) }
    single { LanTransferEngine(get(), get(), get(), get(), get(), get()) }
}
