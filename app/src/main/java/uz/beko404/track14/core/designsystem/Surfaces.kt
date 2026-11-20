package uz.beko404.track14.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    val dark = isSystemInDarkTheme()
    val top = if (dark) Color(0xFF111B33) else Color(0xFFEAF1FF)
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
    val dark = isSystemInDarkTheme()
    Column(
        modifier
            .fillMaxWidth()
            .shadow(if (dark) 0.dp else 6.dp, shape, ambientColor = Track14Colors.Navy.copy(alpha = 0.05f), spotColor = Track14Colors.Navy.copy(alpha = 0.08f))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (dark) Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** Main action: 52dp, 16dp corners, brand blue, Inter 16/600. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Track14Colors.Blue600, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
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
        border = BorderStroke(1.5.dp, if (danger) Color(0xFFFECACA) else Color(0xFFBFDBFE)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = color),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Filter pill: selected is navy with white text, the rest white with a hairline border. */
@Composable
fun T14Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(999.dp)
    val selectedBg = if (isSystemInDarkTheme()) Color.White else Track14Colors.Navy
    val selectedFg = if (isSystemInDarkTheme()) Track14Colors.Navy else Color.White
    Box(
        modifier.height(36.dp).clip(shape)
            .background(if (selected) selectedBg else MaterialTheme.colorScheme.surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
            color = if (selected) selectedFg else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
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
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
        }
        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize.times(18f / 17f)), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

/** Section heading inside a page: Manrope 18/800 with an optional action on the right. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize.times(18f / 17f)), modifier = Modifier.weight(1f))
        if (action != null) {
            Text(action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp))
        }
    }
}

/** Coin balance pill from the canvas header: gold coin, amount in amber-brown. */
@Composable
fun CoinPill(amount: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(999.dp)
    val dark = isSystemInDarkTheme()
    Row(
        modifier.height(40.dp).clip(shape)
            .background(if (dark) Color(0xFF3A2A06) else Color(0xFFFFFBEB))
            .border(1.dp, if (dark) Color(0xFF78590F) else Color(0xFFFDE68A), shape)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Coin14Icon(size = 20.dp)
        Text(amount, style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize), color = if (dark) Color(0xFFFDE68A) else Color(0xFF92400E))
    }
}
