package com.nanami.koishi.feature.tools.worth_calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorthCalculatorEngineTest {

    @Test
    fun testCalculateWorkingDays() {
        val days = WorthCalculatorEngine.calculateWorkingDays(
            workDaysPerWeek = 5.0,
            annualLeave = 10.0,
            publicHolidays = 11.0,
            paidSickLeave = 5.0
        )
        val expected = 52.0 * 5.0 - (10.0 + 11.0 + 5.0 * 0.6)
        assertEquals(expected, days, 0.001)
    }

    @Test
    fun testCalculateEffectiveCommuteHours() {
        val commuteNormal = WorthCalculatorEngine.calculateEffectiveCommuteHours(
            commuteHours = 2.0,
            workDaysPerWeek = 5.0,
            wfhDaysPerWeek = 0.0,
            hasShuttle = false,
            shuttleQuality = ShuttleQuality.NONE
        )
        assertEquals(2.0, commuteNormal, 0.001)

        val commuteWithWfh = WorthCalculatorEngine.calculateEffectiveCommuteHours(
            commuteHours = 2.0,
            workDaysPerWeek = 5.0,
            wfhDaysPerWeek = 2.0,
            hasShuttle = false,
            shuttleQuality = ShuttleQuality.NONE
        )
        assertEquals(1.2, commuteWithWfh, 0.001)

        val commuteWithShuttle = WorthCalculatorEngine.calculateEffectiveCommuteHours(
            commuteHours = 2.0,
            workDaysPerWeek = 5.0,
            wfhDaysPerWeek = 0.0,
            hasShuttle = true,
            shuttleQuality = ShuttleQuality.DIRECT
        )
        assertEquals(1.0, commuteWithShuttle, 0.001)
    }

    @Test
    fun testCalculateEducationFactor() {
        val bachelorFirstTier = WorthCalculatorEngine.calculateEducationFactor(
            degreeType = DegreeType.BACHELOR,
            schoolTier = SchoolTier.FIRST_TIER,
            bachelorTier = SchoolTier.FIRST_TIER
        )
        assertEquals(1.0, bachelorFirstTier, 0.001)

        val mastersElite = WorthCalculatorEngine.calculateEducationFactor(
            degreeType = DegreeType.MASTERS,
            schoolTier = SchoolTier.ELITE,
            bachelorTier = SchoolTier.ELITE
        )
        assertEquals(1.8, mastersElite, 0.001)

        val belowBachelor = WorthCalculatorEngine.calculateEducationFactor(
            degreeType = DegreeType.BELOW_BACHELOR,
            schoolTier = SchoolTier.FIRST_TIER,
            bachelorTier = SchoolTier.FIRST_TIER
        )
        assertEquals(0.8, belowBachelor, 0.001)
    }

    @Test
    fun testCalculateExperienceMultiplier() {
        val freshState = WorthCalculatorEngine.calculateExperienceMultiplier(
            workYears = WorkYears.FRESH,
            jobStability = JobStability.STATE
        )
        assertEquals(0.9, freshState, 0.001)

        val midPrivate = WorthCalculatorEngine.calculateExperienceMultiplier(
            workYears = WorkYears.YEARS_3_5,
            jobStability = JobStability.PRIVATE
        )
        val expected = 1.0 + (WorkYears.YEARS_3_5.baseMultiplier - 1.0) * JobStability.PRIVATE.growthRate
        assertEquals(expected, midPrivate, 0.001)
    }

    @Test
    fun testCalculateZeroOrNegativeSalary() {
        val resultZero = WorthCalculatorEngine.calculate(
            salary = 0.0,
            country = WorthCountries.getByCode("CN"),
            workDaysPerWeek = 5.0,
            wfhDaysPerWeek = 0.0,
            annualLeave = 10.0,
            publicHolidays = 11.0,
            paidSickLeave = 0.0,
            workHours = 8.0,
            commuteHours = 1.0,
            restTime = 1.0,
            jobStability = JobStability.PRIVATE,
            cityTier = CityTier.TIER2,
            workEnvironment = WorkEnvironment.NORMAL,
            leadership = LeadershipRelation.NORMAL,
            teamwork = TeamAtmosphere.NORMAL,
            isHometown = true,
            hasShuttle = false,
            shuttleQuality = ShuttleQuality.NONE,
            hasCanteen = false,
            canteenQuality = CanteenQuality.NONE,
            degreeType = DegreeType.BACHELOR,
            schoolTier = SchoolTier.FIRST_TIER,
            bachelorTier = SchoolTier.FIRST_TIER,
            workYears = WorkYears.YEARS_1_3
        )

        assertEquals(0.0, resultZero.score, 0.001)
        assertEquals(0.0, resultZero.dailySalary, 0.001)
    }

    @Test
    fun testCalculatePositiveScenario() {
        val country = WorthCountries.getByCode("CN")
        val result = WorthCalculatorEngine.calculate(
            salary = 150000.0,
            country = country,
            workDaysPerWeek = 5.0,
            wfhDaysPerWeek = 0.0,
            annualLeave = 10.0,
            publicHolidays = 11.0,
            paidSickLeave = 5.0,
            workHours = 8.0,
            commuteHours = 1.0,
            restTime = 1.0,
            jobStability = JobStability.PRIVATE,
            cityTier = CityTier.TIER2,
            workEnvironment = WorkEnvironment.NORMAL,
            leadership = LeadershipRelation.NORMAL,
            teamwork = TeamAtmosphere.NORMAL,
            isHometown = true,
            hasShuttle = false,
            shuttleQuality = ShuttleQuality.NONE,
            hasCanteen = false,
            canteenQuality = CanteenQuality.NONE,
            degreeType = DegreeType.BACHELOR,
            schoolTier = SchoolTier.FIRST_TIER,
            bachelorTier = SchoolTier.FIRST_TIER,
            workYears = WorkYears.YEARS_1_3
        )

        assertTrue(result.score > 0.0)
        assertTrue(result.workingDaysPerYear > 200.0)
        assertTrue(result.dailySalary > 0.0)
        assertTrue(result.adviceList.isNotEmpty())
    }

    @Test
    fun testPppStandardization() {
        val cn = WorthCountries.getByCode("CN")
        val us = WorthCountries.getByCode("US")

        val resultCn = WorthCalculatorEngine.calculate(
            salary = 100000.0,
            country = cn,
            workDaysPerWeek = 5.0,
            wfhDaysPerWeek = 0.0,
            annualLeave = 10.0,
            publicHolidays = 11.0,
            paidSickLeave = 0.0,
            workHours = 8.0,
            commuteHours = 1.0,
            restTime = 1.0,
            jobStability = JobStability.PRIVATE,
            cityTier = CityTier.TIER2,
            workEnvironment = WorkEnvironment.NORMAL,
            leadership = LeadershipRelation.NORMAL,
            teamwork = TeamAtmosphere.NORMAL,
            isHometown = true,
            hasShuttle = false,
            shuttleQuality = ShuttleQuality.NONE,
            hasCanteen = false,
            canteenQuality = CanteenQuality.NONE,
            degreeType = DegreeType.BACHELOR,
            schoolTier = SchoolTier.FIRST_TIER,
            bachelorTier = SchoolTier.FIRST_TIER,
            workYears = WorkYears.YEARS_1_3
        )

        val resultUs = WorthCalculatorEngine.calculate(
            salary = 100000.0,
            country = us,
            workDaysPerWeek = 5.0,
            wfhDaysPerWeek = 0.0,
            annualLeave = 10.0,
            publicHolidays = 11.0,
            paidSickLeave = 0.0,
            workHours = 8.0,
            commuteHours = 1.0,
            restTime = 1.0,
            jobStability = JobStability.PRIVATE,
            cityTier = CityTier.TIER2,
            workEnvironment = WorkEnvironment.NORMAL,
            leadership = LeadershipRelation.NORMAL,
            teamwork = TeamAtmosphere.NORMAL,
            isHometown = true,
            hasShuttle = false,
            shuttleQuality = ShuttleQuality.NONE,
            hasCanteen = false,
            canteenQuality = CanteenQuality.NONE,
            degreeType = DegreeType.BACHELOR,
            schoolTier = SchoolTier.FIRST_TIER,
            bachelorTier = SchoolTier.FIRST_TIER,
            workYears = WorkYears.YEARS_1_3
        )

        val ratio = resultUs.score / resultCn.score
        val expectedRatio = 4.19 / us.pppFactor
        assertEquals(expectedRatio, ratio, 0.01)
    }
}
