package com.nanami.koishi.feature.tools.worth_calculator.engine

import androidx.annotation.StringRes
import com.nanami.koishi.R
import kotlinx.serialization.Serializable

enum class JobStability(
    val id: String,
    @StringRes val labelRes: Int,
    val freshMultiplier: Double,
    val growthRate: Double
) {
    GOVERNMENT("government", R.string.worth_job_government, 0.8, 0.2),
    STATE("state", R.string.worth_job_state, 0.9, 0.4),
    FOREIGN("foreign", R.string.worth_job_foreign, 0.95, 0.8),
    PRIVATE("private", R.string.worth_job_private, 1.0, 1.0),
    DISPATCH("dispatch", R.string.worth_job_dispatch, 1.1, 1.2),
    FREELANCE("freelance", R.string.worth_job_freelance, 1.1, 1.2);

    companion object {
        fun fromId(id: String): JobStability = entries.find { it.id == id } ?: PRIVATE
    }
}

enum class CityTier(
    val id: String,
    @StringRes val labelRes: Int,
    val factor: Double
) {
    TIER1("tier1", R.string.worth_city_tier1, 0.70),
    NEW_TIER1("new_tier1", R.string.worth_city_newtier1, 0.80),
    TIER2("tier2", R.string.worth_city_tier2, 1.00),
    TIER3("tier3", R.string.worth_city_tier3, 1.10),
    TIER4("tier4", R.string.worth_city_tier4, 1.25),
    COUNTY("county", R.string.worth_city_county, 1.40),
    TOWN("town", R.string.worth_city_town, 1.50);

    companion object {
        fun fromId(id: String): CityTier = entries.find { it.id == id } ?: TIER2
    }
}

enum class WorkEnvironment(
    val id: String,
    @StringRes val labelRes: Int,
    val factor: Double
) {
    REMOTE("remote", R.string.worth_env_remote, 0.8),
    FACTORY("factory", R.string.worth_env_factory, 0.9),
    NORMAL("normal", R.string.worth_env_normal, 1.0),
    CBD("cbd", R.string.worth_env_cbd, 1.1);

    companion object {
        fun fromId(id: String): WorkEnvironment = entries.find { it.id == id } ?: NORMAL
    }
}

enum class LeadershipRelation(
    val id: String,
    @StringRes val labelRes: Int,
    val factor: Double
) {
    BAD("bad", R.string.worth_leader_bad, 0.7),
    STRICT("strict", R.string.worth_leader_strict, 0.9),
    NORMAL("normal", R.string.worth_leader_normal, 1.0),
    GOOD("good", R.string.worth_leader_good, 1.1),
    FAVORITE("favorite", R.string.worth_leader_favorite, 1.3);

    companion object {
        fun fromId(id: String): LeadershipRelation = entries.find { it.id == id } ?: NORMAL
    }
}

enum class TeamAtmosphere(
    val id: String,
    @StringRes val labelRes: Int,
    val factor: Double
) {
    BAD("bad", R.string.worth_team_bad, 0.9),
    NORMAL("normal", R.string.worth_team_normal, 1.0),
    GOOD("good", R.string.worth_team_good, 1.1),
    EXCELLENT("excellent", R.string.worth_team_excellent, 1.2);

    companion object {
        fun fromId(id: String): TeamAtmosphere = entries.find { it.id == id } ?: NORMAL
    }
}

enum class ShuttleQuality(
    val id: String,
    @StringRes val labelRes: Int,
    val factor: Double
) {
    NONE("none", R.string.worth_shuttle_none, 1.0),
    INCONVENIENT("inconvenient", R.string.worth_shuttle_inconvenient, 0.9),
    CONVENIENT("convenient", R.string.worth_shuttle_convenient, 0.7),
    DIRECT("direct", R.string.worth_shuttle_direct, 0.5);

    companion object {
        fun fromId(id: String): ShuttleQuality = entries.find { it.id == id } ?: NONE
    }
}

enum class CanteenQuality(
    val id: String,
    @StringRes val labelRes: Int,
    val factor: Double
) {
    NONE("none", R.string.worth_canteen_none, 1.0),
    AVERAGE("average", R.string.worth_canteen_average, 1.05),
    GOOD("good", R.string.worth_canteen_good, 1.10),
    EXCELLENT("excellent", R.string.worth_canteen_excellent, 1.15);

    companion object {
        fun fromId(id: String): CanteenQuality = entries.find { it.id == id } ?: NONE
    }
}

enum class DegreeType(
    val id: String,
    @StringRes val labelRes: Int
) {
    BELOW_BACHELOR("below_bachelor", R.string.worth_degree_below_bachelor),
    BACHELOR("bachelor", R.string.worth_degree_bachelor),
    MASTERS("masters", R.string.worth_degree_masters),
    PHD("phd", R.string.worth_degree_phd);

    companion object {
        fun fromId(id: String): DegreeType = entries.find { it.id == id } ?: BACHELOR
    }
}

enum class SchoolTier(
    val id: String,
    @StringRes val bachelorLabelRes: Int,
    @StringRes val postgradLabelRes: Int
) {
    SECOND_TIER("second_tier", R.string.worth_school_second_tier_bachelor, R.string.worth_school_second_tier_higher),
    FIRST_TIER("first_tier", R.string.worth_school_first_tier_bachelor, R.string.worth_school_first_tier_higher),
    ELITE("elite", R.string.worth_school_elite_bachelor, R.string.worth_school_elite_higher);

    companion object {
        fun fromId(id: String): SchoolTier = entries.find { it.id == id } ?: FIRST_TIER
    }
}

