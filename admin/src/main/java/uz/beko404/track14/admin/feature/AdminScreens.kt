package uz.beko404.track14.admin.feature

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.admin.logic.AdminRules
import uz.beko404.track14.admin.logic.DashboardMath
import uz.beko404.track14.admin.logic.Role
import uz.beko404.track14.admin.ui.AdminColors
import uz.beko404.track14.admin.ui.AdminHeader
import uz.beko404.track14.admin.ui.Card
import uz.beko404.track14.admin.ui.KeyValue
import uz.beko404.track14.admin.ui.Loading
import uz.beko404.track14.admin.ui.ReasonDialog
import uz.beko404.track14.admin.ui.StatTile
import uz.beko404.track14.admin.ui.StatusLine
import uz.beko404.track14.admin.ui.ago
import uz.beko404.track14.admin.ui.dateTime
import uz.beko404.track14.domain.model.AppStatus

private val ListPadding = PaddingValues(16.dp)

// ---------------------------------------------------------------- A2 dashboard

@Composable
fun DashboardScreen(role: Role, onOpen: (String) -> Unit, vm: DashboardViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var confirmMaintenance by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) { vm.load() }
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Boshqaruv", onRefresh = vm::load)
        StatusLine(s)
        val c = s.data
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Rolingiz: ${role.claim}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (c == null && s.loading) Loading()
            if (c != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(c.users.toString(), "foydalanuvchi · +${c.usersToday} bugun", Modifier.weight(1f).clickable { onOpen("users") })
                    StatTile(c.activeCampaigns.toString(), "aktiv ilova · ${c.newCampaignsWeek} ta 7 kunda", Modifier.weight(1f).clickable { onOpen("apps") }, AdminColors.Success)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(c.activeTests.toString(), "aktiv test", Modifier.weight(1f))
                    StatTile(c.openReports.toString(), "yangi shikoyat", Modifier.weight(1f).clickable { onOpen("reports") }, AdminColors.Danger)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(c.blockedUsers.toString(), "bloklangan", Modifier.weight(1f))
                    StatTile(
                        "$" + String.format(java.util.Locale.US, "%.2f", c.revenueTodayUsd),
                        "bugungi tushum · ${c.purchasesToday} ta xarid",
                        Modifier.weight(1f),
                        AdminColors.Amber,
                    )
                }
                Text("Tushum Play narxlari bo'yicha, Google komissiyasisiz hisoblangan. Push navbatda: ${c.scheduledPushes}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                SectionTitle("TEZKOR AMALLAR")
                if (role.canSendPush) MenuRow("Push yuborish", "Hammaga, tanlanganlarga yoki rejalashtirish") { onOpen("push") }
                if (role.canEditConfig) {
                    MenuRow("Ilova versiyasi", "Majburiy va ixtiyoriy yangilanish") { onOpen("config/app_version") }
                    Card {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Texnik ishlar rejimi", fontWeight = FontWeight.SemiBold)
                                Text("Yoqilsa, hamma foydalanuvchiga to'xtash oynasi chiqadi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = c.maintenance, onCheckedChange = { confirmMaintenance = it }, enabled = !s.busy)
                        }
                    }
                }

                SectionTitle("SO'NGGI FAOLIYAT (AUDIT)")
                Card(Modifier.clickable { onOpen("audit") }) {
                    if (c.recentAudit.isEmpty()) Text("Hali yozuv yo'q", style = MaterialTheme.typography.bodySmall)
                    c.recentAudit.forEach { e ->
                        Column {
                            Text("${auditLabel(e.action)} · ${e.target}", fontWeight = FontWeight.SemiBold)
                            Text("${e.actor} · ${ago(e.createdAtMillis)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    confirmMaintenance?.let { on ->
        AlertDialog(
            onDismissRequest = { confirmMaintenance = null },
            title = { Text(if (on) "Texnik ishlarni yoqamizmi?" else "Texnik ishlarni o'chiramizmi?") },
            text = {
                Text(
                    if (on) "Hamma foydalanuvchida ilova to'xtaydi va xabar chiqadi. Xodimlar ishlashda davom etadi." else "Foydalanuvchilar yana ilovadan foydalana oladi.",
                )
            },
            confirmButton = { TextButton(onClick = { vm.setMaintenance(on); confirmMaintenance = null }) { Text(if (on) "Yoqish" else "O'chirish") } },
            dismissButton = { TextButton(onClick = { confirmMaintenance = null }) { Text("Bekor qilish") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
}

/** Audit action ids in plain words. */
fun auditLabel(action: String): String = when (action) {
    "block_user" -> "Bloklandi"
    "unblock_user" -> "Blokdan chiqarildi"
    "adjust_coins" -> "Coin14 tuzatildi"
    "set_role" -> "Rol o'zgardi"
    "suspend_app" -> "Ilova to'xtatildi"
    "unsuspend_app" -> "Ilova tiklandi"
    "resolve_report" -> "Shikoyat ko'rildi"
    "set_config" -> "Sozlama saqlandi"
    "send_push", "schedule_push" -> "Push"
    "cancel_push" -> "Push bekor qilindi"
    "delete_account" -> "Akkaunt o'chirildi (o'zi)"
    "admin_delete_account" -> "Akkaunt o'chirildi (admin)"
    else -> action
}

// ---------------------------------------------------------------- A3 users, A4 user

@Composable
fun UsersScreen(onOpen: (String) -> Unit, vm: UsersViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (s.data == null) vm.load() }
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Foydalanuvchilar", onRefresh = vm::load)
        StatusLine(s)
        Row(Modifier.padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it.take(128) },
                label = { Text("E-mail yoki UID") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = vm::load) { Text("Qidirish") }
        }
        LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.data.orEmpty(), key = { it.uid }) { u ->
                Card(Modifier.clickable { onOpen(u.uid) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(u.name.ifBlank { "—" }, fontWeight = FontWeight.Bold)
                            Text(u.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (u.blocked) Text("BLOK", color = AdminColors.Danger, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(
                        "Testlar: ${u.stats.testsCompleted} yakunladi · ${u.stats.testsFailed + u.stats.testsLeft} uzildi · ishonch ${u.stats.reliability?.let { "$it%" } ?: "—"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (s.data?.isEmpty() == true) item { Text("Hech kim topilmadi") }
        }
    }
}

@Composable
fun UserDetailScreen(role: Role, onBack: () -> Unit, onPush: (String) -> Unit, vm: UserViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    var blockSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var adjustReason by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(deleted) { if (deleted) onBack() }

    Column(Modifier.fillMaxSize()) {
        AdminHeader("Foydalanuvchi", onBack = onBack, onRefresh = vm::load)
        StatusLine(s)
        val d = s.data
        if (d == null) {
            if (s.loading) Loading()
            return@Column
        }
        val u = d.user
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(u.name.ifBlank { "—" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(u.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        if (u.blocked) "Bloklangan" else "Faol",
                        color = if (u.blocked) AdminColors.Danger else AdminColors.Success,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (u.blocked) {
                Card {
                    Text(
                        if (u.blockedUntilMillis > 0) "Bloklangan: ${dateTime(u.blockedUntilMillis)} gacha" else "Doimiy bloklangan",
                        color = AdminColors.Danger,
                        fontWeight = FontWeight.Bold,
                    )
                    if (u.blockedReason.isNotBlank()) Text("Foydalanuvchiga ko'rinadi: ${u.blockedReason}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(d.balance.toString(), "Coin14", Modifier.weight(1f), AdminColors.Amber)
                StatTile(u.stats.testsStarted.toString(), "test", Modifier.weight(1f))
                StatTile(d.apps.toString(), "ilova", Modifier.weight(1f))
                StatTile(u.stats.reliability?.let { "$it%" } ?: "—", "ishonch", Modifier.weight(1f), AdminColors.Success)
            }
            Card {
                Text("Ma'lumot", fontWeight = FontWeight.Bold)
                KeyValue("UID", u.uid)
                KeyValue("Ro'yxatdan o'tgan", dateTime(u.createdAtMillis))
                KeyValue("Til", u.lang.ifBlank { "—" })
                KeyValue("Qurilma", u.deviceModel.ifBlank { "—" })
                KeyValue("Yakunlagan / uzilgan / chiqqan", "${u.stats.testsCompleted} / ${u.stats.testsFailed} / ${u.stats.testsLeft}")
                KeyValue("Tasdiqlangan kun", u.stats.daysPassed.toString())
            }
            if (role.canAdjustCoins) {
                Card {
                    Text("Coin14 tuzatish", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.take(8) },
                        label = { Text("Miqdor (500 yoki -200)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(value = adjustReason, onValueChange = { adjustReason = it.take(300) }, label = { Text("Sabab (majburiy)") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { vm.adjust(amount, adjustReason); amount = ""; adjustReason = "" },
                        enabled = !s.busy && AdminRules.adjustAmount(amount) != null && AdminRules.isReason(adjustReason),
                    ) { Text("Saqlash") }
                }
            }
            if (role.canSendPush) {
                OutlinedButton(onClick = { onPush(u.uid) }, modifier = Modifier.fillMaxWidth()) { Text("Push yuborish") }
            }
            if (role.canBlockUsers) {
                if (u.blocked) {
                    OutlinedButton(onClick = vm::unblock, enabled = !s.busy, modifier = Modifier.fillMaxWidth()) { Text("Blokdan chiqarish") }
                } else {
                    Button(onClick = { blockSheet = true }, enabled = !s.busy, modifier = Modifier.fillMaxWidth()) { Text("Bloklash") }
                }
            }
            if (role.canSetRoles) {
                Card {
                    Text("Rol", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Role.entries.forEach { r -> OutlinedButton(onClick = { vm.setRole(r) }, enabled = !s.busy) { Text(r.claim) } }
                    }
                    TextButton(onClick = { vm.setRole(null) }, enabled = !s.busy) { Text("Rolni olib tashlash") }
                    Text("Yangi rol foydalanuvchi qayta kirgandan keyin ishlaydi.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Card {
                Text("Oxirgi harakatlar", fontWeight = FontWeight.Bold)
                d.ledger.forEach { KeyValue("${it.type} · ${ago(it.createdAtMillis)}", (if (it.amount > 0) "+" else "") + it.amount) }
                if (d.ledger.isEmpty()) Text("Yo'q", style = MaterialTheme.typography.bodySmall)
            }
            if (role == Role.SUPERADMIN) {
                TextButton(onClick = { confirmDelete = true }, enabled = !s.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Akkauntni o'chirish", color = AdminColors.Danger)
                }
            }
        }
    }
    if (blockSheet) {
        BlockDialog(name = s.data?.user?.name.orEmpty(), onConfirm = { reason, days -> vm.block(reason, days); blockSheet = false }, onDismiss = { blockSheet = false })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Akkaunt o'chirilsinmi?") },
            text = {
                Text("Foydalanuvchi so'rovi bo'yicha: profil, balans, bildirishnomalar va kirish akkaunti o'chiriladi, faol testlar va kampaniyalar yopiladi. Buni qaytarib bo'lmaydi.")
            },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete() }) { Text("O'chirish", color = AdminColors.Danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Bekor qilish") } },
        )
    }
}

/** Reason chips fill the user-visible note; length 7 / 30 days or permanent. */
@Composable
private fun BlockDialog(name: String, onConfirm: (String, Int?) -> Unit, onDismiss: () -> Unit) {
    var chip by remember { mutableStateOf(0) }
    var days by remember { mutableStateOf<Int?>(30) }
    var note by remember { mutableStateOf(DashboardMath.BLOCK_REASONS[0].second) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${name.ifBlank { "Foydalanuvchi" }} ni bloklaymizmi?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SABAB", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DashboardMath.BLOCK_REASONS.size) { i ->
                        val (label, text) = DashboardMath.BLOCK_REASONS[i]
                        FilterChip(selected = chip == i, onClick = { chip = i; if (text.isNotEmpty()) note = text }, label = { Text(label) })
                    }
                }
                Text("MUDDAT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DashboardMath.BLOCK_DAYS.forEach { d ->
                        FilterChip(selected = days == d, onClick = { days = d }, label = { Text(d?.let { "$it kun" } ?: "Doimiy") })
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(300) },
                    label = { Text("Foydalanuvchiga ko'rinadigan izoh") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note, days) }, enabled = note.trim().length >= 3) { Text("Bloklash", color = AdminColors.Danger) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor qilish") } },
    )
}

// ---------------------------------------------------------------- A5 apps, A6 app

private fun statusLabel(s: AppStatus?): String = when (s) {
    AppStatus.DRAFT -> "Qoralama"
    AppStatus.ACTIVE -> "Aktiv"
    AppStatus.PAUSED -> "To'xtatilgan"
    AppStatus.PUBLISHED -> "Chiqqan"
    AppStatus.SUSPENDED -> "Bloklangan"
    AppStatus.REMOVED -> "O'chirilgan"
    null -> "—"
}

@Composable
fun AppsScreen(onOpen: (String) -> Unit, vm: AppsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (s.data == null) vm.load() }
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Ilovalar", onRefresh = vm::load)
        StatusLine(s)
        LazyRow(contentPadding = PaddingValues(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = filter == null, onClick = { vm.setFilter(null) }, label = { Text("Hammasi") }) }
            items(AppStatus.entries.filter { it != AppStatus.REMOVED }) { st ->
                FilterChip(selected = filter == st, onClick = { vm.setFilter(st) }, label = { Text(statusLabel(st)) })
            }
        }
        LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.data.orEmpty(), key = { it.id }) { a ->
                Card(Modifier.clickable { onOpen(a.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(a.name, fontWeight = FontWeight.Bold)
                            Text(a.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(statusLabel(a.status), color = if (a.status == AppStatus.SUSPENDED) AdminColors.Danger else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            if (s.data?.isEmpty() == true) item { Text("Ilova yo'q") }
        }
    }
}

@Composable
fun AppDetailScreen(role: Role, onBack: () -> Unit, onOpenOwner: (String) -> Unit, vm: AppViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var suspendDialog by remember { mutableStateOf(false) }
    var refund by remember { mutableStateOf(false) }
    var dismissDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.load() }
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Ilova", onBack = onBack, onRefresh = vm::load)
        StatusLine(s)
        val d = s.data
        if (d == null) {
            if (s.loading) Loading()
            return@Column
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card {
                Text(d.app.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                KeyValue("Paket", d.app.packageName)
                KeyValue("Kategoriya", d.app.category)
                KeyValue("Holat", statusLabel(d.app.status))
                KeyValue("Kampaniya", d.campaignStatus ?: "—")
                KeyValue("Tarif", d.plan ?: "—")
                KeyValue("To'langan", "${d.feePaid} Coin14")
                KeyValue("Testerlar", "${d.joined} · yakunladi ${d.completed} · uzildi ${d.failed}")
                if (d.suspendedReason.isNotBlank()) KeyValue("Blok sababi", d.suspendedReason)
                TextButton(onClick = { onOpenOwner(d.app.ownerUid) }) { Text("Egasi: ${d.ownerName.ifBlank { d.app.ownerUid }}") }
            }
            val open = d.reports.filter { it.status == "open" }
            Card {
                Text("Sabablar (${d.reports.size} ta shikoyat, ${open.size} ochiq)", fontWeight = FontWeight.Bold)
                val counts = DashboardMath.reasonCounts(d.reports.map { it.reason })
                val max = counts.maxOfOrNull { it.second } ?: 1
                if (counts.isEmpty()) Text("Shikoyat yo'q", style = MaterialTheme.typography.bodySmall)
                counts.forEach { (reason, n) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(reasonLabel(reason), modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = { n / max.toFloat() }, modifier = Modifier.weight(0.5f), color = AdminColors.Danger)
                        Text("$n", fontWeight = FontWeight.Bold)
                    }
                }
            }
            d.reports.filter { it.comment.isNotBlank() }.take(20).forEach { r ->
                Card {
                    Row {
                        Text(reasonLabel(r.reason), color = AdminColors.Danger, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        Text(ago(r.createdAtMillis), style = MaterialTheme.typography.labelSmall)
                    }
                    Text(r.comment)
                }
            }
            if (open.isNotEmpty() && role.canResolveReports) {
                OutlinedButton(onClick = { dismissDialog = true }, enabled = !s.busy, modifier = Modifier.fillMaxWidth()) { Text("Shikoyatlarni rad etish (${open.size})") }
            }
            if (role.canSuspendApps) {
                if (d.app.status == AppStatus.SUSPENDED) {
                    OutlinedButton(onClick = vm::restore, enabled = !s.busy, modifier = Modifier.fillMaxWidth()) { Text("Blokdan chiqarish") }
                } else {
                    Button(onClick = { suspendDialog = true }, enabled = !s.busy, modifier = Modifier.fillMaxWidth()) { Text("Ilovani bloklash (suspend)") }
                }
            }
        }
    }
    if (dismissDialog) {
        ReasonDialog(
            title = "Ochiq shikoyatlarni rad etamizmi?",
            confirm = "Rad etish",
            requireReason = false,
            onConfirm = { vm.dismissReports(it); dismissDialog = false },
            onDismiss = { dismissDialog = false },
        )
    }
    if (suspendDialog) {
        ReasonDialog(
            title = "Ilovani bloklash",
            confirm = "Bloklash",
            requireReason = true,
            onConfirm = { vm.suspend(it, refund); suspendDialog = false },
            onDismiss = { suspendDialog = false },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = refund, onCheckedChange = { refund = it })
                Text("Joylash to'lovini egasiga qaytarish")
            }
        }
    }
}

// ---------------------------------------------------------------- A7 reports

/** Report reason ids (firestore.rules) in plain words. */
fun reasonLabel(reason: String): String = when (reason) {
    "spam" -> "Aldamchi / spam"
    "fake" -> "Soxta"
    "inappropriate" -> "Nomaqbul"
    "not_working" -> "Ishlamaydi"
    "fraud" -> "Firibgarlik"
    "fake_activity" -> "Soxta faollik"
    else -> "Boshqa"
}

@Composable
fun ReportsScreen(role: Role, onBack: () -> Unit, onOpenTarget: (String, String) -> Unit, vm: ReportsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    var resolving by remember { mutableStateOf<Pair<String, String>?>(null) }
    val openUser by vm.openUser.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(openUser) { openUser?.let { vm.openUser.value = null; onOpenTarget("user", it) } }
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Shikoyatlar", onBack = onBack, onRefresh = vm::load)
        StatusLine(s)
        Row(Modifier.padding(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("open" to "Ochiq", "resolved" to "Yopilgan", "dismissed" to "Rad etilgan").forEach { (key, label) ->
                FilterChip(selected = tab == key, onClick = { vm.setTab(key) }, label = { Text(label) })
            }
        }
        LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.data.orEmpty(), key = { it.id }) { r ->
                Card {
                    Text("${if (r.targetType == "app") "Ilova" else "Tester"} · ${reasonLabel(r.reason)}", fontWeight = FontWeight.Bold)
                    if (r.comment.isNotBlank()) Text(r.comment)
                    Text("${ago(r.createdAtMillis)} · ${r.targetId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (r.targetType == "app") TextButton(onClick = { onOpenTarget("app", r.targetId) }) { Text("Ilovani ochish") }
                        if (r.targetType == "tester") TextButton(onClick = { vm.openTester(r.targetId) }) { Text("Testerni ochish") }
                        if (r.status == "open" && role.canResolveReports) {
                            TextButton(onClick = { resolving = r.id to "resolved" }) { Text("Yopish") }
                            TextButton(onClick = { resolving = r.id to "dismissed" }) { Text("Rad etish") }
                        }
                    }
                }
            }
            if (s.data?.isEmpty() == true) item { Text("Shikoyat yo'q") }
        }
    }
    resolving?.let { (id, status) ->
        ReasonDialog(
            title = if (status == "resolved") "Shikoyatni yopish" else "Shikoyatni rad etish",
            confirm = "Saqlash",
            requireReason = false,
            onConfirm = { vm.resolve(id, status, it); resolving = null },
            onDismiss = { resolving = null },
        )
    }
}

// ---------------------------------------------------------------- A8 push, A9 history

@Composable
fun PushScreen(role: Role, onBack: (() -> Unit)? = null, vm: PushViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val f by vm.form.collectAsStateWithLifecycle()
    var delayHours by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.load() }
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Push", onBack = onBack, onRefresh = vm::load)
        StatusLine(s)
        LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (role.canSendPush) item {
                Card {
                    Text("Yangi xabar", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = f.toAll, onClick = { vm.update { it.copy(toAll = true) } }, label = { Text("Hammaga") })
                        FilterChip(selected = !f.toAll, onClick = { vm.update { it.copy(toAll = false) } }, label = { Text("Tanlanganlarga") })
                    }
                    if (!f.toAll) {
                        OutlinedTextField(
                            value = f.uids,
                            onValueChange = { v -> vm.update { it.copy(uids = v.take(70_000)) } },
                            label = { Text("UID'lar (vergul yoki qator bilan, 500 tagacha)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    OutlinedTextField(value = f.title, onValueChange = { v -> vm.update { it.copy(title = v.take(65)) } }, label = { Text("Sarlavha (65)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = f.body, onValueChange = { v -> vm.update { it.copy(body = v.take(240)) } }, label = { Text("Matn (240)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = delayHours,
                        onValueChange = { v ->
                            delayHours = v.filter(Char::isDigit).take(3)
                            val h = delayHours.toLongOrNull()
                            vm.update { it.copy(sendAt = h?.takeIf { x -> x > 0 }?.let { x -> System.currentTimeMillis() + x * 3_600_000 }) }
                        },
                        label = { Text("Necha soatdan keyin (bo'sh = darhol, 720 gacha)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    f.sendAt?.let { Text("Yuboriladi: ${dateTime(it)}", style = MaterialTheme.typography.bodySmall) }
                    Button(onClick = { vm.send(System.currentTimeMillis()); delayHours = "" }, enabled = !s.busy) {
                        Text(if (f.sendAt == null) "Yuborish" else "Rejalashtirish")
                    }
                }
            }
            item { Text("Tarix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(s.data.orEmpty(), key = { it.id }) { j ->
                Card {
                    Text(j.title, fontWeight = FontWeight.Bold)
                    Text(j.body, style = MaterialTheme.typography.bodySmall)
                    KeyValue("Kimga", j.target)
                    KeyValue("Vaqt", dateTime(j.sendAtMillis))
                    KeyValue("Holat", j.status)
                    if (j.status == "scheduled" && role.canSendPush) TextButton(onClick = { vm.cancel(j.id) }, enabled = !s.busy) { Text("Bekor qilish") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- A13 more

@Composable
fun MoreScreen(role: Role, onOpen: (String) -> Unit, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Ko'proq")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuRow("Shikoyatlar") { onOpen("reports") }
            if (role.canEditConfig) {
                MenuRow("Ilova versiyasi (majburiy / ixtiyoriy)") { onOpen("config/app_version") }
                MenuRow("Flaglar: texnik ishlar, yangi ilova, xaridlar") { onOpen("config/flags") }
                MenuRow("Iqtisod: narxlar, mukofotlar, jarimalar") { onOpen("config/economy") }
                MenuRow("Yordam va aloqa havolalari") { onOpen("config/support") }
            }
            MenuRow("Audit log") { onOpen("audit") }
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Chiqish") }
        }
    }
}

@Composable
private fun MenuRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Card(Modifier.clickable(onClick = onClick)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

// ---------------------------------------------------------------- A10-A12 config editor

private val CONFIG_FIELDS = mapOf(
    "app_version" to listOf("minBuild", "latestBuild", "message"),
    "support" to listOf("telegram", "email", "groupUrl", "faqUrl"),
    "flags" to listOf("maintenance", "maintenanceMessage", "newApps", "purchases"),
    "economy" to AdminRules.ECONOMY_KEYS,
)
private val BOOLEAN_FIELDS = setOf("maintenance", "newApps", "purchases")
private val BOOLEAN_DEFAULT_TRUE = setOf("newApps", "purchases")
private val NUMBER_FIELDS = setOf("minBuild", "latestBuild") + AdminRules.ECONOMY_KEYS

@Composable
fun ConfigScreen(role: Role, onBack: () -> Unit, vm: ConfigViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val fields by vm.fields.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    val title = when (vm.doc) {
        "app_version" -> "Ilova versiyasi"
        "support" -> "Yordam va aloqa"
        "flags" -> "Flaglar"
        "economy" -> "Iqtisod"
        else -> "Sozlama"
    }
    Column(Modifier.fillMaxSize()) {
        AdminHeader(title, onBack = onBack, onRefresh = vm::load)
        StatusLine(s)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (vm.doc == "app_version") {
                Text("minBuild dan past versiyalar majburiy yangilanadi, latestBuild dan pastlariga taklif chiqadi.", style = MaterialTheme.typography.bodySmall)
            }
            if (vm.doc == "economy") {
                Text("Bo'sh qoldirilgan maydon o'zgarmaydi (standart qiymat ishlaydi).", style = MaterialTheme.typography.bodySmall)
            }
            CONFIG_FIELDS[vm.doc].orEmpty().forEach { key ->
                if (key in BOOLEAN_FIELDS) {
                    val on = fields[key]?.let { it == "true" } ?: (key in BOOLEAN_DEFAULT_TRUE)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(key, modifier = Modifier.weight(1f))
                        Switch(checked = on, onCheckedChange = { vm.set(key, it.toString()) }, enabled = role.canEditConfig)
                    }
                } else {
                    OutlinedTextField(
                        value = fields[key].orEmpty(),
                        onValueChange = { vm.set(key, it.take(300)) },
                        label = { Text(key) },
                        singleLine = key != "message" && key != "maintenanceMessage",
                        keyboardOptions = KeyboardOptions(keyboardType = if (key in NUMBER_FIELDS) KeyboardType.Number else KeyboardType.Text),
                        enabled = role.canEditConfig,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (role.canEditConfig) Button(onClick = vm::save, enabled = !s.busy, modifier = Modifier.fillMaxWidth()) { Text("Saqlash") }
        }
    }
}

// ---------------------------------------------------------------- audit

@Composable
fun AuditScreen(onBack: () -> Unit, vm: AuditViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Audit log", onBack = onBack, onRefresh = vm::load)
        StatusLine(s)
        LazyColumn(contentPadding = ListPadding, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(s.data.orEmpty(), key = { it.id }) { e ->
                Card {
                    Text(auditLabel(e.action), fontWeight = FontWeight.Bold)
                    Text("${e.target} · ${ago(e.createdAtMillis)}", style = MaterialTheme.typography.bodySmall)
                    Text("actor: ${e.actor}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
