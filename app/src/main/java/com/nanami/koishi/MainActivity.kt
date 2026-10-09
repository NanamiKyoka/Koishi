package com.nanami.koishi

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nanami.koishi.core.designsystem.theme.KoishiTheme
import com.nanami.koishi.core.designsystem.theme.isDarkTheme
import com.nanami.koishi.core.util.LocaleHelper
import com.nanami.koishi.feature.settings.AppLanguage
import com.nanami.koishi.feature.settings.SettingsViewModel
import com.nanami.koishi.feature.tools.lan_transfer.LanTransferShareStore
import com.nanami.koishi.navigation.KoishiNavHost
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    private fun handleShareIntent(intent: Intent?) {
        val uris = when (intent?.action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM) as? android.net.Uri)
            Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM).orEmpty()
            else -> emptyList()
        }
        if (uris.isEmpty()) return
        lifecycleScope.launch(Dispatchers.IO) {
            LanTransferShareStore.offerUris(uris, contentResolver)
        }
    }


    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)

        setContent {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            val targetLocale = when (settingsState.language) {
                AppLanguage.ZH -> Locale.SIMPLIFIED_CHINESE
                AppLanguage.EN -> Locale.ENGLISH
                AppLanguage.SYSTEM -> null
            }

            val currentConfig = LocalConfiguration.current
            val (effectiveConfig, localizedContext) = remember(settingsState.language, currentConfig) {
                LocaleHelper.wrapWithActivity(this@MainActivity, currentConfig, targetLocale)
            }

            CompositionLocalProvider(
                LocalConfiguration provides effectiveConfig,
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                KoishiTheme(
                    appTheme = settingsState.appTheme,
                    amoled = settingsState.amoled,
                    darkTheme = settingsState.themeMode.isDarkTheme
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        androidx.compose.runtime.key(settingsState.refreshKey) {
                            KoishiNavHost(settingsViewModel = settingsViewModel)
                        }
                    }
                }
            }
        }
    }
}
