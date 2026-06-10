package uz.beko404.track14.presentation.testing

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import uz.beko404.track14.R
import uz.beko404.track14.data.Track14RepositoryProvider
import uz.beko404.track14.domain.model.DailyTestResult
import uz.beko404.track14.domain.model.JoinAppResult
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
    val repository = Track14RepositoryProvider.current
    val joinedTests = repository.snapshot.joinedTests
        .filter { it.membership.status == TestMembershipStatus.Active }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                repository.completeEligibleDailyTests()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryActionButton(
                            text = "Testni boshlash",
                            onClick = {
                                startDailyTestFlow(
                                    context = context,
                                    packageName = joinedTest.app.packageName,
                                    appName = joinedTest.app.name,
                                    onStartDailyTest = {
                                        repository.startDailyTest(joinedTest.membership.id)
                                    },
                                )
                            },
                        )
                        TextButton(
                            onClick = {
                                when (val result = repository.leaveJoinedTest(joinedTest.membership.id)) {
                                    is JoinAppResult.Success -> {
                                        Toast.makeText(context, "Testdan chiqdingiz.", Toast.LENGTH_SHORT).show()
                                    }
                                    is JoinAppResult.Failure -> {
                                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        ) {
                            Text(text = "Chiqish")
                        }
                    }
                }
            }
        }
    }
}

private fun startDailyTestFlow(
    context: Context,
    packageName: String,
    appName: String,
    onStartDailyTest: () -> DailyTestResult,
) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent == null) {
        Toast.makeText(context, "Ilova qurilmada topilmadi. Play install linkini qayta oching.", Toast.LENGTH_LONG).show()
        return
    }

    when (val result = onStartDailyTest()) {
        is DailyTestResult.Failure -> {
            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
        }
        is DailyTestResult.Success -> {
            scheduleDailyTestReminder(context = context, appName = appName)
            context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

private fun scheduleDailyTestReminder(
    context: Context,
    appName: String,
) {
    Handler(Looper.getMainLooper()).postDelayed(
        {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return@postDelayed
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        DAILY_TEST_CHANNEL_ID,
                        "Daily test",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ),
                )
            }

            val notification = NotificationCompat.Builder(context, DAILY_TEST_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Bugungi test tayyor")
                .setContentText("$appName uchun 30 soniya o'tdi. Track14ga qayting.")
                .setAutoCancel(true)
                .build()

            notificationManager.notify(appName.hashCode(), notification)
        },
        DAILY_TEST_MIN_MILLIS,
    )
}

private const val DAILY_TEST_CHANNEL_ID = "daily_test"
private const val DAILY_TEST_MIN_MILLIS = 30_000L
