package com.nanami.koishi.feature.tools.prism_tank

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.PillShape
import com.nanami.koishi.core.designsystem.ToolCardShape
import com.nanami.koishi.core.designsystem.adaptive.AdaptiveContentWidth
import com.nanami.koishi.core.image.picker.ImageSourceCard
import com.nanami.koishi.core.image.preview.ImagePreviewDialog
import com.nanami.koishi.core.util.AlbumFolders
import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankFill
import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankParams
import com.nanami.koishi.feature.tools.prism_tank.engine.PrismTankRevealConfig
import java.util.Locale
import kotlin.math.roundToInt
@Composable
fun PrismTankRoute(
    viewModel: PrismTankViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(PrismTankUiEvent.OnDismissMessage)
        }
    }

    PrismTankScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrismTankScreen(
    uiState: PrismTankUiState,
    onEvent: (PrismTankUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var previewImages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var previewIndex by remember { mutableIntStateOf(0) }

    val innerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onEvent(PrismTankUiEvent.OnInnerImageSelected(it)) }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onEvent(PrismTankUiEvent.OnCoverImageSelected(it)) }
    }
    val sourcePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onEvent(PrismTankUiEvent.OnSourceImageSelected(it)) }
    }

    if (uiState.isHelpDialogOpen) {
        PrismTankHelpDialog(onDismiss = { onEvent(PrismTankUiEvent.OnToggleHelpDialog(false)) })
    }

    if (previewImages.isNotEmpty()) {
        ImagePreviewDialog(
            images = previewImages,
            initialIndex = previewIndex,
            albumFolder = AlbumFolders.PRISM_TANK,
            onDismissRequest = { previewImages = emptyList() }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.prism_tank_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
                    if (uiState.panel == PrismTankPanel.CREATE &&
                        (uiState.innerImage != null || uiState.coverImage != null)
                    ) {
                        IconButton(onClick = { onEvent(PrismTankUiEvent.OnSwapImages) }) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = stringResource(R.string.prism_tank_swap_images),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    TextButton(onClick = { onEvent(PrismTankUiEvent.OnToggleHelpDialog(true)) }) {
                        Text(
                            text = stringResource(R.string.help),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        AdaptiveContentWidth(maxWidth = 1200.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PrimaryTabRow(
                    selectedTabIndex = if (uiState.panel == PrismTankPanel.CREATE) 0 else 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PillShape),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = uiState.panel == PrismTankPanel.CREATE,
                        onClick = { onEvent(PrismTankUiEvent.OnPanelChanged(PrismTankPanel.CREATE)) },
                        text = {
                            Text(
                                text = stringResource(R.string.prism_tank_tab_create),
                                fontWeight = if (uiState.panel == PrismTankPanel.CREATE) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = uiState.panel == PrismTankPanel.REVEAL,
                        onClick = { onEvent(PrismTankUiEvent.OnPanelChanged(PrismTankPanel.REVEAL)) },
                        text = {
                            Text(
                                text = stringResource(R.string.prism_tank_tab_reveal),
                                fontWeight = if (uiState.panel == PrismTankPanel.REVEAL) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                when (uiState.panel) {
                    PrismTankPanel.CREATE -> CreatePanel(
                        uiState = uiState,
                        onEvent = onEvent,
                        onPickInner = {
                            innerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onPickCover = {
                            coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onPreview = {
                            previewImages = listOfNotNull(uiState.resultBitmap)
                            previewIndex = 0
                        }
                    )

                    PrismTankPanel.REVEAL -> RevealPanel(
                        uiState = uiState,
                        onEvent = onEvent,
                        onPickSource = {
                            sourcePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onPreview = { index ->
                            previewImages = listOfNotNull(uiState.revealedInner, uiState.revealedCover)
                            previewIndex = index
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun CreatePanel(
    uiState: PrismTankUiState,
    onEvent: (PrismTankUiEvent) -> Unit,
    onPickInner: () -> Unit,
    onPickCover: () -> Unit,
    onPreview: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ImageSourceCard(
                title = stringResource(R.string.prism_tank_inner_image),
                subTitle = stringResource(R.string.prism_tank_inner_image_desc),
                badgeColor = MaterialTheme.colorScheme.tertiary,
                bitmap = uiState.innerImage?.bitmap,
                placeholderText = stringResource(R.string.prism_tank_tap_to_pick),
                onPick = onPickInner,
                onClear = { onEvent(PrismTankUiEvent.OnClearInnerImage) },
                modifier = Modifier.weight(1f)
            )
            ImageSourceCard(
                title = stringResource(R.string.prism_tank_cover_image),
                subTitle = stringResource(R.string.prism_tank_cover_image_desc),
                badgeColor = MaterialTheme.colorScheme.primary,
                bitmap = uiState.coverImage?.bitmap,
                placeholderText = stringResource(R.string.prism_tank_tap_to_pick),
                onPick = onPickCover,
                onClear = { onEvent(PrismTankUiEvent.OnClearCoverImage) },
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ToolCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.prism_tank_params),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onEvent(PrismTankUiEvent.OnResetParams) }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.prism_tank_reset_params))
                    }
                }

                IntSlider(
                    label = stringResource(R.string.prism_tank_inner_threshold),
                    value = uiState.params.innerThreshold,
                    range = PrismTankParams.MIN_THRESHOLD..PrismTankParams.MAX_THRESHOLD,
                    onValueChange = { onEvent(PrismTankUiEvent.OnInnerThresholdChanged(it)) }
                )
                IntSlider(
                    label = stringResource(R.string.prism_tank_cover_threshold),
                    value = uiState.params.coverThreshold,
                    range = PrismTankParams.MIN_THRESHOLD..PrismTankParams.MAX_THRESHOLD,
                    onValueChange = { onEvent(PrismTankUiEvent.OnCoverThresholdChanged(it)) }
                )
                IntSlider(
                    label = stringResource(R.string.prism_tank_gap),
                    value = uiState.params.gap,
                    range = PrismTankParams.MIN_GAP..PrismTankParams.MAX_GAP,
                    onValueChange = { onEvent(PrismTankUiEvent.OnGapChanged(it)) }
                )
                IntSlider(
                    label = stringResource(R.string.prism_tank_slope),
                    value = uiState.params.slope,
                    range = PrismTankParams.MIN_SLOPE..PrismTankParams.MAX_SLOPE,
                    onValueChange = { onEvent(PrismTankUiEvent.OnSlopeChanged(it)) }
                )

                SwitchRow(
                    label = stringResource(R.string.prism_tank_inner_gray),
                    checked = uiState.params.isInnerGray,
                    onCheckedChange = { onEvent(PrismTankUiEvent.OnInnerGrayToggled(it)) }
                )
                SwitchRow(
                    label = stringResource(R.string.prism_tank_cover_gray),
                    checked = uiState.params.isCoverGray,
                    onCheckedChange = { onEvent(PrismTankUiEvent.OnCoverGrayToggled(it)) }
                )
                SwitchRow(
                    label = stringResource(R.string.prism_tank_reverse),
                    checked = uiState.params.isReverse,
                    onCheckedChange = { onEvent(PrismTankUiEvent.OnReverseToggled(it)) }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ToolCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.prism_tank_preview_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(enabled = uiState.resultBitmap != null) { onPreview() },
                    contentAlignment = Alignment.Center
                ) {
                    val result = uiState.resultBitmap
                    if (result != null) {
                        Image(
                            bitmap = result.asImageBitmap(),
                            contentDescription = stringResource(R.string.prism_tank_preview_title),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            colorFilter = exposureFilter(uiState.exposureGain)
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.hasBothImages) {
                                    stringResource(R.string.prism_tank_hint_ready)
                                } else {
                                    stringResource(R.string.prism_tank_hint_pick_both)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (uiState.isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                FloatSlider(
                    label = stringResource(R.string.prism_tank_exposure),
                    value = uiState.exposureGain,
                    range = PrismTankUiState.MIN_EXPOSURE_GAIN..PrismTankUiState.MAX_EXPOSURE_GAIN,
                    display = { String.format(Locale.US, "%.1fx", it) },
                    onValueChange = { onEvent(PrismTankUiEvent.OnExposureGainChanged(it)) }
                )
                Text(
                    text = stringResource(R.string.prism_tank_exposure_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onEvent(PrismTankUiEvent.OnGenerateTank) },
                enabled = uiState.hasBothImages && !uiState.isProcessing,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = PillShape
            ) {
                Icon(imageVector = Icons.Rounded.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.prism_tank_generate), fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { onEvent(PrismTankUiEvent.OnSaveTank) },
                enabled = uiState.canSave,
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = Icons.Rounded.SaveAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.prism_tank_save_lossless), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
@Composable
private fun RevealPanel(
    uiState: PrismTankUiState,
    onEvent: (PrismTankUiEvent) -> Unit,
    onPickSource: () -> Unit,
    onPreview: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ImageSourceCard(
            title = stringResource(R.string.prism_tank_source_image),
            subTitle = stringResource(R.string.prism_tank_source_desc),
            badgeColor = MaterialTheme.colorScheme.primary,
            bitmap = uiState.sourceImage?.bitmap,
            placeholderText = stringResource(R.string.prism_tank_tap_to_pick),
            onPick = onPickSource,
            onClear = { onEvent(PrismTankUiEvent.OnClearSourceImage) },
            modifier = Modifier.fillMaxWidth(fraction = 0.5f)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ToolCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.prism_tank_reveal_band),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onEvent(PrismTankUiEvent.OnResetRevealConfig) }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.prism_tank_reset_params))
                    }
                }

                IntSlider(
                    label = stringResource(R.string.prism_tank_reveal_lower),
                    value = uiState.revealConfig.lowerThreshold,
                    range = PrismTankRevealConfig.MIN_LOWER_THRESHOLD..PrismTankRevealConfig.MAX_THRESHOLD,
                    onValueChange = { onEvent(PrismTankUiEvent.OnRevealLowerChanged(it)) }
                )
                IntSlider(
                    label = stringResource(R.string.prism_tank_reveal_upper),
                    value = uiState.revealConfig.higherThreshold,
                    range = PrismTankRevealConfig.MIN_LOWER_THRESHOLD..PrismTankRevealConfig.MAX_THRESHOLD,
                    onValueChange = { onEvent(PrismTankUiEvent.OnRevealUpperChanged(it)) }
                )
                FloatSlider(
                    label = stringResource(R.string.prism_tank_reveal_contrast),
                    value = uiState.revealConfig.contrast,
                    range = PrismTankRevealConfig.MIN_CONTRAST..PrismTankRevealConfig.MAX_CONTRAST,
                    display = { String.format(Locale.US, "%.2f", it) },
                    onValueChange = { onEvent(PrismTankUiEvent.OnRevealContrastChanged(it)) }
                )

                Text(
                    text = stringResource(R.string.prism_tank_reveal_fill),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FillChip(
                        label = stringResource(R.string.prism_tank_fill_black),
                        selected = uiState.revealConfig.fill == PrismTankFill.BLACK,
                        onClick = { onEvent(PrismTankUiEvent.OnRevealFillChanged(PrismTankFill.BLACK)) },
                        modifier = Modifier.weight(1f)
                    )
                    FillChip(
                        label = stringResource(R.string.prism_tank_fill_white),
                        selected = uiState.revealConfig.fill == PrismTankFill.WHITE,
                        onClick = { onEvent(PrismTankUiEvent.OnRevealFillChanged(PrismTankFill.WHITE)) },
                        modifier = Modifier.weight(1f)
                    )
                    FillChip(
                        label = stringResource(R.string.prism_tank_fill_transparent),
                        selected = uiState.revealConfig.fill == PrismTankFill.TRANSPARENT,
                        onClick = { onEvent(PrismTankUiEvent.OnRevealFillChanged(PrismTankFill.TRANSPARENT)) },
                        modifier = Modifier.weight(1f)
                    )
                    FillChip(
                        label = stringResource(R.string.prism_tank_fill_neighbor),
                        selected = uiState.revealConfig.fill == PrismTankFill.NEIGHBOR_AVG,
                        onClick = { onEvent(PrismTankUiEvent.OnRevealFillChanged(PrismTankFill.NEIGHBOR_AVG)) },
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ToolCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.prism_tank_preview_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ResultThumb(
                        title = stringResource(R.string.prism_tank_result_inner),
                        bitmap = uiState.revealedInner,
                        onClick = { onPreview(0) },
                        modifier = Modifier.weight(1f)
                    )
                    ResultThumb(
                        title = stringResource(R.string.prism_tank_result_cover),
                        bitmap = uiState.revealedCover,
                        onClick = { onPreview(1) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (uiState.revealedInner == null && uiState.revealedCover == null) {
                    Text(
                        text = stringResource(R.string.prism_tank_hint_pick_source),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onEvent(PrismTankUiEvent.OnSaveRevealedInner) },
                enabled = uiState.revealedInner != null && !uiState.isSaving,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = PillShape
            ) {
                Icon(imageVector = Icons.Rounded.SaveAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.prism_tank_save_inner), fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { onEvent(PrismTankUiEvent.OnSaveRevealedCover) },
                enabled = uiState.revealedCover != null && !uiState.isSaving,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = Icons.Rounded.SaveAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.prism_tank_save_cover), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
@Composable
private fun ResultThumb(
    title: String,
    bitmap: Bitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(enabled = bitmap != null) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.bodySmall) },
        modifier = modifier
    )
}

@Composable
private fun IntSlider(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0)
        )
    }
}

@Composable
private fun FloatSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = display(value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

@Composable
private fun PrismTankHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.prism_tank_help_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(text = stringResource(R.string.prism_tank_help_p1), style = MaterialTheme.typography.bodyMedium)
                Text(text = stringResource(R.string.prism_tank_help_p2), style = MaterialTheme.typography.bodyMedium)
                Text(text = stringResource(R.string.prism_tank_help_p3), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.help))
            }
        }
    )
}

private fun exposureFilter(gain: Float): ColorFilter {
    val factor = gain.coerceAtLeast(0f)
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                factor, 0f, 0f, 0f, 0f,
                0f, factor, 0f, 0f, 0f,
                0f, 0f, factor, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
}