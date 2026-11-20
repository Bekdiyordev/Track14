@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package uz.beko404.track14.feature.myapps

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.AppTile
import uz.beko404.track14.core.designsystem.BackButton
import uz.beko404.track14.core.designsystem.Categories
import uz.beko404.track14.core.designsystem.Coin14Icon
import uz.beko404.track14.core.designsystem.Manrope
import uz.beko404.track14.core.designsystem.PrimaryButton
import uz.beko404.track14.core.designsystem.T14Card
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.core.designsystem.tileBrush
import uz.beko404.track14.feature.messageRes
import uz.beko404.track14.domain.model.AppCategories
import uz.beko404.track14.domain.model.AppStatus
import uz.beko404.track14.domain.model.CampaignTester
import uz.beko404.track14.domain.model.DayStatus
import uz.beko404.track14.domain.model.Feedback
import uz.beko404.track14.domain.model.FeedbackTag
import uz.beko404.track14.domain.model.OwnerAction
import uz.beko404.track14.domain.model.OwnerApp
import uz.beko404.track14.domain.model.Plan
import uz.beko404.track14.domain.model.PlayLinks
import uz.beko404.track14.domain.model.TestStatus
import uz.beko404.track14.domain.rules.NewAppField
import uz.beko404.track14.feature.common.formatCoins

// ---------------------------------------------------------------- shared pieces

/** Header used by the owner screens: back (or close) button, Manrope 18/800 title, optional sub line. */
@Composable
private fun OwnerHeader(
    title: String,
    onBack: () -> Unit,
    close: Boolean = false,
    leading: @Composable (() -> Unit)? = null,
    sub: @Composable (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (close) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onBack)
                    .semantics { contentDescription = "close" },
                contentAlignment = Alignment.Center,
            ) { T14Icon(T14Paths.Close, size = 24.dp, tint = MaterialTheme.colorScheme.onSurface, strokeWidth = 2.2f) }
        } else {
            BackButton(onBack)
        }
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 1)
            sub?.invoke()
        }
        trailing()
    }
}

/** Three labelled bars on top of the add-app wizard: done green, current blue, rest grey. */
@Composable
private fun WizardSteps(current: Int) {
    val c = T14Theme.colors
    val names = listOf(R.string.add_step_details, R.string.add_step_console, R.string.add_step_payment)
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        names.forEachIndexed { k, name ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(
                        when {
                            k < current -> Color(0xFF22C55E)
                            k == current -> Track14Colors.Blue600
                            else -> if (c.dark) Color(0xFF263247) else Color(0xFFE2E8F0)
                        },
                    ),
                )
                Text(
                    stringResource(name), fontSize = 12.sp,
                    fontWeight = if (k == current) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        k == current -> c.blueSoftText
                        k < current -> c.successText
                        else -> c.textSecondary
                    },
                )
            }
        }
    }
}

/** White bar pinned to the bottom with a hairline on top. */
@Composable
private fun BottomBar(content: @Composable ColumnScope.() -> Unit) {
    val c = T14Theme.colors
    Column(
        Modifier.fillMaxWidth().background(c.navBar)
            .drawBehind { drawLine(c.navBorder, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

/** Form field from the canvas: label 14/600 on top, 50dp box with 14dp corners, hint or error under it. */
@Composable
private fun T14Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    error: String? = null,
    hint: String? = null,
    counter: String? = null,
    mono: Boolean = false,
    minLines: Int = 1,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    val c = T14Theme.colors
    val border = when {
        error != null -> c.dangerText
        c.dark -> c.fieldBorder
        else -> Color(0xFFCBD5E1)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (counter != null) Text(counter, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.textSecondary)
        }
        val single = minLines == 1
        Box(
            Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(14.dp)).background(c.field)
                .border(1.dp, border, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = if (single) 0.dp else 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = if (mono) FontFamily.Monospace else MaterialTheme.typography.bodyMedium.fontFamily,
                lineHeight = 22.sp,
            )
            if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, style = style.copy(color = c.textTertiary))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = single,
                minLines = minLines,
                textStyle = style,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when {
            error != null -> Text(error, fontSize = 12.sp, color = c.dangerText)
            hint != null -> Text(hint, fontSize = 12.sp, color = c.textSecondary)
        }
    }
}

/** 22dp radio ring with a 10dp dot. */
@Composable
private fun Radio(on: Boolean) {
    Box(
        Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (on) Track14Colors.Blue600 else Color(0xFFCBD5E1), CircleShape),
        contentAlignment = Alignment.Center,
    ) { if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(Track14Colors.Blue600)) }
}

/** Rounded square tick box: green or blue when on. */
@Composable
private fun TickBox(on: Boolean, size: Dp, onColor: Color, modifier: Modifier = Modifier) {
    val c = T14Theme.colors
    Box(
        modifier.size(size).clip(RoundedCornerShape(size / 3)).background(if (on) onColor else c.field)
            .border(2.dp, if (on) onColor else Color(0xFFCBD5E1), RoundedCornerShape(size / 3)),
        contentAlignment = Alignment.Center,
    ) { if (on) T14Icon(T14Paths.Check, size = size * 0.57f, tint = Color.White, strokeWidth = 3.2f) }
}

