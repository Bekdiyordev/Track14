package uz.beko404.track14.presentation.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import uz.beko404.track14.domain.model.TestMembershipStatus
import uz.beko404.track14.domain.model.TrackAppStatus

fun TrackAppStatus.label(): String =
    when (this) {
        TrackAppStatus.Active -> "Faol"
        TrackAppStatus.Paused -> "Pauza"
        TrackAppStatus.Completed -> "Tugagan"
    }

fun TestMembershipStatus.label(): String =
    when (this) {
        TestMembershipStatus.Pending -> "Jarayonda"
        TestMembershipStatus.Active -> "Faol"
        TestMembershipStatus.Finished -> "Tugagan"
        TestMembershipStatus.Rejected -> "Rad etilgan"
        TestMembershipStatus.Left -> "Chiqib ketgan"
    }

@Composable
fun TrackAppStatus.containerColor(): Color =
    when (this) {
        TrackAppStatus.Active -> MaterialTheme.colorScheme.primaryContainer
        TrackAppStatus.Paused -> MaterialTheme.colorScheme.tertiaryContainer
        TrackAppStatus.Completed -> MaterialTheme.colorScheme.secondaryContainer
    }

@Composable
fun TrackAppStatus.contentColor(): Color =
    when (this) {
        TrackAppStatus.Active -> MaterialTheme.colorScheme.onPrimaryContainer
        TrackAppStatus.Paused -> MaterialTheme.colorScheme.onTertiaryContainer
        TrackAppStatus.Completed -> MaterialTheme.colorScheme.onSecondaryContainer
    }
