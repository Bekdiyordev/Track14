package uz.beko404.track14.feature.welcome

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.beko404.track14.R
import uz.beko404.track14.core.designsystem.Coin14Icon
import uz.beko404.track14.core.designsystem.Track14Colors
import uz.beko404.track14.core.settings.AppLanguage
import uz.beko404.track14.core.settings.ThemeMode

// ---------------------------------------------------------------- language (first launch)

@Composable
fun LanguageScreen(
    current: AppLanguage,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onContinue: (AppLanguage) -> Unit,
) {
    var selected by remember { mutableStateOf(current) }
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            // Theme toggle: sun in dark mode, moon in light mode (canvas decision), monochrome.
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.size(44.dp).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
            ) {
                Icon(
                    painterResource(if (darkTheme) R.drawable.ic_sun else R.drawable.ic_moon),
                    contentDescription = stringResource(if (darkTheme) R.string.theme_to_light else R.string.theme_to_dark),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            BrandMark(size = 88)
            Text(stringResource(R.string.lang_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.lang_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppLanguage.entries.forEach { lang ->
                LanguageOption(lang, selected == lang) { selected = lang }
            }
        }
        Button(
            onClick = { onContinue(selected) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text(stringResource(R.string.action_continue)) }
    }
}

@Composable
private fun LanguageOption(lang: AppLanguage, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        label = "lang",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(lang.label()), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

fun AppLanguage.label(): Int = when (this) {
    AppLanguage.UZ -> R.string.lang_uz
    AppLanguage.RU -> R.string.lang_ru
    AppLanguage.EN -> R.string.lang_en
}

@Composable
fun BrandMark(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(Brush.linearGradient(listOf(Track14Colors.Blue500, Track14Colors.Blue700)), RoundedCornerShape((size * 0.28).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size((size * 1.4).dp))
    }
}

// ---------------------------------------------------------------- onboarding

private data class Slide(val title: Int, val body: Int, val icon: ImageVector?, val coin: Boolean = false)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val slides = listOf(
        Slide(R.string.onb1_title, R.string.onb1_body, Icons.Filled.CheckCircle),
        Slide(R.string.onb2_title, R.string.onb2_body, null, coin = true),
        Slide(R.string.onb3_title, R.string.onb3_body, Icons.Filled.Send),
    )
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == slides.lastIndex

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!last) TextButton(onClick = onDone) { Text(stringResource(R.string.onb_skip)) }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val s = slides[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Brush.linearGradient(listOf(Track14Colors.Blue500, Track14Colors.Blue700)), RoundedCornerShape(40.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        s.coin -> Coin14Icon(72.dp)
                        s.icon != null -> Icon(s.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                    }
                    if (page == 0) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFDE68A), modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).size(22.dp))
                    }
                }
                Text(stringResource(s.title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Text(stringResource(s.body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.Center) {
            slides.indices.forEach { i ->
                val width by animateDpAsState(if (i == pager.currentPage) 24.dp else 8.dp, label = "dot")
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(width)
                        .background(
                            if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(999.dp),
                        ),
                )
            }
        }
        Button(
            onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text(stringResource(if (last) R.string.onb_start else R.string.onb_next)) }
    }
}

fun ThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> systemDark
}