/** Selected option card: 2dp blue border on soft blue, otherwise hairline on white. */
private fun Modifier.optionCard(on: Boolean, radius: Dp, dark: Boolean, surface: Color, soft: Color, hairline: Color): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.clip(shape).background(if (on) soft else surface).border(if (on) 2.dp else 1.dp, if (on) Track14Colors.Blue600 else hairline, shape)
}

// ---------------------------------------------------------------- add app (wizard steps 1 and 2)

private const val APK_MIME = "application/vnd.android.package-archive"
private const val GROUP_EMAIL = "track14-testers@googlegroups.com"

@Composable
fun AddAppRoute(onBack: () -> Unit, onCreated: (String) -> Unit, viewModel: AddAppViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdAppId) { state.createdAppId?.let(onCreated) }
    val pickApk = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), viewModel::onApkPicked)
    var page by rememberSaveable { mutableIntStateOf(0) }
    var done by rememberSaveable { mutableStateOf(setOf<Int>()) }
    BackHandler(enabled = page == 1) { page = 0 }

    Column(Modifier.fillMaxSize()) {
        OwnerHeader(stringResource(R.string.add_title), onBack = { if (page == 1) page = 0 else onBack() }, close = page == 0)
        WizardSteps(page)
        AnimatedContent(targetState = page, modifier = Modifier.weight(1f), label = "add_page") { p ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                if (p == 0) {
                    DetailsPage(state, viewModel, onPickApk = { pickApk.launch(arrayOf(APK_MIME)) })
                } else {
                    ConsolePage(state, viewModel, done, onToggle = { k -> done = if (k in done) done - k else done + k })
                }
            }
        }
        BottomBar {
            state.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            PrimaryButton(
                stringResource(if (state.saving) R.string.add_saving else R.string.add_next),
                onClick = {
                    if (page == 0) {
                        if (viewModel.detailsValid()) page = 1
                    } else {
                        viewModel.submit()
                    }
                },
                enabled = !state.saving,
                height = 56.dp,
            )
        }
    }
}

@Composable
private fun DetailsPage(state: AddAppUiState, viewModel: AddAppViewModel, onPickApk: () -> Unit) {
    val c = T14Theme.colors
    val errors = state.errors
    val form = state.form
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        val apk = state.apk
        if (apk is ApkState.Filled) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.successSoft)
                    .border(1.dp, if (c.dark) Color(0xFF14532D) else Color(0xFFBBF7D0), RoundedCornerShape(16.dp))
                    .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                T14Icon(T14Paths.Check, size = 20.dp, tint = c.successText, strokeWidth = 2.6f)
                Text(
                    stringResource(R.string.add_apk_filled_short, apk.packageName),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.successText, modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.add_apk_again), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.successText,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onPickApk).padding(horizontal = 10.dp, vertical = 10.dp),
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.blueSoft)
                    .border(1.dp, c.blueBorder, RoundedCornerShape(18.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                    T14Icon("M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8zM14 3v5h5M12 11v6M9 14l3 3 3-3", size = 24.dp, tint = Track14Colors.Blue600)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.add_apk_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (c.dark) Color(0xFFDBEAFE) else Color(0xFF172554))
                    val sub = when (apk) {
                        ApkState.TooLarge -> stringResource(R.string.add_apk_too_large)
                        ApkState.Unreadable -> stringResource(R.string.add_apk_unreadable)
                        else -> stringResource(R.string.add_apk_short)
                    }
                    val bad = apk is ApkState.TooLarge || apk is ApkState.Unreadable
                    Text(sub, fontSize = 12.sp, lineHeight = 17.sp, color = if (bad) c.dangerText else if (c.dark) Color(0xFF93C5FD) else Color(0xFF1E40AF))
                }
                Box(
                    Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(Track14Colors.Blue600)
                        .clickable(enabled = apk != ApkState.Reading, onClick = onPickApk).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(if (apk == ApkState.Reading) R.string.add_apk_reading else R.string.add_apk_choose),
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(1.dp).background(c.fieldBorder))
            Text(
                stringResource(if (apk is ApkState.Filled) R.string.add_or_edit else R.string.add_or_manual),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.textSecondary,
            )
            Box(Modifier.weight(1f).height(1.dp).background(c.fieldBorder))
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (form.name.isNotBlank()) {
                AppTile(form.packageName.ifEmpty { form.name }, form.name, size = 84.dp, category = form.category.ifEmpty { null })
            } else {
                Box(
                    Modifier.size(84.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface)
                        .drawBehind {
                            drawRoundRect(
                                color = Color(0xFF93C5FD),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()),
                                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        T14Icon(T14Paths.Apps, size = 24.dp, tint = c.blueSoftText)
                        Text(stringResource(R.string.add_icon), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.blueSoftText)
                    }
                }
            }
            Text(stringResource(R.string.add_icon_hint), fontSize = 13.sp, lineHeight = 19.sp, color = c.textSecondary)
        }

        T14Field(
            stringResource(R.string.add_name), form.name, viewModel::onName,
            placeholder = stringResource(R.string.add_name_placeholder),
            error = if (NewAppField.NAME in errors) stringResource(R.string.add_name_error) else null,
        )
        T14Field(
            stringResource(R.string.add_package), form.packageName, viewModel::onPackage,
            placeholder = "com.example.app", mono = true, keyboard = KeyboardType.Uri,
            error = if (NewAppField.PACKAGE in errors) stringResource(R.string.add_package_error) else null,
            hint = stringResource(R.string.add_package_where),
        )
        CategoryField(form.category, viewModel::onCategory, error = NewAppField.CATEGORY in errors)
        T14Field(
            stringResource(R.string.add_description), form.shortDesc, viewModel::onDescription,
            placeholder = stringResource(R.string.add_description_placeholder),
            counter = "${form.shortDesc.length} / 300", minLines = 2,
            error = if (NewAppField.DESCRIPTION in errors) stringResource(R.string.add_description_error) else null,
        )
    }
}

