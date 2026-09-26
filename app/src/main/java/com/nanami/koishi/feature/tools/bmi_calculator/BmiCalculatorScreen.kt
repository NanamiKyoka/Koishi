package com.nanami.koishi.feature.tools.bmi_calculator

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.bmi_calculator.components.BmiCategoryChip
import com.nanami.koishi.feature.tools.bmi_calculator.components.BmiChartLegend
import com.nanami.koishi.feature.tools.bmi_calculator.components.BmiClearAllDialog
import com.nanami.koishi.feature.tools.bmi_calculator.components.BmiRecordItem
import com.nanami.koishi.feature.tools.bmi_calculator.components.BmiScaleGauge
import com.nanami.koishi.feature.tools.bmi_calculator.components.BmiTrendChart
import com.nanami.koishi.feature.tools.bmi_calculator.components.rememberBmiBandColors
import com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiCategory

@Composable
fun BmiCalculatorRoute(
    viewModel: BmiCalculatorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BmiCalculatorScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BmiCalculatorScreen(
    uiState: BmiCalculatorUiState,
    onEvent: (BmiCalculatorUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.bmi_undo)
    val messageText = uiState.messageRes?.let { messageRes ->
        if (uiState.messageArgs.isEmpty()) {
            stringResource(messageRes)
        } else {
            stringResource(messageRes, *uiState.messageArgs.toTypedArray())
        }
    }

    LaunchedEffect(messageText, uiState.messageHasUndo) {
        if (messageText == null) return@LaunchedEffect
        val result = if (uiState.messageHasUndo) {
            snackbarHostState.showSnackbar(
                message = messageText,
                actionLabel = undoLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        } else {
            snackbarHostState.showSnackbar(message = messageText)
        }
        if (uiState.messageHasUndo && result == SnackbarResult.ActionPerformed) {
            onEvent(BmiCalculatorUiEvent.OnUndoDelete)
        }
        onEvent(BmiCalculatorUiEvent.OnDismissMessage)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.bmi_title),
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
                    if (uiState.hasRecords) {
                        IconButton(onClick = { onEvent(BmiCalculatorUiEvent.OnRequestClearAll) }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = stringResource(R.string.bmi_history_clear)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "input") {
                BmiInputCard(uiState = uiState, onEvent = onEvent)
            }

            uiState.previewCategory?.let { category ->
                item(key = "result") {
                    BmiResultCard(uiState = uiState, category = category)
                }
            }

            item(key = "trend") {
                BmiTrendCard(uiState = uiState, onEvent = onEvent)
            }

            if (uiState.hasRecords) {
                item(key = "history-header") {
                    BmiSectionHeader(
                        title = stringResource(R.string.bmi_history_title),
                        actionLabel = stringResource(R.string.bmi_history_clear),
                        onAction = { onEvent(BmiCalculatorUiEvent.OnRequestClearAll) }
                    )
                }

                items(
                    items = uiState.historyRecords,
                    key = { it.id }
                ) { record ->
                    BmiRecordItem(
                        record = record,
                        isSelected = record.id == uiState.selectedRecordId,
                        onClick = { onEvent(BmiCalculatorUiEvent.OnSelectRecord(record.id)) },
                        onDelete = { onEvent(BmiCalculatorUiEvent.OnDeleteRecord(record.id)) }
                    )
                }
            }

            item(key = "reference") {
                BmiReferenceCard()
            }
        }
    }

    if (uiState.showClearConfirm) {
        BmiClearAllDialog(
            recordCount = uiState.trendRecords.size,
            onConfirm = { onEvent(BmiCalculatorUiEvent.OnConfirmClearAll) },
            onDismiss = { onEvent(BmiCalculatorUiEvent.OnDismissClearAll) }
        )
    }
}

@Composable
private fun BmiInputCard(
    uiState: BmiCalculatorUiState,
    onEvent: (BmiCalculatorUiEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.heightInput,
                    onValueChange = { onEvent(BmiCalculatorUiEvent.OnHeightChange(it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.bmi_height_label)) },
                    suffix = { Text(stringResource(R.string.bmi_unit_cm)) },
                    isError = uiState.heightError,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                OutlinedTextField(
                    value = uiState.weightInput,
                    onValueChange = { onEvent(BmiCalculatorUiEvent.OnWeightChange(it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.bmi_weight_label)) },
                    suffix = { Text(stringResource(R.string.bmi_unit_kg)) },
                    isError = uiState.weightError,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onEvent(BmiCalculatorUiEvent.OnSaveRecord) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
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
                Text(
                    text = stringResource(R.string.bmi_save_record),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
private fun BmiResultCard(
    uiState: BmiCalculatorUiState,
    category: BmiCategory
) {
    val bandColors = rememberBmiBandColors()
    val accentColor = bandColors.of(category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = stringResource(R.string.bmi_current_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = uiState.previewBmiText,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                BmiCategoryChip(category = category, emphasized = true)
            }

            Spacer(modifier = Modifier.height(16.dp))

            BmiScaleGauge(
                fraction = uiState.gaugeFraction,
                category = category,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.MonitorWeight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        R.string.bmi_healthy_weight,
                        uiState.healthyWeightMinText,
                        uiState.healthyWeightMaxText
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val hintRes = uiState.healthyHintRes
            if (hintRes != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (uiState.healthyHintArgs.isEmpty()) {
                        stringResource(hintRes)
                    } else {
                        stringResource(hintRes, *uiState.healthyHintArgs.toTypedArray())
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BmiTrendCard(
    uiState: BmiCalculatorUiState,
    onEvent: (BmiCalculatorUiEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 8.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bmi_trend_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (uiState.hasRecords) {
                        val selected = uiState.selectedRecord
                        Text(
                            text = if (selected != null) {
                                stringResource(
                                    R.string.bmi_trend_selected,
                                    selected.timeText,
                                    selected.bmiText
                                )
                            } else {
                                stringResource(R.string.bmi_trend_count, uiState.trendRecords.size)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uiState.selectedRecord != null) {
                    IconButton(onClick = { onEvent(BmiCalculatorUiEvent.OnClearSelection) }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.bmi_trend_clear_selection),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (uiState.hasRecords) {
                BmiTrendChart(
                    records = uiState.trendRecords,
                    selectedRecordId = uiState.selectedRecordId,
                    onSelectRecord = { onEvent(BmiCalculatorUiEvent.OnSelectRecord(it)) },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                BmiChartLegend(
                    modifier = Modifier.padding(
                        start = 18.dp,
                        end = 18.dp,
                        top = 4.dp,
                        bottom = 16.dp
                    )
                )
            } else {
                BmiEmptyState()
            }
        }
    }
}

@Composable
private fun BmiEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.bmi_empty_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.bmi_empty_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BmiSectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAction) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun BmiReferenceCard() {
    val bandColors = rememberBmiBandColors()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = stringResource(R.string.bmi_reference_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.bmi_reference_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            BmiCategory.entries.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(bandColors.of(category))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(category.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(category.rangeRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun BmiCategory.rangeRes(): Int = when (this) {
    BmiCategory.UNDERWEIGHT -> R.string.bmi_range_underweight
    BmiCategory.NORMAL -> R.string.bmi_range_normal
    BmiCategory.OVERWEIGHT -> R.string.bmi_range_overweight
    BmiCategory.OBESE -> R.string.bmi_range_obese
}
