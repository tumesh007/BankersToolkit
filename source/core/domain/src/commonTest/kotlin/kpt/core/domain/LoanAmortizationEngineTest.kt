package kpt.core.domain

import kpt.core.domain.demo.calc.amortizationSchedule
import kpt.core.domain.demo.calc.computeEmi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoanAmortizationEngineTest {

    @Test
    fun testStandardEmiCalculation() {
        val principal = 500000.0
        val annualRate = 8.5
        val tenureMonths = 60

        val result = computeEmi(principal, annualRate, tenureMonths)

        assertTrue(result.emi > 10000.0 && result.emi < 11000.0)
        assertEquals(result.totalPayment, result.emi * tenureMonths, 0.01)
        assertEquals(result.totalInterest, result.totalPayment - principal, 0.01)
    }

    @Test
    fun testZeroInterestLoanEmi() {
        val principal = 120000.0
        val annualRate = 0.0
        val tenureMonths = 12

        val result = computeEmi(principal, annualRate, tenureMonths)

        assertEquals(10000.0, result.emi, 0.001)
        assertEquals(120000.0, result.totalPayment, 0.001)
        assertEquals(0.0, result.totalInterest, 0.001)
    }

    @Test
    fun testAmortizationScheduleConvergence() {
        val principal = 100000.0
        val annualRate = 10.0
        val tenureMonths = 12

        val schedule = amortizationSchedule(principal, annualRate, tenureMonths)

        assertEquals(12, schedule.size)
        val finalRow = schedule.last()
        assertTrue(finalRow.balanceRemaining < 1.0)
    }
}