/** The canvas uses a select: a 50dp box with the value and a chevron that opens the category list. */
@Composable
private fun CategoryField(value: String, onPick: (String) -> Unit, error: Boolean) {
    val c = T14Theme.colors
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.add_category), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Box {
            Row(
                Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp)).background(c.field)
                    .border(1.dp, if (error) c.dangerText else if (c.dark) c.fieldBorder else Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                    .clickable { open = true }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (value.isNotEmpty()) {
                    val st = Categories.style(value, value)
                    Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(tileBrush(st.color)), contentAlignment = Alignment.Center) {
                        T14Icon(st.path, size = 15.dp, tint = Color.White)
                    }
                }
                Text(
                    value.ifEmpty { stringResource(R.string.add_category_pick) }, fontSize = 15.sp,
                    color = if (value.isEmpty()) c.textTertiary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f),
                )
                T14Icon("M6 9l6 6 6-6", size = 20.dp, tint = c.textSecondary)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = MaterialTheme.colorScheme.surface) {
                AppCategories.all.forEach { cat ->
                    val st = Categories.style(cat, cat)
                    DropdownMenuItem(
                        text = { Text(cat, fontSize = 15.sp, fontWeight = if (cat == value) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(tileBrush(st.color)), contentAlignment = Alignment.Center) {
                                T14Icon(st.path, size = 15.dp, tint = Color.White)
                            }
                        },
                        onClick = { onPick(cat); open = false },
                    )
                }
            }
        }
        if (error) Text(stringResource(R.string.add_category_error), fontSize = 12.sp, color = c.dangerText)
    }
}

