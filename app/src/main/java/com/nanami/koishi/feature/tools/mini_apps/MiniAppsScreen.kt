package com.nanami.koishi.feature.tools.mini_apps

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.mini_apps.components.AddMiniAppDialog
import com.nanami.koishi.feature.tools.mini_apps.components.ClearBrowsingDataDialog
import com.nanami.koishi.feature.tools.mini_apps.components.MiniAppReorderableList
import com.nanami.koishi.feature.tools.mini_apps.engine.MiniAppEntry
import com.nanami.koishi.feature.tools.mini_apps.engine.WebViewDataCleaner
import kotlinx.coroutines.launch

@Composable
fun MiniAppsRoute(
    viewModel: MiniAppsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeEntry by remember { mutableStateOf<MiniAppEntry?>(null) }
    var showCleanDialog by remember { mutableStateOf(false) }

    val userMessage = uiState.userMessageRes?.let { messageRes ->
        stringResource(messageRes, *uiState.userMessageArgs.toTypedArray())
    }

    LaunchedEffect(uiState.userMessageRes, userMessage) {
        if (userMessage != null) {
            Toast.makeText(context, userMessage, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(MiniAppsUiEvent.OnDismissMessage)
        }
    }

    val openedEntry = activeEntry
    if (openedEntry == null) {
        MiniAppsScreen(
            state = uiState,
            onEvent = viewModel::onEvent,
            onBack = onBack,
            onOpenEntry = { activeEntry = it },
            onCleanRequest = { showCleanDialog = true },
            modifier = modifier
        )
    } else {
        val entry = uiState.entries.firstOrNull { it.id == openedEntry.id } ?: openedEntry
        MiniWebScreen(
            entry = entry,
            onExit = { activeEntry = null },
            onToggleDesktopMode = { viewModel.setDesktopMode(entry.id, !entry.desktopMode) },
            onPageTitleResolved = { title -> viewModel.resolvePageTitle(entry.id, title, entry.url) },
            modifier = modifier
        )
    }

    if (uiState.isAddDialogVisible) {
        AddMiniAppDialog(
            onDismiss = { viewModel.onEvent(MiniAppsUiEvent.OnShowAddDialog(false)) },
            onConfirm = { url, title -> viewModel.onEvent(MiniAppsUiEvent.OnAddEntry(url, title)) }
        )
    }

    if (showCleanDialog) {
        ClearBrowsingDataDialog(
            onDismiss = { showCleanDialog = false },
            onConfirm = {
                showCleanDialog = false
                scope.launch {
                    val cleared = try {
                        WebViewDataCleaner.clearAll(context)
                        true
                    } catch (error: Exception) {
                        false
                    }
                    Toast.makeText(
                        context,
                        if (cleared) R.string.mini_apps_clean_done else R.string.mini_apps_clean_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppsScreen(
    state: MiniAppsUiState,
    onEvent: (MiniAppsUiEvent) -> Unit,
    onBack: () -> Unit,
    onOpenEntry: (MiniAppEntry) -> Unit,
    onCleanRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.mini_apps_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (state.hasEntries) {
                            Text(
                                text = stringResource(R.string.mini_apps_count, state.entries.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onCleanRequest) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = stringResource(R.string.mini_apps_clean),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEvent(MiniAppsUiEvent.OnShowAddDialog(true)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.mini_apps_add),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.isEmpty -> MiniAppsEmptyContent(
                    onAdd = { onEvent(MiniAppsUiEvent.OnShowAddDialog(true)) },
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    if (state.entries.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.mini_apps_reorder_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    MiniAppReorderableList(
                        entries = state.entries,
                        onOpenEntry = onOpenEntry,
                        onRemoveEntry = { onEvent(MiniAppsUiEvent.OnRemoveEntry(it.id)) },
                        onMoveEntry = { fromIndex, toIndex ->
                            onEvent(MiniAppsUiEvent.OnMoveEntry(fromIndex, toIndex))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniAppsEmptyContent(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.TravelExplore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.mini_apps_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.mini_apps_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        FilledTonalButton(onClick = onAdd) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(stringResource(R.string.mini_apps_add))
        }
    }
}
