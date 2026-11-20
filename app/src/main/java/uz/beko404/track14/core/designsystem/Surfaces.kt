package uz.beko404.track14.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.beko404.track14.R

/**
 * Shared pieces of the canvas: the soft blue page gradient, white cards with a faint shadow,
 * 52dp buttons with 16dp corners, the pill chips and the screen / section titles.
 * Screens use these instead of raw Material defaults so every page looks the same.
 */

/** Page background: #EAF1FF fading to #F4F7FB over the first 280dp (dark: navy tones). */
@Composable
fun ScreenBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val top = T14Theme.colors.gradientTop
    val bottom = MaterialTheme.colorScheme.background
    val end = with(LocalDensity.current) { 280.dp.toPx() }
    Box(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .background(Brush.verticalGradient(0f to top, 1f to bottom, endY = end)),
        content = content,
    )
}

private val CardShape = RoundedCornerShape(22.dp)

/** White card, 22dp corners, the canvas shadow (0 1 2 / 0 6 18 at 5% navy). */
@Composable
fun T14Card(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    padding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = if (radius == 22.dp) CardShape else RoundedCornerShape(radius)
    val c = T14Theme.colors
    val dark = c.dark
    Column(
        modifier
            .fillMaxWidth()
            .shadow(if (dark) 0.dp else 6.dp, shape, ambientColor = Track14Colors.Navy.copy(alpha = 0.05f), spotColor = Track14Colors.Navy.copy(alpha = 0.08f))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (dark) Modifier.border(1.dp, c.cardBorder, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** Main action: blue gradient (#3B82F6 → #2563EB), 16dp corners, soft blue shadow, Inter 16/600. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, height: Dp = 52.dp) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.fillMaxWidth().height(height)
            .shadow(if (enabled) 10.dp else 0.dp, shape, ambientColor = Color(0x472563EB), spotColor = Color(0x472563EB))
            .clip(shape)
            .background(
                if (enabled) Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB)))
                else Brush.verticalGradient(listOf(Color(0xFF93C5FD), Color(0xFF93C5FD))),
            )
            .clickable(enabled = enabled, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White) }
}

/** Second action: white with the light blue border (#BFDBFE) and blue text. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, danger: Boolean = false) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, if (danger) T14Theme.colors.dangerBorder else T14Theme.colors.blueBorder),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = color),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Filter pill: selected is navy with white text, the rest white with a hairline border. */
@Composable
fun T14Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(999.dp)
    val c = T14Theme.colors
    Box(
        modifier.height(36.dp).clip(shape)
            .background(if (selected) c.chipOn else c.chipOff)
            .then(if (selected) Modifier else Modifier.border(1.dp, c.chipOffBorder, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (selected) c.chipOnText else c.chipOffText,
        )
    }
}

/** Tab screen title row: Manrope 26/800 on the left, optional trailing slot (coin pill, button). */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

/** Pushed screen top bar: 44dp back target plus a Manrope 18/800 title. */
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        BackButton(onBack)
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

/** 44dp back target with the canvas chevron (stroke 2.2). */
@Composable
fun BackButton(onBack: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface) {
    val label = stringResource(R.string.detail_back)
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onBack).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { T14Icon(T14Paths.Back, tint = tint, strokeWidth = 2.2f) }
}

/** Search box from the home screen: 48dp, 14dp corners, hairline border, magnifier on the left. */
@Composable
fun T14SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = T14Theme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().height(48.dp).clip(shape).background(c.field).border(1.dp, c.fieldBorder, shape).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        T14Icon(T14Paths.Search, size = 20.dp, tint = c.textSecondary)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
            )
        }
        if (value.isNotEmpty()) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).clickable { onValueChange("") }, contentAlignment = Alignment.Center) {
                T14Icon(T14Paths.Close, size = 18.dp, tint = c.textSecondary)
            }
        }
    }
}

/** Section heading inside a page: Manrope 18/800 with an optional action on the right. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), modifier = Modifier.weight(1f))
        if (action != null) {
            Text(action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp))
        }
    }
}

/** Coin balance pill from the canvas header: gold coin, amount in amber-brown. */
@Composable
fun CoinPill(amount: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(999.dp)
    val c = T14Theme.colors
    Row(
        modifier.height(40.dp).clip(shape)
            .background(c.coinBg)
            .border(1.dp, c.coinBorder, shape)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Coin14Icon(size = 20.dp)
        Text(amount, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = c.coinText)
    }
}
