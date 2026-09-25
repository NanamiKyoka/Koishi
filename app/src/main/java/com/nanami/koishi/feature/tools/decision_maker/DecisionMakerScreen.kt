package com.nanami.koishi.feature.tools.decision_maker

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.PillShape
import com.nanami.koishi.core.designsystem.ToolCardShape
import com.nanami.koishi.feature.tools.decision_maker.components.DecisionResultPlate
import com.nanami.koishi.feature.tools.decision_maker.components.DecisionWheel
import com.nanami.koishi.feature.tools.decision_maker.components.FortuneStickTube
import com.nanami.koishi.feature.tools.decision_maker.components.OptionEditorSheet
import com.nanami.koishi.feature.tools.decision_maker.components.TopicManagerSheet
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionMode
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionOption
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionPhase
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionTopic
import com.nanami.koishi.feature.tools.decision_maker.engine.ShakeDetector
import com.nanami.koishi.feature.tools.decision_maker.engine.playableOptions

private const val WHEEL_SPIN_DURATION_MS = 3400
private const val EXPORT_FILE_NAME = "koishi_decisions.json"
private val RESULT_PLATE_MIN_HEIGHT = 118.dp

@Composable
fun DecisionMakerRoute(
    viewModel: DecisionMakerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // 在 composable 作用域内解析文案，避免在 LaunchedEffect 中调用 stringResource
    val userMessage = uiState.userMessageRes?.let { res ->
        uiState.userMessageArg?.let { stringResource(res, it) } ?: stringResource(res)
    }

    LaunchedEffect(uiState.userMessageRes, userMessage) {
        if (userMessage != null) {
            Toast.makeText(context, userMessage, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(DecisionMakerUiEvent.OnDismissMessage)
        }
    }

    DecisionMakerScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionMakerScreen(
    state: DecisionMakerUiState,
    onEvent: (DecisionMakerUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onEvent(DecisionMakerUiEvent.OnImportFrom(it)) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { onEvent(DecisionMakerUiEvent.OnExportTo(it)) }
    }

    ShakeSensorEffect(
        enabled = state.mode == DecisionMode.FORTUNE_STICK && state.canPlay && !state.isBusy
    ) {
        onEvent(DecisionMakerUiEvent.OnShakeImpulse)
    }

    if (state.showTopicManager) {
        TopicManagerSheet(
            topics = state.topics,
            currentTopicId = state.currentTopic?.id,
            onDismiss = { onEvent(DecisionMakerUiEvent.OnShowTopicManager(false)) },
            onSelect = { onEvent(DecisionMakerUiEvent.OnSelectTopic(it)) },
            onEdit = { topic ->
                onEvent(DecisionMakerUiEvent.OnSelectTopic(topic.id))
                onEvent(DecisionMakerUiEvent.OnShowTopicManager(false))
                onEvent(DecisionMakerUiEvent.OnShowOptionEditor(true))
            },
            onDelete = { onEvent(DecisionMakerUiEvent.OnDeleteTopic(it.id)) },
            onCreate = { onEvent(DecisionMakerUiEvent.OnCreateTopic) },
            onImport = { importLauncher.launch(arrayOf("*/*")) },
            onExport = { exportLauncher.launch(EXPORT_FILE_NAME) },
            onRestoreBuiltIns = { onEvent(DecisionMakerUiEvent.OnRestoreBuiltInPresets) }
        )
    }

    val currentTopic = state.currentTopic
    if (state.showOptionEditor && currentTopic != null) {
        OptionEditorSheet(
            topic = currentTopic,
            onDismiss = { onEvent(DecisionMakerUiEvent.OnShowOptionEditor(false)) },
            onSave = { onEvent(DecisionMakerUiEvent.OnSaveTopic(it)) }
        )
    }

    if (state.showHelpDialog) {
        DecisionHelpDialog(onDismiss = { onEvent(DecisionMakerUiEvent.OnShowHelpDialog(false)) })
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.decision_maker_title),
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
                    IconButton(onClick = { onEvent(DecisionMakerUiEvent.OnShowHelpDialog(true)) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = stringResource(R.string.decision_help_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.decision_topic_manage)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEvent(DecisionMakerUiEvent.OnShowTopicManager(true))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.decision_topic_import)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.FileDownload, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    importLauncher.launch(arrayOf("*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.decision_topic_export)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.FileUpload, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    exportLauncher.launch(EXPORT_FILE_NAME)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.decision_haptics)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Vibration, contentDescription = null)
                                },
                                trailingIcon = {
                                    Checkbox(
                                        checked = state.hapticsEnabled,
                                        onCheckedChange = null
                                    )
                                },
                                onClick = {
                                    onEvent(
                                        DecisionMakerUiEvent.OnHapticsEnabledChange(!state.hapticsEnabled)
                                    )
                                }
                            )
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
            ModeSelector(
                mode = state.mode,
                onSelect = { onEvent(DecisionMakerUiEvent.OnSelectMode(it)) }
            )

            TopicHeader(
                topic = currentTopic,
                onEdit = { onEvent(DecisionMakerUiEvent.OnShowOptionEditor(true)) },
                onManage = { onEvent(DecisionMakerUiEvent.OnShowTopicManager(true)) }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    currentTopic == null -> EmptyTopicState(
                        actionLabelRes = R.string.decision_topic_create,
                        onAction = { onEvent(DecisionMakerUiEvent.OnCreateTopic) }
                    )

                    currentTopic.playableOptions.isEmpty() -> EmptyTopicState(
                        actionLabelRes = R.string.decision_empty_action,
                        onAction = { onEvent(DecisionMakerUiEvent.OnShowOptionEditor(true)) }
                    )

                    state.mode == DecisionMode.WHEEL -> Column(modifier = Modifier.fillMaxSize()) {
                        // 指针当前指向的选项，转动过程中实时刷新下方结果面板
                        var pointedOption by remember(state.currentTopic) { mutableStateOf<DecisionOption?>(null) }

                        DecisionWheel(
                            topic = currentTopic,
                            result = state.result,
                            highlightResult = state.hasResult,
                            animationToken = state.animationToken,
                            enabled = state.canPlay && !state.isBusy,
                            hapticsEnabled = state.hapticsEnabled,
                            spinDurationMillis = WHEEL_SPIN_DURATION_MS,
                            onSpin = { onEvent(DecisionMakerUiEvent.OnStartSpin) },
                            onSpinFinished = { onEvent(DecisionMakerUiEvent.OnSpinFinished) },
                            onSliceChange = { pointedOption = it },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = RESULT_PLATE_MIN_HEIGHT)
                                .padding(bottom = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DecisionResultPlate(
                                option = if (state.isBusy) pointedOption else state.result,
                                settled = state.hasResult
                            )
                        }
                    }

                    else -> Column(modifier = Modifier.fillMaxSize()) {
                        FortuneStickTube(
                            topic = currentTopic,
                            result = state.result,
                            phase = state.phase,
                            energy = state.shakeEnergy,
                            hapticsEnabled = state.hapticsEnabled,
                            onShakeProgress = {
                                onEvent(DecisionMakerUiEvent.OnShakeProgress(it))
                            },
                            onDrawFinished = { onEvent(DecisionMakerUiEvent.OnDrawFinished) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        ShakeEnergyBar(
                            energy = state.shakeEnergy,
                            phase = state.phase,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp)
                                .padding(bottom = 4.dp)
                        )
                    }
                }
            }

            BottomBar(state = state)
        }
    }
}

@Composable
private fun ShakeSensorEffect(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)

    LifecycleResumeEffect(enabled) {
        val detector = ShakeDetector(onShakeTick = { currentOnShake() })
        if (enabled) detector.start(context)
        onPauseOrDispose { detector.stop() }
    }
}

