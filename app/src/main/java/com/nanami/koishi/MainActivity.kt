package com.nanami.koishi

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.core.designsystem.KoishiTheme
import com.nanami.koishi.feature.settings.AppLanguage
import com.nanami.koishi.feature.settings.SettingsViewModel
import com.nanami.koishi.feature.settings.ThemeMode
import com.nanami.koishi.navigation.KoishiNavHost
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            val isDark = when (settingsState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val targetLocale = when (settingsState.language) {
                AppLanguage.ZH -> Locale.SIMPLIFIED_CHINESE
                AppLanguage.EN -> Locale.ENGLISH
                AppLanguage.SYSTEM -> null
            }

            val currentConfig = LocalConfiguration.current
            val (effectiveConfig, localizedContext) = remember(settingsState.language, currentConfig) {
                if (targetLocale != null) {
                    val config = Configuration(currentConfig).apply {
                        setLocale(targetLocale)
                        setLayoutDirection(targetLocale)
                    }
                    config to createConfigurationContext(config)
                } else {
                    currentConfig to this
                }
            }

            CompositionLocalProvider(
                LocalConfiguration provides effectiveConfig,
                LocalContext provides localizedContext
            ) {
                KoishiTheme(
                    darkTheme = isDark,
                    dynamicColor = settingsState.dynamicColor
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        KoishiNavHost(settingsViewModel = settingsViewModel)
                    }
                }
            }
        }
    }
}
