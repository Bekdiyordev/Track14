@file:OptIn(ExperimentalLayoutApi::class)

package uz.beko404.track14.feature.social

import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Card
import uz.beko404.track14.core.designsystem.PrimaryButton
import uz.beko404.track14.core.designsystem.Manrope
import uz.beko404.track14.core.designsystem.BackButton
import uz.beko404.track14.core.designsystem.AppTile
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import android.text.format.DateUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.core.designsystem.SkeletonList
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.domain.model.AppNotification
import uz.beko404.track14.domain.model.Feedback
import uz.beko404.track14.domain.model.FeedbackTag
import uz.beko404.track14.domain.model.ReportReason
import uz.beko404.track14.feature.messageRes

@Composable
private fun BackHeader(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BackButton(onBack)
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}

// ---------------------------------------------------------------- feedback (bottom sheet over a dim page)

@Composable
fun FeedbackRoute(onDone: () -> Unit, viewModel: FeedbackViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = T14Theme.colors
    LaunchedEffect(state.form.sent) { if (state.form.sent) onDone() }
    Box(Modifier.fillMaxSize().background(Color(0x8C111827)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 5.dp).clip(RoundedCornerShape(999.dp)).background(if (c.dark) Color(0xFF334155) else Color(0xFFCBD5E1)))
            val test = state.test
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (test != null) AppTile(test.appId.ifEmpty { test.id }, test.appName, size = 44.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.feedback_sheet_title, test?.appName.orEmpty()), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text(
                        if (test?.feedbackRewarded == false) stringResource(R.string.feedback_sheet_sub_reward) else stringResource(R.string.feedback_sheet_sub),
                        fontSize = 13.sp, color = c.textSecondary,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.feedback_overall), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Stars(state.form.rating, viewModel::setRating)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FeedbackTag.entries.forEach { tag ->
                    val on = tag in state.form.tags
                    Box(
                        Modifier.height(36.dp).clip(RoundedCornerShape(999.dp))
                            .background(if (on) c.blueSoft else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (on) Color(0xFF93C5FD) else c.fieldBorder, RoundedCornerShape(999.dp))
                            .clickable { viewModel.toggle(tag) }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(tag.label()), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (on) (if (c.dark) Color(0xFF93C5FD) else Color(0xFF1E40AF)) else c.textBody) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.feedback_comment), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                T14TextArea(state.form.text, viewModel::setText, stringResource(R.string.feedback_hint), "${state.form.text.length} / 1000")
            }
            state.form.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            PrimaryButton(
                if (test?.feedbackRewarded == false) stringResource(R.string.feedback_send_reward) else stringResource(R.string.feedback_send),
                onClick = viewModel::send,
                enabled = state.canSend,
                height = 56.dp,
            )
        }
    }
}

/** Five 48dp star buttons: chosen ones gold on cream, the rest outlined. */
@Composable
private fun Stars(rating: Int, onRate: (Int) -> Unit) {
    val c = T14Theme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..5).forEach { i ->
            val on = i <= rating
            val label = stringResource(R.string.feedback_star, i)
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (on) c.amberSoft else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (on) c.amberBorder else c.fieldBorder, RoundedCornerShape(14.dp))
                    .clickable { onRate(i) }.semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                val path = remember { PathParser().parsePathString("M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z").toPath() }
                Canvas(Modifier.size(26.dp)) {
                    val k = size.minDimension / 24f
                    scale(k, k, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                        if (on) drawPath(path, Color(0xFFF59E0B))
                        drawPath(path, if (on) Color(0xFFD97706) else Color(0xFF94A3B8), style = Stroke(width = 1.6f, join = StrokeJoin.Round))
                    }
                }
            }
        }
    }
}

private fun FeedbackTag.label(): Int = when (this) {
    FeedbackTag.BUG -> R.string.tag_bug
    FeedbackTag.IDEA -> R.string.tag_idea
    FeedbackTag.DESIGN -> R.string.tag_design
    FeedbackTag.SPEED -> R.string.tag_speed
    FeedbackTag.POSITIVE -> R.string.tag_positive
}

/** Multi-line field from the canvas: 14dp corners, hairline border, counter under the text. */
@Composable
fun T14TextArea(value: String, onValueChange: (String) -> Unit, placeholder: String, counter: String? = null) {
    val c = T14Theme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.field).border(1.dp, if (c.dark) c.fieldBorder else Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box {
            if (value.isEmpty()) Text(placeholder, fontSize = 15.sp, lineHeight = 22.sp, color = c.textTertiary)
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (counter != null) Text(counter, fontSize = 12.sp, color = c.textTertiary, modifier = Modifier.align(Alignment.End))
    }
}

