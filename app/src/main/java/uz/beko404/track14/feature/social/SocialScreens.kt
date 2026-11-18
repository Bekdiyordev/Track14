package uz.beko404.track14.feature.social

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
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.domain.model.Feedback
import uz.beko404.track14.domain.model.FeedbackTag
import uz.beko404.track14.domain.model.ReportReason
import uz.beko404.track14.feature.messageRes

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun BottomAction(text: String, enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(20.dp).height(52.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) else Text(text)
    }
}

// ---------------------------------------------------------------- feedback

@Composable
fun FeedbackRoute(onDone: () -> Unit, viewModel: FeedbackViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.form.sent) { if (state.form.sent) onDone() }
    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(stringResource(R.string.feedback_title), onDone)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.test?.let { Text(it.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            Stars(state.form.rating, viewModel::setRating)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FeedbackTag.entries) { tag ->
                    FilterChip(selected = tag in state.form.tags, onClick = { viewModel.toggle(tag) }, label = { Text(stringResource(tag.label())) })
                }
            }
            OutlinedTextField(
                value = state.form.text,
                onValueChange = viewModel::setText,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text(stringResource(R.string.feedback_hint)) },
                supportingText = { Text("${state.form.text.length} / 1000") },
            )
            if (state.test?.feedbackRewarded == false) {
                Text(stringResource(R.string.feedback_reward), color = Track14Colors.Success, style = MaterialTheme.typography.bodySmall)
            }
            state.form.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
        }
        BottomAction(stringResource(R.string.feedback_send), state.canSend, state.form.sending, viewModel::send)
    }
}

@Composable
private fun Stars(rating: Int, onRate: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { i ->
            val on = i <= rating
            val scale by animateFloatAsState(if (on) 1.1f else 1f, label = "star")
            Icon(
                Icons.Filled.Star,
                contentDescription = stringResource(R.string.feedback_star, i),
                tint = if (on) Track14Colors.CoinGold else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp).scale(scale).clickable { onRate(i) },
            )
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

/** Owner side: list on the campaign dashboard. */
@Composable
fun FeedbackItem(feedback: Feedback, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("★".repeat(feedback.rating), color = Track14Colors.CoinGold, modifier = Modifier.weight(1f))
            if (feedback.createdAtMillis > 0) {
                Text(
                    DateUtils.getRelativeTimeSpanString(feedback.createdAtMillis).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (feedback.tags.isNotEmpty()) {
            Text(
                feedback.tags.map { stringResource(it.label()) }.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (feedback.text.isNotBlank()) Text(feedback.text, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------------------------------------------------------------- report

@Composable
fun ReportRoute(onDone: () -> Unit, viewModel: ReportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.sent) { if (state.sent) onDone() }
    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(stringResource(R.string.report_title), onDone)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.report_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.reasons.forEach { reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = state.reason == reason, onClick = { viewModel.pick(reason) }, role = Role.RadioButton)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = state.reason == reason, onClick = null)
                    Text(stringResource(reason.label()), modifier = Modifier.padding(start = 8.dp))
                }
            }
            OutlinedTextField(
                value = state.comment,
                onValueChange = viewModel::setComment,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text(stringResource(R.string.report_comment)) },
            )
            state.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
        }
        BottomAction(stringResource(R.string.report_send), state.canSend, state.sending, viewModel::send)
    }
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

// ---------------------------------------------------------------- notifications

@Composable
fun NotificationsRoute(onBack: () -> Unit, viewModel: NotificationsViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(stringResource(R.string.notifications_title), onBack)
        val list = items
        when {
            list == null -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            list.isEmpty() -> Text(
                stringResource(R.string.notifications_empty),
                modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { n ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .clickable { viewModel.open(n) }
                            .padding(14.dp)
                            .animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(8.dp)
                                .background(if (n.read) MaterialTheme.colorScheme.surfaceVariant else Track14Colors.Blue600, CircleShape),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(n.title, fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold)
                            Text(n.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (n.createdAtMillis > 0) {
                            Text(
                                DateUtils.getRelativeTimeSpanString(n.createdAtMillis).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
