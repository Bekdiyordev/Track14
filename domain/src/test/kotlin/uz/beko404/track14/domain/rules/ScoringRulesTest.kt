package uz.beko404.track14.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.model.Plan
import uz.beko404.track14.domain.model.TestFailure
import uz.beko404.track14.domain.model.TestOutcome
import uz.beko404.track14.domain.model.coins

class ScoringRulesTest {
    private val rules = ScoringRules()

    @Test
    fun `day reward depends on plan`() {
        assertEquals(3.coins, rules.dayReward(Plan.STANDARD))
        assertEquals(5.coins, rules.dayReward(Plan.PRO))
    }

    @Test
    fun `completion bonus only on completed tests`() {
        assertEquals(10.coins, rules.completionBonus(TestOutcome.Completed))
        assertEquals(Coins.ZERO, rules.completionBonus(TestOutcome.Active))
        assertEquals(
            Coins.ZERO,
            rules.completionBonus(TestOutcome.Failed(TestFailure.TOO_MANY_MISSED)),
        )
    }

    @Test
    fun `feedback pays once per app`() {
        assertEquals(2.coins, rules.feedbackReward(alreadyRewardedForThisApp = false))
        assertEquals(Coins.ZERO, rules.feedbackReward(alreadyRewardedForThisApp = true))
    }

    @Test
    fun `penalties are negative and depend on the reason`() {
        assertEquals(
            (-10).coins,
            rules.penalty(TestOutcome.Failed(TestFailure.TOO_MANY_MISSED)),
        )
        assertEquals((-15).coins, rules.penalty(TestOutcome.Failed(TestFailure.LEFT_TEST)))
        assertEquals(Coins.ZERO, rules.penalty(TestOutcome.Completed))
    }

    @Test
    fun `listing needs enough balance`() {
        assertTrue(rules.canAffordListing(1_000.coins, Plan.STANDARD))
        assertFalse(rules.canAffordListing(999.coins, Plan.STANDARD))
        assertFalse(rules.canAffordListing(1_799.coins, Plan.PRO))
        assertTrue(rules.canAffordListing(1_800.coins, Plan.PRO))
    }

    @Test
    fun `negative balance cannot spend`() {
        assertFalse(rules.canSpend((-1).coins))
        assertTrue(rules.canSpend(Coins.ZERO))
    }
}
