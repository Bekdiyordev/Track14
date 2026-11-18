package uz.beko404.track14.admin.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.beko404.track14.admin.feature.Screen
import uz.beko404.track14.domain.repository.DomainError

/** Navy header band with an ADMIN badge, as in the A-screens of the canvas. */
@Composable
fun AdminHeader(title: String, onBack: (() -> Unit)? = null, onRefresh: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().background(AdminColors.Navy).statusBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga", tint = Color.White) }
        } else {
            Box(Modifier.padding(start = 12.dp))
        }
        Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        if (onRefresh != null) {
            IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "Yangilash", tint = Color.White) }
        }
        Text(
            "ADMIN",
            color = AdminColors.Navy,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(end = 12.dp).background(Color(0xFFFDE68A), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Loading bar, error and success lines shown under the header of every screen. */
@Composable
fun <T> StatusLine(state: Screen<T>) {
    if (state.loading || state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    state.error?.let { Text(errorText(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp, 8.dp)) }
    state.done?.let { Text(it, color = AdminColors.Success, modifier = Modifier.padding(16.dp, 8.dp)) }
}

fun errorText(e: DomainError): String = when (e) {
    DomainError.PermissionDenied -> "Ruxsat yo'q: rolingiz bu amal uchun yetmaydi"
    DomainError.Network -> "Internet yoki server bilan aloqa yo'q"
    DomainError.NotFound -> "Topilmadi"
    is DomainError.Rejected -> when (e.reason) {
        "invalid_input" -> "Qiymatlar noto'g'ri, tekshirib qayta kiriting"
        "cannot_block_self" -> "O'zingizni bloklay olmaysiz"
        "cannot_demote_self" -> "O'z superadmin rolingizni olib tashlay olmaysiz"
        "not_scheduled" -> "Bu push allaqachon yuborilgan yoki bekor qilingan"
        "invalid_send_at" -> "Vaqt hozirdan 30 kungacha bo'lishi kerak"
        else -> "Server rad etdi: ${e.reason}"
    }
    else -> "Xatolik yuz berdi"
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
fun KeyValue(key: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(key, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accent: Color = AdminColors.Blue) {
    Column(modifier = modifier.background(Color.White, RoundedCornerShape(18.dp)).padding(16.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = accent)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Loading() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Confirmation with an optional reason field; [requireReason] keeps the button disabled until 3+ chars. */
@Composable
fun ReasonDialog(
    title: String,
    confirm: String,
    requireReason: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    extra: @Composable () -> Unit = {},
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = reason, onValueChange = { reason = it.take(300) }, label = { Text("Sabab") }, modifier = Modifier.fillMaxWidth())
                extra()
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason) }, enabled = !requireReason || reason.trim().length >= 3) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor qilish") } },
    )
}

fun ago(millis: Long): String = if (millis <= 0) "—" else DateUtils.getRelativeTimeSpanString(millis).toString()

fun dateTime(millis: Long): String =
    if (millis <= 0) "—" else java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(millis))
