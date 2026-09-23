package com.nanami.koishi.feature.tools.image_obfuscation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.PillShape
import com.nanami.koishi.core.designsystem.ToolCardShape
import com.nanami.koishi.core.image.preview.ImagePreviewDialog
import com.nanami.koishi.feature.tools.image_obfuscation.engine.ObfuscationMode

@Composable
fun ImageObfuscationRoute(
    viewModel: ImageObfuscationViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ImageObfuscationScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageObfuscationScreen(
    uiState: ImageObfuscationUiState,
    onEvent: (ImageObfuscationUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPreview by remember { mutableStateOf(false) }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onEvent(ImageObfuscationUiEvent.OnImagesSelected(uris))
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onEvent(ImageObfuscationUiEvent.OnDismissMessage)
        }
    }

    if (uiState.isHelpDialogOpen) {
        HelpDialog(onDismiss = { onEvent(ImageObfuscationUiEvent.OnToggleHelpDialog(false)) })
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.image_obfuscation_title),
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
                    if (uiState.currentBitmap != null) {
                        IconButton(onClick = { onEvent(ImageObfuscationUiEvent.OnRotateCurrent) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.RotateRight,
                                contentDescription = stringResource(R.string.rotate_90)
                            )
                        }
                    }
                    TextButton(onClick = { onEvent(ImageObfuscationUiEvent.OnToggleHelpDialog(true)) }) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (uiState.totalCount > 0) {
                        stringResource(R.string.selected_images_count, uiState.totalCount)
                    } else {
                        stringResource(R.string.no_images_selected)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.imageWidth > 0 && uiState.imageHeight > 0) {
                    Text(
                        text = stringResource(R.string.resolution_format, uiState.imageWidth, uiState.imageHeight),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            ModeSelectorBar(
                currentMode = uiState.mode,
                onModeSelected = { onEvent(ImageObfuscationUiEvent.OnModeSelected(it)) }
            )

            AnimatedVisibility(visible = uiState.mode.requiresKey) {
                KeyInputField(
                    mode = uiState.mode,
                    key = uiState.key,
                    onKeyChanged = { onEvent(ImageObfuscationUiEvent.OnKeyChanged(it)) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(ToolCardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable {
                        if (uiState.currentBitmap != null) {
                            showPreview = true
                        } else {
                            multiplePhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val currentBitmap = uiState.currentBitmap
                if (currentBitmap != null) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.preview_image),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(PillShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.tap_to_pick_images),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.supported_algorithms_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uiState.isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.calculating),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.processingProgress > 0f) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { uiState.processingProgress },
                                    modifier = Modifier.width(160.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            ThumbnailRow(
                images = uiState.images,
                selectedIndex = uiState.selectedIndex,
                onSelectIndex = { onEvent(ImageObfuscationUiEvent.OnSelectImageIndex(it)) },
                onAddClick = {
                    multiplePhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ActionButtonsGrid(
                hasImage = uiState.totalCount > 0,
                isProcessing = uiState.isProcessing,
                onObfuscateCurrent = { onEvent(ImageObfuscationUiEvent.OnObfuscateCurrent) },
                onDeobfuscateCurrent = { onEvent(ImageObfuscationUiEvent.OnDeobfuscateCurrent) },
                onRemoveCurrent = { onEvent(ImageObfuscationUiEvent.OnRemoveCurrent) },
                onRestoreCurrent = { onEvent(ImageObfuscationUiEvent.OnRestoreCurrent) },
                onSaveCurrent = { onEvent(ImageObfuscationUiEvent.OnSaveCurrent) },
                onObfuscateAll = { onEvent(ImageObfuscationUiEvent.OnObfuscateAll) },
                onDeobfuscateAll = { onEvent(ImageObfuscationUiEvent.OnDeobfuscateAll) },
                onRemoveAll = { onEvent(ImageObfuscationUiEvent.OnRemoveAll) },
                onRestoreAll = { onEvent(ImageObfuscationUiEvent.OnRestoreAll) },
                onSaveAll = { onEvent(ImageObfuscationUiEvent.OnSaveAll) }
            )
        }
    }

    if (showPreview && uiState.currentBitmap != null) {
        val previewImages = if (uiState.images.isNotEmpty()) {
            uiState.images.map { it.bitmap }
        } else {
            listOfNotNull(uiState.currentBitmap)
        }
        val initialIdx = uiState.selectedIndex.coerceIn(0, (previewImages.size - 1).coerceAtLeast(0))

        ImagePreviewDialog(
            images = previewImages,
            initialIndex = initialIdx,
            title = stringResource(R.string.preview_image),
            onIndexChanged = { newIndex ->
                if (newIndex in uiState.images.indices && newIndex != uiState.selectedIndex) {
                    onEvent(ImageObfuscationUiEvent.OnSelectImageIndex(newIndex))
                }
            },
            onDismissRequest = { showPreview = false }
        )
    }
}

@Composable
private fun ModeSelectorBar(
    currentMode: ObfuscationMode,
    onModeSelected: (ObfuscationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "arrowRotation"
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.mode_prefix),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(currentMode.titleRes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = stringResource(R.string.select_mode),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            ObfuscationMode.entries.forEach { mode ->
                val isSelected = mode == currentMode
                DropdownMenuItem(
                    text = {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = stringResource(mode.titleRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(mode.descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else null,
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else Color.Transparent
                        ),
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun KeyInputField(
    mode: ObfuscationMode,
    key: String,
    onKeyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = key,
        onValueChange = onKeyChanged,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        placeholder = { Text(stringResource(mode.keyHintRes), style = MaterialTheme.typography.bodySmall) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        },
        singleLine = true,
        shape = PillShape,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (mode.isNumericKey) KeyboardType.Decimal else KeyboardType.Text
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    )
}

@Composable
private fun ThumbnailRow(
    images: List<ObfuscatedImageItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        itemsIndexed(images, key = { _, item -> item.id }) { index, item ->
            val isSelected = (index == selectedIndex)
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        border = BorderStroke(if (isSelected) 2.5.dp else 1.dp, borderColor),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectIndex(index) }
            ) {
                Image(
                    bitmap = item.thumbnail.asImageBitmap(),
                    contentDescription = item.filename,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.add_more_images),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsGrid(
    hasImage: Boolean,
    isProcessing: Boolean,
    onObfuscateCurrent: () -> Unit,
    onDeobfuscateCurrent: () -> Unit,
    onRemoveCurrent: () -> Unit,
    onRestoreCurrent: () -> Unit,
    onSaveCurrent: () -> Unit,
    onObfuscateAll: () -> Unit,
    onDeobfuscateAll: () -> Unit,
    onRemoveAll: () -> Unit,
    onRestoreAll: () -> Unit,
    onSaveAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val purplePrimary = Color(0xFF6750A4)
    val cyanSecondary = Color(0xFF0097A7)
    val coralRemove = Color(0xFFE65100)
    val greenRestore = Color(0xFF43A047)
    val limeSave = Color(0xFF7CB342)

    val buttonShape = RoundedCornerShape(10.dp)
    val buttonHeight = 44.dp
    val isEnabled = hasImage && !isProcessing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ActionButton(
                text = stringResource(R.string.btn_obfuscate),
                containerColor = purplePrimary,
                enabled = isEnabled,
                onClick = onObfuscateCurrent,
                shape = buttonShape,
                height = buttonHeight,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = stringResource(R.string.btn_deobfuscate),
                containerColor = cyanSecondary,
                enabled = isEnabled,
                onClick = onDeobfuscateCurrent,
                shape = buttonShape,
                height = buttonHeight,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = stringResource(R.string.btn_remove),
                containerColor = coralRemove,
                enabled = isEnabled,
                onClick = onRemoveCurrent,
                shape = buttonShape,
                height = buttonHeight,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = stringResource(R.string.btn_restore),
                containerColor = greenRestore,
                enabled = isEnabled,
                onClick = onRestoreCurrent,
                shape = buttonShape,
                height = buttonHeight,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = stringResource(R.string.btn_save),
                containerColor = limeSave,
                enabled = isEnabled,
                onClick = onSaveCurrent,
                shape = buttonShape,
                height = buttonHeight,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ActionButton(
                text = stringResource(R.string.btn_all),
                containerColor = purplePrimary.copy(alpha = 0.82f),
                enabled = isEnabled,
                onClick = onObfuscateAll,
                shape = buttonShape,
                height = buttonHeight,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = stringResource(R.string.btn_all),
                containerColor = cyanSecondary.copy(alpha = 0.82f),
                enabled = isEnabled,
                onClick = onDeobfuscateAll,
                shape = buttonShape,
                height = buttonHeight,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = stringResource(R.string.btn_all),
                containerColor = coralRemove.copy(alpha = 0.82f),
                enabled = isEnabled,
                onClick = onRemoveAll,
                shape = buttonShape,
                height = buttonHeight,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = stringResource(R.string.btn_all),
                containerColor = greenRestore.copy(alpha = 0.82f),
                enabled = isEnabled,
                onClick = onRestoreAll,
                shape = buttonShape,
                height = buttonHeight,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                text = stringResource(R.string.btn_all),
                containerColor = limeSave.copy(alpha = 0.82f),
                enabled = isEnabled,
                onClick = onSaveAll,
                shape = buttonShape,
                height = buttonHeight,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    containerColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White,
            disabledContainerColor = containerColor.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = modifier.height(height)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_help_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_help_1),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.dialog_help_2),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.dialog_help_3),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.dialog_help_4),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.dialog_help_5),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_got_it), fontWeight = FontWeight.Bold)
            }
        }
    )
}
