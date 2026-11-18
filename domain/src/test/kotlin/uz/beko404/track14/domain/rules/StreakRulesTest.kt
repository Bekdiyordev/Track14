package uz.beko404.track14.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import uz.beko404.track14.domain.model.DayStatus
import uz.beko404.track14.domain.model.DayStatus.MISSED
import uz.beko404.track14.domain.model.DayStatus.PASSED
import uz.beko404.track14.domain.model.DayStatus.PENDING
import uz.beko404.track14.domain.model.TestFailure
import uz.beko404.track14.domain.model.TestOutcome

class StreakRulesTest {

    private fun days(vararg head: DayStatus, fillWith: DayStatus = PENDING): List<DayStatus> =
        head.toList() + List(StreakRules.TEST_DAYS - head.size) { fillWith }

    @Test
    fun `fresh test is active`() {
        assertEquals(TestOutcome.Active, StreakRules.evaluate(emptyList()))
        assertEquals(TestOutcome.Active, StreakRules.evaluate(days()))
    }

    @Test
    fun `one missed day keeps the test going`() {
        assertEquals(TestOutcome.Active, StreakRules.evaluate(days(PASSED, MISSED, PASSED)))
    }

    @Test
    fun `two missed days in a row fail the test`() {
        val outcome = StreakRules.evaluate(days(PASSED, MISSED, MISSED))
        assertEquals(TestOutcome.Failed(TestFailure.TOO_MANY_MISSED), outcome)
    }

    @Test
    fun `three missed days in total fail the test`() {
        val outcome = StreakRules.evaluate(days(MISSED, PASSED, MISSED, PASSED, MISSED))
        assertEquals(TestOutcome.Failed(TestFailure.TOO_MANY_MISSED), outcome)
    }

    @Test
    fun `two separated missed days are tolerated`() {
        assertEquals(
            TestOutcome.Active,
            StreakRules.evaluate(days(MISSED, PASSED, MISSED, PASSED)),
        )
    }

    @Test
    fun `fourteen green days complete the test`() {
        val all = List(StreakRules.TEST_DAYS) { PASSED }
        assertEquals(TestOutcome.Completed, StreakRules.evaluate(all))
    }

    @Test
    fun `twelve green and two scattered misses still complete`() {
        val list = days(PASSED, MISSED, PASSED, PASSED, MISSED, fillWith = PASSED)
        assertEquals(TestOutcome.Completed, StreakRules.evaluate(list))
    }

    @Test
    fun `a pending day keeps a full list active`() {
        val list = List(StreakRules.TEST_DAYS - 1) { PASSED } + PENDING
        assertEquals(TestOutcome.Active, StreakRules.evaluate(list))
    }

    @Test
    fun `more than fourteen days is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            StreakRules.evaluate(List(StreakRules.TEST_DAYS + 1) { PASSED })
        }
    }
}
