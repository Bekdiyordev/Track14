package uz.beko404.track14.feature.testdetail

import uz.beko404.track14.core.designsystem.EmptyIcons
import uz.beko404.track14.core.designsystem.EmptyState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.AppTile
import uz.beko404.track14.core.designsystem.BackButton
import uz.beko404.track14.core.designsystem.Coin14Icon
import uz.beko404.track14.core.designsystem.Manrope
import uz.beko404.track14.core.designsystem.SkeletonList
import uz.beko404.track14.core.designsystem.T14Card
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.domain.model.DayHistory
import uz.beko404.track14.domain.model.DayRecord
import uz.beko404.track14.domain.model.DayStatus
import uz.beko404.track14.domain.model.Feedback
import uz.beko404.track14.domain.model.TestStatus
import uz.beko404.track14.domain.rules.CheckMode
import uz.beko404.track14.feature.messageRes
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    val c = T14Theme.colors

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
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BackButton(onBack)
                        AppTile(test.appId.ifEmpty { test.id }, test.appName, size = 40.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(test.appName, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 1)
                            val start = test.startedAtMillis?.let { DayHistory.dateOf(it, 1, test.timezone) }
                            val line = buildString {
                                append(if (today != null) stringResource(R.string.testdetail_day_of, today) else stringResource(R.string.mytests_status_starting))
                                if (start != null) append(" · ").append(stringResource(R.string.testdetail_since, start.format(DateTimeFormatter.ofPattern("d-MMMM"))))
                            }
                            Text(line, fontSize = 13.sp, color = c.textSecondary, maxLines = 1)
                        }
                        StatusChip(test.status)
                    }

                    SectionCard(Modifier.padding(top = 12.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(stringResource(R.string.testdetail_grid), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                            if (test.status == TestStatus.ACTIVE) {
                                Text(stringResource(R.string.testdetail_days_left, DayHistory.daysLeft(today)), fontSize = 13.sp, color = c.textSecondary)
                            }
                        }
                        DayGrid(test.days, today.takeIf { test.status == TestStatus.ACTIVE }, cellHeight = 52)
                        Legend()
                    }

                    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniCard(stringResource(R.string.testdetail_earned), Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Coin14Icon(20.dp)
                                Text(DayHistory.earned(test).value.toString(), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = c.coinText)
                            }
                        }
                        MiniCard(stringResource(R.string.testdetail_bonus), Modifier.weight(1f)) {
                            Text("+${test.completionBonus.value}", fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        }
                    }

                    DayHistory.warning(test.days, test.status)?.let { w ->
                        val text = if (c.dark) c.dangerText else Color(0xFF7F1D1D)
                        Row(
                            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)).background(c.dangerSoft)
                                .border(1.dp, c.dangerBorder, RoundedCornerShape(16.dp)).padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            T14Icon(T14Paths.Warning, size = 22.dp, tint = c.dangerText)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(stringResource(R.string.testdetail_missed, w.missed, w.limit), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = text)
                                Text(stringResource(R.string.testdetail_missed_rule), fontSize = 13.sp, lineHeight = 19.sp, color = text)
                            }
                        }
                    }

                    ListCard(stringResource(R.string.testdetail_history), Modifier.padding(top = 12.dp)) {
                        val history = DayHistory.closed(records)
                        if (history.isEmpty()) {
                            EmptyState(EmptyIcons.Clock, stringResource(R.string.testdetail_history_empty), stringResource(R.string.testdetail_history_empty_desc), Modifier.padding(bottom = 10.dp), card = false, compact = true)
                        }
                        history.forEach { r ->
                            HistoryRow(r, DayHistory.dateOf(test.startedAtMillis, r.day, test.timezone), isToday = r.day == today, showMode = true)
                        }
                    }

                    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (test.status == TestStatus.ACTIVE || test.status == TestStatus.COMPLETED) {
                            OutlineAction(
                                stringResource(if (test.feedbackRewarded) R.string.mytests_feedback else R.string.mytests_feedback_reward),
                                color = c.blueSoftText, border = c.blueBorder,
                            ) { onFeedback(test.id) }
                        }
                        if (test.status == TestStatus.ACTIVE) {
                            Box(
                                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).clickable(enabled = !busy) { confirmLeave = true },
                                contentAlignment = Alignment.Center,
                            ) { Text(stringResource(R.string.mytests_leave), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.dangerText) }
                        }
                    }
                }
                if (confirmLeave) {
                    AlertDialog(
                        onDismissRequest = { confirmLeave = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp),
                        title = { Text(stringResource(R.string.mytests_leave_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                        text = { Text(stringResource(R.string.mytests_leave_body, test.appName), fontSize = 15.sp, lineHeight = 22.sp, color = c.textBody) },
                        confirmButton = {
                            TextButton(onClick = { confirmLeave = false; viewModel.leave() }) {
                                Text(stringResource(R.string.mytests_leave_confirm), color = c.dangerText, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.cancel), color = c.textSecondary) } },
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

// ---------------------------------------------------------------- screen 14: tester detail (owner)

private val AvatarColors = listOf(
    Color(0xFF0EA5E9), Color(0xFFEC4899), Color(0xFF7C3AED), Color(0xFF16A34A), Color(0xFFEA580C), Color(0xFF475569),
)

@Composable
fun TesterDetailRoute(
    onBack: () -> Unit,
    onReport: (String) -> Unit,
    viewModel: TesterDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = T14Theme.colors
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
                Column(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(76.dp).clip(CircleShape).background(AvatarColors[Math.floorMod(tester.testId.hashCode(), AvatarColors.size)]),
                        contentAlignment = Alignment.Center,
                    ) { Text(tester.initials, color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(name, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        if (tester.device.isNotBlank()) Text(tester.device, fontSize = 14.sp, color = c.textBody)
                    }
                }
                val passed = tester.days.count { it == DayStatus.PASSED }
                val closed = tester.days.count { it != DayStatus.PENDING }
                Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(tester.reliability?.let { "$it%" } ?: "—", stringResource(R.string.testerdetail_reliability), Modifier.weight(1f), c.successText.takeIf { (tester.reliability ?: 0) >= 80 })
                    StatBox(tester.completedTests.toString(), stringResource(R.string.testerdetail_completed), Modifier.weight(1f))
                    StatBox("$passed / $closed", stringResource(R.string.testerdetail_days), Modifier.weight(1f))
                }
                if (DayHistory.usedFallback(records)) {
                    Row(
                        Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)).background(c.amberSoft)
                            .border(1.dp, c.amberBorder, RoundedCornerShape(16.dp)).padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(if (c.dark) Color(0xFF3B2A0A) else Color(0xFFFEF3C7)), contentAlignment = Alignment.Center) {
                            Text("B", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = c.amberText)
                        }
                        Text(stringResource(R.string.testerdetail_fallback), fontSize = 13.sp, lineHeight = 20.sp, color = if (c.dark) c.amberText else Color(0xFF78350F))
                    }
                }
                SectionCard(Modifier.padding(top = 12.dp), radius = 20.dp, padding = 16.dp, spacing = 12.dp) {
                    Text(stringResource(R.string.testerdetail_days_in, appName.ifBlank { "—" }), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    DayGrid(tester.days, today = null, cellHeight = 40)
                }
                ListCard(stringResource(R.string.testerdetail_recent), Modifier.padding(top = 12.dp), radius = 20.dp, titleSize = 16) {
                    val history = DayHistory.closed(records).take(7)
                    if (history.isEmpty()) {
                        EmptyState(EmptyIcons.Clock, stringResource(R.string.testdetail_history_empty), stringResource(R.string.testdetail_history_empty_desc), Modifier.padding(bottom = 10.dp), card = false, compact = true)
                    }
                    history.forEach { r -> HistoryRow(r, DayHistory.dateOf(tester.startedAtMillis, r.day, tester.timezone), isToday = false, showMode = false) }
                }
                feedback.forEach { f -> FeedbackCard(f) }
                Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                    OutlineAction(stringResource(R.string.report_title), color = c.dangerText, border = c.dangerBorder, icon = T14Paths.Flag) { onReport(tester.testId) }
                }
            }
        }
    }
}

