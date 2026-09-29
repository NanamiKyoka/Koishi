package com.nanami.koishi.feature.tools.worth_calculator

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.worth_calculator.components.EducationAndExperienceSection
import com.nanami.koishi.feature.tools.worth_calculator.components.EnvironmentAndTeamSection
import com.nanami.koishi.feature.tools.worth_calculator.components.SalaryAndRegionSection
import com.nanami.koishi.feature.tools.worth_calculator.components.WorkScheduleSection
import com.nanami.koishi.feature.tools.worth_calculator.components.WorthFormulaDialog
import com.nanami.koishi.feature.tools.worth_calculator.components.WorthHistorySheet
import com.nanami.koishi.feature.tools.worth_calculator.components.WorthResultView
import com.nanami.koishi.feature.tools.worth_calculator.components.WorthSaveDialog

@Composable
fun WorthCalculatorRoute(
    viewModel: WorthCalculatorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val message = uiState.userMessageRes?.let { stringResource(it) }
    LaunchedEffect(uiState.userMessageRes, message) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(WorthCalculatorUiEvent.OnDismissMessage)
        }
    }

    WorthCalculatorScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorthCalculatorScreen(
    state: WorthCalculatorUiState,
    onEvent: (WorthCalculatorUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = state.currentScreen == WorthScreenMode.RESULT) {
        onEvent(WorthCalculatorUiEvent.OnNavigateBackToForm)
    }

    if (state.isFormulaDialogOpen) {
        WorthFormulaDialog(
            onDismiss = { onEvent(WorthCalculatorUiEvent.OnToggleFormulaDialog(false)) }
        )
    }

    if (showSaveDialog) {
        val defaultTitle = "测算 · ${state.selectedCountry.currencySymbol}${state.salaryInput}"
        WorthSaveDialog(
            initialTitle = defaultTitle,
            onConfirm = { onEvent(WorthCalculatorUiEvent.OnSaveRecord(it)) },
            onDismiss = { showSaveDialog = false }
        )
    }

    if (state.isHistorySheetOpen) {
        WorthHistorySheet(
            records = state.records,
            onSelectRecord = { onEvent(WorthCalculatorUiEvent.OnSelectRecord(it)) },
            onDeleteRecord = { onEvent(WorthCalculatorUiEvent.OnDeleteRecord(it)) },
            onClearAll = { onEvent(WorthCalculatorUiEvent.OnClearRecords) },
            onDismiss = { onEvent(WorthCalculatorUiEvent.OnToggleHistorySheet(false)) }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    val titleText = if (state.currentScreen == WorthScreenMode.RESULT) {
                        state.activeDetailRecord?.title ?: stringResource(R.string.worth_result_title)
                    } else {
                        stringResource(R.string.tool_worth_calculator_name)
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.currentScreen == WorthScreenMode.RESULT) {
                                onEvent(WorthCalculatorUiEvent.OnNavigateBackToForm)
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (state.currentScreen == WorthScreenMode.FORM) {
                        IconButton(onClick = { onEvent(WorthCalculatorUiEvent.OnToggleHistorySheet(true)) }) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = stringResource(R.string.worth_history_title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onEvent(WorthCalculatorUiEvent.OnToggleFormulaDialog(true)) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                contentDescription = stringResource(R.string.worth_formula_dialog_title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val record = state.activeDetailRecord
                        if (record == null) {
                            IconButton(onClick = { showSaveDialog = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.BookmarkAdd,
                                    contentDescription = stringResource(R.string.worth_save_record),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            IconButton(onClick = { onEvent(WorthCalculatorUiEvent.OnApplyRecord(record)) }) {
                                Icon(
                                    imageVector = Icons.Rounded.EditNote,
                                    contentDescription = stringResource(R.string.worth_apply_to_form),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onEvent(WorthCalculatorUiEvent.OnDeleteRecord(record.id)) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = stringResource(R.string.worth_delete),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onEvent(WorthCalculatorUiEvent.OnToggleFormulaDialog(true)) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                contentDescription = stringResource(R.string.worth_formula_dialog_title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            if (state.currentScreen == WorthScreenMode.FORM) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = { onEvent(WorthCalculatorUiEvent.OnCalculate) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Calculate,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.worth_btn_calculate),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = state.currentScreen,
            transitionSpec = {
                if (targetState == WorthScreenMode.RESULT) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            label = "WorthScreenTransition"
        ) { screen ->
            when (screen) {
                WorthScreenMode.FORM -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SalaryAndRegionSection(
                            state = state,
                            onEvent = onEvent
                        )

                        WorkScheduleSection(
                            state = state,
                            onEvent = onEvent
                        )

                        EnvironmentAndTeamSection(
                            state = state,
                            onEvent = onEvent
                        )

                        EducationAndExperienceSection(
                            state = state,
                            onEvent = onEvent
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                WorthScreenMode.RESULT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                    ) {
                        WorthResultView(
                            state = state,
                            onSaveClick = { showSaveDialog = true },
                            onApplyRecord = { onEvent(WorthCalculatorUiEvent.OnApplyRecord(it)) },
                            onBackToForm = { onEvent(WorthCalculatorUiEvent.OnNavigateBackToForm) }
                        )
                    }
                }
            }
        }
    }
}
