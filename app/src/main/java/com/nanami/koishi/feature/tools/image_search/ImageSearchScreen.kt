package com.nanami.koishi.feature.tools.image_search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.PillShape
import com.nanami.koishi.core.designsystem.ToolCardShape
import com.nanami.koishi.core.image.crop.rememberCropImageLauncher
import com.nanami.koishi.feature.tools.image_search.model.EngineSearchState
import com.nanami.koishi.feature.tools.image_search.model.EngineSearchStatus
import com.nanami.koishi.feature.tools.image_search.model.SearchEngineEnum
import com.nanami.koishi.feature.tools.image_search.model.SearchResultItem

@Composable
fun ImageSearchRoute(
    viewModel: ImageSearchViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    ImageSearchScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImageSearchScreen(
    state: ImageSearchUiState,
    onEvent: (ImageSearchUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 接入升级后的自由全图裁剪启动器 (默认不裁剪全选，四周自由拉伸)
    val cropLauncher = rememberCropImageLauncher { uri ->
        onEvent(ImageSearchUiEvent.OnImageSelected(uri))
    }

    // 在 composable 作用域内解析文案，避免在 LaunchedEffect 中调用 stringResource
    val userMessage = state.userMessageRes?.let { msgRes ->
        if (state.userMessageArgs.isNotEmpty()) {
            stringResource(msgRes, *state.userMessageArgs.toTypedArray())
        } else {
            stringResource(msgRes)
        }
    }

    LaunchedEffect(state.userMessageRes, userMessage) {
        if (userMessage != null) {
            Toast.makeText(context, userMessage, Toast.LENGTH_SHORT).show()
            onEvent(ImageSearchUiEvent.OnDismissMessage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.image_search_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(ImageSearchUiEvent.OnShowApiKeyDialog(true)) }) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = stringResource(R.string.image_search_saucenao_api_key)
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 第一步：图片选择与预览卡片
            ImagePreviewCard(
                state = state,
                onPickImage = {
                    cropLauncher.launch(isSquare = false, target = "image_search")
                },
                onClearImage = { onEvent(ImageSearchUiEvent.OnClearImage) }
            )

            // 搜索源多选配置
            SearchEnginesCard(
                state = state,
                onToggleEngine = { onEvent(ImageSearchUiEvent.OnToggleEngine(it)) },
                onConfigureKey = { onEvent(ImageSearchUiEvent.OnShowApiKeyDialog(true)) }
            )

            // 开始以图搜图按钮
            Button(
                onClick = { onEvent(ImageSearchUiEvent.OnStartSearch) },
                enabled = state.canSearch,
                shape = PillShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (state.isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.image_search_searching),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.ImageSearch,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.image_search_btn_search),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // 第三步：多源搜索结果异步并发与折叠/展开展示区
            AnimatedVisibility(visible = state.engineStates.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (engine in state.selectedEngines) {
                        val engineState = state.engineStates[engine]
                        if (engineState != null) {
                            val isCollapsed = state.collapsedEngines.contains(engine)
                            EngineResultSection(
                                engineState = engineState,
                                isCollapsed = isCollapsed,
                                onToggleCollapse = { onEvent(ImageSearchUiEvent.OnToggleEngineCollapse(engine)) },
                                onOpenUrl = { url -> openWebUrl(context, url) },
                                onSearchGoogleLens = {
                                    launchGoogleLens(context, state.selectedImageUri)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // SauceNAO API Key 配置弹窗
    if (state.showApiKeyDialog) {
        ApiKeyConfigDialog(
            currentKey = state.sauceNaoApiKey,
            onDismiss = { onEvent(ImageSearchUiEvent.OnShowApiKeyDialog(false)) },
            onSave = { onEvent(ImageSearchUiEvent.OnSaveSauceNaoApiKey(it)) },
            onGetKey = {
                openWebUrl(context, "https://saucenao.com/user.php?page=search-api")
            }
        )
    }
}

@Composable
private fun ImagePreviewCard(
    state: ImageSearchUiState,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit
) {
    Card(
        shape = ToolCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        val bitmap = state.previewBitmap
        if (bitmap != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.preview_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPickImage,
                        shape = PillShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.image_search_change_image))
                    }

                    TextButton(
                        onClick = onClearImage,
                        shape = PillShape
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.image_search_clear_image))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPickImage)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.image_search_empty_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.image_search_empty_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onPickImage,
                    shape = PillShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.image_search_select_image))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchEnginesCard(
    state: ImageSearchUiState,
    onToggleEngine: (SearchEngineEnum) -> Unit,
    onConfigureKey: () -> Unit
) {
    Card(
        shape = ToolCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.image_search_engines_label),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (state.selectedEngines.contains(SearchEngineEnum.SAUCENAO)) {
                    val hasKey = state.sauceNaoApiKey.isNotBlank()
                    AssistChip(
                        onClick = onConfigureKey,
                        shape = PillShape,
                        label = {
                            Text(
                                text = stringResource(
                                    if (hasKey) R.string.image_search_saucenao_key_configured
                                    else R.string.image_search_saucenao_key_not_configured
                                ),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (hasKey) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = if (hasKey) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            leadingIconContentColor = if (hasKey) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (engine in SearchEngineEnum.entries) {
                    val isSelected = state.selectedEngines.contains(engine)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleEngine(engine) },
                        shape = PillShape,
                        label = {
                            Text(stringResource(engine.displayNameRes))
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EngineResultSection(
    engineState: EngineSearchState,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onSearchGoogleLens: () -> Unit
) {
    Card(
        shape = ToolCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 引擎标题栏：支持点击折叠/展开，并去除了右侧多余说明小字
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleCollapse)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(engineState.engine.displayNameRes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    if (engineState.results.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = PillShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${engineState.results.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                        contentDescription = stringResource(
                            if (isCollapsed) R.string.image_search_expand else R.string.image_search_collapse
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = !isCollapsed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (engineState.status) {
                        EngineSearchStatus.IDLE -> {
                            Text(
                                text = stringResource(R.string.image_search_engine_status_idle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        EngineSearchStatus.SEARCHING -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.image_search_engine_status_searching),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        EngineSearchStatus.SUCCESS -> {
                            if (engineState.engine == SearchEngineEnum.GOOGLE_LENS) {
                                GoogleLensActionCard(
                                    onSearchLens = onSearchGoogleLens
                                )
                            } else if (engineState.results.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.image_search_engine_status_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    for (item in engineState.results) {
                                        SearchResultCard(
                                            item = item,
                                            onOpenUrl = onOpenUrl
                                        )
                                    }
                                }
                            }
                        }
                        EngineSearchStatus.EMPTY -> {
                            Text(
                                text = stringResource(R.string.image_search_engine_status_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        EngineSearchStatus.ERROR -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val errorMsg = engineState.errorMessageRes?.let { stringResource(it) }
                                    ?: engineState.errorMessage
                                    ?: stringResource(R.string.image_search_error_network)
                                Text(
                                    text = stringResource(R.string.image_search_engine_status_error, errorMsg),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                engineState.fallbackUrl?.let { url ->
                                    OutlinedButton(
                                        onClick = { onOpenUrl(url) },
                                        shape = PillShape
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.image_search_fallback_web))
                                    }
                                }
                            }
                        }
                        EngineSearchStatus.FALLBACK_REQUIRED -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.WarningAmber,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val fallbackText = engineState.errorMessageRes?.let { stringResource(it) }
                                            ?: engineState.errorMessage
                                            ?: stringResource(R.string.image_search_engine_status_fallback)
                                        Text(
                                            text = fallbackText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }

                                    engineState.fallbackUrl?.let { url ->
                                        Button(
                                            onClick = { onOpenUrl(url) },
                                            shape = PillShape,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.image_search_fallback_web))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: SearchResultItem,
    onOpenUrl: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 缩略图 (Coil 异步加载)
            if (!item.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.thumbnailUrl)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 相似度标签与标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayTitle = item.title.ifBlank {
                        when (item.engine) {
                            SearchEngineEnum.TRACE_MOE -> stringResource(R.string.image_search_unknown_anime)
                            SearchEngineEnum.ASCII2D -> stringResource(R.string.image_search_unknown_work)
                            else -> stringResource(R.string.image_search_unknown_title)
                        }
                    }
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    item.similarity?.let { sim ->
                        Spacer(modifier = Modifier.width(6.dp))
                        SimilarityBadge(similarity = sim)
                    }
                }

                // 作者
                if (!item.author.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.image_search_author, item.author),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 额外信息 (如番剧集数时间轴、Pixiv ID 等，使用 StringRes 本地化)
                for (extra in item.extraInfo) {
                    Text(
                        text = "${stringResource(extra.first)}: ${extra.second}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 来源链接按钮
                if (!item.sourceUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    TextButton(
                        onClick = { onOpenUrl(item.sourceUrl) },
                        shape = PillShape,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.engine == SearchEngineEnum.TRACE_MOE) {
                                stringResource(R.string.image_search_open_anilist)
                            } else {
                                stringResource(R.string.image_search_open_source)
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimilarityBadge(similarity: Float) {
    val (bgColor, textColor) = when {
        similarity >= 85f -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        similarity >= 65f -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = PillShape,
        color = bgColor
    ) {
        Text(
            text = String.format(java.util.Locale.US, "%.1f%%", similarity),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun GoogleLensActionCard(
    onSearchLens: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSearchLens,
                shape = PillShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.image_search_google_lens_action))
            }
        }
    }
}

@Composable
private fun ApiKeyConfigDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onGetKey: () -> Unit
) {
    var keyInput by remember { mutableStateOf(currentKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.image_search_api_key_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.image_search_api_key_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text(stringResource(R.string.image_search_saucenao_api_key)) },
                    placeholder = { Text(stringResource(R.string.image_search_api_key_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
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
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = onGetKey,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.image_search_api_key_get))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(keyInput) },
                shape = PillShape,
                enabled = keyInput.isNotBlank()
            ) {
                Text(stringResource(R.string.image_search_api_key_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = PillShape
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

/**
 * 唤起系统 Google 应用或 Google Lens 应用直接以图搜图
 */
private fun launchGoogleLens(context: Context, imageUri: Uri?) {
    if (imageUri != null) {
        val shareUri = if (imageUri.scheme == "file") {
            try {
                val file = java.io.File(imageUri.path ?: "")
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                imageUri
            }
        } else {
            imageUri
        }

        // 1. 尝试直接拉起独立 Google Lens 应用 (com.google.ar.lens)
        val lensIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            setPackage("com.google.ar.lens")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (lensIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(lensIntent)
            return
        }

        // 2. 尝试拉起 Google 主应用内置 Lens (com.google.android.googlequicksearchbox)
        val googleAppIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            setPackage("com.google.android.googlequicksearchbox")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (googleAppIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(googleAppIntent)
            return
        }

        // 3. 尝试通用分享选择器 (允许用户选择 Lens 或其他搜图应用)
        try {
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                context.getString(R.string.image_search_google_lens_action)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            return
        } catch (_: Exception) {}
    }

    // 4. 网页版降级唤起
    openWebUrl(context, "https://lens.google.com/")
}

/**
 * 使用 Custom Tabs 打开链接，如不支持则唤起系统浏览器
 */
private fun openWebUrl(context: Context, url: String) {
    try {
        val uri = Uri.parse(url)
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, uri)
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.image_search_error_browser), Toast.LENGTH_SHORT).show()
        }
    }
}
