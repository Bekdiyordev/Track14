package uz.beko404.track14.presentation.myapps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.beko404.track14.data.Track14RepositoryProvider
import uz.beko404.track14.domain.model.AddOwnerAppRequest
import uz.beko404.track14.domain.model.AddOwnerAppResult
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
    val repository = Track14RepositoryProvider.current
    val snapshot = repository.snapshot
    val ownerApps = snapshot.ownerApps
    val context = LocalContext.current
    val installedApps = rememberSaveableInstalledApps(context)
    var showAddForm by rememberSaveable { mutableStateOf(false) }
    val canAddApp = ownerApps.size < snapshot.currentUser.freeAppLimit

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryActionButton(
                text = if (showAddForm) "Formani yopish" else "Ilova qo'shish",
                enabled = canAddApp || showAddForm,
                onClick = { showAddForm = !showAddForm },
            )
        }

        if (!canAddApp) {
            LimitNotice(limit = snapshot.currentUser.freeAppLimit)
        }

        if (showAddForm) {
            AddOwnerAppCard(
                installedApps = installedApps,
                canAddApp = canAddApp,
                onCancel = { showAddForm = false },
                onSave = { request ->
                    repository.addOwnerApp(request).also { result ->
                        if (result is AddOwnerAppResult.Success) {
                            showAddForm = false
                        }
                    }
                },
            )
        }

        if (ownerApps.isEmpty()) {
            EmptyState(
                title = "Hali ilova qo'shilmagan",
                message = "Installed app tanlang, Google Group va Play test linklarini kiriting.",
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
private fun AddOwnerAppCard(
    installedApps: List<InstalledAppCandidate>,
    canAddApp: Boolean,
    onSave: (AddOwnerAppRequest) -> AddOwnerAppResult,
    onCancel: () -> Unit,
) {
    var selectedPackageName by rememberSaveable { mutableStateOf("") }
    var appName by rememberSaveable { mutableStateOf("") }
    var packageName by rememberSaveable { mutableStateOf("") }
    var googleGroupUrl by rememberSaveable { mutableStateOf("") }
    var playOptInUrl by rememberSaveable { mutableStateOf("") }
    var playInstallUrl by rememberSaveable { mutableStateOf("") }
    var iconUrl by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    val effectiveInstallUrl = playInstallUrl.trim()
        .ifBlank { "https://play.google.com/store/apps/details?id=${packageName.trim()}" }
    val canSubmit = canAddApp &&
        appName.isNotBlank() &&
        packageName.isNotBlank() &&
        isGoogleGroupUrl(googleGroupUrl) &&
        isPlayOptInUrl(playOptInUrl)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Ilova qo'shish",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Launchable installed app tanlang yoki manual maydonlarni to'ldiring.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            InstalledAppPicker(
                installedApps = installedApps,
                selectedPackageName = selectedPackageName,
                onSelect = { app ->
                    selectedPackageName = app.packageName
                    appName = app.label
                    packageName = app.packageName
                    message = null
                },
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = appName,
                onValueChange = {
                    appName = it
                    message = null
                },
                singleLine = true,
                label = { Text(text = "Ilova nomi") },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = packageName,
                onValueChange = {
                    packageName = it
                    selectedPackageName = ""
                    message = null
                },
                singleLine = true,
                label = { Text(text = "Package name") },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = googleGroupUrl,
                onValueChange = {
                    googleGroupUrl = it
                    message = null
                },
                singleLine = true,
                isError = googleGroupUrl.isNotBlank() && !isGoogleGroupUrl(googleGroupUrl),
                label = { Text(text = "Google Group URL") },
                supportingText = {
                    Text(text = "Masalan: https://groups.google.com/g/track14-testers")
                },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = playOptInUrl,
                onValueChange = {
                    playOptInUrl = it
                    message = null
                },
                singleLine = true,
                isError = playOptInUrl.isNotBlank() && !isPlayOptInUrl(playOptInUrl),
                label = { Text(text = "Play opt-in URL") },
                supportingText = {
                    Text(text = "Masalan: https://play.google.com/apps/testing/uz.beko404.track14")
                },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = playInstallUrl,
                onValueChange = {
                    playInstallUrl = it
                    message = null
                },
                singleLine = true,
                label = { Text(text = "Play install URL, ixtiyoriy") },
                supportingText = {
                    Text(text = "Bo'sh qolsa avtomatik: $effectiveInstallUrl")
                },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = iconUrl,
                onValueChange = {
                    iconUrl = it
                    message = null
                },
                singleLine = true,
                label = { Text(text = "Icon URL, ixtiyoriy") },
                supportingText = {
                    Text(text = "Firebase Storage ulanganda installed app icon shu maydonga ko'chiriladi.")
                },
            )

            message?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryActionButton(
                    text = "Saqlash",
                    enabled = canSubmit,
                    onClick = {
                        val result = onSave(
                            AddOwnerAppRequest(
                                name = appName,
                                packageName = packageName,
                                googleGroupUrl = googleGroupUrl,
                                playOptInUrl = playOptInUrl,
                                playInstallUrl = playInstallUrl,
                                iconUrl = iconUrl,
                            ),
                        )
                        if (result is AddOwnerAppResult.Failure) {
                            message = result.message
                        }
                    },
                )
                TextButton(onClick = onCancel) {
                    Text(text = "Bekor qilish")
                }
            }
        }
    }
}

