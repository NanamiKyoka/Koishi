package com.nanami.koishi.feature.tools.worth_calculator

import androidx.annotation.StringRes
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
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthCalculationResult
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthCountries
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthCountry
import com.nanami.koishi.feature.tools.worth_calculator.engine.WorthRecord

enum class WorthScreenMode {
    FORM,
    RESULT
}

data class WorthCalculatorUiState(
    val currentScreen: WorthScreenMode = WorthScreenMode.FORM,
    val activeDetailRecord: WorthRecord? = null,
    val salaryInput: String = "150000",
    val selectedCountry: WorthCountry = WorthCountries.supportedCountries[0],
    val workDaysPerWeek: String = "5",
    val wfhDaysPerWeek: String = "0",
    val annualLeave: String = "5",
    val publicHolidays: String = "13",
    val paidSickLeave: String = "3",
    val workHours: String = "10",
    val commuteHours: String = "2",
    val restTime: String = "2",
    val jobStability: JobStability = JobStability.PRIVATE,
    val cityTier: CityTier = CityTier.TIER2,
    val workEnvironment: WorkEnvironment = WorkEnvironment.NORMAL,
    val leadership: LeadershipRelation = LeadershipRelation.NORMAL,
    val teamwork: TeamAtmosphere = TeamAtmosphere.NORMAL,
    val isHometown: Boolean = false,
    val hasShuttle: Boolean = false,
    val shuttleQuality: ShuttleQuality = ShuttleQuality.CONVENIENT,
    val hasCanteen: Boolean = false,
    val canteenQuality: CanteenQuality = CanteenQuality.GOOD,
    val degreeType: DegreeType = DegreeType.BACHELOR,
    val schoolTier: SchoolTier = SchoolTier.FIRST_TIER,
    val bachelorTier: SchoolTier = SchoolTier.FIRST_TIER,
    val workYears: WorkYears = WorkYears.YEARS_1_3,
    val result: WorthCalculationResult? = null,
    val records: List<WorthRecord> = emptyList(),
    val isFormulaDialogOpen: Boolean = false,
    val isHistorySheetOpen: Boolean = false,
    @StringRes val userMessageRes: Int? = null,
    val userMessageArgs: List<String> = emptyList()
)

sealed interface WorthCalculatorUiEvent {
    data class OnSalaryChange(val value: String) : WorthCalculatorUiEvent
    data class OnCountryChange(val country: WorthCountry) : WorthCalculatorUiEvent
    data class OnWorkDaysChange(val value: String) : WorthCalculatorUiEvent
    data class OnWfhDaysChange(val value: String) : WorthCalculatorUiEvent
    data class OnAnnualLeaveChange(val value: String) : WorthCalculatorUiEvent
    data class OnPublicHolidaysChange(val value: String) : WorthCalculatorUiEvent
    data class OnPaidSickLeaveChange(val value: String) : WorthCalculatorUiEvent
    data class OnWorkHoursChange(val value: String) : WorthCalculatorUiEvent
    data class OnCommuteHoursChange(val value: String) : WorthCalculatorUiEvent
    data class OnRestTimeChange(val value: String) : WorthCalculatorUiEvent
    data class OnJobStabilityChange(val stability: JobStability) : WorthCalculatorUiEvent
    data class OnCityTierChange(val tier: CityTier) : WorthCalculatorUiEvent
    data class OnWorkEnvironmentChange(val environment: WorkEnvironment) : WorthCalculatorUiEvent
    data class OnLeadershipChange(val relation: LeadershipRelation) : WorthCalculatorUiEvent
    data class OnTeamworkChange(val atmosphere: TeamAtmosphere) : WorthCalculatorUiEvent
    data class OnIsHometownChange(val value: Boolean) : WorthCalculatorUiEvent
    data class OnHasShuttleChange(val value: Boolean) : WorthCalculatorUiEvent
    data class OnShuttleQualityChange(val quality: ShuttleQuality) : WorthCalculatorUiEvent
    data class OnHasCanteenChange(val value: Boolean) : WorthCalculatorUiEvent
    data class OnCanteenQualityChange(val quality: CanteenQuality) : WorthCalculatorUiEvent
    data class OnDegreeTypeChange(val degree: DegreeType) : WorthCalculatorUiEvent
    data class OnSchoolTierChange(val tier: SchoolTier) : WorthCalculatorUiEvent
    data class OnBachelorTierChange(val tier: SchoolTier) : WorthCalculatorUiEvent
    data class OnWorkYearsChange(val years: WorkYears) : WorthCalculatorUiEvent
    data object OnCalculate : WorthCalculatorUiEvent
    data object OnNavigateBackToForm : WorthCalculatorUiEvent
    data class OnSelectRecord(val record: WorthRecord) : WorthCalculatorUiEvent
    data class OnApplyRecord(val record: WorthRecord) : WorthCalculatorUiEvent
    data class OnSaveRecord(val title: String) : WorthCalculatorUiEvent
    data class OnDeleteRecord(val recordId: String) : WorthCalculatorUiEvent
    data object OnClearRecords : WorthCalculatorUiEvent
    data class OnToggleFormulaDialog(val open: Boolean) : WorthCalculatorUiEvent
    data class OnToggleHistorySheet(val open: Boolean) : WorthCalculatorUiEvent
    data object OnDismissMessage : WorthCalculatorUiEvent
}
