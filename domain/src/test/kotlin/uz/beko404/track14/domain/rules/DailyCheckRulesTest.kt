package uz.beko404.track14.domain.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyCheckRulesTest {
    @Test
    fun `thirty seconds is the minimum`() {
        assertFalse(DailyCheckRules.isPassed(29))
        assertTrue(DailyCheckRules.isPassed(30))
        assertTrue(DailyCheckRules.isPassed(300))
    }
}