/** Owner side: one feedback card (dashboard, tester page, feedback list). */
@Composable
fun FeedbackItem(feedback: Feedback, modifier: Modifier = Modifier) {
    val c = T14Theme.colors
    T14Card(modifier, radius = 18.dp, padding = PaddingValues(16.dp), spacing = 8.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("★".repeat(feedback.rating) + "☆".repeat(5 - feedback.rating), color = Color(0xFF92400E).takeUnless { c.dark } ?: Color(0xFFFCD34D), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (feedback.createdAtMillis > 0) {
                Text(DateUtils.getRelativeTimeSpanString(feedback.createdAtMillis).toString(), fontSize = 12.sp, color = c.textSecondary)
            }
        }
        if (feedback.text.isNotBlank()) Text("\u201C${feedback.text}\u201D", fontSize = 14.sp, lineHeight = 21.sp, color = c.textBody)
        if (feedback.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                feedback.tags.forEach { tag ->
                    Text(
                        stringResource(tag.label()), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.blueSoftText,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.blueSoft).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- report

@Composable
fun ReportRoute(onDone: () -> Unit, viewModel: ReportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val app by viewModel.app.collectAsStateWithLifecycle()
    val c = T14Theme.colors
    if (state.sent) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Box(Modifier.size(84.dp).clip(CircleShape).background(c.successSoft), contentAlignment = Alignment.Center) {
                T14Icon("M5 12l5 5 9-10", size = 42.dp, tint = c.successText, strokeWidth = 2.6f)
            }
            Text(stringResource(R.string.report_sent_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            Text(stringResource(R.string.report_sent_body), fontSize = 15.sp, lineHeight = 22.sp, color = c.textBody, textAlign = TextAlign.Center)
            PrimaryButton(stringResource(R.string.report_back_to_apps), onClick = onDone, height = 56.dp, modifier = Modifier.padding(top = 10.dp))
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        BackHeader(stringResource(R.string.report_title), onDone)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            app?.let { a ->
                T14Card(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp), radius = 16.dp, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppTile(a.id, a.name, size = 44.dp, category = a.category)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(a.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(a.packageName, fontSize = 12.sp, color = c.textSecondary)
                        }
                    }
                }
            }
            Caps(stringResource(R.string.report_reason_caps))
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.reasons.forEach { reason ->
                    val on = state.reason == reason
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.fillMaxWidth()
                            .shadow(if (on || c.dark) 0.dp else 4.dp, shape, ambientColor = Color(0x0D0F172A), spotColor = Color(0x140F172A))
                            .clip(shape).background(if (on) c.blueSoft else MaterialTheme.colorScheme.surface)
                            .border(if (on) 2.dp else 1.dp, if (on) Track14Colors.Blue600 else c.fieldBorder, shape)
                            .selectable(selected = on, onClick = { viewModel.pick(reason) }, role = Role.RadioButton)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (on) Track14Colors.Blue600 else Color(0xFFCBD5E1), CircleShape), contentAlignment = Alignment.Center) {
                            if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(Track14Colors.Blue600))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(reason.label()), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(reason.hint()), fontSize = 12.sp, lineHeight = 17.sp, color = if (on) c.textBody else c.textSecondary)
                        }
                    }
                }
            }
            Caps(stringResource(R.string.report_comment_caps))
            Box(Modifier.padding(horizontal = 20.dp)) {
                T14TextArea(state.comment, viewModel::setComment, stringResource(R.string.report_comment), "${state.comment.length} / 1000")
            }
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.blueSoft).padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                T14Icon("M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M12 11v5M12 7.5v.5", size = 20.dp, tint = Track14Colors.Blue600)
                Text(stringResource(R.string.report_hint_long), fontSize = 13.sp, lineHeight = 19.sp, color = c.textBody)
            }
            state.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp)) }
        }
        BottomBar {
            if (state.canSend) {
                PrimaryButton(stringResource(R.string.report_send), onClick = viewModel::send, height = 56.dp)
            } else {
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(if (c.dark) Color(0xFF1E293B) else Color(0xFFE2E8F0)), contentAlignment = Alignment.Center) {
                    Text(stringResource(if (state.sending) R.string.report_sending else R.string.report_pick_reason), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
private fun Caps(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = T14Theme.colors.textSecondary, letterSpacing = 0.4.sp, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp))
}

@Composable
private fun BottomBar(content: @Composable () -> Unit) {
    val c = T14Theme.colors
    Box(
        Modifier.fillMaxWidth().background(c.navBar)
            .drawBehind { drawLine(c.navBorder, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp),
    ) { content() }
}

private fun ReportReason.label(): Int = when (this) {
    ReportReason.NOT_WORKING -> R.string.reason_not_working
    ReportReason.FAKE -> R.string.reason_fake
    ReportReason.INAPPROPRIATE -> R.string.reason_inappropriate
    ReportReason.FRAUD -> R.string.reason_fraud
    ReportReason.SPAM -> R.string.reason_spam
    ReportReason.FAKE_ACTIVITY -> R.string.reason_fake_activity
    ReportReason.OTHER -> R.string.reason_other
}

private fun ReportReason.hint(): Int = when (this) {
    ReportReason.NOT_WORKING -> R.string.reason_not_working_hint
    ReportReason.FAKE -> R.string.reason_fake_hint
    ReportReason.INAPPROPRIATE -> R.string.reason_inappropriate_hint
    ReportReason.FRAUD -> R.string.reason_fraud_hint
    ReportReason.SPAM -> R.string.reason_spam_hint
    ReportReason.FAKE_ACTIVITY -> R.string.reason_fake_activity_hint
    ReportReason.OTHER -> R.string.reason_other_hint
}

// ---------------------------------------------------------------- notifications

@Composable
fun NotificationsRoute(onBack: () -> Unit, viewModel: NotificationsViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val c = T14Theme.colors
    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(stringResource(R.string.notifications_title), onBack) {
            if (items.orEmpty().any { !it.read }) {
                Text(
                    stringResource(R.string.notifications_read_all), color = c.blueSoftText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = viewModel::readAll).padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }
        }
        val list = items
        when {
            list == null -> SkeletonList(count = 4, modifier = Modifier.padding(20.dp))
            list.isEmpty() -> T14Card(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.notifications_empty), color = c.textBody, fontSize = 15.sp)
            }
            else -> {
                val todayLabel = stringResource(R.string.day_today_caps)
                val yesterdayLabel = stringResource(R.string.day_yesterday_caps)
                LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                var lastDay = ""
                list.forEach { n ->
                    val day = dayHeader(n.createdAtMillis, todayLabel, yesterdayLabel)
                    if (day != lastDay) {
                        lastDay = day
                        item(key = "h_$day") {
                            Text(day, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.textSecondary, letterSpacing = 0.4.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                        }
                    }
                    item(key = n.id) { NotificationRow(n, onClick = { viewModel.open(n) }, modifier = Modifier.animateItem()) }
                }
                }
            }
        }
    }
}

