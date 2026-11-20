package uz.beko404.track14.admin.ui

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.beko404.track14.admin.feature.Screen
import uz.beko404.track14.domain.repository.DomainError

// ---------------------------------------------------------------- icons

/** Stroke icons from the admin canvas (24-unit SVG paths). */
object AdminPaths {
    const val Home = "M3 11l9-8 9 8v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"
    const val Users = "M9 4.5a3.5 3.5 0 1 0 0 7a3.5 3.5 0 1 0 0-7z M2.5 20c.8-3.5 3.3-5 6.5-5s5.7 1.5 6.5 5 M17 6.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z M17 14.5c2.5.2 4 1.7 4.5 5"
    const val Apps = "M5 3h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M16 3h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M5 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2z M16 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2z"
    const val Bell = "M6 16V11a6 6 0 0 1 12 0v5l2 2H4zM10 21h4"
    const val Back = "M15 5l-7 7 7 7"
    const val Chevron = "M9 5l7 7-7 7"
    const val Search = "M11 4a7 7 0 1 0 0 14a7 7 0 1 0 0-14z M20 20l-4-4"
    const val Refresh = "M20 11a8 8 0 1 0-2.3 5.7 M20 4v7h-7"
    const val Upload = "M12 3v12M7 8l5-5 5 5 M4 15v4a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4"
    const val Flag = "M5 21V4M5 4h11l-2 4 2 4H5"
    const val Ban = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0 M5.6 5.6l12.8 12.8"
    const val Gear = "M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6zM4 12h2M18 12h2M12 4v2M12 18v2"
    const val Help = "M9.5 9a2.5 2.5 0 1 1 3.5 2.3c-.6.3-1 .9-1 1.7M12 17v.5 M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0"
    const val History = "M12 7v5l3 2 M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0"
    const val Logout = "M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3 M10 16l-4-4 4-4 M6 12h10"
    const val List = "M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01"
}

/** Draws a 24-unit stroke path like the canvas SVGs. */
@Composable
fun AdminIcon(path: String, modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = AdminColors.Text, strokeWidth: Float = 2f) {
    val parsed = remember(path) { PathParser().parsePathString(path).toPath() }
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 24f
        scale(k, k, pivot = Offset.Zero) {
            drawPath(parsed, tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Dots icon (filled circles) for the "Ko'proq" tab. */
@Composable
fun DotsIcon(tint: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val k = this.size.minDimension / 24f
        listOf(5f, 12f, 19f).forEach { x -> drawCircle(tint, radius = 1.6f * k, center = Offset(x * k, 12f * k)) }
    }
}

/** Track14 mark: blue rounded square, ring of 14 segments, "14". */
@Composable
fun AdminLogo(size: Dp) {
    val textSize = with(LocalDensity.current) { (size * 0.25f).toSp() }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val k = this.size.minDimension / 120f
            drawRoundRect(
                Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)), Offset.Zero, Offset(this.size.width, this.size.height)),
                cornerRadius = CornerRadius(30f * k),
            )
            val r = 38f * k
            repeat(14) { i ->
                drawArc(
                    Color.White.copy(alpha = 0.22f + 0.06f * i), -80.24f + i * (360f / 14f), 6.2f, false,
                    topLeft = Offset(60f * k - r, 60f * k - r), size = Size(r * 2, r * 2),
                    style = Stroke(width = 9f * k, cap = StrokeCap.Round),
                )
            }
        }
        Text("14", color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = textSize)
    }
}

// ---------------------------------------------------------------- header and status

/** 64dp navy band: optional back, Manrope 19/800 title, optional refresh, blue ADMIN badge. */
@Composable
fun AdminHeader(title: String, onBack: (() -> Unit)? = null, onRefresh: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().background(AdminColors.Navy).statusBarsPadding().height(64.dp).padding(start = 6.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (onBack != null) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                AdminIcon(AdminPaths.Back, tint = Color.White, strokeWidth = 2.2f)
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(title, color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, maxLines = 1, modifier = Modifier.weight(1f))
        if (onRefresh != null) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onRefresh), contentAlignment = Alignment.Center) {
                AdminIcon(AdminPaths.Refresh, size = 20.dp, tint = Color(0xFFCBD5E1))
            }
        }
        Text(
            "ADMIN", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AdminColors.Blue).padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** Loading bar, error and success lines shown under the header of every screen. */
