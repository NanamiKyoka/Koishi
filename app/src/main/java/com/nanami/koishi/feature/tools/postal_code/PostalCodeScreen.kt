package com.nanami.koishi.feature.tools.postal_code

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.PillShape
import com.nanami.koishi.core.designsystem.ToolCardShape
import com.nanami.koishi.feature.tools.postal_code.components.PostalEmptyState
import com.nanami.koishi.feature.tools.postal_code.components.PostalHistorySection
import com.nanami.koishi.feature.tools.postal_code.components.PostalResultCard
import com.nanami.koishi.feature.tools.postal_code.components.PostalResultHeader
import com.nanami.koishi.feature.tools.postal_code.engine.PostalCountries
import com.nanami.koishi.feature.tools.postal_code.engine.PostalDirection
import com.nanami.koishi.feature.tools.postal_code.engine.PostalSource

@Composable
fun PostalCodeRoute(
    viewModel: PostalCodeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PostalCodeScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostalCodeScreen(
    uiState: PostalCodeUiState,
    onEvent: (PostalCodeUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessageRes) {
        val res = uiState.userMessageRes
        if (res != null) {
            snackbarHostState.showSnackbar(context.getString(res))
            onEvent(PostalCodeUiEvent.OnDismissMessage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.postal_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        (uiState.datasetState as? PostalDatasetState.Ready)?.let { ready ->
                            Text(
                                text = stringResource(R.string.postal_dataset_ready_label, ready.areaCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                    val datasetReady = uiState.datasetState is PostalDatasetState.Ready
                    if (datasetReady) {
                        IconButton(
                            onClick = { onEvent(PostalCodeUiEvent.OnClearDataset) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.postal_dataset_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { onEvent(PostalCodeUiEvent.OnRefresh) },
                        enabled = uiState.records.isNotEmpty() && !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.postal_refresh),
                            tint = if (uiState.records.isNotEmpty()) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.outline
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
            PostalControlPanel(uiState = uiState, onEvent = onEvent)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (uiState.requiresDatasetDownload) {
                PostalDatasetSection(
                    datasetState = uiState.datasetState,
                    onDownload = { onEvent(PostalCodeUiEvent.OnDownloadDataset) },
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                )
            } else {
                PostalContent(
                    uiState = uiState,
                    onEvent = onEvent,
                    onCopy = { copyToClipboard(context, it) }
                )
            }
        }
    }
}

@Composable
private fun PostalDatasetSection(
    datasetState: PostalDatasetState,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (datasetState) {
        is PostalDatasetState.Absent -> PostalEmptyState(
            title = stringResource(R.string.postal_dataset_title),
            description = stringResource(R.string.postal_dataset_desc),
            icon = Icons.Rounded.CloudDownload,
            modifier = modifier,
            action = {
                FilledTonalButton(onClick = onDownload, shape = PillShape) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.postal_dataset_download_action))
                }
            },
            footer = {
                Text(
                    text = stringResource(R.string.postal_dataset_size_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        is PostalDatasetState.Downloading -> Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                progress = { datasetState.progress / 100f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.postal_dataset_downloading, datasetState.progress),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.postal_dataset_size_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        is PostalDatasetState.Failed -> PostalEmptyState(
            title = stringResource(R.string.postal_dataset_failed_title),
            description = stringResource(datasetState.messageRes),
            icon = Icons.Rounded.ErrorOutline,
            modifier = modifier,
            action = {
                FilledTonalButton(onClick = onDownload, shape = PillShape) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.postal_retry))
                }
            }
        )

        is PostalDatasetState.Ready -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostalControlPanel(
    uiState: PostalCodeUiState,
    onEvent: (PostalCodeUiEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PostalDirection.entries.forEachIndexed { index, direction ->
                SegmentedButton(
                    selected = direction == uiState.direction,
                    onClick = { onEvent(PostalCodeUiEvent.OnDirectionChange(direction)) },
                    shape = SegmentedButtonDefaults.itemShape(index, PostalDirection.entries.size)
                ) {
                    Text(stringResource(direction.labelRes()))
                }
            }
        }

        if (uiState.direction == PostalDirection.CODE_TO_REGION) {
            Spacer(modifier = Modifier.height(8.dp))
            PostalCountrySelector(
                selectedCode = uiState.countryCode,
                onSelect = { onEvent(PostalCodeUiEvent.OnCountryChange(it)) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        PostalSearchField(
            value = uiState.queryInput,
            direction = uiState.direction,
            canSubmit = uiState.canSubmit,
            onValueChange = { onEvent(PostalCodeUiEvent.OnQueryInputChange(it)) },
            onSubmit = { onEvent(PostalCodeUiEvent.OnSubmit) }
        )
    }
}

@Composable
private fun PostalCountrySelector(
    selectedCode: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = PostalCountries.supported,
            key = { it.code }
        ) { country ->
            val isSelected = country.code.equals(selectedCode, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(country.code) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PostalCountryBadge(code = country.code)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(country.nameRes),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                shape = PillShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = null
            )
        }
    }
}

@Composable
private fun PostalCountryBadge(code: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = code.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun PostalSearchField(
    value: String,
    direction: PostalDirection,
    canSubmit: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(direction.hintRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (direction == PostalDirection.CODE_TO_REGION) {
                            KeyboardType.Ascii
                        } else {
                            KeyboardType.Text
                        },
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() })
                )
            }

            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = stringResource(R.string.clear_search_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        FilledTonalIconButton(
            onClick = onSubmit,
            enabled = canSubmit,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = stringResource(R.string.postal_search_action),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PostalContent(
    uiState: PostalCodeUiState,
    onEvent: (PostalCodeUiEvent) -> Unit,
    onCopy: (String) -> Unit
) {
    when (val loadState = uiState.loadState) {
        PostalLoadState.Idle -> PostalIdleContent(uiState = uiState, onEvent = onEvent)
        PostalLoadState.Loading -> PostalLoading()
        is PostalLoadState.Success -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                PostalResultHeader(
                    resultCount = loadState.records.size,
                    sourceLabel = stringResource(loadState.source.labelRes()),
                    fromCache = loadState.fromCache
                )
            }
            itemsIndexed(
                items = loadState.records,
                key = { index, record -> "$index-${record.postalCode}-${record.regionPath.joinToString()}" }
            ) { _, record ->
                PostalResultCard(
                    record = record,
                    onCopy = { onCopy(record.postalCode) }
                )
            }
        }

        PostalLoadState.Empty -> PostalEmptyState(
            title = stringResource(R.string.postal_empty_title),
            description = stringResource(R.string.postal_empty_desc),
            icon = Icons.Rounded.SearchOff,
            modifier = Modifier.navigationBarsPadding()
        )

        is PostalLoadState.Error -> PostalEmptyState(
            title = stringResource(R.string.postal_error_title),
            description = stringResource(loadState.messageRes),
            icon = Icons.Rounded.ErrorOutline,
            modifier = Modifier.navigationBarsPadding(),
            action = {
                FilledTonalButton(
                    onClick = { onEvent(PostalCodeUiEvent.OnSubmit) },
                    shape = PillShape
                ) {
                    Text(stringResource(R.string.postal_retry))
                }
            }
        )
    }
}

@Composable
private fun PostalIdleContent(
    uiState: PostalCodeUiState,
    onEvent: (PostalCodeUiEvent) -> Unit
) {
    if (uiState.history.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item(key = "history") {
                PostalHistorySection(
                    history = uiState.history,
                    onPick = { onEvent(PostalCodeUiEvent.OnHistoryPick(it)) },
                    onClearAll = { onEvent(PostalCodeUiEvent.OnClearHistory) }
                )
            }
            item(key = "guide") {
                PostalGuideCard(
                    direction = uiState.direction,
                    showAttribution = uiState.isDomestic
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            PostalGuideCard(
                direction = uiState.direction,
                showAttribution = uiState.isDomestic
            )
        }
    }
}

@Composable
private fun PostalGuideCard(direction: PostalDirection, showAttribution: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            shape = ToolCardShape,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.TravelExplore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(direction.guideRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showAttribution) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.postal_dataset_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun PostalLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.postal_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


private fun PostalDirection.labelRes(): Int = when (this) {
    PostalDirection.CODE_TO_REGION -> R.string.postal_direction_code_to_region
    PostalDirection.REGION_TO_CODE -> R.string.postal_direction_region_to_code
}

private fun PostalDirection.hintRes(): Int = when (this) {
    PostalDirection.CODE_TO_REGION -> R.string.postal_code_hint
    PostalDirection.REGION_TO_CODE -> R.string.postal_region_hint
}

private fun PostalDirection.guideRes(): Int = when (this) {
    PostalDirection.CODE_TO_REGION -> R.string.postal_code_guide
    PostalDirection.REGION_TO_CODE -> R.string.postal_region_guide
}

private fun PostalSource.labelRes(): Int = when (this) {
    PostalSource.OFFLINE_DATASET -> R.string.postal_source_offline
    PostalSource.ZIPPOPOTAM -> R.string.postal_source_zippopotam
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Postal Code", text))
}
