package uz.beko404.track14.presentation.common

import uz.beko404.track14.domain.model.DailyTest
import uz.beko404.track14.domain.model.DailyTestStatus

fun List<DailyTest>.toStreakDayStates(): List<StreakDayState> =
    map { it.status.toStreakDayState() }

fun List<DailyTestStatus>.toStreakStates(): List<StreakDayState> =
    map { it.toStreakDayState() }

private fun DailyTestStatus.toStreakDayState(): StreakDayState =
    when (this) {
        DailyTestStatus.Pending -> StreakDayState.Pending
        DailyTestStatus.Completed -> StreakDayState.Completed
        DailyTestStatus.Missed -> StreakDayState.Missed
    }