@Composable
private fun ConsolePage(state: AddAppUiState, viewModel: AddAppViewModel, done: Set<Int>, onToggle: (Int) -> Unit) {
    val c = T14Theme.colors
    val clipboard = LocalClipboardManager.current
    val todo = listOf(
        R.string.add_todo_track to R.string.add_todo_track_sub,
        R.string.add_todo_group to R.string.add_todo_group_sub,
        R.string.add_todo_countries to R.string.add_todo_countries_sub,
        R.string.add_todo_review to R.string.add_todo_review_sub,
    )
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.add_console_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp)
        Text(stringResource(R.string.add_console_sub, done.size), fontSize = 15.sp, lineHeight = 22.sp, color = c.textBody)
    }
    T14Card(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), radius = 20.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), spacing = 0.dp) {
        todo.forEachIndexed { i, (title, sub) ->
            val on = i in done
            Row(
                Modifier.fillMaxWidth()
                    .then(if (i < todo.lastIndex) Modifier.drawBehind { drawLine(c.divider, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) } else Modifier)
                    .clickable(role = Role.Checkbox) { onToggle(i) }.padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TickBox(on, 28.dp, Color(0xFF22C55E))
                Column(Modifier.padding(top = 3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(sub), fontSize = 13.sp, lineHeight = 19.sp, color = c.textSecondary)
                }
            }
        }
    }
    Column(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.blueSoft).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.add_group_label), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.blueSoftText)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(GROUP_EMAIL, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (c.dark) Color(0xFFDBEAFE) else Color(0xFF172554), modifier = Modifier.weight(1f))
            val copy = stringResource(R.string.copy)
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)
                    .clickable { clipboard.setText(AnnotatedString(GROUP_EMAIL)) }.semantics { contentDescription = copy },
                contentAlignment = Alignment.Center,
            ) { T14Icon("M10.5 8h7A2.5 2.5 0 0 1 20 10.5v7a2.5 2.5 0 0 1-2.5 2.5h-7A2.5 2.5 0 0 1 8 17.5v-7A2.5 2.5 0 0 1 10.5 8zM16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2", size = 18.dp, tint = c.blueSoftText) }
        }
    }
    val opt = state.form.optInUrl.trim()
    val pkg = state.form.packageName.trim()
    val valid = PlayLinks.isValidOptInUrl(opt)
    val matches = valid && pkg.isNotEmpty() && opt.endsWith(pkg)
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
        T14Field(
            stringResource(R.string.add_optin), state.form.optInUrl, viewModel::onOptIn,
            placeholder = "https://play.google.com/apps/testing/…", keyboard = KeyboardType.Uri,
            error = if (NewAppField.OPT_IN in state.errors && !valid) stringResource(R.string.add_optin_error) else null,
        )
        if (valid) {
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                T14Icon(if (matches) T14Paths.Check else T14Paths.Warning, size = 14.dp, tint = if (matches) c.successText else c.amberText, strokeWidth = 3f)
                Text(
                    stringResource(if (matches) R.string.add_optin_match else R.string.add_optin_mismatch),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (matches) c.successText else c.amberText,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- plan (wizard step 3)

@Composable
fun PlanRoute(onBack: () -> Unit, onPaid: (String) -> Unit, onBuy: () -> Unit = {}, viewModel: PlanViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = T14Theme.colors
    LaunchedEffect(state.paid) { if (state.paid) state.app?.let { onPaid(it.id) } }

    Column(Modifier.fillMaxSize()) {
        OwnerHeader(stringResource(R.string.add_title), onBack)
        WizardSteps(2)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.plan_choose), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp)
                Text(stringResource(R.string.plan_once), fontSize = 14.sp, color = c.textBody)
            }
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Plan.entries.forEach { plan ->
                    val on = state.selected == plan
                    val reward = state.economy.rewardPerDay(plan).value.toInt()
                    Column(
                        Modifier.fillMaxWidth()
                            .shadow(if (on || c.dark) 0.dp else 6.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x0D0F172A), spotColor = Color(0x140F172A))
                            .optionCard(on, 22.dp, c.dark, MaterialTheme.colorScheme.surface, c.blueSoft, c.fieldBorder)
                            .clickable(role = Role.RadioButton) { viewModel.select(plan) }
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Radio(on)
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(if (plan == Plan.PRO) R.string.plan_pro else R.string.plan_standard), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                                if (plan == Plan.PRO) {
                                    Text(
                                        stringResource(R.string.plan_faster), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp,
                                        color = if (c.dark) Color(0xFF0B1220) else Color.White,
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (c.dark) Color(0xFFF1F5F9) else Color(0xFF0F172A)).padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Coin14Icon(18.dp)
                                Text(formatCoins(state.economy.fee(plan).value), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = c.coinText)
                            }
                        }
                        Column(Modifier.padding(start = 34.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            val lines = if (plan == Plan.PRO) {
                                listOf(stringResource(R.string.plan_pro_line1, reward), stringResource(R.string.plan_pro_line2))
                            } else {
                                listOf(stringResource(R.string.plan_std_line1, reward), stringResource(R.string.plan_std_line2))
                            }
                            lines.forEach { l ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    T14Icon(T14Paths.Check, size = 18.dp, tint = c.successText, strokeWidth = 2.8f, modifier = Modifier.padding(top = 1.dp))
                                    Text(l, fontSize = 14.sp, lineHeight = 20.sp, color = c.textBody)
                                }
                            }
                        }
                    }
                }
            }
            T14Card(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp), radius = 18.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), spacing = 8.dp) {
                Text(stringResource(R.string.plan_both_caps), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.textSecondary, letterSpacing = 0.4.sp)
                Text(stringResource(R.string.plan_both_body), fontSize = 13.sp, lineHeight = 20.sp, color = c.textBody)
            }
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth()
                    .optionCard(state.top, 20.dp, c.dark, MaterialTheme.colorScheme.surface, c.blueSoft, c.fieldBorder)
                    .clickable(role = Role.Checkbox, onClick = viewModel::toggleTop).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TickBox(state.top, 24.dp, Track14Colors.Blue600)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.plan_top_title, PlanUiState.TOP_DAYS), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.plan_top_sub), fontSize = 12.sp, color = c.textSecondary)
                }
                Text("+" + formatCoins(state.topPrice.value), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.coinText)
            }
        }
        BottomBar {
            val balance = state.balance?.value
            Row {
                Text(
                    stringResource(R.string.plan_balance_total, balance?.let(::formatCoins) ?: "…", formatCoins(state.total.value)),
                    fontSize = 13.sp, color = c.textBody, modifier = Modifier.weight(1f),
                )
                if (state.shortage > 0) {
                    Text(stringResource(R.string.plan_shortage, formatCoins(state.shortage)), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.dangerText)
                }
            }
            state.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            if (state.shortage > 0) {
                Box(
                    Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF59E0B)).clickable(onClick = onBuy),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.plan_buy), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) }
            } else {
                val planName = stringResource(if (state.selected == Plan.PRO) R.string.plan_pro else R.string.plan_standard)
                PrimaryButton(
                    if (state.paying) stringResource(R.string.plan_paying) else stringResource(R.string.plan_pay_total, planName, formatCoins(state.total.value)),
                    onClick = viewModel::pay,
                    enabled = state.canPay,
                    height = 52.dp,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- dashboard

private val AvatarColors = listOf(
    Color(0xFF0EA5E9), Color(0xFFEC4899), Color(0xFF7C3AED), Color(0xFF16A34A), Color(0xFFEA580C), Color(0xFF475569),
)

private fun avatarColor(key: String): Color = AvatarColors[Math.floorMod(key.hashCode(), AvatarColors.size)]

@Composable
private fun Avatar(initials: String, key: String, size: Dp = 34.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(avatarColor(key)), contentAlignment = Alignment.Center) {
        Text(initials, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private enum class TesterFilter { ALL, WAITING, BROKEN }

private enum class TodayState { OK, WAITING, BROKEN, DONE, OTHER }

private fun CampaignTester.todayState(now: Long): TodayState = when (status) {
    TestStatus.FAILED, TestStatus.LEFT -> TodayState.BROKEN
    TestStatus.COMPLETED -> TodayState.DONE
    TestStatus.ACTIVE -> {
        val d = dayNumber(now)
        if (d != null && d in 1..days.size && days[d - 1] == DayStatus.PASSED) TodayState.OK else TodayState.WAITING
    }
    TestStatus.JOINING -> TodayState.OTHER
}

@Composable
fun DashboardRoute(
    onBack: () -> Unit,
    onReportTester: (String) -> Unit,
    onOpenTester: (String) -> Unit = {},
    onOpenFeedback: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val economy by viewModel.economyConfig.collectAsStateWithLifecycle()
    var confirmPublish by remember { mutableStateOf(false) }
    var showTop by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(TesterFilter.ALL) }
    val app = state.app
    val c = T14Theme.colors
    val now = remember(state.testers) { viewModel.now() }

    Column(Modifier.fillMaxSize()) {
        when {
            state.loading -> {
                OwnerHeader(stringResource(R.string.dashboard_title), onBack)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            app == null -> {
                OwnerHeader(stringResource(R.string.dashboard_title), onBack)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.detail_not_found), color = c.textSecondary) }
            }
            else -> {
                val campaign = app.campaign
                val campaignDay = campaign?.startedAtMillis?.let { start ->
                    uz.beko404.track14.domain.rules.DailyCheckRules.dayNumber(
                        java.time.Instant.ofEpochMilli(start), java.time.Instant.ofEpochMilli(now), java.time.ZoneId.systemDefault(),
                    )
                }
                OwnerHeader(
                    app.name, onBack,
                    leading = { AppTile(app.id, app.name, size = 40.dp, category = app.category) },
                    sub = { StatusLine(app.status, campaignDay) },
                )
                val active = state.testers.filter { it.status == TestStatus.ACTIVE }
                val counts = state.testers.groupingBy { it.todayState(now) }.eachCount()
                val shown = state.testers.filter {
                    when (filter) {
                        TesterFilter.ALL -> true
                        TesterFilter.WAITING -> it.todayState(now) == TodayState.WAITING
                        TesterFilter.BROKEN -> it.todayState(now) == TodayState.BROKEN
                    }
                }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
                    if (campaign != null) {
                        item { ProductionCard(active, campaign.completed, now) }
                        item {
                            val needed = campaign.needed
                            val avg = state.feedback.takeIf { it.isNotEmpty() }?.map { it.rating }?.average()
                            val best = active.maxOfOrNull { it.streak(now) } ?: 0
                            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    StatTile(
                                        stringResource(R.string.dash_testers), "${campaign.joined}",
                                        stringResource(R.string.dash_testers_sub, needed, (campaign.joined - needed).coerceAtLeast(0)), Modifier.weight(1f),
                                    )
                                    StatTile(
                                        stringResource(R.string.dash_opened_today), "${counts[TodayState.OK] ?: 0} / ${active.size}",
                                        stringResource(R.string.dash_opened_today_sub, counts[TodayState.WAITING] ?: 0), Modifier.weight(1f),
                                        valueColor = c.successText,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    StatTile(
                                        stringResource(R.string.dash_reached), "${campaign.completed} / $needed",
                                        stringResource(R.string.dash_reached_sub, best), Modifier.weight(1f),
                                    )
                                    StatTile(
                                        stringResource(R.string.dash_feedback),
                                        if (avg != null) "${state.feedback.size} · ${"%.1f".format(avg).replace('.', ',')}★" else "0",
                                        stringResource(R.string.dash_view), Modifier.weight(1f), subColor = c.blueSoftText, onClick = onOpenFeedback,
                                    )
                                }
                            }
                        }
                        item {
                            ActionsRow(
                                app = app,
                                busy = state.busy,
                                onAct = viewModel::act,
                                onTop = { showTop = true },
                                onPublish = { confirmPublish = true },
                            )
                        }
                    }
                    state.error?.let { e ->
                        item { Text(stringResource(e.messageRes()), color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) }
                    }
                    item {
                        T14Card(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), padding = PaddingValues(16.dp), spacing = 12.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.dash_testers), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                                Text(stringResource(R.string.dash_14_state), fontSize = 12.sp, color = c.textSecondary)
                            }
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterPill(stringResource(R.string.dash_filter_all, state.testers.size), filter == TesterFilter.ALL) { filter = TesterFilter.ALL }
                                FilterPill(stringResource(R.string.dash_filter_waiting, counts[TodayState.WAITING] ?: 0), filter == TesterFilter.WAITING) { filter = TesterFilter.WAITING }
                                FilterPill(stringResource(R.string.dash_filter_broken, counts[TodayState.BROKEN] ?: 0), filter == TesterFilter.BROKEN) { filter = TesterFilter.BROKEN }
                            }
                            if (shown.isEmpty()) {
                                Text(stringResource(R.string.dashboard_no_testers), fontSize = 13.sp, lineHeight = 19.sp, color = c.textSecondary)
                            }
                            shown.forEach { t -> TesterRow(t, now, onClick = { onOpenTester(t.testId) }) }
                        }
                    }
                }
            }
        }
    }

    if (showTop && app != null) {
        TopSheet(
            app = app,
            prices = listOf(1, 3, 7).map { it to economy.featurePrice(it).value },
            featuredUntil = app.campaign?.featuredUntilMillis?.takeIf { it > now },
            busy = state.busy,
            onDismiss = { showTop = false },
            onBuy = { days -> viewModel.feature(days); showTop = false },
        )
    }

    if (confirmPublish) {
        AlertDialog(
            onDismissRequest = { confirmPublish = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.dashboard_publish_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
            text = { Text(stringResource(R.string.dashboard_publish_body), fontSize = 15.sp, lineHeight = 22.sp, color = c.textBody) },
            confirmButton = {
                TextButton(onClick = { viewModel.act(OwnerAction.PUBLISH); confirmPublish = false }) {
                    Text(stringResource(R.string.dashboard_publish), fontWeight = FontWeight.Bold, color = c.successText)
                }
            },
            dismissButton = { TextButton(onClick = { confirmPublish = false }) { Text(stringResource(R.string.cancel), color = c.textSecondary) } },
        )
    }
}