@Composable
fun <T> StatusLine(state: Screen<T>) {
    if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AdminColors.Blue, trackColor = AdminColors.BlueSoft)
    state.error?.let {
        Notice(errorText(it), AdminColors.DangerSoft, AdminColors.DangerText, Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp))
    }
    state.done?.let {
        Notice(it, AdminColors.SuccessSoft, AdminColors.Success, Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp))
    }
}

@Composable
fun Notice(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Text(
        text, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).padding(14.dp),
    )
}

fun errorText(e: DomainError): String = when (e) {
    DomainError.PermissionDenied -> "Ruxsat yo'q: rolingiz bu amal uchun yetmaydi"
    DomainError.Network -> "Internet yoki server bilan aloqa yo'q"
    DomainError.NotFound -> "Topilmadi"
    is DomainError.Rejected -> when (e.reason) {
        "invalid_input" -> "Qiymatlar noto'g'ri, tekshirib qayta kiriting"
        "cannot_block_self" -> "O'zingizni bloklay olmaysiz"
        "cannot_demote_self" -> "O'z superadmin rolingizni olib tashlay olmaysiz"
        "cannot_delete_self" -> "O'z akkauntingizni bu yerdan o'chira olmaysiz"
        "not_scheduled" -> "Bu push allaqachon yuborilgan yoki bekor qilingan"
        "invalid_send_at" -> "Vaqt hozirdan 30 kungacha bo'lishi kerak"
        else -> "Server rad etdi: ${e.reason}"
    }
    else -> "Xatolik yuz berdi"
}

// ---------------------------------------------------------------- blocks

/** "SECTION" caps title: 13/700 slate, letter spacing 0.4. */
@Composable
fun Caps(text: String, top: Dp = 20.dp) {
    Text(
        text.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdminColors.Muted, letterSpacing = 0.4.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = top, bottom = 8.dp),
    )
}

/** White rounded block (18dp) with 20dp side margins. */
@Composable
fun Card(modifier: Modifier = Modifier, radius: Dp = 18.dp, padding: PaddingValues = PaddingValues(16.dp), spacing: Dp = 8.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(radius)).background(Color.White).padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** Card holding rows separated by hairlines (lists, menus). */
@Composable
fun RowsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp).then(modifier).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White), content = content)
}

fun Modifier.hairline(on: Boolean = true): Modifier =
    if (!on) this else drawBehind { drawLine(AdminColors.Divider, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }

@Composable
fun KeyValue(key: String, value: String, divider: Boolean = true) {
    Row(Modifier.fillMaxWidth().hairline(divider).padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(key, color = AdminColors.Muted, fontSize = 14.sp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accent: Color = AdminColors.Text, sub: String? = null, onClick: (() -> Unit)? = null) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, fontSize = 12.sp, color = AdminColors.Muted)
        Text(value, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = accent, maxLines = 1)
        if (sub != null) Text(sub, fontSize = 12.sp, color = AdminColors.Muted, maxLines = 1)
    }
}

/** Small stat box (user page): value Manrope 19/800 over an 11sp label. */
@Composable
fun MiniStat(value: String, label: String, modifier: Modifier = Modifier, accent: Color = AdminColors.Text) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = accent, maxLines = 1)
        Text(label, fontSize = 11.sp, color = AdminColors.Muted)
    }
}

enum class Tone(val fg: Color, val bg: Color) {
    GREEN(AdminColors.Success, AdminColors.SuccessSoft),
    RED(AdminColors.DangerText, AdminColors.DangerSoft),
    AMBER(AdminColors.AmberText, AdminColors.AmberSoft),
    BLUE(AdminColors.BlueDark, AdminColors.BlueSoft),
    GREY(AdminColors.Body, AdminColors.Divider),
}

