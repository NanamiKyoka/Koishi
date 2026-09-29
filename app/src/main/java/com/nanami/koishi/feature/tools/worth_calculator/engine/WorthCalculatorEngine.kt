package com.nanami.koishi.feature.tools.worth_calculator.engine

import com.nanami.koishi.R
import kotlin.math.max

object WorthCalculatorEngine {

    private const val BASELINE_HOURLY_DENOMINATOR = 35.0
    private const val BASELINE_PPP_CHINA = 4.19

    fun calculateWorkingDays(
        workDaysPerWeek: Double,
        annualLeave: Double,
        publicHolidays: Double,
        paidSickLeave: Double
    ): Double {
        val totalWorkDays = 52.0 * workDaysPerWeek
        val totalLeaves = annualLeave + publicHolidays + paidSickLeave * 0.6
        return max(totalWorkDays - totalLeaves, 1.0)
    }

    fun calculateEffectiveCommuteHours(
        commuteHours: Double,
        workDaysPerWeek: Double,
        wfhDaysPerWeek: Double,
        hasShuttle: Boolean,
        shuttleQuality: ShuttleQuality
    ): Double {
        val clampedWfh = wfhDaysPerWeek.coerceIn(0.0, workDaysPerWeek)
        val officeRatio = if (workDaysPerWeek > 0.0) (workDaysPerWeek - clampedWfh) / workDaysPerWeek else 0.0
        val shuttleFactor = if (hasShuttle) shuttleQuality.factor else 1.0
        return commuteHours * officeRatio * shuttleFactor
    }

    fun calculateEnvironmentFactor(
        workEnvironment: WorkEnvironment,
        leadership: LeadershipRelation,
        teamwork: TeamAtmosphere,
        cityTier: CityTier,
        hasCanteen: Boolean,
        canteenQuality: CanteenQuality
    ): Double {
        val canteenFactor = if (hasCanteen) canteenQuality.factor else 1.0
        return workEnvironment.factor * leadership.factor * teamwork.factor * cityTier.factor * canteenFactor
    }

    fun calculateEducationFactor(
        degreeType: DegreeType,
        schoolTier: SchoolTier,
        bachelorTier: SchoolTier
    ): Double {
        return when (degreeType) {
            DegreeType.BELOW_BACHELOR -> 0.8
            DegreeType.BACHELOR -> when (schoolTier) {
                SchoolTier.SECOND_TIER -> 0.9
                SchoolTier.FIRST_TIER -> 1.0
                SchoolTier.ELITE -> 1.2
            }
            DegreeType.MASTERS -> {
                val bachelorBase = when (bachelorTier) {
                    SchoolTier.SECOND_TIER -> 0.9
                    SchoolTier.FIRST_TIER -> 1.0
                    SchoolTier.ELITE -> 1.2
                }
                val mastersBonus = when (schoolTier) {
                    SchoolTier.SECOND_TIER -> 0.4
                    SchoolTier.FIRST_TIER -> 0.5
                    SchoolTier.ELITE -> 0.6
                }
                bachelorBase + mastersBonus
            }
            DegreeType.PHD -> when (schoolTier) {
                SchoolTier.SECOND_TIER -> 1.6
                SchoolTier.FIRST_TIER -> 1.8
                SchoolTier.ELITE -> 2.0
            }
        }
    }

    fun calculateExperienceMultiplier(
        workYears: WorkYears,
        jobStability: JobStability
    ): Double {
        return if (workYears == WorkYears.FRESH) {
            jobStability.freshMultiplier
        } else {
            1.0 + (workYears.baseMultiplier - 1.0) * jobStability.growthRate
        }
    }

