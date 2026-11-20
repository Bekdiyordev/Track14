package uz.beko404.track14.feature.welcome

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.Coin14Icon
import uz.beko404.track14.core.designsystem.Manrope
import uz.beko404.track14.core.designsystem.PrimaryButton
import uz.beko404.track14.core.designsystem.ScreenBackground
import uz.beko404.track14.core.designsystem.T14Icon
import uz.beko404.track14.core.designsystem.T14Paths
import uz.beko404.track14.core.designsystem.T14Theme
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.core.designsystem.Track14Logo
import uz.beko404.track14.core.designsystem.Track14Wordmark
import uz.beko404.track14.core.settings.AppLanguage
import uz.beko404.track14.core.settings.ThemeMode

// ---------------------------------------------------------------- language (first launch)

@Composable
fun LanguageScreen(
    current: AppLanguage,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onContinue: (AppLanguage) -> Unit,
    onSelect: (AppLanguage) -> Unit = {},
) {
    var selected by remember { mutableStateOf(current) }
    val c = T14Theme.colors
    ScreenBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                // The bottom card grows with the theme picker, so the hero must fit whatever height is left.
                val full = maxHeight >= 300.dp
                val compact = !full && maxHeight >= 190.dp
                // Theme toggle: moon in light mode, sun in dark mode.
                val label = stringResource(if (darkTheme) R.string.theme_to_light else R.string.theme_to_dark)
                Box(
                    Modifier.align(Alignment.TopEnd).padding(16.dp).size(44.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface).border(1.dp, c.fieldBorder, CircleShape)
                        .clickable(onClick = onToggleTheme).semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    T14Icon(
                        if (darkTheme) SUN else MOON,
                        size = 22.dp, tint = MaterialTheme.colorScheme.onSurface, strokeWidth = 2.2f,
                    )
                }
                Column(
                    Modifier.fillMaxSize().padding(
                        start = 24.dp, end = 24.dp,
                        top = if (full) 40.dp else 12.dp, bottom = if (full) 24.dp else 12.dp,
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (full) 16.dp else 8.dp, Alignment.CenterVertically),
                ) {
                    Track14Logo(if (full) 112.dp else if (compact) 76.dp else 56.dp)
                    Track14Wordmark(if (full) 44 else if (compact) 32 else 26)
                    if (full || compact) {
                        Text(stringResource(R.string.brand_tagline), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.textBody)
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.lang_title), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text(stringResource(R.string.lang_subtitle), fontSize = 14.sp, color = c.textSecondary)
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppLanguage.entries.forEach { lang -> LanguageOption(lang, selected == lang) { selected = lang; onSelect(lang) } }
                }
                PrimaryButton(stringResource(R.string.action_continue), onClick = { onContinue(selected) }, height = 56.dp)
            }
        }
    }
}

private const val MOON = "M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z"
private const val SUN = "M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8 M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4"

@Composable
private fun LanguageOption(lang: AppLanguage, selected: Boolean, onClick: () -> Unit) {
    val c = T14Theme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape)
            .background(if (selected) c.blueSoft else MaterialTheme.colorScheme.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) Track14Colors.Blue600 else c.fieldBorder, shape)
            .clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (selected) Track14Colors.Blue600 else Color(0xFFCBD5E1), CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(Track14Colors.Blue600)) }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(lang.label()), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(lang.script()), fontSize = 13.sp, color = if (selected) c.textBody else c.textSecondary)
        }
    }
}

fun AppLanguage.label(): Int = when (this) {
    AppLanguage.UZ -> R.string.lang_uz
    AppLanguage.RU -> R.string.lang_ru
    AppLanguage.EN -> R.string.lang_en
}

private fun AppLanguage.script(): Int = when (this) {
    AppLanguage.UZ -> R.string.lang_uz_script
    AppLanguage.RU -> R.string.lang_ru_script
    AppLanguage.EN -> R.string.lang_en_script
}

/** Kept for callers that still show the small brand tile. */
@Composable
fun BrandMark(size: Int) {
    Track14Logo(size.dp)
}

