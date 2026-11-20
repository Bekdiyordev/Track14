package uz.beko404.track14.feature.testdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.core.designsystem.SkeletonList
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.AppTile
import uz.beko404.track14.core.designsystem.CoinAmount
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.core.designsystem.accentFor
import uz.beko404.track14.domain.model.DayHistory
import uz.beko404.track14.domain.model.DayRecord
import uz.beko404.track14.domain.model.DayStatus
import uz.beko404.track14.domain.model.TestStatus
import uz.beko404.track14.domain.rules.CheckMode
import uz.beko404.track14.feature.messageRes
import uz.beko404.track14.feature.social.FeedbackItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// ---------------------------------------------------------------- screen 9: test detail (tester)

@Composable
fun TestDetailRoute(
    onBack: () -> Unit,
    onFeedback: (String) -> Unit,
    viewModel: TestDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    var confirmLeave by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel, resources) {
        viewModel.messages.collect { e ->
            when (e) {
                TestDetailEvent.Left -> onBack()
                is TestDetailEvent.Failed -> snackbar.showSnackbar(resources.getString(e.error.messageRes()))
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            DetailState.Loading -> SkeletonList(count = 3, withDays = true, modifier = Modifier.padding(20.dp))
            DetailState.NotFound -> NotFoundState(onBack)
            is DetailState.Error -> Column {
                Header("", onBack)
                Centered { Text(stringResource(s.error.messageRes()), color = MaterialTheme.colorScheme.error) }
            }
            is DetailState.Content -> {
                val (test, records, now) = s.value
                val today = test.dayNumber(now)?.takeIf { it in 1..14 }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
                    Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.detail_back)) }
                        AppTile(test.appId.ifEmpty { test.id }, test.appName, size = 40.dp)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(test.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Text(
                                if (today != null) stringResource(R.string.testdetail_day_of, today) else stringResource(R.string.mytests_status_starting),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusChip(test.status)
                    }

                    SectionCard {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(stringResource(R.string.testdetail_grid), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                            if (test.status == TestStatus.ACTIVE) {
                                Text(stringResource(R.string.testdetail_days_left, DayHistory.daysLeft(today)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        DayGrid(test.days, today.takeIf { test.status == TestStatus.ACTIVE }, cellHeight = 52)
                        Legend()
                    }

                    Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniCard(stringResource(R.string.testdetail_earned), Modifier.weight(1f)) {
                            CoinAmount(DayHistory.earned(test).value.toString())
                        }
                        MiniCard(stringResource(R.string.testdetail_bonus), Modifier.weight(1f)) {
                            Text("+${test.completionBonus.value}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    DayHistory.warning(test.days, test.status)?.let { w ->
                        Column(
                            Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                        ) {
                            Text(stringResource(R.string.testdetail_missed, w.missed, w.limit), fontWeight = FontWeight.Bold, color = Color(0xFF7F1D1D))
                            Text(stringResource(R.string.testdetail_missed_rule), style = MaterialTheme.typography.bodySmall, color = Color(0xFF7F1D1D))
                        }
                    }

                    SectionCard {
                        Text(stringResource(R.string.testdetail_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        val history = DayHistory.closed(records)
                        if (history.isEmpty()) {
                            Text(stringResource(R.string.testdetail_history_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        history.forEach { r ->
                            HistoryRow(r, DayHistory.dateOf(test.startedAtMillis, r.day, test.timezone), isToday = r.day == today, showMode = true)
                        }
                    }

                    if (test.status == TestStatus.ACTIVE || test.status == TestStatus.COMPLETED) {
                        OutlinedButton(
                            onClick = { onFeedback(test.id) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) { Text(stringResource(if (test.feedbackRewarded) R.string.mytests_feedback else R.string.mytests_feedback_reward)) }
                    }
                    if (test.status == TestStatus.ACTIVE) {
                        TextButton(
                            onClick = { confirmLeave = true },
                            enabled = !busy,
                            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                        ) { Text(stringResource(R.string.mytests_leave), color = MaterialTheme.colorScheme.error) }
                    }
                }
                if (confirmLeave) {
                    AlertDialog(
                        onDismissRequest = { confirmLeave = false },
                        title = { Text(stringResource(R.string.mytests_leave_title)) },
                        text = { Text(stringResource(R.string.mytests_leave_body, test.appName)) },
                        confirmButton = {
                            TextButton(onClick = { confirmLeave = false; viewModel.leave() }) {
                                Text(stringResource(R.string.mytests_leave_confirm), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.cancel)) } },
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

// ---------------------------------------------------------------- screen 14: tester detail (owner)

@Composable
fun TesterDetailRoute(
    onBack: () -> Unit,
    onReport: (String) -> Unit,
    viewModel: TesterDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        DetailState.Loading -> SkeletonList(count = 3, withDays = true, modifier = Modifier.padding(20.dp))
        DetailState.NotFound -> NotFoundState(onBack)
        is DetailState.Error -> Column {
            Header(stringResource(R.string.testerdetail_title), onBack)
            Centered { Text(stringResource(s.error.messageRes()), color = MaterialTheme.colorScheme.error) }
        }
        is DetailState.Content -> {
            val (tester, records, feedback, appName) = s.value
            val name = tester.name.ifBlank { stringResource(R.string.testerdetail_anonymous) }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
                Header(stringResource(R.string.testerdetail_title), onBack)
                Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(76.dp).background(accentFor(tester.testId), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(tester.initials, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) }
                    Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    if (tester.device.isNotBlank()) {
                        Text(tester.device, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val passed = tester.days.count { it == DayStatus.PASSED }
                val closed = tester.days.count { it != DayStatus.PENDING }
                Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(tester.reliability?.let { "$it%" } ?: "—", stringResource(R.string.testerdetail_reliability), Modifier.weight(1f), Track14Colors.Success.takeIf { (tester.reliability ?: 0) >= 80 })
                    StatBox(tester.completedTests.toString(), stringResource(R.string.testerdetail_completed), Modifier.weight(1f))
                    StatBox("$passed / $closed", stringResource(R.string.testerdetail_days), Modifier.weight(1f))
                }
                if (DayHistory.usedFallback(records)) {
                    Row(
                        Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                            .background(Color(0xFFFFFBEB), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("B", fontWeight = FontWeight.ExtraBold, color = Color(0xFF92400E), modifier = Modifier.background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                        Text(stringResource(R.string.testerdetail_fallback), style = MaterialTheme.typography.bodySmall, color = Color(0xFF78350F))
                    }
                }
                SectionCard {
                    Text(stringResource(R.string.testerdetail_days_in, appName.ifBlank { "—" }), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    DayGrid(tester.days, today = null, cellHeight = 40)
                }
                SectionCard {
                    Text(stringResource(R.string.testerdetail_recent), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    val history = DayHistory.closed(records).take(7)
                    if (history.isEmpty()) {
                        Text(stringResource(R.string.testdetail_history_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    history.forEach { r -> HistoryRow(r, DayHistory.dateOf(tester.startedAtMillis, r.day, tester.timezone), isToday = false, showMode = true) }
                }
                if (feedback.isNotEmpty()) {
                    Text(
                        stringResource(R.string.testerdetail_feedback),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                    )
                    feedback.forEach { FeedbackItem(it, Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
                }
                OutlinedButton(
                    onClick = { onReport(tester.testId) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(stringResource(R.string.report_title), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

// ---------------------------------------------------------------- shared parts

/** 14 cells in two rows of 7, with the day number and a ✓ / ✕ mark. */
@Composable
fun DayGrid(days: List<DayStatus>, today: Int?, cellHeight: Int) {
    val desc = stringResource(R.string.mytests_days_description, days.count { it == DayStatus.PASSED })
    Column(Modifier.fillMaxWidth().semantics { contentDescription = desc }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        days.chunked(7).forEachIndexed { row, week ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                week.forEachIndexed { col, status ->
                    val n = row * 7 + col + 1
                    val (bg, fg) = when (status) {
                        DayStatus.PASSED -> Track14Colors.Success to Color.White
                        DayStatus.MISSED -> Track14Colors.Danger to Color.White
                        DayStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val shape = RoundedCornerShape(12.dp)
                    Column(
                        Modifier.weight(1f).height(cellHeight.dp).background(bg, shape)
                            .then(if (n == today) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("$n", color = fg, fontWeight = FontWeight.Bold)
                        if (cellHeight >= 48) {
                            val mark = when {
                                n == today -> stringResource(R.string.testdetail_today)
                                status == DayStatus.PASSED -> "✓"
                                status == DayStatus.MISSED -> "✕"
                                else -> ""
                            }
                            if (mark.isNotEmpty()) Text(mark, color = fg, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LegendItem(Track14Colors.Success, stringResource(R.string.testdetail_legend_done))
        LegendItem(Track14Colors.Danger, stringResource(R.string.testdetail_legend_missed))
        LegendItem(MaterialTheme.colorScheme.surfaceVariant, stringResource(R.string.testdetail_legend_future))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(4.dp)))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HistoryRow(record: DayRecord, date: LocalDate?, isToday: Boolean, showMode: Boolean) {
    val passed = record.status == DayStatus.PASSED
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(10.dp).background(if (passed) Track14Colors.Success else Track14Colors.Danger, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(
                if (isToday) stringResource(R.string.testdetail_day_today, record.day) else stringResource(R.string.testdetail_day, record.day),
                fontWeight = FontWeight.SemiBold,
            )
            date?.let { Text(it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Text(
            if (passed) duration(record.seconds) else stringResource(R.string.testdetail_missed_day),
            color = if (passed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall,
        )
        if (showMode) {
            Text(
                when (record.mode) { CheckMode.USAGE_ACCESS -> "A"; CheckMode.FALLBACK -> "B"; null -> "—" },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun duration(seconds: Int?): String {
    val s = seconds ?: return "—"
    return if (s < 60) stringResource(R.string.duration_seconds, s) else stringResource(R.string.duration_minutes, s / 60, s % 60)
}

@Composable
private fun StatusChip(status: TestStatus) {
    val (label, bg, fg) = when (status) {
        TestStatus.ACTIVE -> Triple(R.string.testdetail_active, Color(0xFFDCFCE7), Color(0xFF15803D))
        TestStatus.COMPLETED -> Triple(R.string.mytests_status_completed, Color(0xFFDBEAFE), Color(0xFF1D4ED8))
        TestStatus.FAILED -> Triple(R.string.mytests_status_failed, Color(0xFFFEE2E2), Color(0xFFB91C1C))
        TestStatus.LEFT -> Triple(R.string.mytests_status_left, Color(0xFFF1F5F9), Color(0xFF475569))
        TestStatus.JOINING -> Triple(R.string.mytests_status_starting, Color(0xFFF1F5F9), Color(0xFF475569))
    }
    Text(stringResource(label), color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.background(bg, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun MiniCard(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    Column(modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        value()
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier, color: Color? = null) {
    Column(modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color ?: MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.detail_back)) }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun NotFoundState(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("", onBack)
        Centered { Text(stringResource(R.string.testdetail_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