@Composable
fun Pill(text: String, tone: Tone, modifier: Modifier = Modifier) {
    Text(
        text, color = tone.fg, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(999.dp)).background(tone.bg).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Filter chip: navy when on, white with a hairline when off. */
@Composable
fun FilterPill(text: String, on: Boolean, onClick: () -> Unit) {
    Text(
        text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else AdminColors.Body,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (on) AdminColors.Navy else Color.White)
            .border(1.dp, if (on) AdminColors.Navy else AdminColors.Border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** Option chip inside sheets and forms: blue outline when on, grey when off. */
@Composable
fun OptionChip(text: String, on: Boolean, onClick: () -> Unit, danger: Boolean = false) {
    val (bg, fg) = when {
        on && danger -> AdminColors.DangerSoft to AdminColors.DangerText
        on -> AdminColors.BlueSoft to AdminColors.BlueDark
        else -> AdminColors.Divider to AdminColors.Body
    }
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg)
            .then(if (on && !danger) Modifier.border(1.5.dp, AdminColors.Blue, RoundedCornerShape(999.dp)) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

/** Circle with initials (users). */
@Composable
fun Avatar(name: String, key: String, size: Dp = 44.dp) {
    val initials = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }.ifEmpty { "?" }
    Box(Modifier.size(size).clip(CircleShape).background(accentFor(key)), contentAlignment = Alignment.Center) {
        Text(initials, color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.39f).sp)
    }
}

/** Rounded square with the first letter (apps). */
@Composable
fun AppSquare(name: String, key: String, size: Dp = 46.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(accentFor(key)), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase().ifEmpty { "?" }, color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.39f).sp)
    }
}

private val Accents = listOf(
    Color(0xFF0EA5E9), Color(0xFFEC4899), Color(0xFF7C3AED), Color(0xFFEA580C), Color(0xFF16A34A), Color(0xFF0891B2), Color(0xFF6366F1),
)

fun accentFor(key: String): Color = Accents[Math.floorMod(key.hashCode(), Accents.size)]

/** Row inside a [RowsCard]: 40dp tinted icon tile, title + sub, optional badge, chevron. */
@Composable
fun MenuRow(
    title: String,
    subtitle: String? = null,
    icon: String? = null,
    iconBg: Color = AdminColors.BlueSoft,
    iconFg: Color = AdminColors.BlueDark,
    badge: String? = null,
    divider: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().hairline(divider).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                AdminIcon(icon, size = 20.dp, tint = iconFg)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, fontSize = 12.sp, color = AdminColors.Muted) }
        }
        if (badge != null) Pill(badge, Tone.RED)
        AdminIcon(AdminPaths.Chevron, size = 18.dp, tint = AdminColors.Faint)
    }
}

// ---------------------------------------------------------------- inputs and buttons

/** Labelled field: 46dp box (or multi-line), 12dp corners, hairline border. */
@Composable
fun AdminField(
    label: String?,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minLines: Int = 1,
    keyboard: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    hint: String? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AdminColors.Body)
        val single = minLines == 1
        Box(
            Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(RoundedCornerShape(12.dp))
                .background(if (enabled) Color.White else AdminColors.Divider)
                .border(1.dp, AdminColors.Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = if (single) 0.dp else 11.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val style = MaterialTheme.typography.bodyMedium.copy(color = AdminColors.Text, lineHeight = 21.sp)
            if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, style = style.copy(color = AdminColors.Faint))
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = single, minLines = minLines, enabled = enabled,
                textStyle = style, cursorBrush = SolidColor(AdminColors.Blue),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard), modifier = Modifier.fillMaxWidth(),
            )
        }
        hint?.let { Text(it, fontSize = 12.sp, color = AdminColors.Muted) }
    }
}

