package com.nanami.koishi.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanami.koishi.BuildConfig
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.adaptive.AdaptiveContentWidth
import com.nanami.koishi.core.designsystem.theme.ThemeMode
import com.nanami.koishi.core.designsystem.theme.isDarkTheme
import com.nanami.koishi.feature.settings.components.AppThemePicker
import com.nanami.koishi.feature.settings.components.LocalBackupDialog
import com.nanami.koishi.feature.settings.components.LocalImportConfirmDialog
import com.nanami.koishi.feature.settings.components.ThemeModeSelector
import com.nanami.koishi.feature.settings.components.ToolVisibilityDialog
import com.nanami.koishi.feature.settings.components.UpdateDialog
import com.nanami.koishi.feature.settings.components.WebDavSyncDialog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage = uiState.userMessage

    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(context.getString(userMessage))
            onEvent(SettingsUiEvent.OnDismissUserMessage)
        }
    }

    LaunchedEffect(Unit) {
        onEvent(SettingsUiEvent.OnRefreshStorageUsage)
    }

    if (uiState.showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = uiState.language,
            onSelect = { onEvent(SettingsUiEvent.OnLanguageSelected(it)) },
            onDismiss = { onEvent(SettingsUiEvent.OnShowLanguageDialog(false)) }
        )
    }

    if (uiState.showToolVisibilityDialog) {
        ToolVisibilityDialog(
            tools = uiState.allTools,
            hiddenToolIds = uiState.hiddenToolIds,
            onToolHiddenChange = { toolId, hidden ->
                onEvent(SettingsUiEvent.OnSetToolHidden(toolId, hidden))
            },
            onShowAll = { onEvent(SettingsUiEvent.OnShowAllTools) },
            onDismiss = { onEvent(SettingsUiEvent.OnShowToolVisibilityDialog(false)) }
        )
    }

    if (uiState.showWebDavDialog) {
        WebDavSyncDialog(
            config = uiState.webDavConfig,
            syncState = uiState.webDavSyncState,
            onUpdateConfig = { onEvent(SettingsUiEvent.OnUpdateWebDavConfig(it)) },
            onTestConnection = { onEvent(SettingsUiEvent.OnTestWebDavConnection) },
            onBackup = { onEvent(SettingsUiEvent.OnWebDavBackup) },
            onRestore = { onEvent(SettingsUiEvent.OnWebDavRestore) },
            onDismissStatus = { onEvent(SettingsUiEvent.OnDismissWebDavStatus) },
            onDismiss = { onEvent(SettingsUiEvent.OnShowWebDavDialog(false)) }
        )
    }

    if (uiState.updateState !is UpdateUiState.Idle) {
        val canInstall = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
        UpdateDialog(
            state = uiState.updateState,
            canInstallPackages = canInstall,
            onDismiss = { onEvent(SettingsUiEvent.OnDismissUpdateDialog) },
            onDownload = { onEvent(SettingsUiEvent.OnDownloadUpdate(it)) },
            onCancelDownload = { onEvent(SettingsUiEvent.OnCancelDownload) },
            onInstall = { onEvent(SettingsUiEvent.OnInstallUpdate(it)) },
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        val intent = Intent(
                            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                }
            },
            onOpenWebPage = { openWebPage(context, it) }
        )
    }

    if (uiState.showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.OnShowClearCacheDialog(false)) },
            title = { Text(stringResource(R.string.settings_storage_cache)) },
            text = { Text(stringResource(R.string.settings_storage_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.OnClearCache) }) {
                    Text(stringResource(R.string.settings_storage_clear_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.OnShowClearCacheDialog(false)) }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (uiState.showLocalBackupDialog) {
        LocalBackupDialog(
            includeSensitive = uiState.localBackupIncludeSensitive,
            busy = uiState.isLocalBackupBusy,
            onIncludeSensitiveChange = { onEvent(SettingsUiEvent.OnLocalBackupSensitiveChange(it)) },
            onExport = { onEvent(SettingsUiEvent.OnExportLocalBackup(it)) },
            onImport = { onEvent(SettingsUiEvent.OnImportLocalBackup(it)) },
            onDismiss = { onEvent(SettingsUiEvent.OnShowLocalBackupDialog(false)) }
        )
    }

    if (uiState.pendingImportBackup != null) {
        LocalImportConfirmDialog(
            onConfirm = { onEvent(SettingsUiEvent.OnConfirmLocalImport) },
            onDismiss = { onEvent(SettingsUiEvent.OnCancelLocalImport) }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )

        AdaptiveContentWidth(
            maxWidth = 840.dp,
            modifier = Modifier.weight(1f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                SettingsCategoryHeader(title = stringResource(R.string.settings_category_appearance))

                ThemeModeSelector(
                    value = uiState.themeMode,
                    onSelect = { onEvent(SettingsUiEvent.OnThemeModeSelected(it)) }
                )

                AppThemePicker(
                    value = uiState.appTheme,
                    amoled = uiState.amoled,
                    darkTheme = uiState.themeMode.isDarkTheme,
                    onSelect = { onEvent(SettingsUiEvent.OnAppThemeSelected(it)) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsSwitchItem(
                    icon = Icons.Rounded.DarkMode,
                    title = stringResource(R.string.settings_amoled),
                    subtitle = stringResource(R.string.settings_amoled_desc),
                    checked = uiState.amoled,
                    enabled = uiState.themeMode != ThemeMode.LIGHT,
                    onCheckedChange = { onEvent(SettingsUiEvent.OnAmoledToggled(it)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsCategoryHeader(title = stringResource(R.string.settings_category_language))

                SettingsClickableItem(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(uiState.language.titleRes),
                    onClick = { onEvent(SettingsUiEvent.OnShowLanguageDialog(true)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsCategoryHeader(title = stringResource(R.string.settings_category_tools))

                SettingsClickableItem(
                    icon = Icons.Rounded.VisibilityOff,
                    title = stringResource(R.string.settings_tool_visibility),
                    subtitle = stringResource(
                        R.string.settings_tool_visibility_desc,
                        uiState.allTools.count { it.id in uiState.hiddenToolIds }
                    ),
                    onClick = { onEvent(SettingsUiEvent.OnShowToolVisibilityDialog(true)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsCategoryHeader(title = stringResource(R.string.settings_category_data))

                SettingsClickableItem(
                    icon = Icons.Rounded.CloudSync,
                    title = stringResource(R.string.settings_webdav_sync),
                    subtitle = stringResource(R.string.settings_webdav_sync_desc),
                    onClick = { onEvent(SettingsUiEvent.OnShowWebDavDialog(true)) }
                )

                SettingsClickableItem(
                    icon = Icons.Rounded.Backup,
                    title = stringResource(R.string.settings_local_backup),
                    subtitle = stringResource(R.string.settings_local_backup_desc),
                    onClick = { onEvent(SettingsUiEvent.OnShowLocalBackupDialog(true)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsCategoryHeader(title = stringResource(R.string.settings_category_storage))

                SettingsClickableItem(
                    icon = Icons.Rounded.DeleteSweep,
                    title = stringResource(R.string.settings_storage_cache),
                    subtitle = if (uiState.cacheSizeBytes > 0) {
                        stringResource(R.string.settings_storage_cache_usage, formatBytes(uiState.cacheSizeBytes))
                    } else {
                        stringResource(R.string.settings_storage_cache_empty)
                    },
                    onClick = { onEvent(SettingsUiEvent.OnShowClearCacheDialog(true)) },
                    trailing = if (uiState.isClearingCache) {
                        {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        null
                    }
                )

                SettingsClickableItem(
                    icon = Icons.Rounded.Storage,
                    title = stringResource(R.string.settings_storage_data),
                    subtitle = stringResource(R.string.settings_storage_data_usage, formatBytes(uiState.dataSizeBytes)),
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsCategoryHeader(title = stringResource(R.string.settings_category_about))

                SettingsClickableItem(
                    icon = Icons.Rounded.SystemUpdate,
                    title = stringResource(R.string.settings_check_update),
                    subtitle = stringResource(R.string.settings_check_update_desc),
                    onClick = { onEvent(SettingsUiEvent.OnCheckForUpdate) },
                    trailing = if (uiState.isCheckingUpdate) {
                        {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        null
                    }
                )

                val projectUrl = stringResource(R.string.settings_project_url)
                SettingsClickableItem(
                    icon = Icons.Rounded.Info,
                    title = "Koishi",
                    subtitle = "${stringResource(R.string.settings_version)} ${BuildConfig.VERSION_NAME}\n${stringResource(R.string.settings_about_desc)}",
                    onClick = { openWebPage(context, projectUrl) }
                )

                val developerUrl = stringResource(R.string.settings_developer_url)
                SettingsClickableItem(
                    icon = Icons.Rounded.Person,
                    title = stringResource(R.string.settings_developer),
                    subtitle = "NanamiKyoka",
                    onClick = { openWebPage(context, developerUrl) }
                )
            }
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column {
                AppLanguage.entries.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(lang) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (lang == currentLanguage),
                            onClick = { onSelect(lang) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(lang.titleRes),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

private fun openWebPage(context: Context, url: String) {
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
    } catch (_: Exception) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}
