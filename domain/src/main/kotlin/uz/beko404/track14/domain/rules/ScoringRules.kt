package uz.beko404.track14.domain.rules

import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.model.EconomyConfig
import uz.beko404.track14.domain.model.Plan
import uz.beko404.track14.domain.model.TestOutcome

/** Coin14 movements produced by the test life-cycle. Pure functions. */
class ScoringRules(private val economy: EconomyConfig = EconomyConfig()) {

    /** Tester earns this for every verified day. The platform pays, not the owner. */
    fun dayReward(plan: Plan): Coins = economy.rewardPerDay(plan)

    /** Extra reward paid once, when the 14 days are completed. */
    fun completionBonus(outcome: TestOutcome): Coins =
        if (outcome is TestOutcome.Completed) economy.completionBonus else Coins.ZERO

    /** Feedback pays once per app; later feedback pays nothing. */
    fun feedbackReward(alreadyRewardedForThisApp: Boolean): Coins =
        if (alreadyRewardedForThisApp) Coins.ZERO else economy.feedbackReward

    /** Non-positive amount to add to the balance when the test failed; zero otherwise. */
    fun penalty(outcome: TestOutcome): Coins = when (outcome) {
        is TestOutcome.Failed -> -economy.penalty(outcome.reason)
        TestOutcome.Active, TestOutcome.Completed -> Coins.ZERO
    }

    /** Owner pays the listing fee once; balance must cover it. */
    fun canAffordListing(balance: Coins, plan: Plan): Boolean = balance >= economy.fee(plan)

    /** Spending is blocked while the balance is negative (see plan, section 5). */
    fun canSpend(balance: Coins): Boolean = !balance.isNegative
}