@Composable
private fun ModeSelector(mode: DecisionMode, onSelect: (DecisionMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        DecisionMode.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = entry == mode,
                onClick = { onSelect(entry) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = DecisionMode.entries.size
                ),
                icon = {
                    Icon(
                        imageVector = if (entry == DecisionMode.WHEEL) {
                            Icons.Rounded.Casino
                        } else {
                            Icons.Rounded.Vibration
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(
                            if (entry == DecisionMode.WHEEL) {
                                R.string.decision_mode_wheel
                            } else {
                                R.string.decision_mode_stick
                            }
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}

@Composable
private fun TopicHeader(
    topic: DecisionTopic?,
    onEdit: () -> Unit,
    onManage: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 6.dp),
        shape = ToolCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic?.title ?: stringResource(R.string.decision_topic_empty),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.decision_topic_summary, topic?.options?.size ?: 0),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit, enabled = topic != null) {
                Icon(
                    imageVector = Icons.Rounded.EditNote,
                    contentDescription = stringResource(R.string.decision_topic_edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onManage) {
                Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = stringResource(R.string.decision_topic_manage),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShakeEnergyBar(energy: Float, phase: DecisionPhase, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = energy,
        animationSpec = tween(durationMillis = 90),
        label = "shakeEnergy"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.decision_shake_energy),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(
                    R.string.decision_shake_energy_value,
                    (progress.coerceIn(0f, 1f) * 100).toInt()
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (progress >= 1f) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(PillShape),
            color = if (phase == DecisionPhase.SHAKING) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.tertiary
            },
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
private fun BottomBar(state: DecisionMakerUiState) {
    val hint = when {
        state.playableOptionCount == 0 -> R.string.decision_empty_title
        !state.canPlay -> R.string.decision_need_two_options
        state.mode == DecisionMode.WHEEL && state.isBusy -> R.string.decision_hint_wheel_busy
        state.mode == DecisionMode.WHEEL && state.hasResult -> R.string.decision_hint_wheel_done
        state.mode == DecisionMode.WHEEL -> R.string.decision_hint_wheel
        state.hasResult -> R.string.decision_hint_stick_done
        state.phase == DecisionPhase.DRAWING -> R.string.decision_hint_drawing
        state.phase == DecisionPhase.SHAKING -> R.string.decision_hint_shaking
        else -> R.string.decision_hint_stick
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Casino,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptyTopicState(actionLabelRes: Int, onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Casino,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.decision_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.decision_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = onAction,
            shape = PillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(actionLabelRes))
        }
    }
}

@Composable
private fun DecisionHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.decision_help_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    R.string.decision_help_p1,
                    R.string.decision_help_p2,
                    R.string.decision_help_p3,
                    R.string.decision_help_p4
                ).forEach { res ->
                    Text(
                        text = stringResource(res),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.dialog_got_it))
            }
        },
        shape = ToolCardShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