@Composable
private fun InstalledAppPicker(
    installedApps: List<InstalledAppCandidate>,
    selectedPackageName: String,
    onSelect: (InstalledAppCandidate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Installed app tanlash",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (installedApps.isEmpty()) {
            Text(
                text = "Launchable app topilmadi. Manual maydonlarni to'ldiring.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        installedApps.take(8).forEach { app ->
            InstalledAppRow(
                app = app,
                selected = app.packageName == selectedPackageName,
                onClick = { onSelect(app) },
            )
        }
        if (installedApps.size > 8) {
            Text(
                text = "Jami ${installedApps.size} ta launchable app topildi. Keraklisi ko'rinmasa, manual kiriting.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InstalledAppRow(
    app: InstalledAppCandidate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${app.packageName} - ${if (app.hasIcon) "icon topildi" else "icon topilmadi"}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LimitNotice(limit: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            modifier = Modifier.padding(14.dp),
            text = "Bepul tarif limiti tugagan: $limit ta ilova. Yangi ilova qo'shish backenddagi plan limitiga bog'lanadi.",
            style = MaterialTheme.typography.bodyMedium,
        )
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

@Composable
private fun rememberSaveableInstalledApps(context: Context): List<InstalledAppCandidate> =
    androidx.compose.runtime.remember(context) {
        loadLaunchableInstalledApps(context)
    }

private fun loadLaunchableInstalledApps(context: Context): List<InstalledAppCandidate> {
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(0L),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(intent, 0)
    }

    return resolveInfos
        .mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            val packageName = activityInfo.packageName ?: return@mapNotNull null
            val label = resolveInfo.loadLabel(packageManager)
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: packageName
            val hasIcon = runCatching { resolveInfo.loadIcon(packageManager) }.getOrNull() != null

            InstalledAppCandidate(
                label = label,
                packageName = packageName,
                hasIcon = hasIcon,
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

private fun isGoogleGroupUrl(value: String): Boolean {
    val normalized = value.trim().lowercase()
    return normalized.startsWith("https://groups.google.")
}

private fun isPlayOptInUrl(value: String): Boolean {
    val normalized = value.trim().lowercase()
    return normalized.startsWith("https://play.google.com/apps/testing/")
}

private data class InstalledAppCandidate(
    val label: String,
    val packageName: String,
    val hasIcon: Boolean,
)
