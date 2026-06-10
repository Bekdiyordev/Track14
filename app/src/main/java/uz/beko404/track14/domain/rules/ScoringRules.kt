package uz.beko404.track14.domain.rules

object ScoringRules {
    const val DAILY_TEST_POINTS = 2
    const val MISSED_DAY_POINTS = -1
    const val LEAVE_TEST_POINTS = -5

    fun rankingScore(
        ownerScore: Int,
        testerCount: Int,
        requiredTesterCount: Int,
    ): Int {
        val testerNeedBoost = (requiredTesterCount - testerCount).coerceIn(0, requiredTesterCount)
        return ownerScore + testerNeedBoost
    }
}
