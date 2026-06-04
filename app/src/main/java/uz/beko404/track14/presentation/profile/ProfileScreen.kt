package uz.beko404.track14.presentation.profile

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
import androidx.compose.ui.text.font.FontWeight
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
    val user = Track14RepositoryProvider.current.snapshot.currentUser

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
            subtitle = "Saqlash keyingi stepda ulanadi.",
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
                    )
                }
            }
        }
    }
}

private fun Track14ThemeMode.matches(themeMode: ThemeMode): Boolean =
    when (this) {
        Track14ThemeMode.System -> themeMode == ThemeMode.System
        Track14ThemeMode.Light -> themeMode == ThemeMode.Light
        Track14ThemeMode.Dark -> themeMode == ThemeMode.Dark
    }

@Composable
private fun ThemeOptionRow(
    mode: Track14ThemeMode,
    selected: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = mode.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
