package com.nanami.koishi.feature.tools.color_picker

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.color_picker.components.ColorDetailsDialog
import com.nanami.koishi.feature.tools.color_picker.components.ColorFavoritesView
import com.nanami.koishi.feature.tools.color_picker.components.ColorLoupe
import com.nanami.koishi.feature.tools.color_picker.components.ColorPreviewCard
import com.nanami.koishi.feature.tools.color_picker.components.MagnificationSheet
import com.nanami.koishi.feature.tools.color_picker.components.RepeatingIconButton
import java.util.Locale

@Composable
fun ColorPickerRoute(
    viewModel: ColorPickerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val userMessage = uiState.userMessageRes?.let { res ->
        uiState.userMessageArg?.let { stringResource(res, it) } ?: stringResource(res)
    }

    LaunchedEffect(uiState.userMessageRes, userMessage) {
        if (userMessage != null) {
            Toast.makeText(context, userMessage, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(ColorPickerUiEvent.OnDismissMessage)
        }
    }

    ColorPickerScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerScreen(
    state: ColorPickerUiState,
    onEvent: (ColorPickerUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onEvent(ColorPickerUiEvent.OnImageSelected(it)) }
    }

    val launchPhotoPicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    if (state.showMagnificationSheet) {
        MagnificationSheet(
            magnification = state.magnification,
            onMagnificationChange = { onEvent(ColorPickerUiEvent.OnMagnificationChange(it)) },
            onDismissRequest = { onEvent(ColorPickerUiEvent.OnShowMagnificationSheet(false)) }
        )
    }

    state.selectedFavoriteForDetails?.let { favorite ->
        ColorDetailsDialog(
            favorite = favorite,
            onDismissRequest = { onEvent(ColorPickerUiEvent.OnSelectFavoriteForDetails(null)) },
            onCopyText = { text, msgRes ->
                onEvent(ColorPickerUiEvent.OnCopyText(text, msgRes))
            }
        )
    }

    if (state.showClearFavoritesDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(ColorPickerUiEvent.OnDismissClearFavoritesDialog) },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = stringResource(R.string.color_picker_clear_favorites),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.color_picker_clear_favorites_confirm),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(ColorPickerUiEvent.OnConfirmClearFavorites) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.color_picker_confirm_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ColorPickerUiEvent.OnDismissClearFavoritesDialog) }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.color_picker_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
                    if (state.currentTab == ColorPickerTab.PICKER) {
                        IconButton(onClick = { onEvent(ColorPickerUiEvent.OnShowMagnificationSheet(true)) }) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomIn,
                                contentDescription = stringResource(R.string.color_picker_zoom_title)
                            )
                        }
                        if (state.bitmap != null) {
                            IconButton(onClick = { onEvent(ColorPickerUiEvent.OnClearImage) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = stringResource(R.string.color_picker_clear_image),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        if (state.favorites.isNotEmpty()) {
                            IconButton(onClick = { onEvent(ColorPickerUiEvent.OnShowClearFavoritesDialog) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = stringResource(R.string.color_picker_clear_favorites),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
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
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = state.currentTab == ColorPickerTab.PICKER,
                    onClick = { onEvent(ColorPickerUiEvent.OnSelectTab(ColorPickerTab.PICKER)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Colorize,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.color_picker_tab_picker),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )

                SegmentedButton(
                    selected = state.currentTab == ColorPickerTab.FAVORITES,
                    onClick = { onEvent(ColorPickerUiEvent.OnSelectTab(ColorPickerTab.FAVORITES)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.color_picker_tab_favorites),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }

            if (state.currentTab == ColorPickerTab.PICKER) {
                ColorPreviewCard(
                    color = state.selectedColor,
                    hexString = state.hexString,
                    rgbString = state.rgbString,
                    coordinateString = state.pixelCoordinateString,
                    isFavorite = state.isCurrentColorFavorite,
                    onToggleFavorite = { onEvent(ColorPickerUiEvent.OnToggleFavoriteCurrentColor) },
                    onCopyHex = {
                        onEvent(
                            ColorPickerUiEvent.OnCopyText(
                                state.hexString,
                                R.string.color_picker_copied_hex
                            )
                        )
                    },
                    onCopyRgb = {
                        onEvent(
                            ColorPickerUiEvent.OnCopyText(
                                "rgb(${state.rgbString})",
                                R.string.color_picker_copied_rgb
                            )
                        )
                    }
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val containerWidth = constraints.maxWidth.toFloat()
                    val containerHeight = constraints.maxHeight.toFloat()
                    val bitmap = state.bitmap

                    if (bitmap == null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable(onClick = launchPhotoPicker),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(76.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = stringResource(R.string.color_picker_upload_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = stringResource(R.string.color_picker_upload_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = launchPhotoPicker,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.color_picker_select_image))
                                }
                            }
                        }
                    } else {
                        val scale = minOf(
                            containerWidth / bitmap.width.toFloat(),
                            containerHeight / bitmap.height.toFloat()
                        )
                        val displayedWidth = bitmap.width.toFloat() * scale
                        val displayedHeight = bitmap.height.toFloat() * scale
                        val offsetX = (containerWidth - displayedWidth) / 2f
                        val offsetY = (containerHeight - displayedHeight) / 2f

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141414))
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(bitmap, displayedWidth, displayedHeight, offsetX, offsetY) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val updatePos: (Offset) -> Unit = { pos ->
                                                val relX = (pos.x - offsetX).coerceIn(0f, displayedWidth - 0.001f)
                                                val relY = (pos.y - offsetY).coerceIn(0f, displayedHeight - 0.001f)
                                                val px = (relX / displayedWidth * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                                                val py = (relY / displayedHeight * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                                                onEvent(ColorPickerUiEvent.OnCursorMove(px, py))
                                            }
                                            updatePos(down.position)
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull() ?: break
                                                if (change.pressed) {
                                                    updatePos(change.position)
                                                    change.consume()
                                                } else {
                                                    break
                                                }
                                            }
                                        }
                                    }
                            )

                            ColorLoupe(
                                bitmap = bitmap,
                                cursorX = state.cursorX,
                                cursorY = state.cursorY,
                                selectedColor = state.selectedColor,
                                magnification = state.magnification,
                                displayedWidth = displayedWidth,
                                displayedHeight = displayedHeight,
                                offsetX = offsetX,
                                offsetY = offsetY
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = launchPhotoPicker,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = stringResource(R.string.color_picker_change_image),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RepeatingIconButton(
                                onClick = { onEvent(ColorPickerUiEvent.OnNudgeCursor(Direction.UP)) },
                                enabled = state.bitmap != null
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = stringResource(R.string.color_picker_nudge_up),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            RepeatingIconButton(
                                onClick = { onEvent(ColorPickerUiEvent.OnNudgeCursor(Direction.DOWN)) },
                                enabled = state.bitmap != null
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.color_picker_nudge_down),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            RepeatingIconButton(
                                onClick = { onEvent(ColorPickerUiEvent.OnNudgeCursor(Direction.LEFT)) },
                                enabled = state.bitmap != null
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = stringResource(R.string.color_picker_nudge_left),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            RepeatingIconButton(
                                onClick = { onEvent(ColorPickerUiEvent.OnNudgeCursor(Direction.RIGHT)) },
                                enabled = state.bitmap != null
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.color_picker_nudge_right),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            } else {
                ColorFavoritesView(
                    favorites = state.favorites,
                    onSelectDetails = { onEvent(ColorPickerUiEvent.OnSelectFavoriteForDetails(it)) },
                    onDeleteFavorite = { onEvent(ColorPickerUiEvent.OnDeleteFavorite(it)) },
                    onCopyText = { text, msgRes ->
                        onEvent(ColorPickerUiEvent.OnCopyText(text, msgRes))
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                )
            }
        }
    }
}
