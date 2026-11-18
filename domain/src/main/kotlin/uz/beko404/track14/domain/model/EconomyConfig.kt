package uz.beko404.track14.domain.model

/**
 * Economy numbers. The real values live in Firestore `config/economy` and Remote
 * Config; the defaults below only mirror the agreed starting values and are used
 * for tests and as an offline fallback. All numbers are tuned together at the end.
 */
data class EconomyConfig(
    val standardFee: Coins = Coins(1_000),
    val proFee: Coins = Coins(1_800),
    val standardRewardPerDay: Coins = Coins(3),
    val proRewardPerDay: Coins = Coins(5),
    val completionBonus: Coins = Coins(10),
    val feedbackReward: Coins = Coins(2),
    val failedPenalty: Coins = Coins(10),
    val leftPenalty: Coins = Coins(15),
    val feature1Price: Coins = Coins(150),
    val feature3Price: Coins = Coins(350),
    val feature7Price: Coins = Coins(700),
) {
    /** Top placement price for 1, 3 or 7 days. */
    fun featurePrice(days: Int): Coins = when (days) {
        1 -> feature1Price
        3 -> feature3Price
        7 -> feature7Price
        else -> throw IllegalArgumentException("days must be 1, 3 or 7")
    }

    fun fee(plan: Plan): Coins = when (plan) {
        Plan.STANDARD -> standardFee
        Plan.PRO -> proFee
    }

    fun rewardPerDay(plan: Plan): Coins = when (plan) {
        Plan.STANDARD -> standardRewardPerDay
        Plan.PRO -> proRewardPerDay
    }

    fun penalty(reason: TestFailure): Coins = when (reason) {
        TestFailure.TOO_MANY_MISSED -> failedPenalty
        TestFailure.LEFT_TEST -> leftPenalty
    }
}
