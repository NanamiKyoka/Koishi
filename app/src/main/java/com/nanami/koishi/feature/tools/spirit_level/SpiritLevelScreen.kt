package com.nanami.koishi.feature.tools.spirit_level

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.adaptive.AdaptiveContentWidth
import com.nanami.koishi.feature.tools.spirit_level.components.BubbleLevel
import com.nanami.koishi.feature.tools.spirit_level.components.InclineGauge
import com.nanami.koishi.feature.tools.spirit_level.engine.LevelMode

@Composable
fun SpiritLevelRoute(
    viewModel: SpiritLevelViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current

    DisposableEffect(viewModel, view) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        viewModel.onEvent(SpiritLevelUiEvent.OnStart)
        onDispose {
            viewModel.onEvent(SpiritLevelUiEvent.OnStop)
            view.keepScreenOn = previousKeepScreenOn
        }
    }

    SpiritLevelScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiritLevelScreen(
    state: SpiritLevelUiState,
    onEvent: (SpiritLevelUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tool_spirit_level_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        AdaptiveContentWidth(maxWidth = 720.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (!state.isSupported) {
                    SpiritLevelStatusCard(
                        state = state.copy(errorRes = R.string.spirit_level_sensor_unavailable)
                    )
                } else {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        LevelMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = state.mode == mode,
                                onClick = { onEvent(SpiritLevelUiEvent.OnModeChange(mode)) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = LevelMode.entries.size
                                )
                            ) {
                                Text(
                                    text = stringResource(
                                        when (mode) {
                                            LevelMode.SURFACE -> R.string.spirit_level_mode_surface
                                            LevelMode.INCLINE -> R.string.spirit_level_mode_incline
                                        }
                                    )
                                )
                            }
                        }
                    }

                    when (state.mode) {
                        LevelMode.SURFACE -> BubbleLevel(state = state)
                        LevelMode.INCLINE -> InclineGauge(state = state)
                    }

                    SpiritLevelStatusCard(state = state)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.spirit_level_reading_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            ReadingRow(
                                label = stringResource(R.string.spirit_level_pitch),
                                value = stringResource(
                                    R.string.spirit_level_angle_format,
                                    state.frame.pitchDeg
                                )
                            )
                            ReadingRow(
                                label = stringResource(R.string.spirit_level_roll),
                                value = stringResource(
                                    R.string.spirit_level_angle_format,
                                    state.frame.rollDeg
                                )
                            )
                            ReadingRow(
                                label = stringResource(R.string.spirit_level_tilt_angle),
                                value = stringResource(
                                    R.string.spirit_level_angle_format,
                                    state.frame.tiltDeg
                                )
                            )
                            ReadingRow(
                                label = stringResource(R.string.spirit_level_calibration_state),
                                value = stringResource(
                                    if (state.hasCalibration) {
                                        R.string.spirit_level_calibrated
                                    } else {
                                        R.string.spirit_level_not_calibrated
                                    }
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onEvent(SpiritLevelUiEvent.OnCalibrate) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Straighten,
                                contentDescription = null
                            )
                            Text(
                                text = stringResource(R.string.spirit_level_calibrate),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        OutlinedButton(
                            onClick = { onEvent(SpiritLevelUiEvent.OnResetCalibration) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null
                            )
                            Text(
                                text = stringResource(R.string.spirit_level_reset),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onEvent(SpiritLevelUiEvent.OnToggleHold) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (state.isHeld) {
                                Icons.Rounded.LockOpen
                            } else {
                                Icons.Rounded.Lock
                            },
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(
                                if (state.isHeld) {
                                    R.string.spirit_level_resume
                                } else {
                                    R.string.spirit_level_hold
                                }
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.spirit_level_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SpiritLevelStatusCard(
    state: SpiritLevelUiState,
    modifier: Modifier = Modifier
) {
    val isIncline = state.mode == LevelMode.INCLINE
    val icon = when {
        state.errorRes != null -> Icons.Rounded.ErrorOutline
        isIncline -> Icons.Rounded.Straighten
        state.status == LevelStatus.LEVEL -> Icons.Rounded.CheckCircle
        state.status == LevelStatus.NEAR -> Icons.Rounded.Straighten
        else -> Icons.Rounded.WarningAmber
    }
    val titleRes = when {
        state.errorRes != null -> R.string.spirit_level_status_error
        isIncline -> R.string.spirit_level_status_incline
        else -> state.status.labelRes
    }
    val messageRes = state.errorRes ?: when {
        isIncline -> R.string.spirit_level_status_incline_desc
        state.status == LevelStatus.LEVEL -> R.string.spirit_level_status_level_desc
        state.status == LevelStatus.NEAR -> R.string.spirit_level_status_near_desc
        else -> R.string.spirit_level_status_off_desc
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReadingRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
