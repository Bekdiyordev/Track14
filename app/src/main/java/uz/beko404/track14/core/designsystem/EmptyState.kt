package uz.beko404.track14.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** How an empty block is tinted: neutral blue, or red for errors. */
enum class EmptyTone { INFO, ERROR }

/**
 * Empty list: soft round icon, Manrope title, one or two lines of explanation, optional button.
 * [card] wraps it in a white card; otherwise it sits on the page (inside an existing card, say).
 * [compact] is the small form used inside cards (history lists).
 */
@Composable
fun EmptyState(
    icon: String,
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    card: Boolean = true,
    compact: Boolean = false,
    tone: EmptyTone = EmptyTone.INFO,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = T14Theme.colors
    val (bg, fg) = when (tone) {
        EmptyTone.INFO -> c.blueSoft to c.blueSoftText
        EmptyTone.ERROR -> c.dangerSoft to c.dangerText
    }
    val body: @Composable () -> Unit = {
        Column(
            Modifier.fillMaxWidth().padding(vertical = if (compact) 8.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
        ) {
            Box(Modifier.size(if (compact) 44.dp else 64.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                T14Icon(icon, size = if (compact) 22.dp else 30.dp, tint = fg)
            }
            Text(
                title, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = if (compact) 15.sp else 17.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
            )
            if (description != null) {
                Text(description, fontSize = if (compact) 13.sp else 14.sp, lineHeight = if (compact) 19.sp else 21.sp, color = c.textSecondary, textAlign = TextAlign.Center)
            }
            if (action != null && onAction != null) {
                PrimaryButton(action, onClick = onAction, modifier = Modifier.padding(top = 6.dp), height = 48.dp)
            }
        }
    }
    if (card) {
        T14Card(modifier, padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) { body() }
    } else {
        Box(modifier) { body() }
    }
}

/** Icons used only by empty states. */
object EmptyIcons {
    const val Clock = "M12 7v5l3 2 M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0"
    const val Message = "M4 5h16v11H8l-4 4z"
    const val Cart = "M3 4h2l2.4 11.2a2 2 0 0 0 2 1.6h7.7a2 2 0 0 0 2-1.5L21 8H6 M9 21h.01M18 21h.01"
    const val Cloud = "M7 18a4 4 0 0 1-.5-8 6 6 0 0 1 11.6 1.5A3.5 3.5 0 0 1 17.5 18z M12 11v3M12 16.5v.5"
}