private fun dayHeader(millis: Long, todayLabel: String, yesterdayLabel: String): String {
    if (millis <= 0) return ""
    val zone = java.time.ZoneId.systemDefault()
    val date = java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val today = java.time.LocalDate.now(zone)
    return when (date) {
        today -> todayLabel
        today.minusDays(1) -> yesterdayLabel
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM")).uppercase()
    }
}

/** Icon and tone per notification type, from the canvas list. */
private data class NotificationLook(val icon: String, val bg: Color, val fg: Color)

private fun lookFor(type: String): NotificationLook = when (type) {
    "daily_reminder" -> NotificationLook("M12 7v5l3 2M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z", Color(0xFFFEF3C7), Color(0xFFB45309))
    "tester_joined" -> NotificationLook("M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4 21c1.5-4 4.5-6 8-6s6.5 2 8 6", Color(0xFFEFF6FF), Color(0xFF1D4ED8))
    "feedback_received" -> NotificationLook("M4 5h16v11H8l-4 4z", Color(0xFFEFF6FF), Color(0xFF1D4ED8))
    "test_completed", "bonus" -> NotificationLook("M12 3l2.5 5.5 6 .7-4.5 4 1.2 6L12 16.3 6.8 19.2 8 13.2l-4.5-4 6-.7z", Color(0xFFFFFBEB), Color(0xFFB45309))
    "test_failed", "penalty", "app_suspended" -> NotificationLook("M12 3l9.5 17h-19zM12 10v4M12 17v.5", Color(0xFFFEE2E2), Color(0xFFB91C1C))
    else -> NotificationLook("M5 12l5 5 9-10", Color(0xFFDCFCE7), Color(0xFF15803D))
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit, modifier: Modifier) {
    val c = T14Theme.colors
    val look = lookFor(n.type)
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.fillMaxWidth().clip(shape)
            .background(if (n.read) Color.Transparent else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (n.read) Color.Transparent else c.blueBorder.copy(alpha = if (c.dark) 1f else 0.7f), shape)
            .clickable(onClick = onClick).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (c.dark) look.fg.copy(alpha = 0.18f) else look.bg), contentAlignment = Alignment.Center) {
            T14Icon(look.icon, size = 20.dp, tint = if (c.dark) androidx.compose.ui.graphics.lerp(look.fg, Color.White, 0.45f) else look.fg, strokeWidth = 2.2f)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(n.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (n.createdAtMillis > 0) {
                    Text(
                        java.time.Instant.ofEpochMilli(n.createdAtMillis).atZone(java.time.ZoneId.systemDefault()).toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = 12.sp, color = c.textSecondary,
                    )
                }
            }
            Text(n.body, fontSize = 13.sp, lineHeight = 19.sp, color = c.textBody)
        }
        if (!n.read) Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(Track14Colors.Blue600))
    }
}
