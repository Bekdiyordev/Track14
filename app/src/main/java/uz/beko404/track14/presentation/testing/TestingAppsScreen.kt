package uz.beko404.track14.presentation.testing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.beko404.track14.data.Track14RepositoryProvider
import uz.beko404.track14.domain.model.TestMembershipStatus
import uz.beko404.track14.presentation.auth.AuthPromptCard
import uz.beko404.track14.presentation.auth.AuthUiState
import uz.beko404.track14.presentation.common.AppCard
import uz.beko404.track14.presentation.common.PlaceholderScreen
import uz.beko404.track14.presentation.common.PrimaryActionButton
import uz.beko404.track14.presentation.common.SectionHeader
import uz.beko404.track14.presentation.common.StreakGrid
import uz.beko404.track14.presentation.common.label
import uz.beko404.track14.presentation.common.toStreakDayStates

@Composable
fun TestingAppsScreen(
    contentPadding: PaddingValues,
    authState: AuthUiState,
    onSignIn: (email: String, password: String) -> Unit,
) {
    val joinedTests = Track14RepositoryProvider.current.snapshot.joinedTests
        .filter { it.membership.status != TestMembershipStatus.Pending }

    PlaceholderScreen(
        title = "Testlar",
        subtitle = "Siz qo'shilgan testlar va 14 kunlik davomiylik shu tabda yuritiladi.",
        highlights = listOf(
            "Har bir qo'shilgan ilova uchun bugungi holat.",
            "Testni boshlash tugmasi va 30 soniya qoidasi.",
            "14 kunlik streak grid kutmoqda, bajarilgan va o'tkazib yuborilgan holatlarni ko'rsatadi.",
        ),
        contentPadding = contentPadding,
    ) {
        if (!authState.isSignedIn) {
            AuthPromptCard(
                title = "Testlarni ko'rish uchun kirish kerak",
                message = "Qo'shilgan testlar, streak va Testni boshlash actionlari email hisob bilan ishlaydi.",
                onSignIn = onSignIn,
            )
            return@PlaceholderScreen
        }

        SectionHeader(
            title = "Qo'shilgan testlar",
            subtitle = "Vaqtinchalik repository bugungi streak holatlarini ko'rsatmoqda.",
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            joinedTests.forEach { joinedTest ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppCard(
                        appName = joinedTest.app.name,
                        packageName = joinedTest.app.packageName,
                        ownerScore = joinedTest.app.ownerScore,
                        testerCount = joinedTest.app.testerCount,
                        requiredTesterCount = joinedTest.app.requiredTesterCount,
                        statusLabel = joinedTest.membership.status.label(),
                    )
                    Text(
                        text = "${joinedTest.membership.completedDaysCount} kun bajarildi, ${joinedTest.membership.missedDaysCount} kun o'tkazib yuborildi, ${joinedTest.membership.pointsDelta} ball",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    StreakGrid(states = joinedTest.dailyTests.toStreakDayStates())
                    PrimaryActionButton(
                        text = "Testni boshlash",
                        onClick = {},
                    )
                }
            }
        }
    }
}