@Composable
private fun StatusLine(status: AppStatus, day: Int?) {
    val c = T14Theme.colors
    val (label, color) = when (status) {
        AppStatus.ACTIVE -> stringResource(R.string.status_active) to c.successText
        AppStatus.PAUSED -> stringResource(R.string.status_paused) to c.amberText
        AppStatus.PUBLISHED -> stringResource(R.string.status_published) to c.blueSoftText
        AppStatus.SUSPENDED -> stringResource(R.string.status_suspended) to c.dangerText
        AppStatus.DRAFT -> stringResource(R.string.status_draft) to c.textSecondary
        AppStatus.REMOVED -> stringResource(R.string.status_removed) to c.textSecondary
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        val text = if (status == AppStatus.ACTIVE && day != null && day > 0) "$label · " + stringResource(R.string.dash_day_n, day) else label
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

/** Blue card: estimated date the owner can apply for production, days left, and the leader's 14 cells. */
@Composable
private fun ProductionCard(active: List<CampaignTester>, completed: Int, now: Long) {
    val best = active.maxOfOrNull { it.streak(now) } ?: 0
    val left = (14 - best).coerceAtLeast(0)
    val date = java.time.LocalDate.now().plusDays(left.toLong())
    val soft = Color(0xFFEFF6FF)
    Column(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Track14Colors.Blue600).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.dash_production), fontSize = 13.sp, color = soft)
                Text(
                    if (active.isEmpty() && completed == 0) stringResource(R.string.dash_waiting_testers)
                    else java.time.format.DateTimeFormatter.ofPattern("d-MMMM").format(date),
                    fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Color.White,
                )
            }
            if (active.isNotEmpty()) {
                Text(
                    stringResource(R.string.dash_days_left, left), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8),
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(14) { k ->
                    Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(if (k < best) Color.White else Color.White.copy(alpha = 0.18f)))
                }
            }
            Text(
                if (best > 0) stringResource(R.string.dash_leader, best) else stringResource(R.string.dash_leader_none),
                fontSize = 12.sp, lineHeight = 18.sp, color = soft,
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    subColor: Color = T14Theme.colors.textBody,
    onClick: (() -> Unit)? = null,
) {
    T14Card(modifier, radius = 18.dp, padding = PaddingValues(14.dp), spacing = 4.dp, onClick = onClick) {
        Text(label, fontSize = 12.sp, color = T14Theme.colors.textSecondary)
        Text(value, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = valueColor, maxLines = 1)
        Text(sub, fontSize = 12.sp, color = subColor, fontWeight = if (onClick != null) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

/** Pause / Resume, Top, Published: 64dp white tiles with an icon over a 12sp label. */
@Composable
private fun ActionsRow(app: OwnerApp, busy: Boolean, onAct: (OwnerAction) -> Unit, onTop: () -> Unit, onPublish: () -> Unit) {
    val c = T14Theme.colors
    val actions = app.availableActions
    val isActive = app.campaign?.status == AppStatus.ACTIVE
    if (actions.isEmpty() && !isActive) return
    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (OwnerAction.PAUSE in actions) ActionTile(stringResource(R.string.dashboard_pause), "M9 6v12M15 6v12", c.textBody, Modifier.weight(1f), enabled = !busy) { onAct(OwnerAction.PAUSE) }
        if (OwnerAction.RESUME in actions) ActionTile(stringResource(R.string.dashboard_resume), T14Paths.Play, c.blueSoftText, Modifier.weight(1f), enabled = !busy) { onAct(OwnerAction.RESUME) }
        if (isActive) ActionTile(stringResource(R.string.dash_top), "M12 19V5M6 11l6-6 6 6", Color(0xFFD97706), Modifier.weight(1f), labelColor = c.textBody, enabled = !busy, onClick = onTop)
        if (OwnerAction.PUBLISH in actions) ActionTile(stringResource(R.string.dashboard_publish), T14Paths.Check, c.successText, Modifier.weight(1f), green = true, enabled = !busy, onClick = onPublish)
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: String,
    tint: Color,
    modifier: Modifier,
    labelColor: Color = tint,
    green: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = T14Theme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.height(64.dp)
            .then(if (green || c.dark) Modifier else Modifier.shadow(6.dp, shape, ambientColor = Color(0x0D0F172A), spotColor = Color(0x140F172A)))
            .clip(shape)
            .background(if (green) c.successSoft else MaterialTheme.colorScheme.surface)
            .then(if (c.dark && !green) Modifier.border(1.dp, c.cardBorder, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        T14Icon(icon, size = 20.dp, tint = tint, strokeWidth = if (green) 2.6f else 2.2f)
        Text(label, fontSize = 12.sp, fontWeight = if (green) FontWeight.Bold else FontWeight.SemiBold, color = labelColor, maxLines = 1)
    }
}

@Composable
private fun FilterPill(text: String, on: Boolean, onClick: () -> Unit) {
    val c = T14Theme.colors
    Box(
        Modifier.height(34.dp).clip(RoundedCornerShape(999.dp)).background(if (on) c.chipOn else c.chipOff)
            .border(1.dp, if (on) c.chipOn else c.chipOffBorder, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.chipOnText else c.chipOffText) }
}

@Composable
private fun TesterRow(t: CampaignTester, now: Long, onClick: () -> Unit) {
    val c = T14Theme.colors
    val state = t.todayState(now)
    val today = t.dayNumber(now)
    Column(
        Modifier.fillMaxWidth()
            .drawBehind { drawLine(c.divider, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(t.initials, t.testId)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(t.name.ifBlank { stringResource(R.string.dash_tester) }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (t.device.isNotBlank()) Text(t.device, fontSize = 12.sp, color = c.textSecondary)
            }
            val (label, fg, bg) = when (state) {
                TodayState.OK -> Triple(stringResource(R.string.dash_state_ok), c.successText, c.successSoft)
                TodayState.WAITING -> Triple(stringResource(R.string.dash_state_waiting), c.amberText, if (c.dark) c.amberSoft else Color(0xFFFEF3C7))
                TodayState.BROKEN -> Triple(stringResource(R.string.dash_state_broken), c.dangerText, if (c.dark) c.dangerSoft else Color(0xFFFEE2E2))
                TodayState.DONE -> Triple(stringResource(R.string.dash_state_done), c.blueSoftText, c.blueSoft)
                TodayState.OTHER -> Triple(stringResource(R.string.dash_state_joining), c.textSecondary, c.divider)
            }
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 8.dp, vertical = 4.dp))
        }
        DayCells(t, today, state)
    }
}

/** 14 cells: green passed, red missed, amber today still open, grey after the test broke, light for days ahead. */
@Composable
private fun DayCells(t: CampaignTester, today: Int?, state: TodayState) {
    val c = T14Theme.colors
    val empty = if (c.dark) Color(0xFF263247) else Color(0xFFE2E8F0)
    val brokenAt = if (state == TodayState.BROKEN) t.days.indexOfLast { it != DayStatus.PENDING } + 1 else Int.MAX_VALUE
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(14) { i ->
            val s = t.days.getOrNull(i) ?: DayStatus.PENDING
            val color = when {
                s == DayStatus.PASSED -> Color(0xFF22C55E)
                s == DayStatus.MISSED -> Color(0xFFDC2626)
                i >= brokenAt && today != null && i < today -> Color(0xFF94A3B8)
                state == TodayState.WAITING && today == i + 1 -> Color(0xFFF59E0B)
                else -> empty
            }
            Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}

/** "Topga chiqarish" bottom sheet: preview, 1/3/7 day options, one pay button. */
@Composable
private fun TopSheet(
    app: OwnerApp,
    prices: List<Pair<Int, Long>>,
    featuredUntil: Long?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onBuy: (Int) -> Unit,
) {
    val c = T14Theme.colors
    var pick by remember { mutableIntStateOf(1) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color(0x8C111827),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp).size(width = 40.dp, height = 5.dp).clip(RoundedCornerShape(999.dp)).background(if (c.dark) Color(0xFF334155) else Color(0xFFCBD5E1)))
        },
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.feature_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text(stringResource(R.string.top_sheet_body), fontSize = 14.sp, lineHeight = 21.sp, color = c.textBody)
                if (featuredUntil != null) {
                    Text(
                        stringResource(
                            R.string.feature_active,
                            android.text.format.DateUtils.formatDateTime(
                                androidx.compose.ui.platform.LocalContext.current, featuredUntil,
                                android.text.format.DateUtils.FORMAT_SHOW_DATE or android.text.format.DateUtils.FORMAT_SHOW_TIME,
                            ),
                        ),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.blueSoftText,
                    )
                }
            }
            val st = Categories.style(app.category, app.id)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(st.color).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    Text(app.name.take(1).uppercase(), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = st.color)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(app.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                    Text(stringResource(R.string.top_preview_sub), fontSize = 12.sp, color = Color.White)
                }
                Text("TOP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.lerp(st.color, Color.Black, 0.55f), modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White).padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                prices.forEachIndexed { i, (days, price) ->
                    val on = i == pick
                    val sub = when (days) {
                        1 -> stringResource(R.string.top_sub_try)
                        3 -> stringResource(R.string.top_sub_popular, formatCoins(price / days))
                        else -> stringResource(R.string.top_sub_best, formatCoins(price / days))
                    }
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 60.dp)
                            .optionCard(on, 16.dp, c.dark, MaterialTheme.colorScheme.surface, c.blueSoft, c.fieldBorder)
                            .clickable(role = Role.RadioButton) { pick = i }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Radio(on)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.feature_days, days), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(sub, fontSize = 12.sp, color = c.textSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Coin14Icon(16.dp)
                            Text(formatCoins(price), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = c.coinText)
                        }
                    }
                }
            }
            val (days, price) = prices[pick]
            PrimaryButton(stringResource(R.string.top_pay, formatCoins(price)), onClick = { onBuy(days) }, enabled = !busy, height = 56.dp)
        }
    }
}