// ---------------------------------------------------------------- onboarding

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val slides = listOf(
        R.string.onb1_title to R.string.onb1_body,
        R.string.onb2_title to R.string.onb2_body,
        R.string.onb3_title to R.string.onb3_body,
    )
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == slides.lastIndex
    val c = T14Theme.colors

    ScreenBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp), horizontalArrangement = Arrangement.End) {
                Box(
                    Modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onDone).padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.onb_skip), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.textBody) }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                        when (page) {
                            0 -> SlideDays()
                            1 -> SlideCoins()
                            else -> SlideOwner()
                        }
                    }
                    Column(Modifier.padding(horizontal = 28.dp).heightIn(min = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(slides[page].first), fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp)
                        Text(stringResource(slides[page].second), fontSize = 16.sp, lineHeight = 24.sp, color = c.textBody)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 22.dp, bottom = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    slides.indices.forEach { i ->
                        val width by animateDpAsState(if (i == pager.currentPage) 28.dp else 8.dp, label = "dot")
                        Box(
                            Modifier.height(8.dp).width(width).clip(RoundedCornerShape(999.dp))
                                .background(if (i == pager.currentPage) Track14Colors.Blue600 else (if (c.dark) Color(0xFF1E3A8A) else Color(0xFFBFDBFE))),
                        )
                    }
                }
                PrimaryButton(
                    stringResource(if (last) R.string.onb_start else R.string.onb_next),
                    onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    modifier = Modifier.width(IntrinsicSize.Max).widthIn(min = 132.dp),
                    height = 56.dp,
                )
            }
        }
    }
}

@Composable
private fun SlideCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.width(300.dp).shadow(if (T14Theme.colors.dark) 0.dp else 2.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface)
            .border(if (T14Theme.colors.dark) 1.dp else 0.dp, T14Theme.colors.cardBorder, RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

/** Slide 1: a test card with 14 cells filling up and today's check done. */
@Composable
private fun SlideDays() {
    val c = T14Theme.colors
    val green = Color(0xFF22C55E)
    val cells = listOf(green, green, green, green, Color(0xFFDC2626), green, green, green, Color(0xFFF59E0B)) +
        List(5) { if (c.dark) Color(0xFF263247) else Color(0xFFE2E8F0) }
    SlideCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF0EA5E9)), contentAlignment = Alignment.Center) {
                Text("L", color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Lug'at Pro", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.testdetail_day_of, 9), fontSize = 13.sp, color = c.textSecondary)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            cells.chunked(7).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { color -> Box(Modifier.weight(1f).height(30.dp).clip(RoundedCornerShape(8.dp)).background(color)) }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            T14Icon(T14Paths.Check, size = 18.dp, tint = c.successText, strokeWidth = 2.4f)
            Text(stringResource(R.string.onb1_done), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.successText)
        }
    }
}

/** Slide 2: a big coin with the per-day and bonus amounts. */
@Composable
private fun SlideCoins() {
    val c = T14Theme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.size(150.dp).clip(CircleShape).background(if (c.dark) Color(0xFF2A200A) else Color(0xFFFEF3C7)), contentAlignment = Alignment.Center) {
            Coin14Icon(88.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(stringResource(R.string.onb2_chip_day), stringResource(R.string.onb2_chip_bonus)).forEach {
                Text(
                    it, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (c.dark) c.coinText else Color(0xFFB45309),
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** Slide 3: the owner's campaign card filling up with testers. */
@Composable
private fun SlideOwner() {
    val c = T14Theme.colors
    SlideCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Brush.verticalGradient(listOf(Color(0xFF3B82F6), Track14Colors.Blue600))),
                contentAlignment = Alignment.Center,
            ) { Text("M", color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.onb3_card_title), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.onb3_card_sub), fontSize = 13.sp, color = c.textSecondary)
            }
        }
        Row {
            Text(stringResource(R.string.dash_testers), fontSize = 14.sp, color = c.textBody, modifier = Modifier.weight(1f))
            Text("9 / 12", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)).background(c.progressTrack)) {
            Box(Modifier.fillMaxWidth(0.75f).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(Track14Colors.Blue600))
        }
        Box(Modifier.height(32.dp)) {
            listOf("A" to 0xFF0EA5E9, "D" to 0xFF22C55E, "S" to 0xFFF59E0B, "M" to 0xFFEC4899, "B" to 0xFF8B5CF6, "+4" to 0xFF475569).forEachIndexed { i, (t, color) ->
                Box(
                    Modifier.offset(x = (i * 24).dp).size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).padding(2.dp).clip(CircleShape).background(Color(color)),
                    contentAlignment = Alignment.Center,
                ) { Text(t, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

fun ThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> systemDark
}