@Composable
private fun FeedbackCard(f: Feedback) {
    val c = T14Theme.colors
    SectionCard(Modifier.padding(top = 12.dp), radius = 20.dp, padding = 16.dp, spacing = 8.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dash_feedback), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("★".repeat(f.rating) + "☆".repeat(5 - f.rating), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.coinText)
        }
        if (f.text.isNotBlank()) Text("“${f.text}”", fontSize = 14.sp, lineHeight = 21.sp, color = c.textBody)
        if (f.createdAtMillis > 0) {
            Text(
                java.time.Instant.ofEpochMilli(f.createdAtMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("d-MMMM")),
                fontSize = 12.sp, color = c.textSecondary,
            )
        }
    }
}

// ---------------------------------------------------------------- shared parts

/** 14 cells in two rows of 7, with the day number and a ✓ / ✕ mark. */
@Composable
fun DayGrid(days: List<DayStatus>, today: Int?, cellHeight: Int) {
    val c = T14Theme.colors
    val desc = stringResource(R.string.mytests_days_description, days.count { it == DayStatus.PASSED })
    val big = cellHeight >= 48
    val gap = if (big) 8.dp else 6.dp
    val future = if (c.dark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    Column(Modifier.fillMaxWidth().semantics { contentDescription = desc }, verticalArrangement = Arrangement.spacedBy(gap)) {
        days.chunked(7).forEachIndexed { row, week ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                week.forEachIndexed { col, status ->
                    val n = row * 7 + col + 1
                    val (bg, fg) = when (status) {
                        DayStatus.PASSED -> Color(0xFF22C55E) to Color.White
                        DayStatus.MISSED -> Color(0xFFDC2626) to Color.White
                        DayStatus.PENDING -> future to c.textSecondary
                    }
                    val shape = RoundedCornerShape(if (big) 12.dp else 10.dp)
                    val ring = when {
                        n != today -> null
                        status == DayStatus.PASSED -> Color(0xFF14532D)
                        else -> Track14Colors.Blue600
                    }
                    Column(
                        Modifier.weight(1f).height(cellHeight.dp).clip(shape).background(bg)
                            .then(if (ring != null) Modifier.border(2.dp, ring, shape) else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    ) {
                        Text("$n", color = fg, fontSize = if (big) 15.sp else 13.sp, fontWeight = FontWeight.Bold)
                        if (big) {
                            val mark = when {
                                n == today -> stringResource(R.string.testdetail_today)
                                status == DayStatus.PASSED -> "✓"
                                status == DayStatus.MISSED -> "✕"
                                else -> ""
                            }
                            if (mark.isNotEmpty()) Text(mark, color = if (n == today && status == DayStatus.PENDING) c.blueSoftText else fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend() {
    val c = T14Theme.colors
    FlowRowCompat {
        LegendItem(Modifier.background(Color(0xFF22C55E)), stringResource(R.string.testdetail_legend_done))
        LegendItem(Modifier.background(Color(0xFFDC2626)), stringResource(R.string.testdetail_legend_missed))
        LegendItem(Modifier.border(2.dp, Track14Colors.Blue600, RoundedCornerShape(4.dp)), stringResource(R.string.testdetail_today_legend))
        LegendItem(Modifier.background(if (c.dark) Color(0xFF263247) else Color(0xFFE2E8F0)), stringResource(R.string.testdetail_legend_future))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCompat(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
}

@Composable
private fun LegendItem(swatch: Modifier, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).then(swatch))
        Text(label, fontSize = 12.sp, color = T14Theme.colors.textBody)
    }
}

@Composable
private fun HistoryRow(record: DayRecord, date: LocalDate?, isToday: Boolean, showMode: Boolean) {
    val c = T14Theme.colors
    val passed = record.status == DayStatus.PASSED
    Row(
        Modifier.fillMaxWidth()
            .drawBehind { drawLine(c.divider, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(vertical = if (showMode) 12.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (passed) Color(0xFF22C55E) else Color(0xFFDC2626)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                if (isToday) stringResource(R.string.testdetail_day_today, record.day) else stringResource(R.string.testdetail_day, record.day),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            )
            if (showMode) date?.let { Text(it.format(DateTimeFormatter.ofPattern("d-MMMM")), fontSize = 12.sp, color = c.textSecondary) }
        }
        val modeLetter = when (record.mode) { CheckMode.USAGE_ACCESS -> "A"; CheckMode.FALLBACK -> "B"; null -> "—" }
        Text(
            when {
                !passed -> stringResource(R.string.testdetail_missed_day)
                showMode -> duration(record.seconds)
                else -> duration(record.seconds) + " · " + modeLetter
            },
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (passed) MaterialTheme.colorScheme.onSurface else c.dangerText,
        )
        if (showMode) {
            Text(
                modeLetter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.textBody,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.divider).padding(horizontal = 7.dp, vertical = 3.dp),
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
    val c = T14Theme.colors
    val (label, bg, fg) = when (status) {
        TestStatus.ACTIVE -> Triple(R.string.testdetail_active, c.successSoft, c.successText)
        TestStatus.COMPLETED -> Triple(R.string.mytests_status_completed, c.blueSoft, c.blueSoftText)
        TestStatus.FAILED -> Triple(R.string.mytests_status_failed, c.dangerSoft, c.dangerText)
        TestStatus.LEFT -> Triple(R.string.mytests_status_left, c.divider, c.textBody)
        TestStatus.JOINING -> Triple(R.string.mytests_status_starting, c.divider, c.textBody)
    }
    Text(
        stringResource(label), color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    padding: Dp = 18.dp,
    spacing: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    T14Card(modifier.padding(horizontal = 20.dp), radius = radius, padding = PaddingValues(padding), spacing = spacing, content = content)
}

/** Card with a title and hairline-separated rows (history lists). */
@Composable
private fun ListCard(title: String, modifier: Modifier = Modifier, radius: Dp = 22.dp, titleSize: Int = 17, content: @Composable ColumnScope.() -> Unit) {
    T14Card(modifier.padding(horizontal = 20.dp), radius = radius, padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), spacing = 0.dp) {
        Text(title, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = titleSize.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        content()
    }
}

@Composable
private fun MiniCard(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    T14Card(modifier, radius = 18.dp, padding = PaddingValues(14.dp), spacing = 4.dp) {
        Text(label, fontSize = 12.sp, color = T14Theme.colors.textSecondary)
        value()
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier, color: Color? = null) {
    T14Card(modifier, radius = 16.dp, padding = PaddingValues(12.dp), spacing = 2.dp) {
        Text(value, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color ?: MaterialTheme.colorScheme.onSurface, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(label, fontSize = 11.sp, color = T14Theme.colors.textSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

/** 52dp outlined button on white: blue for feedback, red for report. */
@Composable
private fun OutlineAction(text: String, color: Color, border: Color, icon: String? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(shape).background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, border, shape).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) T14Icon(icon, size = 18.dp, tint = color)
        Text(text, fontSize = if (icon != null) 15.sp else 16.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BackButton(onBack)
        Text(title, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    }
}

@Composable
private fun NotFoundState(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("", onBack)
        Centered { Text(stringResource(R.string.testdetail_not_found), color = T14Theme.colors.textSecondary) }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