// ---------------------------------------------------------------- feedback list

@Composable
fun FeedbackListRoute(onBack: () -> Unit, viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = T14Theme.colors
    var tag by rememberSaveable { mutableStateOf<FeedbackTag?>(null) }
    val all = state.feedback
    val testers = state.testers.associateBy { it.testId }
    val shown = all.filter { tag == null || tag in it.tags }

    Column(Modifier.fillMaxSize()) {
        OwnerHeader(
            stringResource(R.string.fb_title), onBack,
            sub = { state.app?.let { Text(it.name, fontSize = 13.sp, color = c.textSecondary) } },
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                T14Card(padding = PaddingValues(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        val avg = all.takeIf { it.isNotEmpty() }?.map { it.rating }?.average() ?: 0.0
                        Column(Modifier.widthIn(min = 80.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("%.1f".format(avg).replace('.', ','), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp)
                            Text(stars(kotlin.math.round(avg).toInt()), fontSize = 14.sp, color = Color(0xFFD97706), letterSpacing = 1.sp)
                            Text(stringResource(R.string.fb_count, all.size), fontSize = 12.sp, color = c.textSecondary)
                        }
                        val max = (1..5).maxOf { s -> all.count { it.rating == s } }.coerceAtLeast(1)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            (5 downTo 1).forEach { s ->
                                val n = all.count { it.rating == s }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("$s", fontSize = 12.sp, color = c.textBody, modifier = Modifier.width(10.dp))
                                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(999.dp)).background(c.divider)) {
                                        Box(Modifier.fillMaxWidth(n / max.toFloat()).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(Color(0xFFF59E0B)))
                                    }
                                    Text("$n", fontSize = 12.sp, color = c.textBody, textAlign = TextAlign.End, modifier = Modifier.width(14.dp))
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPill(stringResource(R.string.fb_all), tag == null) { tag = null }
                    FeedbackTag.entries.forEach { t ->
                        val n = all.count { t in it.tags }
                        if (n > 0) FilterPill("${stringResource(t.filterLabel())} ($n)", tag == t) { tag = t }
                    }
                }
            }
            if (shown.isEmpty()) {
                item { Text(stringResource(R.string.fb_empty), fontSize = 14.sp, lineHeight = 20.sp, color = c.textSecondary, modifier = Modifier.padding(top = 8.dp)) }
            }
            items(shown, key = { it.id }) { f -> FeedbackCard(f, testers[f.testId]) }
        }
    }
}

