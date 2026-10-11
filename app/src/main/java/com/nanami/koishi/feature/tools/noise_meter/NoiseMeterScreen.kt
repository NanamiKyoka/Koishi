package com.nanami.koishi.feature.tools.noise_meter

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.core.designsystem.adaptive.AdaptiveContentWidth
import com.nanami.koishi.feature.tools.noise_meter.components.NoiseGauge
import com.nanami.koishi.feature.tools.noise_meter.components.NoiseStatCard
import com.nanami.koishi.feature.tools.noise_meter.components.color
import com.nanami.koishi.feature.tools.noise_meter.components.descriptionRes
import com.nanami.koishi.feature.tools.noise_meter.components.labelRes
import com.nanami.koishi.feature.tools.noise_meter.engine.FrequencyWeighting
import com.nanami.koishi.feature.tools.noise_meter.engine.NoiseConfig
import com.nanami.koishi.feature.tools.noise_meter.engine.TimeWeighting

@Composable
fun NoiseMeterRoute(
    viewModel: NoiseMeterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onEvent(NoiseMeterUiEvent.OnPermissionResult(granted))
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onEvent(NoiseMeterUiEvent.OnPermissionResult(granted))
    }

    val errorMessage = uiState.errorRes?.let { stringResource(it) }
    LaunchedEffect(uiState.errorRes) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(NoiseMeterUiEvent.OnDismissError)
        }
    }

    NoiseMeterScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoiseMeterScreen(
    state: NoiseMeterUiState,
    onEvent: (NoiseMeterUiEvent) -> Unit,
    onRequestPermission: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unit = "dB(${state.frequencyWeighting.name})"
    val levelColor = state.level.color()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tool_noise_meter_name),
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        AdaptiveContentWidth(maxWidth = 720.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.hasPermission) {
                    MeasurementContent(state = state, unit = unit, levelColor = levelColor, onEvent = onEvent)
                } else {
                    PermissionCard(onRequestPermission = onRequestPermission)
                }

                SettingsCard(
                    calibrationOffsetDb = state.calibrationOffsetDb,
                    frequencyWeighting = state.frequencyWeighting,
                    timeWeighting = state.timeWeighting,
                    onEvent = onEvent
                )

                Text(
                    text = stringResource(R.string.noise_meter_reading_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MeasurementContent(
    state: NoiseMeterUiState,
    unit: String,
    levelColor: androidx.compose.ui.graphics.Color,
    onEvent: (NoiseMeterUiEvent) -> Unit

) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.noise_meter_approx_unit, unit),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(modifier = Modifier.widthIn(max = 300.dp)) {
            NoiseGauge(
                value = state.levelDb,
                fraction = if (state.hasReading) state.gaugeFraction else 0f,
                unit = unit,
                color = levelColor,
                isActive = state.hasReading,
                levelLabel = if (state.hasReading) stringResource(state.level.labelRes()) else ""
            )
        }

        Text(
            text = if (state.hasReading) {
                stringResource(state.level.descriptionRes())
            } else {
                stringResource(R.string.noise_meter_hint_idle)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NoiseStatCard(
                label = stringResource(R.string.noise_meter_peak),
                value = formatValue(state, state.peakDb),
                unit = unit,
                modifier = Modifier.weight(1f)
            )
            NoiseStatCard(
                label = stringResource(R.string.noise_meter_minimum),
                value = formatValue(state, state.minimumDb),
                unit = unit,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NoiseStatCard(
                label = stringResource(R.string.noise_meter_leq),
                value = formatValue(state, state.leqDb),
                unit = unit,
                modifier = Modifier.weight(1f)
            )
            NoiseStatCard(
                label = stringResource(R.string.noise_meter_elapsed),
                value = formatDuration(state.elapsedMs),
                unit = "",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (state.isMeasuring) {
                        onEvent(NoiseMeterUiEvent.OnStop)
                    } else {
                        onEvent(NoiseMeterUiEvent.OnStart)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (state.isMeasuring) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = stringResource(
                        if (state.isMeasuring) R.string.noise_meter_stop else R.string.noise_meter_start
                    )
                )
            }
            OutlinedButton(
                onClick = { onEvent(NoiseMeterUiEvent.OnReset) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = stringResource(R.string.noise_meter_reset))
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MicOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.noise_meter_permission_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.noise_meter_permission_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRequestPermission) {
                Text(text = stringResource(R.string.noise_meter_grant_permission))
            }
        }
    }
}

@Composable
private fun SettingsCard(
    calibrationOffsetDb: Double,
    frequencyWeighting: FrequencyWeighting,
    timeWeighting: TimeWeighting,
    onEvent: (NoiseMeterUiEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.noise_meter_calibration),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%.1f dB".format(calibrationOffsetDb),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = stringResource(R.string.noise_meter_calibration_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = calibrationOffsetDb.toFloat(),
                    onValueChange = { onEvent(NoiseMeterUiEvent.OnCalibrationChange(it.toDouble())) },
                    valueRange = NoiseConfig.MIN_CALIBRATION_OFFSET_DB.toFloat()..
                        NoiseConfig.MAX_CALIBRATION_OFFSET_DB.toFloat()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.noise_meter_weighting),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    FrequencyWeighting.entries.forEachIndexed { index, weighting ->
                        SegmentedButton(
                            selected = weighting == frequencyWeighting,
                            onClick = {
                                onEvent(NoiseMeterUiEvent.OnFrequencyWeightingChange(weighting))
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = FrequencyWeighting.entries.size
                            )
                        ) {
                            Text(text = weighting.name)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.noise_meter_time_weighting),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TimeWeighting.entries.forEachIndexed { index, weighting ->
                        SegmentedButton(
                            selected = weighting == timeWeighting,
                            onClick = {
                                onEvent(NoiseMeterUiEvent.OnTimeWeightingChange(weighting))
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = TimeWeighting.entries.size
                            )
                        ) {
                            Text(
                                text = stringResource(
                                    when (weighting) {
                                        TimeWeighting.FAST -> R.string.noise_meter_time_fast
                                        TimeWeighting.SLOW -> R.string.noise_meter_time_slow
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatValue(state: NoiseMeterUiState, value: Double): String =
    if (state.hasReading) value.toInt().toString() else "--"

private fun formatDuration(elapsedMs: Long): String {
    val totalSeconds = elapsedMs / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}