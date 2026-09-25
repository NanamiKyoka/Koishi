package com.nanami.koishi.feature.tools.today_in_history

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.PillShape
import com.nanami.koishi.core.designsystem.ToolCardShape
import com.nanami.koishi.core.image.preview.ImagePreviewDialog
import com.nanami.koishi.feature.tools.today_in_history.components.TimelineEmptyState
import com.nanami.koishi.feature.tools.today_in_history.components.TimelineEventItem
import com.nanami.koishi.feature.tools.today_in_history.engine.HistoryEngines
import com.nanami.koishi.feature.tools.today_in_history.engine.HistorySource

@Composable
fun TodayInHistoryRoute(
    viewModel: TodayInHistoryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // 在 composable 作用域内解析文案，避免在 LaunchedEffect 中调用 stringResource
    val userMessage = uiState.userMessageRes?.let { stringResource(it) }

    LaunchedEffect(uiState.userMessageRes, userMessage) {
        if (userMessage != null) {
            Toast.makeText(context, userMessage, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(TodayInHistoryUiEvent.OnDismissMessage)
        }
    }

    TodayInHistoryScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayInHistoryScreen(
    state: TodayInHistoryUiState,
    onEvent: (TodayInHistoryUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var previewIndex by remember { mutableStateOf<Int?>(null) }

    val imageEvents = state.events.filter { it.imageUrl.isNotBlank() }

    previewIndex?.let { index ->
        if (index in imageEvents.indices) {
            ImagePreviewDialog(
                images = imageEvents.map { it.imageUrl },
                initialIndex = index,
                title = stringResource(R.string.history_title),
                onDismissRequest = { previewIndex = null }
            )
        }
    }

    if (state.showDatePicker) {
        HistoryDatePickerDialog(
            initialMonth = state.month,
            initialDay = state.day,
            onConfirm = { month, day ->
                onEvent(TodayInHistoryUiEvent.OnDatePicked(month, day))
            },
            onDismiss = { onEvent(TodayInHistoryUiEvent.OnShowDatePicker(false)) }
        )
    }

    if (state.showApiKeyDialog) {
        ShowApiKeyDialog(
            configured = state.showApiKeyConfigured,
            onSave = { onEvent(TodayInHistoryUiEvent.OnSaveShowApiKey(it)) },
            onClear = { onEvent(TodayInHistoryUiEvent.OnClearShowApiKey) },
            onDismiss = { onEvent(TodayInHistoryUiEvent.OnShowApiKeyDialog(false)) },
            onOpenConsole = {
                openWebPage(context, HistoryEngines.SHOW_API_HOME)
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.history_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onEvent(TodayInHistoryUiEvent.OnToggleAllEvents(true)) },
                        enabled = state.events.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.UnfoldMore,
                            contentDescription = stringResource(R.string.history_expand_all),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onEvent(TodayInHistoryUiEvent.OnShowApiKeyDialog(true)) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = stringResource(R.string.history_api_key_label),
                            tint = if (state.showApiKeyConfigured) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            DateNavigator(
                state = state,
                onPrevious = { onEvent(TodayInHistoryUiEvent.OnPreviousDay) },
                onNext = { onEvent(TodayInHistoryUiEvent.OnNextDay) },
                onJumpToToday = { onEvent(TodayInHistoryUiEvent.OnJumpToToday) },
                onRefresh = { onEvent(TodayInHistoryUiEvent.OnRefresh) },
                onLongPressDate = { onEvent(TodayInHistoryUiEvent.OnShowDatePicker(true)) }
            )

            SourceBanner(
                state = state,
                onConfigureKey = { onEvent(TodayInHistoryUiEvent.OnShowApiKeyDialog(true)) }
            )

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { onEvent(TodayInHistoryUiEvent.OnRefresh) },
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                when (val loadState = state.loadState) {
                    is HistoryLoadState.Loading -> LoadingState()
                    is HistoryLoadState.Success -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 28.dp
                        )
                    ) {
                        itemsIndexed(
                            items = loadState.day.events,
                            key = { index, event ->
                                TodayInHistoryViewModel.eventKey(index, event)
                            }
                        ) { index, event ->
                            val key = TodayInHistoryViewModel.eventKey(index, event)
                            TimelineEventItem(
                                event = event,
                                index = index,
                                expanded = state.expandedEventKeys.contains(key),
                                isFirst = index == 0,
                                isLast = index == loadState.day.events.lastIndex,
                                onToggle = {
                                    onEvent(TodayInHistoryUiEvent.OnToggleEventExpanded(key))
                                },
                                onImageClick = {
                                    previewIndex = imageEvents.indexOf(event)
                                }
                            )
                        }
                    }
                    is HistoryLoadState.Empty -> TimelineEmptyState(
                        title = stringResource(R.string.history_empty_title),
                        description = stringResource(
                            R.string.history_empty_desc,
                            state.month,
                            state.day
                        ),
                        icon = Icons.Rounded.EventBusy,
                        action = {
                            FilledTonalButton(
                                onClick = { onEvent(TodayInHistoryUiEvent.OnPreviousDay) },
                                shape = PillShape
                            ) {
                                Text(stringResource(R.string.history_empty_action))
                            }
                        }
                    )
                    is HistoryLoadState.Error -> TimelineEmptyState(
                        title = stringResource(R.string.history_error_title),
                        description = stringResource(loadState.messageRes),
                        icon = Icons.Rounded.ErrorOutline,
                        action = {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onEvent(TodayInHistoryUiEvent.OnRetry) },
                                    shape = PillShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.history_retry))
                                }
                                OutlinedButton(
                                    onClick = { onEvent(TodayInHistoryUiEvent.OnShowApiKeyDialog(true)) },
                                    shape = PillShape,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Text(stringResource(R.string.history_api_key_label))
                                }
                            }
                        }
                    )
                    is HistoryLoadState.Idle -> LoadingState()
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.history_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DateNavigator(
    state: TodayInHistoryUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJumpToToday: () -> Unit,
    onRefresh: () -> Unit,
    onLongPressDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(42.dp)) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = stringResource(R.string.history_previous_day),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(PillShape)
                .combinedClickable(
                    onClick = { if (!state.isToday) onJumpToToday() },
                    onLongClick = onLongPressDate
                ),
            shape = PillShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Today,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(
                            R.string.history_date_label,
                            state.month,
                            state.day
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (state.isToday) {
                            stringResource(R.string.history_long_press_to_pick)
                        } else {
                            stringResource(R.string.history_tap_to_today)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
        }

        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            IconButton(onClick = onNext, modifier = Modifier.size(42.dp)) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = stringResource(R.string.history_next_day),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            IconButton(onClick = onRefresh, modifier = Modifier.size(42.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.history_refresh),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SourceBanner(
    state: TodayInHistoryUiState,
    onConfigureKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loadState = state.loadState
    if (loadState !is HistoryLoadState.Success) return

    val fromCache = loadState.fromCache
    val isPrimary = loadState.day.source == HistorySource.SHOW_API

    val icon: ImageVector
    val text: String
    val containerColor = when {
        isPrimary -> MaterialTheme.colorScheme.primaryContainer
        state.showApiKeyConfigured -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        isPrimary -> MaterialTheme.colorScheme.onPrimaryContainer
        state.showApiKeyConfigured -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    when {
        isPrimary && fromCache -> {
            icon = Icons.Rounded.HistoryEdu
            text = stringResource(R.string.history_source_cached, stringResource(R.string.history_source_showapi))
        }
        isPrimary -> {
            icon = Icons.Rounded.HistoryEdu
            text = stringResource(R.string.history_source_showapi)
        }
        state.showApiKeyConfigured -> {
            icon = Icons.Rounded.CloudOff
            text = stringResource(R.string.history_source_degraded)
        }
        else -> {
            icon = Icons.Rounded.CloudOff
            text = stringResource(R.string.history_source_free)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !state.showApiKeyConfigured, onClick = onConfigureKey),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.history_events_count, loadState.day.totalCount),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun HistoryDatePickerDialog(
    initialMonth: Int,
    initialDay: Int,
    onConfirm: (month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val monthOptions = remember { (1..12).map { it.toString() } }
    val dayOptions = remember(initialMonth) {
        (1..daysInMonth(initialMonth)).map { it.toString() }
    }
    var monthIndex by remember { mutableIntStateOf((initialMonth - 1).coerceIn(0, 11)) }
    var dayIndex by remember {
        mutableIntStateOf((initialDay - 1).coerceIn(0, daysInMonth(initialMonth) - 1))
    }
    var monthExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(initialMonth) {
        val maxDay = daysInMonth(initialMonth)
        if (dayIndex > maxDay - 1) dayIndex = maxDay - 1
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.history_date_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateUnitSelector(
                    label = stringResource(R.string.history_date_picker_month),
                    options = monthOptions,
                    selectedIndex = monthIndex,
                    expanded = monthExpanded,
                    onExpandedChange = { monthExpanded = it },
                    onSelect = { monthIndex = it },
                    modifier = Modifier.weight(1f)
                )
                DateUnitSelector(
                    label = stringResource(R.string.history_date_picker_day),
                    options = dayOptions,
                    selectedIndex = dayIndex,
                    expanded = dayExpanded,
                    onExpandedChange = { dayExpanded = it },
                    onSelect = { dayIndex = it },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(monthIndex + 1, dayIndex + 1) },
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.history_date_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
        shape = ToolCardShape
    )
}

@Composable
private fun DateUnitSelector(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onExpandedChange(true) },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = options.getOrElse(selectedIndex) { "" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            options.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(index)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

private fun daysInMonth(month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    else -> 29
}

@Composable
private fun ShowApiKeyDialog(
    configured: Boolean,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    onOpenConsole: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.history_api_key_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.history_api_key_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenConsole)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.history_api_key_get),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.history_api_key_label)) },
                    placeholder = { Text(stringResource(R.string.history_api_key_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )

                Text(
                    text = stringResource(
                        if (configured) R.string.history_api_key_status_on
                        else R.string.history_api_key_status_off
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (configured) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(keyInput) },
                shape = PillShape,
                enabled = keyInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.history_api_key_save))
            }
        },
        dismissButton = {
            Row {
                if (configured) {
                    TextButton(onClick = onClear) {
                        Text(
                            text = stringResource(R.string.history_api_key_clear),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        },
        shape = ToolCardShape
    )
}

private fun openWebPage(context: android.content.Context, url: String) {
    try {
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(context, Uri.parse(url))
    } catch (e: Exception) {
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
