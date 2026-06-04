package uz.beko404.track14.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.beko404.track14.data.Track14RepositoryProvider
import uz.beko404.track14.presentation.common.AppCard
import uz.beko404.track14.presentation.common.EmptyState
import uz.beko404.track14.presentation.common.PlaceholderScreen
import uz.beko404.track14.presentation.common.SectionHeader
import uz.beko404.track14.presentation.common.containerColor
import uz.beko404.track14.presentation.common.contentColor
import uz.beko404.track14.presentation.common.label

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onAppClick: (String) -> Unit,
) {
    val apps = Track14RepositoryProvider.current.snapshot.rankedApps
    var query by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    val filteredApps = apps.filter { app ->
        query.isBlank() ||
            app.name.contains(query, ignoreCase = true) ||
            app.packageName.contains(query, ignoreCase = true)
    }

    PlaceholderScreen(
        title = "Asosiy",
        subtitle = "Google Play yopiq testiga tester kerak bo'lgan ilovalar shu yerda ko'rinadi.",
        highlights = listOf(
            "Ranking bo'yicha saralangan ilovalar ro'yxati.",
            "Testerlar soni va kerakli tester ehtiyoji.",
            "App card bosilganda detail va join flow ochiladi.",
        ),
        contentPadding = contentPadding,
    ) {
        SectionHeader(
            title = "Tester kerak bo'lgan ilovalar",
            subtitle = "Vaqtinchalik repository ma'lumotlari ranking bo'yicha saralangan.",
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = {
                Text(text = "Ilova yoki package qidirish")
            },
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (filteredApps.isEmpty()) {
                EmptyState(
                    title = "Natija topilmadi",
                    message = "Boshqa app nomi yoki package name bilan qidirib ko'ring.",
                )
            }
            filteredApps.forEach { app ->
                AppCard(
                    appName = app.name,
                    packageName = app.packageName,
                    ownerScore = app.ownerScore,
                    testerCount = app.testerCount,
                    requiredTesterCount = app.requiredTesterCount,
                    statusLabel = app.status.label(),
                    statusContainerColor = app.status.containerColor(),
                    statusContentColor = app.status.contentColor(),
                    onClick = { onAppClick(app.id) },
                )
            }
        }
    }
}