/** Search box from the list screens: 46dp white, hairline, magnifier. */
@Composable
fun SearchBox(value: String, onChange: (String) -> Unit, placeholder: String, onSearch: () -> Unit = {}) {
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp).fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp))
            .background(Color.White).border(1.dp, AdminColors.Border, RoundedCornerShape(14.dp)).padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AdminIcon(AdminPaths.Search, size = 20.dp, tint = AdminColors.Faint)
        Box(Modifier.weight(1f)) {
            val style = MaterialTheme.typography.bodyMedium.copy(color = AdminColors.Text)
            if (value.isEmpty()) Text(placeholder, style = style.copy(color = AdminColors.Faint), maxLines = 1)
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true, textStyle = style,
                cursorBrush = SolidColor(AdminColors.Blue),
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Text(
                "Qidirish", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AdminColors.Blue,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onSearch).padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

enum class ButtonStyle { PRIMARY, OUTLINE, NEUTRAL, DANGER, DANGER_OUTLINE }

@Composable
fun AdminButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.PRIMARY,
    enabled: Boolean = true,
    height: Dp = 52.dp,
) {
    val shape = RoundedCornerShape(16.dp)
    val (bg, fg, border) = when (style) {
        ButtonStyle.PRIMARY -> Triple(AdminColors.Blue, Color.White, null)
        ButtonStyle.OUTLINE -> Triple(Color.White, AdminColors.Blue, AdminColors.Blue)
        ButtonStyle.NEUTRAL -> Triple(Color.White, AdminColors.Body, AdminColors.Border)
        ButtonStyle.DANGER -> Triple(AdminColors.Danger, Color.White, null)
        ButtonStyle.DANGER_OUTLINE -> Triple(Color.White, AdminColors.DangerText, AdminColors.DangerBorder)
    }
    Box(
        modifier.fillMaxWidth().height(height).clip(shape)
            .background(if (enabled) bg else if (style == ButtonStyle.PRIMARY || style == ButtonStyle.DANGER) AdminColors.Border else bg)
            .then(if (border != null) Modifier.border(1.5.dp, border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = if (enabled) fg else if (style == ButtonStyle.PRIMARY || style == ButtonStyle.DANGER) AdminColors.Faint else fg.copy(alpha = 0.5f),
        )
    }
}

/** 50x30 switch from the canvas. */
@Composable
fun AdminSwitch(on: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val track by animateColorAsState(if (on) AdminColors.Blue else Color(0xFFCBD5E1), label = "track")
    val knob by animateDpAsState(if (on) 23.dp else 3.dp, label = "knob")
    Box(
        Modifier.size(width = 50.dp, height = 30.dp).clip(RoundedCornerShape(999.dp)).background(if (enabled) track else track.copy(alpha = 0.5f))
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!on) },
    ) {
        Box(Modifier.padding(start = knob, top = 3.dp).size(24.dp).shadow(1.dp, CircleShape).clip(CircleShape).background(Color.White))
    }
}

/** Title + sub with a switch at the end. */
@Composable
fun SwitchRow(title: String, sub: String?, on: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true, divider: Boolean = false) {
    Row(Modifier.fillMaxWidth().hairline(divider).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            sub?.let { Text(it, fontSize = 12.sp, lineHeight = 17.sp, color = AdminColors.Muted) }
        }
        AdminSwitch(on, onChange, enabled)
    }
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AdminColors.Blue) }
}

@Composable
fun Empty(text: String) {
    Text(text, fontSize = 14.sp, color = AdminColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(28.dp))
}

// ---------------------------------------------------------------- sheets

/** Bottom sheet from the A-screens: white, 28dp top corners, Manrope 20 title, actions at the bottom. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        scrimColor = Color(0x8C111827),
        dragHandle = null,
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            content()
        }
    }
}

/** Cancel + confirm pair (1 : 1.4) at the bottom of a sheet. */
@Composable
fun SheetActions(confirm: String, onConfirm: () -> Unit, onCancel: () -> Unit, danger: Boolean = true, enabled: Boolean = true) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AdminButton("Bekor qilish", onCancel, Modifier.weight(1f), ButtonStyle.NEUTRAL, height = 54.dp)
        AdminButton(confirm, onConfirm, Modifier.weight(1.4f), if (danger) ButtonStyle.DANGER else ButtonStyle.PRIMARY, enabled, height = 54.dp)
    }
}

/** Confirmation with an optional reason field; [requireReason] keeps the button disabled until 3+ chars. */
@Composable
fun ReasonDialog(
    title: String,
    confirm: String,
    requireReason: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    body: String? = null,
    danger: Boolean = true,
    extra: @Composable () -> Unit = {},
) {
    var reason by remember { mutableStateOf("") }
    AdminSheet(title, onDismiss) {
        body?.let { Text(it, fontSize = 14.sp, lineHeight = 21.sp, color = Color(0xFF475569)) }
        AdminField(if (requireReason) "Sabab (majburiy)" else "Izoh (ixtiyoriy)", reason, { reason = it.take(300) }, minLines = 3)
        extra()
        SheetActions(confirm, { onConfirm(reason) }, onDismiss, danger, enabled = !requireReason || reason.trim().length >= 3)
    }
}

fun ago(millis: Long): String = if (millis <= 0) "—" else DateUtils.getRelativeTimeSpanString(millis).toString()

fun dateTime(millis: Long): String =
    if (millis <= 0) "—" else java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(millis))

fun shortDate(millis: Long): String =
    if (millis <= 0) "—" else java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(millis))

fun formatNumber(v: Long): String {
    val digits = kotlin.math.abs(v).toString().reversed().chunked(3).joinToString(" ").reversed()
    return if (v < 0) "−$digits" else digits
}
