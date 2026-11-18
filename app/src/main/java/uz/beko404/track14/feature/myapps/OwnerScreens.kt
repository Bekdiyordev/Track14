package uz.beko404.track14.feature.myapps

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.AppTile
import uz.beko404.track14.core.designsystem.CoinAmount
import uz.beko404.track14.core.designsystem.DayDots
import uz.beko404.track14.core.designsystem.StatusPill
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.domain.model.AppCategories
import uz.beko404.track14.domain.model.DayStatus
import uz.beko404.track14.domain.model.OwnerAction
import uz.beko404.track14.domain.model.Plan
import uz.beko404.track14.domain.model.TestStatus
import uz.beko404.track14.domain.rules.NewAppField
import uz.beko404.track14.feature.messageRes
import uz.beko404.track14.feature.social.FeedbackItem

@Composable
private fun BackHeader(title: String, onBack: () -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
}

// ---------------------------------------------------------------- add app

@Composable
fun AddAppRoute(onBack: () -> Unit, onCreated: (String) -> Unit, viewModel: AddAppViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdAppId) { state.createdAppId?.let(onCreated) }
    val errors = state.errors

    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(stringResource(R.string.add_title), onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Field(state.form.name, viewModel::onName, R.string.add_name, NewAppField.NAME in errors, R.string.add_name_error)
            Field(
                state.form.packageName, viewModel::onPackage, R.string.add_package, NewAppField.PACKAGE in errors,
                R.string.add_package_error, hint = R.string.add_package_hint, keyboard = KeyboardType.Uri,
            )
            Text(stringResource(R.string.add_category), style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AppCategories.all) { c ->
                    FilterChip(selected = state.form.category == c, onClick = { viewModel.onCategory(c) }, label = { Text(c) })
                }
            }
            if (NewAppField.CATEGORY in errors) {
                Text(stringResource(R.string.add_category_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Field(
                state.form.shortDesc, viewModel::onDescription, R.string.add_description, NewAppField.DESCRIPTION in errors,
                R.string.add_description_error, singleLine = false,
            )
            Field(
                state.form.optInUrl, viewModel::onOptIn, R.string.add_optin, NewAppField.OPT_IN in errors,
                R.string.add_optin_error, hint = R.string.add_optin_hint, keyboard = KeyboardType.Uri,
            )
        }
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = viewModel::submit,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text(stringResource(R.string.add_continue)) }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: Int,
    isError: Boolean,
    errorText: Int,
    hint: Int? = null,
    singleLine: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        isError = isError,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = RoundedCornerShape(14.dp),
        supportingText = if (isError || hint != null) {
            { Text(stringResource(if (isError) errorText else hint ?: errorText)) }
        } else {
            null
        },
    )
}

// ---------------------------------------------------------------- plan

