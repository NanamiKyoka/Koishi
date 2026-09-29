package com.nanami.koishi.feature.tools.worth_calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.worth_calculator.engine.CanteenQuality
import com.nanami.koishi.feature.tools.worth_calculator.engine.CityTier
import com.nanami.koishi.feature.tools.worth_calculator.engine.DegreeType
import com.nanami.koishi.feature.tools.worth_calculator.engine.JobStability
import com.nanami.koishi.feature.tools.worth_calculator.engine.LeadershipRelation
import com.nanami.koishi.feature.tools.worth_calculator.engine.SchoolTier
import com.nanami.koishi.feature.tools.worth_calculator.engine.ShuttleQuality
import com.nanami.koishi.feature.tools.worth_calculator.engine.TeamAtmosphere
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorkEnvironment
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorkYears
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthCalculatorData
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthCalculatorEngine
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthCountries
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthCountry
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthRecord
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class WorthCalculatorViewModel(
    private val repository: WorthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorthCalculatorUiState())
    val uiState: StateFlow<WorthCalculatorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.dataFlow.collect { data ->
                _uiState.update { current ->
                    current.copy(
                        records = data.records,
                        salaryInput = if (data.draftSalary.isNotBlank()) data.draftSalary else current.salaryInput,
                        selectedCountry = WorthCountries.getByCode(data.draftCountryCode),
                        workDaysPerWeek = data.draftWorkDaysPerWeek.ifBlank { current.workDaysPerWeek },
                        wfhDaysPerWeek = data.draftWfhDaysPerWeek.ifBlank { current.wfhDaysPerWeek },
                        annualLeave = data.draftAnnualLeave.ifBlank { current.annualLeave },
                        publicHolidays = data.draftPublicHolidays.ifBlank { current.publicHolidays },
                        paidSickLeave = data.draftPaidSickLeave.ifBlank { current.paidSickLeave },
                        workHours = data.draftWorkHours.ifBlank { current.workHours },
                        commuteHours = data.draftCommuteHours.ifBlank { current.commuteHours },
                        restTime = data.draftRestTime.ifBlank { current.restTime },
                        jobStability = JobStability.fromId(data.draftJobStabilityId),
                        cityTier = CityTier.fromId(data.draftCityTierId),
                        workEnvironment = WorkEnvironment.fromId(data.draftWorkEnvironmentId),
                        leadership = LeadershipRelation.fromId(data.draftLeadershipId),
                        teamwork = TeamAtmosphere.fromId(data.draftTeamworkId),
                        isHometown = data.draftIsHometown,
                        hasShuttle = data.draftHasShuttle,
                        shuttleQuality = ShuttleQuality.fromId(data.draftShuttleQualityId),
                        hasCanteen = data.draftHasCanteen,
                        canteenQuality = CanteenQuality.fromId(data.draftCanteenQualityId),
                        degreeType = DegreeType.fromId(data.draftDegreeTypeId),
                        schoolTier = SchoolTier.fromId(data.draftSchoolTierId),
                        bachelorTier = SchoolTier.fromId(data.draftBachelorTierId),
                        workYears = WorkYears.fromId(data.draftWorkYearsId)
                    )
                }
                recalculate()
            }
        }
    }

    fun onEvent(event: WorthCalculatorUiEvent) {
        when (event) {
            is WorthCalculatorUiEvent.OnSalaryChange -> {
                _uiState.update { it.copy(salaryInput = event.value) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnCountryChange -> {
                _uiState.update { it.copy(selectedCountry = event.country) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnWorkDaysChange -> {
                _uiState.update { it.copy(workDaysPerWeek = event.value) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnWfhDaysChange -> {
                _uiState.update { it.copy(wfhDaysPerWeek = event.value) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnAnnualLeaveChange -> {
                _uiState.update { it.copy(annualLeave = event.value) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnPublicHolidaysChange -> {
                _uiState.update { it.copy(publicHolidays = event.value) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnPaidSickLeaveChange -> {
                _uiState.update { it.copy(paidSickLeave = event.value) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnWorkHoursChange -> {
                _uiState.update { it.copy(workHours = event.value) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnCommuteHoursChange -> {
                _uiState.update { it.copy(commuteHours = event.value) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnRestTimeChange -> {
                _uiState.update { it.copy(restTime = event.value) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnJobStabilityChange -> {
                _uiState.update { it.copy(jobStability = event.stability) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnCityTierChange -> {
                _uiState.update { it.copy(cityTier = event.tier) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnWorkEnvironmentChange -> {
                _uiState.update { it.copy(workEnvironment = event.environment) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnLeadershipChange -> {
                _uiState.update { it.copy(leadership = event.relation) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnTeamworkChange -> {
                _uiState.update { it.copy(teamwork = event.atmosphere) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnIsHometownChange -> {
                _uiState.update { it.copy(isHometown = event.value) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnHasShuttleChange -> {
                _uiState.update { it.copy(hasShuttle = event.value) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnShuttleQualityChange -> {
                _uiState.update { it.copy(shuttleQuality = event.quality) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnHasCanteenChange -> {
                _uiState.update { it.copy(hasCanteen = event.value) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnCanteenQualityChange -> {
                _uiState.update { it.copy(canteenQuality = event.quality) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnDegreeTypeChange -> {
                _uiState.update { it.copy(degreeType = event.degree) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnSchoolTierChange -> {
                _uiState.update { it.copy(schoolTier = event.tier) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnBachelorTierChange -> {
                _uiState.update { it.copy(bachelorTier = event.tier) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnWorkYearsChange -> {
                _uiState.update { it.copy(workYears = event.years) }
                persistDraft()
                recalculate()
            }
            is WorthCalculatorUiEvent.OnCalculate -> {
                recalculate()
                val s = _uiState.value
                val salary = s.salaryInput.toDoubleOrNull() ?: 0.0
                if (salary <= 0.0) {
                    _uiState.update { it.copy(userMessageRes = R.string.worth_error_salary_empty) }
                } else {
                    _uiState.update { it.copy(currentScreen = WorthScreenMode.RESULT, activeDetailRecord = null) }
                }
            }
            is WorthCalculatorUiEvent.OnNavigateBackToForm -> {
                _uiState.update { it.copy(currentScreen = WorthScreenMode.FORM, activeDetailRecord = null) }
            }
            is WorthCalculatorUiEvent.OnSelectRecord -> {
                _uiState.update {
                    it.copy(
                        currentScreen = WorthScreenMode.RESULT,
                        activeDetailRecord = event.record,
                        isHistorySheetOpen = false
                    )
                }
            }
            is WorthCalculatorUiEvent.OnApplyRecord -> {
                applyRecordToForm(event.record)
            }
            is WorthCalculatorUiEvent.OnSaveRecord -> {
                saveRecord(event.title)
            }
            is WorthCalculatorUiEvent.OnDeleteRecord -> {
                viewModelScope.launch {
                    repository.deleteRecord(event.recordId)
                    _uiState.update {
                        val isCurrentActive = it.activeDetailRecord?.id == event.recordId
                        it.copy(
                            userMessageRes = R.string.worth_record_deleted,
                            activeDetailRecord = if (isCurrentActive) null else it.activeDetailRecord,
                            currentScreen = if (isCurrentActive) WorthScreenMode.FORM else it.currentScreen
                        )
                    }
                }
            }
            is WorthCalculatorUiEvent.OnClearRecords -> {
                viewModelScope.launch {
                    repository.clearRecords()
                    _uiState.update {
                        it.copy(
                            userMessageRes = R.string.worth_history_cleared,
                            activeDetailRecord = null,
                            currentScreen = WorthScreenMode.FORM
                        )
                    }
                }
            }
            is WorthCalculatorUiEvent.OnToggleFormulaDialog -> {
                _uiState.update { it.copy(isFormulaDialogOpen = event.open) }
            }
            is WorthCalculatorUiEvent.OnToggleHistorySheet -> {
                _uiState.update { it.copy(isHistorySheetOpen = event.open) }
            }
            is WorthCalculatorUiEvent.OnDismissMessage -> {
                _uiState.update { it.copy(userMessageRes = null, userMessageArgs = emptyList()) }
            }
        }
    }

    private fun recalculate() {
        val s = _uiState.value
        val salaryVal = s.salaryInput.toDoubleOrNull() ?: 0.0
        val workDaysVal = s.workDaysPerWeek.toDoubleOrNull() ?: 5.0
        val wfhDaysVal = s.wfhDaysPerWeek.toDoubleOrNull() ?: 0.0
        val annualLeaveVal = s.annualLeave.toDoubleOrNull() ?: 5.0
        val publicHolidaysVal = s.publicHolidays.toDoubleOrNull() ?: 13.0
        val paidSickLeaveVal = s.paidSickLeave.toDoubleOrNull() ?: 3.0
        val workHoursVal = s.workHours.toDoubleOrNull() ?: 10.0
        val commuteHoursVal = s.commuteHours.toDoubleOrNull() ?: 2.0
        val restTimeVal = s.restTime.toDoubleOrNull() ?: 2.0

        val result = WorthCalculatorEngine.calculate(
            salary = salaryVal,
            country = s.selectedCountry,
            workDaysPerWeek = workDaysVal,
            wfhDaysPerWeek = wfhDaysVal,
            annualLeave = annualLeaveVal,
            publicHolidays = publicHolidaysVal,
            paidSickLeave = paidSickLeaveVal,
            workHours = workHoursVal,
            commuteHours = commuteHoursVal,
            restTime = restTimeVal,
            jobStability = s.jobStability,
            cityTier = s.cityTier,
            workEnvironment = s.workEnvironment,
            leadership = s.leadership,
            teamwork = s.teamwork,
            isHometown = s.isHometown,
            hasShuttle = s.hasShuttle,
            shuttleQuality = s.shuttleQuality,
            hasCanteen = s.hasCanteen,
            canteenQuality = s.canteenQuality,
            degreeType = s.degreeType,
            schoolTier = s.schoolTier,
            bachelorTier = s.bachelorTier,
            workYears = s.workYears
        )

        _uiState.update { it.copy(result = result) }
    }

    private fun saveRecord(customTitle: String) {
        val s = _uiState.value
        val res = s.result ?: return
        if (res.score <= 0.0) return

        val currency = s.selectedCountry.currencySymbol
        val formattedDaily = String.format(Locale.US, "%.1f", res.dailySalary)
        val formattedDays = String.format(Locale.US, "%.0f", res.workingDaysPerYear)
        val formattedHours = String.format(Locale.US, "%.1f", res.effectiveDailyHours)

        val title = customTitle.ifBlank {
            "测算 · $currency${s.salaryInput}"
        }

        val record = WorthRecord(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            title = title,
            score = res.score,
            assessmentId = res.assessment.id,
            salaryText = "$currency${s.salaryInput}",
            rawSalary = s.salaryInput,
            countryCode = s.selectedCountry.code,
            dailySalaryText = "$currency$formattedDaily",
            workingDaysText = "${formattedDays}天",
            effectiveHoursText = "${formattedHours}h",
            environmentFactor = res.environmentFactor,
            educationFactor = res.educationFactor,
            experienceMultiplier = res.experienceMultiplier,
            adviceList = res.adviceList,
            workDaysPerWeek = s.workDaysPerWeek,
            wfhDaysPerWeek = s.wfhDaysPerWeek,
            annualLeave = s.annualLeave,
            publicHolidays = s.publicHolidays,
            paidSickLeave = s.paidSickLeave,
            workHours = s.workHours,
            commuteHours = s.commuteHours,
            restTime = s.restTime,
            jobStabilityId = s.jobStability.id,
            cityTierId = s.cityTier.id,
            workEnvironmentId = s.workEnvironment.id,
            leadershipId = s.leadership.id,
            teamworkId = s.teamwork.id,
            isHometown = s.isHometown,
            hasShuttle = s.hasShuttle,
            shuttleQualityId = s.shuttleQuality.id,
            hasCanteen = s.hasCanteen,
            canteenQualityId = s.canteenQuality.id,
            degreeTypeId = s.degreeType.id,
            schoolTierId = s.schoolTier.id,
            bachelorTierId = s.bachelorTier.id,
            workYearsId = s.workYears.id
        )

        viewModelScope.launch {
            repository.saveRecord(record)
            _uiState.update {
                it.copy(
                    activeDetailRecord = record,
                    userMessageRes = R.string.worth_record_saved
                )
            }
        }
    }

    private fun applyRecordToForm(record: WorthRecord) {
        val country = WorthCountries.getByCode(record.countryCode)
        _uiState.update { current ->
            current.copy(
                salaryInput = record.rawSalary.ifBlank {
                    record.salaryText.filter { it.isDigit() || it == '.' }
                },
                selectedCountry = country,
                workDaysPerWeek = record.workDaysPerWeek,
                wfhDaysPerWeek = record.wfhDaysPerWeek,
                annualLeave = record.annualLeave,
                publicHolidays = record.publicHolidays,
                paidSickLeave = record.paidSickLeave,
                workHours = record.workHours,
                commuteHours = record.commuteHours,
                restTime = record.restTime,
                jobStability = JobStability.fromId(record.jobStabilityId),
                cityTier = CityTier.fromId(record.cityTierId),
                workEnvironment = WorkEnvironment.fromId(record.workEnvironmentId),
                leadership = LeadershipRelation.fromId(record.leadershipId),
                teamwork = TeamAtmosphere.fromId(record.teamworkId),
                isHometown = record.isHometown,
                hasShuttle = record.hasShuttle,
                shuttleQuality = ShuttleQuality.fromId(record.shuttleQualityId),
                hasCanteen = record.hasCanteen,
                canteenQuality = CanteenQuality.fromId(record.canteenQualityId),
                degreeType = DegreeType.fromId(record.degreeTypeId),
                schoolTier = SchoolTier.fromId(record.schoolTierId),
                bachelorTier = SchoolTier.fromId(record.bachelorTierId),
                workYears = WorkYears.fromId(record.workYearsId),
                currentScreen = WorthScreenMode.FORM,
                activeDetailRecord = null,
                userMessageRes = R.string.worth_apply_success
            )
        }
        persistDraft()
        recalculate()
    }

    private fun persistDraft() {
        val s = _uiState.value
        viewModelScope.launch {
            val draft = WorthCalculatorData(
                records = s.records,
                draftSalary = s.salaryInput,
                draftCountryCode = s.selectedCountry.code,
                draftWorkDaysPerWeek = s.workDaysPerWeek,
                draftWfhDaysPerWeek = s.wfhDaysPerWeek,
                draftAnnualLeave = s.annualLeave,
                draftPublicHolidays = s.publicHolidays,
                draftPaidSickLeave = s.paidSickLeave,
                draftWorkHours = s.workHours,
                draftCommuteHours = s.commuteHours,
                draftRestTime = s.restTime,
                draftJobStabilityId = s.jobStability.id,
                draftCityTierId = s.cityTier.id,
                draftWorkEnvironmentId = s.workEnvironment.id,
                draftLeadershipId = s.leadership.id,
                draftTeamworkId = s.teamwork.id,
                draftIsHometown = s.isHometown,
                draftHasShuttle = s.hasShuttle,
                draftShuttleQualityId = s.shuttleQuality.id,
                draftHasCanteen = s.hasCanteen,
                draftCanteenQualityId = s.canteenQuality.id,
                draftDegreeTypeId = s.degreeType.id,
                draftSchoolTierId = s.schoolTier.id,
                draftBachelorTierId = s.bachelorTier.id,
                draftWorkYearsId = s.workYears.id
            )
            repository.saveDraft(draft)
        }
    }
}
