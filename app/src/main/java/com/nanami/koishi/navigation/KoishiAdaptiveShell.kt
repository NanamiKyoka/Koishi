package com.nanami.koishi.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.adaptive.BottomBarVisibilityController
import com.nanami.koishi.core.designsystem.adaptive.LocalBottomBarVisibilityController
import com.nanami.koishi.core.designsystem.adaptive.LocalWindowLayoutMode
import com.nanami.koishi.core.designsystem.adaptive.WindowLayoutMode
import com.nanami.koishi.core.designsystem.adaptive.windowLayoutModeForWidth
import com.nanami.koishi.feature.home.MainTab

@Composable
fun KoishiAdaptiveShell(
    selectedTab: MainTab,
    showBottomNavigation: Boolean,
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layoutMode = windowLayoutModeForWidth(maxWidth)
        val density = LocalDensity.current
        val hideThresholdPx = with(density) { 16.dp.toPx() }
        val showThresholdPx = with(density) { 10.dp.toPx() }
        val bottomBarVisibilityController = remember(hideThresholdPx, showThresholdPx) {
            BottomBarVisibilityController(
                hideThresholdPx = hideThresholdPx,
                showThresholdPx = showThresholdPx
            )
        }

        LaunchedEffect(selectedTab, showBottomNavigation, layoutMode) {
            if (
                layoutMode != WindowLayoutMode.COMPACT ||
                selectedTab != MainTab.TOOLBOX ||
                !showBottomNavigation
            ) {
                bottomBarVisibilityController.show()
            }
        }

        CompositionLocalProvider(
            LocalWindowLayoutMode provides layoutMode,
            LocalBottomBarVisibilityController provides bottomBarVisibilityController
        ) {
            if (layoutMode == WindowLayoutMode.COMPACT) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (showBottomNavigation) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                AnimatedVisibility(
                                    visible = bottomBarVisibilityController.isVisible,
                                    enter = slideInVertically(initialOffsetY = { it }) +
                                        expandVertically(expandFrom = Alignment.Bottom),
                                    exit = slideOutVertically(targetOffsetY = { it }) +
                                        shrinkVertically(shrinkTowards = Alignment.Bottom)
                                ) {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        tonalElevation = 0.dp,
                                        windowInsets = WindowInsets(0)
                                    ) {
                                        MainTab.entries.forEach { tab ->
                                            NavigationBarItem(
                                                selected = selectedTab == tab,
                                                onClick = { onSelectTab(tab) },
                                                icon = {
                                                    Icon(
                                                        imageVector = tab.icon(),
                                                        contentDescription = stringResource(tab.titleRes),
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = stringResource(tab.titleRes),
                                                        fontWeight = if (selectedTab == tab) {
                                                            FontWeight.Bold
                                                        } else {
                                                            FontWeight.Normal
                                                        },
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                ),
                                            )
                                        }
                                    }
                                }
                                Spacer(
                                    modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
                                )
                            }
                        }
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        content(PaddingValues(0.dp))
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        MainTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { onSelectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon(),
                                        contentDescription = stringResource(tab.titleRes),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.titleRes),
                                        fontWeight = if (selectedTab == tab) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        content(PaddingValues(0.dp))
                    }
                }
            }
        }
    }
}

private val MainTab.titleRes: Int
    get() = when (this) {
        MainTab.FAVORITES -> R.string.nav_favorites
        MainTab.TOOLBOX -> R.string.nav_toolbox
        MainTab.SETTINGS -> R.string.nav_settings
    }

private fun MainTab.icon(): ImageVector = when (this) {
    MainTab.FAVORITES -> Icons.Rounded.Favorite
    MainTab.TOOLBOX -> Icons.Rounded.Widgets
    MainTab.SETTINGS -> Icons.Rounded.Settings
}