enum class WorkYears(
    val id: String,
    @StringRes val labelRes: Int,
    val baseMultiplier: Double
) {
    FRESH("0", R.string.worth_years_fresh, 1.0),
    YEARS_1_3("1", R.string.worth_years_1_3, 1.5),
    YEARS_3_5("2", R.string.worth_years_3_5, 2.2),
    YEARS_5_8("4", R.string.worth_years_5_8, 2.7),
    YEARS_8_10("6", R.string.worth_years_8_10, 3.2),
    YEARS_10_12("10", R.string.worth_years_10_12, 3.6),
    YEARS_ABOVE_12("15", R.string.worth_years_above_12, 3.9);

    companion object {
        fun fromId(id: String): WorkYears = entries.find { it.id == id } ?: FRESH
    }
}

enum class WorthAssessmentLevel(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int
) {
    TERRIBLE("terrible", R.string.worth_rating_terrible, R.string.worth_comment_terrible),
    POOR("poor", R.string.worth_rating_poor, R.string.worth_comment_poor),
    AVERAGE("average", R.string.worth_rating_average, R.string.worth_comment_average),
    GOOD("good", R.string.worth_rating_good, R.string.worth_comment_good),
    GREAT("great", R.string.worth_rating_great, R.string.worth_comment_great),
    EXCELLENT("excellent", R.string.worth_rating_excellent, R.string.worth_comment_excellent),
    PERFECT("perfect", R.string.worth_rating_perfect, R.string.worth_comment_perfect);

    companion object {
        fun fromScore(score: Double): WorthAssessmentLevel = when {
            score < 0.6 -> TERRIBLE
            score < 1.0 -> POOR
            score <= 1.8 -> AVERAGE
            score <= 2.5 -> GOOD
            score <= 3.2 -> GREAT
            score <= 4.0 -> EXCELLENT
            else -> PERFECT
        }
    }
}

data class WorthCountry(
    val code: String,
    @StringRes val nameRes: Int,
    val currencySymbol: String,
    val pppFactor: Double
)

data class WorthCalculationResult(
    val score: Double,
    val assessment: WorthAssessmentLevel,
    val workingDaysPerYear: Double,
    val dailySalary: Double,
    val effectiveDailyHours: Double,
    val environmentFactor: Double,
    val educationFactor: Double,
    val experienceMultiplier: Double,
    val adviceList: List<Int>
)

@Serializable
data class WorthRecord(
    val id: String,
    val timestamp: Long,
    val title: String,
    val score: Double,
    val assessmentId: String,
    val salaryText: String,
    val rawSalary: String = "",
    val countryCode: String,
    val dailySalaryText: String,
    val workingDaysText: String,
    val effectiveHoursText: String = "",
    val environmentFactor: Double = 1.0,
    val educationFactor: Double = 1.0,
    val experienceMultiplier: Double = 1.0,
    val adviceList: List<Int> = emptyList(),
    val workDaysPerWeek: String = "5",
    val wfhDaysPerWeek: String = "0",
    val annualLeave: String = "5",
    val publicHolidays: String = "13",
    val paidSickLeave: String = "3",
    val workHours: String = "10",
    val commuteHours: String = "2",
    val restTime: String = "2",
    val jobStabilityId: String = "private",
    val cityTierId: String = "tier2",
    val workEnvironmentId: String = "normal",
    val leadershipId: String = "normal",
    val teamworkId: String = "normal",
    val isHometown: Boolean = false,
    val hasShuttle: Boolean = false,
    val shuttleQualityId: String = "none",
    val hasCanteen: Boolean = false,
    val canteenQualityId: String = "none",
    val degreeTypeId: String = "bachelor",
    val schoolTierId: String = "first_tier",
    val bachelorTierId: String = "first_tier",
    val workYearsId: String = "0"
)

@Serializable
data class WorthCalculatorData(
    val records: List<WorthRecord> = emptyList(),
    val draftSalary: String = "",
    val draftCountryCode: String = "CN",
    val draftWorkDaysPerWeek: String = "5",
    val draftWfhDaysPerWeek: String = "0",
    val draftAnnualLeave: String = "5",
    val draftPublicHolidays: String = "13",
    val draftPaidSickLeave: String = "3",
    val draftWorkHours: String = "10",
    val draftCommuteHours: String = "2",
    val draftRestTime: String = "2",
    val draftJobStabilityId: String = "private",
    val draftCityTierId: String = "tier2",
    val draftWorkEnvironmentId: String = "normal",
    val draftLeadershipId: String = "normal",
    val draftTeamworkId: String = "normal",
    val draftIsHometown: Boolean = false,
    val draftHasShuttle: Boolean = false,
    val draftShuttleQualityId: String = "none",
    val draftHasCanteen: Boolean = false,
    val draftCanteenQualityId: String = "none",
    val draftDegreeTypeId: String = "bachelor",
    val draftSchoolTierId: String = "first_tier",
    val draftBachelorTierId: String = "first_tier",
    val draftWorkYearsId: String = "0"
)
