package uz.beko404.track14.presentation.myapps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.beko404.track14.data.Track14RepositoryProvider
import uz.beko404.track14.domain.model.OwnerTesterSnapshot
import uz.beko404.track14.presentation.auth.AuthPromptCard
import uz.beko404.track14.presentation.auth.AuthUiState
import uz.beko404.track14.presentation.common.AppCard
import uz.beko404.track14.presentation.common.EmptyState
import uz.beko404.track14.presentation.common.PlaceholderScreen
import uz.beko404.track14.presentation.common.PrimaryActionButton
import uz.beko404.track14.presentation.common.SectionHeader
import uz.beko404.track14.presentation.common.StreakGrid
import uz.beko404.track14.presentation.common.containerColor
import uz.beko404.track14.presentation.common.contentColor
import uz.beko404.track14.presentation.common.label
import uz.beko404.track14.presentation.common.toStreakStates

@Composable
fun MyAppsScreen(
    contentPadding: PaddingValues,
    authState: AuthUiState,
    onSignIn: (email: String, password: String) -> Unit,
) {
    val snapshot = Track14RepositoryProvider.current.snapshot
    val ownerApps = snapshot.ownerApps

    PlaceholderScreen(
        title = "Ilovalarim",
        subtitle = "O'z ilovalaringiz, Google Group linklari va testerlar holati shu yerda bo'ladi.",
        highlights = listOf(
            "Free limit: MVP da 3 tagacha ilova.",
            "Device ichidan installed app tanlash oqimi.",
            "Owner testerlarni finish yoki reject qila oladi.",
        ),
        contentPadding = contentPadding,
    ) {
        if (!authState.isSignedIn) {
            AuthPromptCard(
                title = "Ilovalarim uchun kirish kerak",
                message = "O'z ilovangizni qo'shish va testerlar holatini ko'rish uchun email hisobingiz bilan kiring.",
                onSignIn = onSignIn,
            )
            return@PlaceholderScreen
        }

        SectionHeader(
            title = "Mening ilovalarim",
            subtitle = "${ownerApps.size}/${snapshot.currentUser.freeAppLimit} bepul joy ishlatilgan.",
        )
        if (ownerApps.isEmpty()) {
            EmptyState(
                title = "Hali ilova qo'shilmagan",
                message = "Installed app tanlang, Google Group va Play test linklarini kiriting.",
                action = {
                    PrimaryActionButton(
                        text = "Ilova qo'shish",
                        onClick = {},
                    )
                },
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ownerApps.forEach { app ->
                    AppCard(
                        appName = app.name,
                        packageName = app.packageName,
                        ownerScore = app.ownerScore,
                        testerCount = app.testerCount,
                        requiredTesterCount = app.requiredTesterCount,
                        statusLabel = app.status.label(),
                        statusContainerColor = app.status.containerColor(),
                        statusContentColor = app.status.contentColor(),
                    )
                }
            }
        }

        SectionHeader(
            title = "Tester streaklari",
            subtitle = "Ilova egasi ko'radigan tester holati uchun boshlang'ich ko'rinish.",
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            snapshot.ownerTesterSnapshots.forEach { tester ->
                OwnerTesterCard(tester = tester)
            }
        }
    }
}

@Composable
private fun OwnerTesterCard(tester: OwnerTesterSnapshot) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = tester.testerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = tester.membershipStatus.label(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            StreakGrid(states = tester.streak.toStreakStates())
        }
    }
}