@Composable
fun PlanRoute(onBack: () -> Unit, onPaid: (String) -> Unit, viewModel: PlanViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.paid) { if (state.paid) state.app?.let { onPaid(it.id) } }

    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(stringResource(R.string.plan_title), onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.app?.let { Text(it.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            Plan.entries.forEach { plan ->
                PlanCard(
                    plan = plan,
                    fee = state.economy.fee(plan).value,
                    reward = state.economy.rewardPerDay(plan).value,
                    selected = state.selected == plan,
                    onClick = { viewModel.select(plan) },
                )
            }
            Text(stringResource(R.string.plan_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val balance = state.balance?.value
            Row {
                Text(stringResource(R.string.plan_balance), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                CoinAmount(balance?.toString() ?: "…")
            }
            if (balance != null && !state.canPay && !state.paying) {
                Text(stringResource(R.string.error_insufficient_balance), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            state.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = viewModel::pay,
                enabled = state.canPay,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (state.paying) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.plan_pay, state.fee.value.toInt()))
                }
            }
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, fee: Long, reward: Long, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        label = "plan",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (plan == Plan.PRO) R.string.plan_pro else R.string.plan_standard),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            CoinAmount(fee.toString())
        }
        Text(
            stringResource(if (plan == Plan.PRO) R.string.plan_pro_body else R.string.plan_standard_body, reward.toInt()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------- dashboard

@Composable
fun DashboardRoute(onBack: () -> Unit, onReportTester: (String) -> Unit, viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val economy by viewModel.economyConfig.collectAsStateWithLifecycle()
    var confirmPublish by remember { mutableStateOf(false) }
    val app = state.app

    Column(modifier = Modifier.fillMaxSize()) {
        BackHeader(app?.name ?: stringResource(R.string.dashboard_title), onBack)
        when {
            state.loading -> Centered { CircularProgressIndicator() }
            app == null -> Centered { Text(stringResource(R.string.detail_not_found)) }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppTile(app.id, app.name, size = 48.dp)
                        Text(app.packageName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StatusPill(app.status)
                    }
                }
                app.campaign?.let { c ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(listOf(Track14Colors.Blue500, Track14Colors.Blue700)))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(stringResource(R.string.dashboard_testers), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                            Text("${c.joined} / ${c.needed}", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                            Text(
                                stringResource(R.string.dashboard_summary, state.testers.count { it.status == TestStatus.ACTIVE }, c.completed, c.failed + c.left),
                                color = Color.White, style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            app.availableActions.forEach { action ->
                                when (action) {
                                    OwnerAction.PUBLISH -> Button(onClick = { confirmPublish = true }, enabled = !state.busy, shape = RoundedCornerShape(12.dp)) {
                                        Text(stringResource(R.string.dashboard_publish))
                                    }
                                    OwnerAction.PAUSE -> OutlinedButton(onClick = { viewModel.act(action) }, enabled = !state.busy, shape = RoundedCornerShape(12.dp)) {
                                        Text(stringResource(R.string.dashboard_pause))
                                    }
                                    OwnerAction.RESUME -> OutlinedButton(onClick = { viewModel.act(action) }, enabled = !state.busy, shape = RoundedCornerShape(12.dp)) {
                                        Text(stringResource(R.string.dashboard_resume))
                                    }
                                }
                            }
                        }
                    }
                }
                app.campaign?.takeIf { it.status == uz.beko404.track14.domain.model.AppStatus.ACTIVE }?.let { c ->
                    item {
                        FeatureCard(
                            featuredUntil = c.featuredUntilMillis.takeIf { it > viewModel.now() },
                            prices = listOf(1, 3, 7).map { it to economy.featurePrice(it).value },
                            busy = state.busy,
                            onFeature = viewModel::feature,
                        )
                    }
                }
                state.error?.let { e -> item { Text(stringResource(e.messageRes()), color = MaterialTheme.colorScheme.error) } }
                item { Text(stringResource(R.string.dashboard_tester_list), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) }
                if (state.testers.isEmpty()) {
                    item { Text(stringResource(R.string.dashboard_no_testers), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(state.testers.size) { i ->
                    val t = state.testers[i]
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row {
                                Text(stringResource(R.string.dashboard_tester_n, i + 1), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(
                                    stringResource(R.string.dashboard_days_passed, t.days.count { it == DayStatus.PASSED }),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DayDots(t.days, today = null, description = "", modifier = Modifier.fillMaxWidth())
                            TextButton(onClick = { onReportTester(t.testId) }) {
                                Text(stringResource(R.string.report_title), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    Text(stringResource(R.string.dashboard_feedback, state.feedback.size), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
                items(state.feedback, key = { it.id }) { FeedbackItem(it) }
            }
        }
    }

    if (confirmPublish) {
        AlertDialog(
            onDismissRequest = { confirmPublish = false },
            title = { Text(stringResource(R.string.dashboard_publish_title)) },
            text = { Text(stringResource(R.string.dashboard_publish_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.act(OwnerAction.PUBLISH); confirmPublish = false }) {
                    Text(stringResource(R.string.dashboard_publish))
                }
            },
            dismissButton = { TextButton(onClick = { confirmPublish = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun FeatureCard(featuredUntil: Long?, prices: List<Pair<Int, Long>>, busy: Boolean, onFeature: (Int) -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.feature_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(
                if (featuredUntil != null) {
                    stringResource(R.string.feature_active, android.text.format.DateUtils.formatDateTime(androidx.compose.ui.platform.LocalContext.current, featuredUntil, android.text.format.DateUtils.FORMAT_SHOW_DATE or android.text.format.DateUtils.FORMAT_SHOW_TIME))
                } else {
                    stringResource(R.string.feature_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                prices.forEach { (days, price) ->
                    OutlinedButton(
                        onClick = { onFeature(days) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.feature_days, days), fontWeight = FontWeight.Bold)
                            CoinAmount(price.toString())
                        }
                    }
                }
            }
        }
    }
}
