package uz.beko404.track14.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoringRulesTest {
    @Test
    fun rankingScoreAddsTesterNeedBoostToOwnerScore() {
        val score = ScoringRules.rankingScore(
            ownerScore = 10,
            testerCount = 7,
            requiredTesterCount = 12,
        )

        assertEquals(15, score)
    }

    @Test
    fun rankingScoreDoesNotApplyNegativeTesterNeedBoost() {
        val score = ScoringRules.rankingScore(
            ownerScore = 10,
            testerCount = 15,
            requiredTesterCount = 12,
        )

        assertEquals(10, score)
    }

    @Test
    fun scoreConstantsMatchMvpRules() {
        assertEquals(2, ScoringRules.DAILY_TEST_POINTS)
        assertEquals(-1, ScoringRules.MISSED_DAY_POINTS)
        assertEquals(-5, ScoringRules.LEAVE_TEST_POINTS)
    }
}
