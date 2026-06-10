package uz.beko404.track14.presentation.profile

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.beko404.track14.data.Track14RepositoryProvider
import uz.beko404.track14.domain.model.ThemeMode
import uz.beko404.track14.presentation.auth.AuthPromptCard
import uz.beko404.track14.presentation.auth.AuthUiState
import uz.beko404.track14.presentation.auth.SignedInAccountCard
import uz.beko404.track14.presentation.common.PlaceholderScreen
import uz.beko404.track14.presentation.common.SectionHeader
import uz.beko404.track14.ui.theme.Track14ThemeMode

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    authState: AuthUiState,
    onSignIn: (email: String, password: String) -> Unit,
    onSignOut: () -> Unit,
) {
    val repository = Track14RepositoryProvider.current
    val user = repository.snapshot.currentUser
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    PlaceholderScreen(
        title = "Profil",
        subtitle = "Hisob, theme, support va community sozlamalari profil ekranida jamlanadi.",
        highlights = listOf(
            "Email orqali sign in va sign out holati.",
            "System, light va dark theme tanlovi.",
            "Support, Track14 Google Group va community linklari.",
        ),
        contentPadding = contentPadding,
    ) {
        SectionHeader(
            title = "Hisob",
            subtitle = if (authState.isSignedIn) {
                "${authState.email} • ${user.ownerScore} ball • ${user.timezoneId}"
            } else {
                "Muhim actionlar uchun email orqali kirish kerak."
            },
        )
        if (authState.isSignedIn) {
            SignedInAccountCard(
                email = authState.email.orEmpty(),
                onSignOut = onSignOut,
            )
        } else {
            AuthPromptCard(
                title = "Email orqali kirish",
                message = "Ilova qo'shish, testga qo'shilish, testni boshlash va profil account actionlari uchun kirish talab qilinadi.",
                onSignIn = onSignIn,
            )
        }

        SectionHeader(
            title = "Mavzu variantlari",
            subtitle = "Tanlov profil bilan birga saqlanadi.",
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Track14ThemeMode.entries.forEach { mode ->
                    ThemeOptionRow(
                        mode = mode,
                        selected = mode.matches(user.themeMode),
                        onClick = {
                            repository.updateThemeMode(mode.toThemeMode())
                        },
                    )
                }
            }
        }

        SectionHeader(
            title = "Linklar",
            subtitle = "Support va community manzillari.",
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProfileLinkRow(
                title = "Support",
                value = TRACK14_SUPPORT_URL,
                onClick = { uriHandler.openUri(TRACK14_SUPPORT_URL) },
            )
            ProfileLinkRow(
                title = "Track14 Google Group",
                value = TRACK14_GOOGLE_GROUP_URL,
                onClick = { uriHandler.openUri(TRACK14_GOOGLE_GROUP_URL) },
            )
            ProfileLinkRow(
                title = "Community",
                value = TRACK14_COMMUNITY_URL,
                onClick = { uriHandler.openUri(TRACK14_COMMUNITY_URL) },
            )
        }

        SectionHeader(
            title = "Ilova",
            subtitle = appVersionLabel(context),
        )
    }
}

private fun Track14ThemeMode.matches(themeMode: ThemeMode): Boolean =
    when (this) {
        Track14ThemeMode.System -> themeMode == ThemeMode.System
        Track14ThemeMode.Light -> themeMode == ThemeMode.Light
        Track14ThemeMode.Dark -> themeMode == ThemeMode.Dark
    }

private fun Track14ThemeMode.toThemeMode(): ThemeMode =
    when (this) {
        Track14ThemeMode.System -> ThemeMode.System
        Track14ThemeMode.Light -> ThemeMode.Light
        Track14ThemeMode.Dark -> ThemeMode.Dark
    }

@Composable
private fun ThemeOptionRow(
    mode: Track14ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(
            text = mode.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ProfileLinkRow(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun appVersionLabel(context: Context): String {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return "Versiya ${packageInfo.versionName}"
}

private const val TRACK14_SUPPORT_URL = "mailto:support@track14.app"
private const val TRACK14_GOOGLE_GROUP_URL = "http://groups.google.com/g/track14-testers"
private const val TRACK14_COMMUNITY_URL = "https://groups.google.com/g/track14-testers"
