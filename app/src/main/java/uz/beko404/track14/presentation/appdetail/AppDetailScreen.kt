package uz.beko404.track14.presentation.appdetail

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.beko404.track14.data.Track14RepositoryProvider
import uz.beko404.track14.domain.model.JoinAppResult
import uz.beko404.track14.domain.model.TrackApp
import uz.beko404.track14.presentation.common.AppCard
import uz.beko404.track14.presentation.common.EmptyState
import uz.beko404.track14.presentation.common.PrimaryActionButton
import uz.beko404.track14.presentation.common.SectionHeader
import uz.beko404.track14.presentation.auth.AuthPromptCard
import uz.beko404.track14.presentation.auth.AuthUiState
import uz.beko404.track14.presentation.common.containerColor
import uz.beko404.track14.presentation.common.contentColor
import uz.beko404.track14.presentation.common.label

@Composable
fun AppDetailScreen(
    appId: String,
    contentPadding: PaddingValues,
    authState: AuthUiState,
    onSignIn: (email: String, password: String) -> Unit,
    onBackClick: () -> Unit,
) {
    val repository = Track14RepositoryProvider.current
    val snapshot = repository.snapshot
    val app = repository.getAppById(appId)

    if (app == null) {
        AppMissingState(
            contentPadding = contentPadding,
            onBackClick = onBackClick,
        )
        return
    }

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val membership = snapshot.joinedTests.firstOrNull { it.app.id == app.id }?.membership
    fun openExternalLink(url: String): Boolean {
        if (!authState.isSignedIn) {
            Toast.makeText(context, "Avval email orqali kiring.", Toast.LENGTH_SHORT).show()
            return false
        }
        return runCatching {
            uriHandler.openUri(url)
            true
        }.getOrElse {
            Toast.makeText(context, "Link ochilmadi. Qayta urinib ko'ring.", Toast.LENGTH_SHORT).show()
            false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        TextButton(onClick = onBackClick) {
            Text(text = "Orqaga")
        }

        SectionHeader(
            title = app.name,
            subtitle = "Yopiq testga qo'shilish uchun bosqichlarni tartib bilan bajaring.",
        )

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

        AppDetailStats(app = app)

        SectionHeader(
            title = "Qo'shilish bosqichlari",
            subtitle = "Track14 link ochilganini qayd qiladi, Google Group a'zoligini avtomatik tekshirmaydi.",
        )

        if (!authState.isSignedIn) {
            AuthPromptCard(
                title = "Join qilish uchun kirish kerak",
                message = "Google Group, Play test va install bosqichlarini boshlashdan oldin email hisobingiz bilan kiring.",
                onSignIn = onSignIn,
            )
        }

        JoinStepCard(
            stepNumber = 1,
            title = "Google Groupga qo'shiling",
            description = "Play closed test ko'rinishi uchun avval testerlar Google Groupiga qo'shiling.",
            actionText = "Google Groupni ochish",
            completed = membership?.googleGroupOpened == true,
            onActionClick = {
                if (openExternalLink(app.googleGroupUrl)) {
                    repository.markGoogleGroupOpened(app.id)
                }
            },
        )
        JoinStepCard(
            stepNumber = 2,
            title = "Play test opt-in sahifasini oching",
            description = "Google hisobingiz Groupga qo'shilgan bo'lsa, Play test sahifasida tester sifatida opt-in qilasiz.",
            actionText = "Play testni ochish",
            completed = membership?.playOptInOpened == true,
            onActionClick = {
                if (openExternalLink(app.playOptInUrl)) {
                    repository.markPlayOptInOpened(app.id)
                }
            },
        )
        JoinStepCard(
            stepNumber = 3,
            title = "Ilovani install yoki open qiling",
            description = "Agar Play link ishlamasa, Google Groupga qayta kirib, Play test linkini yana oching.",
            actionText = "Install yoki ochish",
            completed = membership?.installOpened == true,
            onActionClick = {
                if (openExternalLink(app.playInstallUrl)) {
                    when (val result = repository.joinApp(app.id)) {
                        is JoinAppResult.Success -> {
                            Toast.makeText(context, "Test ro'yxatiga qo'shildingiz.", Toast.LENGTH_SHORT).show()
                        }
                        is JoinAppResult.Failure -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = "Eslatma: MVP Google Group yoki Play Console a'zoligini avtomatik tasdiqlamaydi. Install/open bosqichi ochilgandan keyin Track14 joined record yaratadi.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AppDetailStats(app: TrackApp) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailMetric(
            label = "Ranking",
            value = app.rankingScore.toString(),
            modifier = Modifier.weight(1f),
        )
        DetailMetric(
            label = "Qolgan",
            value = (app.requiredTesterCount - app.testerCount).coerceAtLeast(0).toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DetailMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun JoinStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    actionText: String,
    completed: Boolean,
    onActionClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (completed) "$stepNumber-bosqich • bajarildi" else "$stepNumber-bosqich",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryActionButton(
                text = actionText,
                onClick = onActionClick,
            )
        }
    }
}

@Composable
private fun AppMissingState(
    contentPadding: PaddingValues,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EmptyState(
            title = "Ilova topilmadi",
            message = "Bu app id vaqtinchalik repository ichida yo'q.",
            action = {
                PrimaryActionButton(
                    text = "Orqaga",
                    onClick = onBackClick,
                )
            },
        )
    }
}