    fun calculate(
        salary: Double,
        country: WorthCountry,
        workDaysPerWeek: Double,
        wfhDaysPerWeek: Double,
        annualLeave: Double,
        publicHolidays: Double,
        paidSickLeave: Double,
        workHours: Double,
        commuteHours: Double,
        restTime: Double,
        jobStability: JobStability,
        cityTier: CityTier,
        workEnvironment: WorkEnvironment,
        leadership: LeadershipRelation,
        teamwork: TeamAtmosphere,
        isHometown: Boolean,
        hasShuttle: Boolean,
        shuttleQuality: ShuttleQuality,
        hasCanteen: Boolean,
        canteenQuality: CanteenQuality,
        degreeType: DegreeType,
        schoolTier: SchoolTier,
        bachelorTier: SchoolTier,
        workYears: WorkYears
    ): WorthCalculationResult {
        if (salary <= 0.0) {
            return WorthCalculationResult(
                score = 0.0,
                assessment = WorthAssessmentLevel.AVERAGE,
                workingDaysPerYear = 0.0,
                dailySalary = 0.0,
                effectiveDailyHours = 0.0,
                environmentFactor = 1.0,
                educationFactor = 1.0,
                experienceMultiplier = 1.0,
                adviceList = emptyList()
            )
        }

        val workingDays = calculateWorkingDays(workDaysPerWeek, annualLeave, publicHolidays, paidSickLeave)
        val dailySalaryRaw = salary / workingDays

        val pppRatio = if (country.code == "CN") 1.0 else BASELINE_PPP_CHINA / country.pppFactor
        val standardizedDailySalary = dailySalaryRaw * pppRatio

        val effectiveCommute = calculateEffectiveCommuteHours(commuteHours, workDaysPerWeek, wfhDaysPerWeek, hasShuttle, shuttleQuality)
        val effectiveDailyHours = max(workHours + effectiveCommute - 0.5 * restTime, 0.5)

        val envFactor = calculateEnvironmentFactor(workEnvironment, leadership, teamwork, cityTier, hasCanteen, canteenQuality)
        val eduFactor = calculateEducationFactor(degreeType, schoolTier, bachelorTier)
        val expMultiplier = calculateExperienceMultiplier(workYears, jobStability)

        val denominator = BASELINE_HOURLY_DENOMINATOR * effectiveDailyHours * eduFactor * expMultiplier
        val score = if (denominator > 0.0) (standardizedDailySalary * envFactor) / denominator else 0.0

        val assessment = WorthAssessmentLevel.fromScore(score)
        val adviceList = generateAdviceList(
            score = score,
            isHometown = isHometown,
            cityTier = cityTier,
            commuteHours = commuteHours,
            wfhDaysPerWeek = wfhDaysPerWeek,
            hasShuttle = hasShuttle,
            leadership = leadership,
            teamwork = teamwork,
            workHours = workHours
        )

        return WorthCalculationResult(
            score = score,
            assessment = assessment,
            workingDaysPerYear = workingDays,
            dailySalary = dailySalaryRaw,
            effectiveDailyHours = effectiveDailyHours,
            environmentFactor = envFactor,
            educationFactor = eduFactor,
            experienceMultiplier = expMultiplier,
            adviceList = adviceList
        )
    }

    private fun generateAdviceList(
        score: Double,
        isHometown: Boolean,
        cityTier: CityTier,
        commuteHours: Double,
        wfhDaysPerWeek: Double,
        hasShuttle: Boolean,
        leadership: LeadershipRelation,
        teamwork: TeamAtmosphere,
        workHours: Double
    ): List<Int> {
        val list = mutableListOf<Int>()

        if (isHometown) {
            list.add(R.string.worth_advice_hometown)
        } else {
            list.add(R.string.worth_advice_not_hometown)
        }

        when (cityTier) {
            CityTier.TIER1, CityTier.NEW_TIER1 -> list.add(R.string.worth_advice_tier1_city)
            CityTier.TIER2, CityTier.TIER3 -> list.add(R.string.worth_advice_tier2_city)
            else -> list.add(R.string.worth_advice_lower_city)
        }

        when {
            commuteHours <= 1.0 -> list.add(R.string.worth_advice_commute_short)
            commuteHours <= 2.5 -> list.add(R.string.worth_advice_commute_medium)
            else -> list.add(R.string.worth_advice_commute_long)
        }

        if (wfhDaysPerWeek >= 2.0) {
            list.add(R.string.worth_advice_wfh_high)
        } else if (hasShuttle) {
            list.add(R.string.worth_advice_shuttle_good)
        }

        when (leadership) {
            LeadershipRelation.FAVORITE -> list.add(R.string.worth_advice_leader_favorite)
            LeadershipRelation.GOOD -> list.add(R.string.worth_advice_leader_good)
            LeadershipRelation.BAD -> list.add(R.string.worth_advice_leader_bad)
            else -> Unit
        }

        when (teamwork) {
            TeamAtmosphere.EXCELLENT -> list.add(R.string.worth_advice_team_excellent)
            TeamAtmosphere.GOOD -> list.add(R.string.worth_advice_team_good)
            TeamAtmosphere.BAD -> list.add(R.string.worth_advice_team_bad)
            else -> Unit
        }

        when {
            workHours <= 8.5 -> list.add(R.string.worth_advice_workhours_balanced)
            workHours <= 10.5 -> list.add(R.string.worth_advice_workhours_long)
            else -> list.add(R.string.worth_advice_workhours_excessive)
        }

        when {
            score < 1.0 -> list.add(R.string.worth_advice_score_low)
            score <= 2.5 -> list.add(R.string.worth_advice_score_medium)
            else -> list.add(R.string.worth_advice_score_high)
        }

        return list
    }
}
