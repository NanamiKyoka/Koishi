package com.nanami.koishi.feature.tools.ruler

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nanami.koishi.core.data.preferences.ThemePreferences
import com.nanami.koishi.core.designsystem.theme.KoishiTheme
import com.nanami.koishi.core.designsystem.theme.isDarkTheme
import com.nanami.koishi.core.util.LocaleHelper
import com.nanami.koishi.feature.tools.ruler.components.ProtractorView
import com.nanami.koishi.feature.tools.ruler.components.RulerBottomBar
import com.nanami.koishi.feature.tools.ruler.components.RulerCalibrationScreen
import com.nanami.koishi.feature.tools.ruler.components.StraightRulerView

class RulerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_INITIAL_MODE = "extra_initial_mode"

        fun createIntent(context: Context, mode: RulerTargetMode): Intent {
            return Intent(context, RulerActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_MODE, mode.name)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    private fun hideSystemStatusBar() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemStatusBar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemStatusBar()

        val initialModeName = intent.getStringExtra(EXTRA_INITIAL_MODE)
        val initialMode = try {
            if (initialModeName != null) RulerTargetMode.valueOf(initialModeName) else RulerTargetMode.RULER
        } catch (e: Exception) {
            RulerTargetMode.RULER
        }

        setContent {
            val themeMode = ThemePreferences.themeMode(this)
            val appTheme = ThemePreferences.appTheme(this)
            val amoled = ThemePreferences.isAmoled(this)

            KoishiTheme(
                appTheme = appTheme,
                amoled = amoled,
                darkTheme = themeMode.isDarkTheme
            ) {
                var currentMode by remember { mutableStateOf(initialMode) }
                var isCalibrating by remember { mutableStateOf(false) }

                BackHandler {
                    if (isCalibrating) {
                        isCalibrating = false
                    } else {
                        finish()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            ) {
                                when (currentMode) {
                                    RulerTargetMode.RULER -> {
                                        StraightRulerView(
                                            onOpenCalibration = { isCalibrating = true },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    RulerTargetMode.PROTRACTOR -> {
                                        ProtractorView(
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { finish() },
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 12.dp, top = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            RulerBottomBar(
                                currentMode = currentMode,
                                onModeSelected = { currentMode = it }
                            )
                        }

                        if (isCalibrating) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                RulerCalibrationScreen(
                                    onDismiss = { isCalibrating = false },
                                    onCalibrationSaved = { isCalibrating = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