private fun stars(n: Int): String = "★".repeat(n.coerceIn(0, 5)) + "☆".repeat(5 - n.coerceIn(0, 5))

private fun FeedbackTag.filterLabel(): Int = when (this) {
    FeedbackTag.BUG -> R.string.fb_filter_bug
    FeedbackTag.IDEA -> R.string.fb_filter_idea
    FeedbackTag.DESIGN -> R.string.fb_filter_design
    FeedbackTag.SPEED -> R.string.fb_filter_speed
    FeedbackTag.POSITIVE -> R.string.fb_filter_positive
}

private fun FeedbackTag.tagLabel(): Int = when (this) {
    FeedbackTag.BUG -> R.string.tag_bug
    FeedbackTag.IDEA -> R.string.tag_idea
    FeedbackTag.DESIGN -> R.string.tag_design
    FeedbackTag.SPEED -> R.string.tag_speed
    FeedbackTag.POSITIVE -> R.string.tag_positive
}

@Composable
private fun FeedbackCard(f: Feedback, tester: CampaignTester?) {
    val c = T14Theme.colors
    T14Card(radius = 20.dp, padding = PaddingValues(16.dp), spacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Avatar(tester?.initials ?: "?", f.testId.ifEmpty { f.id })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(tester?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.dash_tester), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                val date = if (f.createdAtMillis > 0) {
                    java.time.Instant.ofEpochMilli(f.createdAtMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        .format(java.time.format.DateTimeFormatter.ofPattern("d-MMM"))
                } else {
                    null
                }
                val meta = listOfNotNull(tester?.device?.takeIf { it.isNotBlank() }, date).joinToString(" · ")
                if (meta.isNotEmpty()) Text(meta, fontSize = 12.sp, color = c.textSecondary)
            }
            Text(stars(f.rating), fontSize = 13.sp, color = Color(0xFFD97706), letterSpacing = 1.sp)
        }
        if (f.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                f.tags.forEach { t ->
                    val (fg, bg) = when (t) {
                        FeedbackTag.BUG, FeedbackTag.SPEED -> c.dangerText to (if (c.dark) c.dangerSoft else Color(0xFFFEE2E2))
                        FeedbackTag.DESIGN, FeedbackTag.POSITIVE -> c.successText to c.successSoft
                        FeedbackTag.IDEA -> (if (c.dark) Color(0xFF93C5FD) else Color(0xFF1E40AF)) to c.blueSoft
                    }
                    Text(stringResource(t.tagLabel()), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg, modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 9.dp, vertical = 3.dp))
                }
            }
        }
        if (f.text.isNotBlank()) Text(f.text, fontSize = 14.sp, lineHeight = 21.sp, color = c.textBody)
    }
}
