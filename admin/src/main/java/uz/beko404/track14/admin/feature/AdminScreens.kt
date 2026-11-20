@file:OptIn(ExperimentalLayoutApi::class)

package uz.beko404.track14.admin.feature

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.beko404.track14.admin.data.AdminApp
import uz.beko404.track14.admin.data.Report
import uz.beko404.track14.admin.logic.AdminRules
import uz.beko404.track14.admin.logic.DashboardMath
import uz.beko404.track14.admin.logic.Role
import uz.beko404.track14.admin.ui.AdminButton
import uz.beko404.track14.admin.ui.AdminColors
import uz.beko404.track14.admin.ui.AdminField
import uz.beko404.track14.admin.ui.AdminHeader
import uz.beko404.track14.admin.ui.AdminLogo
import uz.beko404.track14.admin.ui.AdminPaths
import uz.beko404.track14.admin.ui.AdminSheet
import uz.beko404.track14.admin.ui.AdminSwitch
import uz.beko404.track14.admin.ui.AppSquare
import uz.beko404.track14.admin.ui.Avatar
import uz.beko404.track14.admin.ui.ButtonStyle
import uz.beko404.track14.admin.ui.Caps
import uz.beko404.track14.admin.ui.Card
import uz.beko404.track14.admin.ui.Empty
import uz.beko404.track14.admin.ui.FilterPill
import uz.beko404.track14.admin.ui.KeyValue
import uz.beko404.track14.admin.ui.Loading
import uz.beko404.track14.admin.ui.Manrope
import uz.beko404.track14.admin.ui.MenuRow
import uz.beko404.track14.admin.ui.MiniStat
import uz.beko404.track14.admin.ui.Notice
import uz.beko404.track14.admin.ui.OptionChip
import uz.beko404.track14.admin.ui.Pill
import uz.beko404.track14.admin.ui.ReasonDialog
import uz.beko404.track14.admin.ui.RowsCard
import uz.beko404.track14.admin.ui.SearchBox
import uz.beko404.track14.admin.ui.SheetActions
import uz.beko404.track14.admin.ui.StatTile
import uz.beko404.track14.admin.ui.StatusLine
import uz.beko404.track14.admin.ui.SwitchRow
import uz.beko404.track14.admin.ui.Tone
import uz.beko404.track14.admin.ui.ago
import uz.beko404.track14.admin.ui.dateTime
import uz.beko404.track14.admin.ui.formatNumber
import uz.beko404.track14.admin.ui.hairline
import uz.beko404.track14.admin.ui.shortDate
import uz.beko404.track14.domain.model.AppStatus

/** Space under the last block of a scrolling screen. */
private val Bottom = 20.dp

@Composable
private fun ScrollBody(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = Bottom), content = content)
}

@Composable
private fun Chips(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

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
        ScrollBody {
            if (c == null && s.loading) Loading()
            if (c != null) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(formatNumber(c.users), "Foydalanuvchilar", Modifier.weight(1f), sub = "+${c.usersToday} bugun") { onOpen("users") }
                        StatTile(formatNumber(c.activeCampaigns), "Aktiv ilovalar", Modifier.weight(1f), sub = "${c.newCampaignsWeek} ta yangi (7 kun)") { onOpen("apps") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(formatNumber(c.activeTests), "Aktiv testlar", Modifier.weight(1f), sub = "hozir davom etmoqda")
                        StatTile(
                            formatNumber(c.openReports), "Yangi shikoyat", Modifier.weight(1f),
                            accent = if (c.openReports > 0) AdminColors.DangerText else AdminColors.Text, sub = "kutilmoqda",
                        ) { onOpen("reports") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(formatNumber(c.blockedUsers), "Bloklangan", Modifier.weight(1f), sub = "foydalanuvchi")
                        StatTile(
                            "$ " + String.format(java.util.Locale.US, "%.2f", c.revenueTodayUsd), "Bugungi tushum", Modifier.weight(1f),
                            sub = "${c.purchasesToday} ta xarid",
                        )
                    }
                    Text(
                        "Tushum Play narxlari bo'yicha, Google komissiyasisiz. Navbatdagi push: ${c.scheduledPushes}. Rolingiz: ${role.claim}.",
                        fontSize = 12.sp, lineHeight = 17.sp, color = AdminColors.Muted,
                    )
                }

                if (role.canSendPush || role.canEditConfig) {
                    Caps("Tezkor amallar")
                    RowsCard {
                        if (role.canSendPush) {
                            MenuRow("Push yuborish", "Hammaga, tanlanganlarga yoki rejalashtirish", AdminPaths.Bell, divider = role.canEditConfig) { onOpen("push") }
                        }
                        if (role.canEditConfig) {
                            MenuRow("Ilova versiyasi", "Majburiy va ixtiyoriy yangilanish", AdminPaths.Upload, AdminColors.SuccessSoft, AdminColors.Success) { onOpen("config/app_version") }
                            Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                SwitchRow(
                                    "Texnik ishlar rejimi", "Yoqilsa, hamma foydalanuvchiga to'xtash oynasi chiqadi",
                                    c.maintenance, { confirmMaintenance = it }, enabled = !s.busy,
                                )
                            }
                        }
                    }
                }

                Caps("So'nggi faoliyat (audit)")
                RowsCard(Modifier.clickable { onOpen("audit") }) {
                    if (c.recentAudit.isEmpty()) Empty("Hali yozuv yo'q")
                    c.recentAudit.forEachIndexed { i, e ->
                        Column(Modifier.fillMaxWidth().hairline(i < c.recentAudit.lastIndex).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${auditLabel(e.action)} · ${e.target}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("${e.actor} · ${ago(e.createdAtMillis)}", fontSize = 12.sp, color = AdminColors.Faint)
                        }
                    }
                }
            }
        }
    }
    confirmMaintenance?.let { on ->
        AdminSheet(if (on) "Texnik ishlarni yoqamizmi?" else "Texnik ishlarni o'chiramizmi?", onDismiss = { confirmMaintenance = null }) {
            Text(
                if (on) "Hamma foydalanuvchida ilova to'xtaydi va xabar chiqadi. Xodimlar ishlashda davom etadi." else "Foydalanuvchilar yana ilovadan foydalana oladi.",
                fontSize = 14.sp, lineHeight = 21.sp, color = Color(0xFF475569),
            )
            SheetActions(if (on) "Yoqish" else "O'chirish", { vm.setMaintenance(on); confirmMaintenance = null }, { confirmMaintenance = null }, danger = on)
        }
    }
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
    val blockedOnly by vm.blockedOnly.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (s.data == null) vm.load() }
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Foydalanuvchilar", onRefresh = vm::load)
        StatusLine(s)
        SearchBox(query, { vm.query.value = it.take(128) }, "Email yoki ID bo'yicha qidirish", onSearch = vm::load)
        Chips {
            FilterPill("Hammasi", !blockedOnly) { vm.setBlockedOnly(false) }
            FilterPill("Bloklangan", blockedOnly) { vm.setBlockedOnly(true) }
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = Bottom)) {
            val list = s.data.orEmpty()
            itemsIndexedCard(list.size) { i ->
                val u = list[i]
                Row(
                    Modifier.fillMaxWidth().hairline(i < list.lastIndex).clickable { onOpen(u.uid) }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Avatar(u.name.ifBlank { u.email }, u.uid)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(u.name.ifBlank { "—" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(
                            listOf(u.email.ifBlank { u.uid }, u.stats.reliability?.let { "ishonch $it%" }).filterNotNull().joinToString(" · "),
                            fontSize = 12.sp, color = AdminColors.Muted, maxLines = 1,
                        )
                    }
                    if (u.blocked) Pill("Bloklangan", Tone.RED) else Pill("Faol", Tone.GREEN)
                }
            }
            if (s.data?.isEmpty() == true) item { Empty("Hech kim topilmadi") }
        }
    }
}

/** Rows of one white rounded block inside a LazyColumn: first/last item get the rounded corners. */
private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedCard(count: Int, row: @Composable (Int) -> Unit) {
    items(count) { i ->
        val top = if (i == 0) 18.dp else 0.dp
        val bottom = if (i == count - 1) 18.dp else 0.dp
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)).background(Color.White)) { row(i) }
    }
}

@Composable
fun UserDetailScreen(role: Role, onBack: () -> Unit, onPush: (String) -> Unit, vm: UserViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val deleted by vm.deleted.collectAsStateWithLifecycle()
    var blockSheet by remember { mutableStateOf(false) }
    var coinSheet by remember { mutableStateOf<Int?>(null) }
    var roleSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
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
        ScrollBody {
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Avatar(u.name.ifBlank { u.email }, u.uid, 56.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(u.name.ifBlank { "—" }, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(u.email, fontSize = 13.sp, color = AdminColors.Muted, maxLines = 1)
                }
                if (u.blocked) Pill("Bloklangan", Tone.RED) else Pill("Faol", Tone.GREEN)
            }
            if (u.blocked) {
                Column(
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFEF2F2)).border(1.dp, AdminColors.DangerBorder, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Foydalanuvchi bloklangan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AdminColors.DangerText)
                    Text(
                        (if (u.blockedUntilMillis > 0) "${shortDate(u.blockedUntilMillis)} gacha. " else "Doimiy. ") +
                            (if (u.blockedReason.isNotBlank()) "Izoh: ${u.blockedReason}" else "Ilovada «Akkaunt bloklangan» oynasi ko'rinadi, Coin14 saqlanadi."),
                        fontSize = 13.sp, lineHeight = 19.sp, color = AdminColors.Body,
                    )
                }
            }
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStat(formatNumber(d.balance), "Coin14", Modifier.weight(1f), AdminColors.AmberText)
                MiniStat(u.stats.testsStarted.toString(), "test", Modifier.weight(1f))
                MiniStat(d.apps.toString(), "ilova", Modifier.weight(1f))
                MiniStat(u.stats.reliability?.let { "$it%" } ?: "—", "ishonch", Modifier.weight(1f))
            }

            Caps("Ma'lumot")
            Card(Modifier.padding(horizontal = 20.dp), padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp), spacing = 0.dp) {
                KeyValue("ID", u.uid)
                KeyValue("Ro'yxatdan o'tgan", shortDate(u.createdAtMillis))
                KeyValue("Til", when (u.lang) { "uz" -> "O'zbekcha"; "ru" -> "Русский"; "en" -> "English"; else -> "—" })
                KeyValue("Qurilma", u.deviceModel.ifBlank { "—" })
                KeyValue("Yakunlagan / uzilgan / chiqqan", "${u.stats.testsCompleted} / ${u.stats.testsFailed} / ${u.stats.testsLeft}")
                KeyValue("Tasdiqlangan kun", u.stats.daysPassed.toString(), divider = false)
            }

            if (role.canAdjustCoins) {
                Caps("Tanga")
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminButton("+ Qo'shish", { coinSheet = 1 }, Modifier.weight(1f), ButtonStyle.OUTLINE, !s.busy, height = 46.dp)
                    AdminButton("− Ayirish", { coinSheet = -1 }, Modifier.weight(1f), ButtonStyle.NEUTRAL, !s.busy, height = 46.dp)
                }
            }

            Caps("Amallar")
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (role.canSendPush) AdminButton("Push yuborish", { onPush(u.uid) }, style = ButtonStyle.OUTLINE, height = 48.dp)
                if (role.canBlockUsers) {
                    if (u.blocked) {
                        AdminButton("Blokdan chiqarish", vm::unblock, style = ButtonStyle.OUTLINE, enabled = !s.busy, height = 48.dp)
                    } else {
                        AdminButton("Bloklash", { blockSheet = true }, style = ButtonStyle.DANGER, enabled = !s.busy, height = 48.dp)
                    }
                }
                if (role.canSetRoles) AdminButton("Rolni o'zgartirish", { roleSheet = true }, style = ButtonStyle.NEUTRAL, enabled = !s.busy, height = 48.dp)
                if (role == Role.SUPERADMIN) {
                    Text(
                        "Akkauntni o'chirish", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AdminColors.DangerText,
                        modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(10.dp)).clickable(enabled = !s.busy) { confirmDelete = true }.padding(10.dp),
                    )
                }
            }

            Caps("Oxirgi harakatlar")
            Card(Modifier.padding(horizontal = 20.dp), padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp), spacing = 0.dp) {
                if (d.ledger.isEmpty()) Empty("Yo'q")
                d.ledger.forEachIndexed { i, row ->
                    KeyValue("${row.type} · ${ago(row.createdAtMillis)}", (if (row.amount > 0) "+" else "") + formatNumber(row.amount), divider = i < d.ledger.lastIndex)
                }
            }
        }
    }
    if (blockSheet) {
        BlockSheet(name = s.data?.user?.name.orEmpty(), onConfirm = { reason, days -> vm.block(reason, days); blockSheet = false }, onDismiss = { blockSheet = false })
    }
    coinSheet?.let { sign -> CoinSheet(sign, onConfirm = { amount, reason -> vm.adjust(amount, reason); coinSheet = null }, onDismiss = { coinSheet = null }) }
    if (roleSheet) {
        AdminSheet("Rol", onDismiss = { roleSheet = false }) {
            Text("Yangi rol foydalanuvchi qayta kirgandan keyin ishlaydi.", fontSize = 14.sp, lineHeight = 21.sp, color = Color(0xFF475569))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Role.entries.forEach { r -> OptionChip(r.claim, false, { vm.setRole(r); roleSheet = false }) }
            }
            AdminButton("Rolni olib tashlash", { vm.setRole(null); roleSheet = false }, style = ButtonStyle.DANGER_OUTLINE, height = 48.dp)
        }
    }
    if (confirmDelete) {
        AdminSheet("Akkaunt o'chirilsinmi?", onDismiss = { confirmDelete = false }) {
            Text(
                "Foydalanuvchi so'rovi bo'yicha: profil, balans, bildirishnomalar va kirish akkaunti o'chiriladi, faol testlar va kampaniyalar yopiladi. Buni qaytarib bo'lmaydi.",
                fontSize = 14.sp, lineHeight = 21.sp, color = Color(0xFF475569),
            )
            SheetActions("Ha, o'chirish", { confirmDelete = false; vm.delete() }, { confirmDelete = false })
        }
    }
}

/** Coin correction: amount (sign from the button) and a required reason. */
@Composable
private fun CoinSheet(sign: Int, onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val signed = if (amount.isBlank()) "" else (if (sign < 0) "-" else "") + amount
    AdminSheet(if (sign > 0) "Coin14 qo'shish" else "Coin14 ayirish", onDismiss) {
        AdminField("Miqdor", amount, { v -> amount = v.filter(Char::isDigit).take(7) }, placeholder = "500", keyboard = KeyboardType.Number)
        AdminField("Sabab (majburiy)", reason, { reason = it.take(300) }, minLines = 2)
        SheetActions(
            if (sign > 0) "Qo'shish" else "Ayirish", { onConfirm(signed, reason) }, onDismiss, danger = sign < 0,
            enabled = AdminRules.adjustAmount(signed) != null && AdminRules.isReason(reason),
        )
    }
}

/** Reason chips fill the user-visible note; length 7 / 30 days or permanent. */
@Composable
private fun BlockSheet(name: String, onConfirm: (String, Int?) -> Unit, onDismiss: () -> Unit) {
    var chip by remember { mutableIntStateOf(0) }
    var days by remember { mutableStateOf<Int?>(30) }
    var note by remember { mutableStateOf(DashboardMath.BLOCK_REASONS[0].second) }
    AdminSheet("${name.ifBlank { "Foydalanuvchi" }} ni bloklaymizmi?", onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SABAB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminColors.Muted, letterSpacing = 0.4.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DashboardMath.BLOCK_REASONS.forEachIndexed { i, (label, text) ->
                    OptionChip(label, chip == i, { chip = i; if (text.isNotEmpty()) note = text }, danger = true)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MUDDAT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminColors.Muted, letterSpacing = 0.4.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardMath.BLOCK_DAYS.forEach { d ->
                    val on = days == d
                    Text(
                        d?.let { "$it kun" } ?: "Doimiy", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (on) AdminColors.BlueDark else AdminColors.Text,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) AdminColors.BlueSoft else AdminColors.Divider)
                            .then(if (on) Modifier.border(2.dp, AdminColors.Blue, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { days = d }.padding(vertical = 10.dp),
                    )
                }
            }
        }
        AdminField("Foydalanuvchiga ko'rinadigan izoh", note, { note = it.take(300) }, minLines = 3)
        SheetActions("Ha, bloklash", { onConfirm(note, days) }, onDismiss, enabled = note.trim().length >= 3)
    }
}

// ---------------------------------------------------------------- A5 apps, A6 app

private fun statusLabel(s: AppStatus?): String = when (s) {
    AppStatus.DRAFT -> "Qoralama"
    AppStatus.ACTIVE -> "Aktiv"
    AppStatus.PAUSED -> "Pauza"
    AppStatus.PUBLISHED -> "Published"
    AppStatus.SUSPENDED -> "To'xtatilgan"
    AppStatus.REMOVED -> "O'chirilgan"
    null -> "—"
}

private fun statusTone(s: AppStatus?): Tone = when (s) {
    AppStatus.ACTIVE -> Tone.GREEN
    AppStatus.PUBLISHED -> Tone.BLUE
    AppStatus.SUSPENDED, AppStatus.REMOVED -> Tone.RED
    AppStatus.PAUSED -> Tone.AMBER
    else -> Tone.GREY
}

@Composable
fun AppsScreen(onOpen: (String) -> Unit, vm: AppsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { if (s.data == null) vm.load() }
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Ilovalar", onRefresh = vm::load)
        StatusLine(s)
        SearchBox(query, { query = it.take(100) }, "Ilova nomi yoki package")
        Chips {
            FilterPill("Hammasi", filter == null) { vm.setFilter(null) }
            listOf(AppStatus.ACTIVE, AppStatus.SUSPENDED, AppStatus.PAUSED, AppStatus.PUBLISHED, AppStatus.DRAFT).forEach { st ->
                FilterPill(statusLabel(st), filter == st) { vm.setFilter(st) }
            }
        }
        val q = query.trim().lowercase()
        val list = s.data.orEmpty().filter { q.isEmpty() || it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = Bottom)) {
            itemsIndexedCard(list.size) { i -> AppRow(list[i], divider = i < list.lastIndex) { onOpen(list[i].id) } }
            if (s.data != null && list.isEmpty()) item { Empty("Ilova yo'q") }
        }
    }
}

@Composable
private fun AppRow(a: AdminApp, divider: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().hairline(divider).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppSquare(a.name, a.id)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(a.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(listOf(a.packageName, a.category).filter { it.isNotBlank() }.joinToString(" · "), fontSize = 12.sp, color = AdminColors.Muted, maxLines = 1)
        }
        Pill(statusLabel(a.status), statusTone(a.status))
    }
}

private val ReasonColors = listOf(AdminColors.Danger, AdminColors.Amber, AdminColors.Blue)

@Composable
fun AppDetailScreen(role: Role, onBack: () -> Unit, onOpenOwner: (String) -> Unit, vm: AppViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var suspendDialog by remember { mutableStateOf(false) }
    var refund by remember { mutableStateOf(false) }
    var dismissDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.load() }
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Ilova boshqaruvi", onBack = onBack, onRefresh = vm::load)
        StatusLine(s)
        val d = s.data
        if (d == null) {
            if (s.loading) Loading()
            return@Column
        }
        val open = d.reports.filter { it.status == "open" }
        Box(Modifier.weight(1f)) {
            ScrollBody {
                Row(
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppSquare(d.app.name, d.app.id, 52.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(d.app.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("${d.app.packageName} · ${d.joined} tester", fontSize = 12.sp, color = AdminColors.Muted, maxLines = 1)
                        Text(
                            "Egasi: ${d.ownerName.ifBlank { d.app.ownerUid.take(10) }}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AdminColors.Blue,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onOpenOwner(d.app.ownerUid) },
                        )
                    }
                    Pill(statusLabel(d.app.status), statusTone(d.app.status))
                }
                if (d.app.status == AppStatus.SUSPENDED) {
                    Column(
                        Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFEF2F2)).border(1.dp, AdminColors.DangerBorder, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Ilova to'xtatildi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AdminColors.DangerText)
                        Text(
                            d.suspendedReason.ifBlank { "Ro'yxatdan olindi, testerlar pauzada." }, fontSize = 13.sp, lineHeight = 19.sp, color = AdminColors.Body,
                        )
                    }
                }

                Caps("Kampaniya")
                Card(Modifier.padding(horizontal = 20.dp), padding = PaddingValues(horizontal = 16.dp, vertical = 2.dp), spacing = 0.dp) {
                    KeyValue("Kategoriya", d.app.category.ifBlank { "—" })
                    KeyValue("Kampaniya holati", d.campaignStatus ?: "—")
                    KeyValue("Tarif", d.plan ?: "—")
                    KeyValue("To'langan", "${formatNumber(d.feePaid)} Coin14")
                    KeyValue("Testerlar", "${d.joined} · yakunladi ${d.completed} · uzildi ${d.failed}", divider = false)
                }

                Caps("Sabablar (${d.reports.size} ta shikoyat)")
                Card(Modifier.padding(horizontal = 20.dp), padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), spacing = 12.dp) {
                    val counts = DashboardMath.reasonCounts(d.reports.map { it.reason })
                    val max = counts.maxOfOrNull { it.second } ?: 1
                    if (counts.isEmpty()) Text("Shikoyat yo'q", fontSize = 13.sp, color = AdminColors.Muted)
                    counts.forEachIndexed { i, (reason, n) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(reasonLabel(reason), fontSize = 13.sp, color = AdminColors.Body, modifier = Modifier.width(120.dp), maxLines = 1)
                            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(AdminColors.Divider)) {
                                Box(Modifier.fillMaxWidth(n / max.toFloat()).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(ReasonColors[minOf(i, ReasonColors.lastIndex)]))
                            }
                            Text("$n", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        }
                    }
                }

                val withComments = d.reports.filter { it.comment.isNotBlank() }.take(20)
                if (withComments.isNotEmpty()) {
                    Caps("Izohlar")
                    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        withComments.forEach { r -> ReportComment(r) }
                    }
                }
            }
        }
        val canAct = role.canSuspendApps || (open.isNotEmpty() && role.canResolveReports)
        if (canAct) {
            Row(
                Modifier.fillMaxWidth().background(Color.White).hairline(false).padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (d.app.status == AppStatus.SUSPENDED) {
                    if (role.canSuspendApps) AdminButton("Qayta faollashtirish", vm::restore, style = ButtonStyle.OUTLINE, enabled = !s.busy, height = 56.dp)
                } else {
                    if (open.isNotEmpty() && role.canResolveReports) {
                        AdminButton("Rad etish (${open.size})", { dismissDialog = true }, Modifier.weight(1f), ButtonStyle.NEUTRAL, !s.busy, height = 56.dp)
                    }
                    if (role.canSuspendApps) {
                        AdminButton("To'xtatish (Suspend)", { suspendDialog = true }, Modifier.weight(1.4f), ButtonStyle.DANGER, !s.busy, height = 56.dp)
                    }
                }
            }
        }
    }
    if (dismissDialog) {
        ReasonDialog(
            title = "Ochiq shikoyatlarni rad etamizmi?",
            confirm = "Rad etish",
            requireReason = false,
            danger = false,
            onConfirm = { vm.dismissReports(it); dismissDialog = false },
            onDismiss = { dismissDialog = false },
        )
    }
    if (suspendDialog) {
        val name = s.data?.app?.name.orEmpty()
        ReasonDialog(
            title = "Ilovani to'xtatamizmi?",
            body = "$name ro'yxatdan olinadi. Aktiv testlar pauzaga qo'yiladi, testerlarga jarima yo'q. Egasiga sabab bilan xabar boradi.",
            confirm = "Ha, to'xtatish",
            requireReason = true,
            onConfirm = { vm.suspend(it, refund); suspendDialog = false },
            onDismiss = { suspendDialog = false },
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AdminColors.Divider).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Joylash to'lovini egasiga qaytarish", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                AdminSwitch(refund, { refund = it })
            }
        }
    }
}

@Composable
private fun ReportComment(r: Report) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(reasonLabel(r.reason), reasonTone(r.reason))
            Spacer(Modifier.weight(1f))
            Text(ago(r.createdAtMillis), fontSize = 12.sp, color = AdminColors.Faint)
        }
        Text(r.comment, fontSize = 14.sp, lineHeight = 20.sp, color = AdminColors.Body)
    }
}

// ---------------------------------------------------------------- A7 reports

/** Report reason ids (firestore.rules) in plain words. */
fun reasonLabel(reason: String): String = when (reason) {
    "spam" -> "Aldamchi / spam"
    "fake" -> "Soxta / taqlid"
    "inappropriate" -> "Nomaqbul"
    "not_working" -> "Ishlamayapti"
    "fraud" -> "Firibgarlik"
    "fake_activity" -> "Soxta faollik"
    else -> "Boshqa"
}

private fun reasonTone(reason: String): Tone = when (reason) {
    "fake", "fraud", "inappropriate" -> Tone.RED
    "spam", "fake_activity" -> Tone.AMBER
    else -> Tone.BLUE
}

/** Open reports grouped by target, newest group first. */
private data class ReportGroup(val type: String, val id: String, val reports: List<Report>) {
    val latest: Report get() = reports.maxBy { it.createdAtMillis }
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
        Chips {
            listOf("open" to "Yangi", "resolved" to "Ko'rilgan", "dismissed" to "Rad etilgan").forEach { (key, label) ->
                val n = if (tab == key) s.data?.size else null
                FilterPill(if (n != null && n > 0) "$label ($n)" else label, tab == key) { vm.setTab(key) }
            }
        }
        val groups = s.data.orEmpty().groupBy { it.targetType to it.targetId }
            .map { (k, v) -> ReportGroup(k.first, k.second, v) }.sortedByDescending { it.latest.createdAtMillis }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = Bottom), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(groups, key = { it.type + it.id }) { g ->
                val many = g.reports.size >= 3
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White)
                        .border(1.dp, if (many && tab == "open") AdminColors.DangerBorder else Color(0xFFEDF2F7), RoundedCornerShape(20.dp))
                        .clickable { if (g.type == "app") onOpenTarget("app", g.id) else vm.openTester(g.id) }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (g.type == "app") AppSquare(g.id, g.id) else Avatar("T", g.id, 46.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(if (g.type == "app") "Ilova" else "Tester", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(g.id, fontSize = 12.sp, color = AdminColors.Muted, maxLines = 1)
                        }
                        Pill("${g.reports.size} ta shikoyat", if (many) Tone.RED else if (g.reports.size == 2) Tone.AMBER else Tone.BLUE)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DashboardMath.reasonCounts(g.reports.map { it.reason }).forEach { (reason, n) -> Pill("${reasonLabel(reason)} · $n", reasonTone(reason)) }
                    }
                    g.reports.firstOrNull { it.comment.isNotBlank() }?.let {
                        Text("“${it.comment}”", fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFF475569), maxLines = 3)
                    }
                    Text("Oxirgisi: ${ago(g.latest.createdAtMillis)}", fontSize = 12.sp, color = AdminColors.Faint)
                    if (tab == "open" && role.canResolveReports && g.type == "tester") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminButton("Yopish", { resolving = g.latest.id to "resolved" }, Modifier.weight(1f), ButtonStyle.OUTLINE, height = 40.dp)
                            AdminButton("Rad etish", { resolving = g.latest.id to "dismissed" }, Modifier.weight(1f), ButtonStyle.NEUTRAL, height = 40.dp)
                        }
                    }
                }
            }
            if (s.data?.isEmpty() == true) item { Empty("Shikoyat yo'q") }
        }
    }
    resolving?.let { (id, status) ->
        ReasonDialog(
            title = if (status == "resolved") "Shikoyatni yopish" else "Shikoyatni rad etish",
            confirm = "Saqlash",
            requireReason = false,
            danger = false,
            onConfirm = { vm.resolve(id, status, it); resolving = null },
            onDismiss = { resolving = null },
        )
    }
}

// ---------------------------------------------------------------- A8 push, A9 history

@Composable
fun PushScreen(role: Role, onBack: (() -> Unit)? = null, onHistory: () -> Unit = {}, vm: PushViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val f by vm.form.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var later by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.load() }
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Push yuborish", onBack = onBack)
        StatusLine(s)
        ScrollBody {
            if (!role.canSendPush) {
                Notice("Rolingiz push yuborishga ruxsat bermaydi.", AdminColors.AmberSoft, AdminColors.AmberText, Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp))
            }
            Caps("Xabar", top = 16.dp)
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminField("Sarlavha", f.title, { v -> vm.update { it.copy(title = v.take(65)) } }, placeholder = "Masalan: Yangi versiya chiqdi", hint = "${f.title.length} / 65")
                AdminField("Matn", f.body, { v -> vm.update { it.copy(body = v.take(240)) } }, minLines = 3, hint = "${f.body.length} / 240")
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).border(1.dp, AdminColors.Border, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AdminLogo(36.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Track14 · hozir", fontSize = 12.sp, color = AdminColors.Muted)
                        Text(f.title.ifBlank { "Sarlavha" }, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(f.body.ifBlank { "Matn shu yerda ko'rinadi" }, fontSize = 13.sp, color = Color(0xFF475569))
                    }
                }
            }

            Caps("Kimga")
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetOption("Hammaga", "Barcha foydalanuvchilar", f.toAll) { vm.update { it.copy(toAll = true) } }
                TargetOption("Tanlanganlarga", "UID'lar ro'yxati (500 tagacha)", !f.toAll) { vm.update { it.copy(toAll = false) } }
                if (!f.toAll) {
                    AdminField(null, f.uids, { v -> vm.update { it.copy(uids = v.take(70_000)) } }, placeholder = "uid1, uid2 … (vergul yoki qator bilan)", minLines = 3)
                    val n = AdminRules.parseUids(f.uids)?.size ?: 0
                    Text("$n ta UID", fontSize = 12.sp, color = AdminColors.Muted)
                }
            }

            Caps("Qachon")
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WhenOption("Hozir", !later, Modifier.weight(1f)) { later = false; vm.update { it.copy(sendAt = null) } }
                    WhenOption("Rejalashtirish", later, Modifier.weight(1f)) { later = true }
                }
                if (later) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val at = f.sendAt
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PickerBox("Sana", at?.let { java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "Tanlang", Modifier.weight(1f)) {
                                pickDateTime(context, at) { millis -> vm.update { it.copy(sendAt = millis) } }
                            }
                            PickerBox("Vaqt", at?.let { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "—", Modifier.weight(1f)) {
                                pickDateTime(context, at) { millis -> vm.update { it.copy(sendAt = millis) } }
                            }
                        }
                        Text("Telefon vaqt zonasi bo'yicha. 30 kungacha oldinga rejalashtirish mumkin.", fontSize = 12.sp, color = AdminColors.Faint)
                    }
                }
            }

            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (f.toAll) "Barcha foydalanuvchilarga yetkaziladi" else "Tanlangan ${AdminRules.parseUids(f.uids)?.size ?: 0} ta foydalanuvchiga yetkaziladi",
                    fontSize = 14.sp, color = AdminColors.Body,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(AdminColors.BlueSoft).padding(horizontal = 14.dp, vertical = 12.dp),
                )
                AdminButton(
                    if (f.sendAt == null) "Yuborish" else "Rejalashtirish · ${dateTime(f.sendAt ?: 0)}",
                    { vm.send(System.currentTimeMillis()) },
                    enabled = role.canSendPush && !s.busy && f.title.isNotBlank() && f.body.isNotBlank() && (!later || f.sendAt != null),
                    height = 54.dp,
                )
                AdminButton("Push tarixi", onHistory, style = ButtonStyle.NEUTRAL, height = 48.dp)
            }
        }
    }
}

private fun pickDateTime(context: android.content.Context, current: Long?, onPicked: (Long) -> Unit) {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = current ?: (System.currentTimeMillis() + 3_600_000) }
    android.app.DatePickerDialog(context, { _, y, m, d ->
        android.app.TimePickerDialog(context, { _, h, min ->
            cal.set(y, m, d, h, min, 0)
            onPicked(cal.timeInMillis)
        }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true).show()
    }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).apply {
        datePicker.minDate = System.currentTimeMillis() - 60_000
        datePicker.maxDate = System.currentTimeMillis() + 30L * 86_400_000
    }.show()
}

@Composable
private fun TargetOption(title: String, sub: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (on) AdminColors.BlueSoft else Color.White)
            .border(if (on) 2.dp else 1.dp, if (on) AdminColors.Blue else AdminColors.Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (on) AdminColors.Blue else Color(0xFFCBD5E1), CircleShape), contentAlignment = Alignment.Center) {
            if (on) Box(Modifier.size(9.dp).clip(CircleShape).background(AdminColors.Blue))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, fontSize = 12.sp, color = if (on) Color(0xFF475569) else AdminColors.Muted)
        }
    }
}

@Composable
private fun WhenOption(text: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (on) AdminColors.BlueSoft else Color.White)
            .border(if (on) 2.dp else 1.dp, if (on) AdminColors.Blue else AdminColors.Border, RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) AdminColors.BlueDark else AdminColors.Body) }
}

@Composable
private fun PickerBox(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AdminColors.Body)
        Box(
            Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                .border(1.dp, AdminColors.Border, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) { Text(value, fontSize = 15.sp) }
    }
}

private fun pushStatus(status: String): Pair<String, Tone> = when (status) {
    "scheduled" -> "Kutilmoqda" to Tone.AMBER
    "sent" -> "Yuborildi" to Tone.GREEN
    "sending" -> "Yuborilmoqda" to Tone.BLUE
    "cancelled" -> "Bekor qilindi" to Tone.GREY
    "failed" -> "Xato" to Tone.RED
    else -> status to Tone.GREY
}

@Composable
fun PushHistoryScreen(role: Role, onBack: () -> Unit, onNew: () -> Unit, vm: PushViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Push tarixi", onBack = onBack, onRefresh = vm::load)
        StatusLine(s)
        val all = s.data.orEmpty()
        val scheduled = all.filter { it.status == "scheduled" }
        val rest = all - scheduled.toSet()
        ScrollBody {
            if (role.canSendPush) Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) { AdminButton("+ Yangi push", onNew) }
            if (s.loading && s.data == null) Loading()
            if (scheduled.isNotEmpty()) {
                Caps("Rejalashtirilgan (${scheduled.size})")
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    scheduled.forEach { j ->
                        PushCard(j.title, j.body, j.target, j.sendAtMillis, j.status) {
                            if (role.canSendPush) AdminButton("Bekor qilish", { vm.cancel(j.id) }, style = ButtonStyle.DANGER_OUTLINE, enabled = !s.busy, height = 40.dp)
                        }
                    }
                }
            }
            Caps("Yuborilgan")
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (rest.isEmpty() && s.data != null) Empty("Hali push yuborilmagan")
                rest.forEach { j -> PushCard(j.title, j.body, j.target, j.sendAtMillis, j.status) {} }
            }
        }
    }
}

@Composable
private fun PushCard(title: String, body: String, target: String, at: Long, status: String, actions: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            val (label, tone) = pushStatus(status)
            Pill(label, tone)
        }
        if (body.isNotBlank()) Text(body, fontSize = 13.sp, lineHeight = 19.sp, color = Color(0xFF475569), maxLines = 3)
        Text(if (target == "all") "Hammaga" else target.replace("users", "foydalanuvchi"), fontSize = 13.sp, color = Color(0xFF475569))
        Text(dateTime(at), fontSize = 12.sp, color = AdminColors.Muted)
        actions()
    }
}

// ---------------------------------------------------------------- A13 more

@Composable
fun MoreScreen(role: Role, onOpen: (String) -> Unit, onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Ko'proq")
        ScrollBody {
            Caps("Moderatsiya", top = 16.dp)
            RowsCard {
                MenuRow("Shikoyatlar", "Ilovalar va testerlar bo'yicha", AdminPaths.Flag, AdminColors.DangerSoft, AdminColors.DangerText) { onOpen("reports") }
                MenuRow("Foydalanuvchilar", "Qidirish, bloklash, Coin14", AdminPaths.Ban, AdminColors.Divider, AdminColors.Body, divider = false) { onOpen("users") }
            }
            if (role.canEditConfig) {
                Caps("Ilova sozlamalari")
                RowsCard {
                    MenuRow("Ilova versiyasi", "Majburiy va ixtiyoriy yangilanish", AdminPaths.Upload, AdminColors.SuccessSoft, AdminColors.Success) { onOpen("config/app_version") }
                    MenuRow("Flaglar", "Texnik ishlar, yangi ilova, xaridlar", AdminPaths.Gear) { onOpen("config/flags") }
                    MenuRow("Iqtisodiyot", "Narxlar, mukofotlar, jarimalar, limitlar", AdminPaths.Gear, AdminColors.AmberSoft, AdminColors.AmberText) { onOpen("config/economy") }
                    MenuRow("Yordam va aloqa", "Telegram, email, Google Group", AdminPaths.Help, Color(0xFFE0F2FE), Color(0xFF0369A1), divider = false) { onOpen("config/support") }
                }
            }
            Caps("Xabarlar va jurnal")
            RowsCard {
                if (role.canSendPush) MenuRow("Push tarixi", "Rejalashtirilgan va yuborilgan", AdminPaths.Bell) { onOpen("pushHistory") }
                MenuRow("Audit log", "Kim, qachon, nima qildi", AdminPaths.History, AdminColors.Divider, AdminColors.Body, divider = false) { onOpen("audit") }
            }
            Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp)) {
                AdminButton("Chiqish", onSignOut, style = ButtonStyle.DANGER_OUTLINE, height = 52.dp)
            }
            Text("Rolingiz: ${role.claim}", fontSize = 12.sp, color = AdminColors.Muted, modifier = Modifier.padding(start = 20.dp, top = 12.dp))
        }
    }
}

// ---------------------------------------------------------------- A10-A12 config editor

private val FIELD_LABELS = mapOf(
    "minBuild" to "Minimal versiya (build) — pastlari majburiy yangilanadi",
    "latestBuild" to "Tavsiya etilgan versiya (build) — pastlariga taklif chiqadi",
    "message" to "Foydalanuvchiga matn (nima yangi)",
    "telegram" to "Telegram havolasi (https://t.me/…)",
    "email" to "Email",
    "groupUrl" to "Google Group havolasi",
    "faqUrl" to "FAQ sahifasi (https://…)",
    "maintenanceMessage" to "Texnik ishlar matni",
    "standardFee" to "Standart joylash narxi",
    "proFee" to "Pro joylash narxi",
    "standardRewardPerDay" to "Standart: tester kunlik mukofoti",
    "proRewardPerDay" to "Pro: tester kunlik mukofoti",
    "completionBonus" to "14 kun bonusi",
    "feedbackReward" to "Fikr mukofoti",
    "failedPenalty" to "Uzilish jarimasi",
    "leftPenalty" to "Chiqib ketish jarimasi",
    "welcomeBonus" to "Xush kelibsiz bonusi",
    "maxActiveTests" to "Bir vaqtda aktiv testlar (1–100)",
    "testersNeeded" to "Kerakli testerlar (1–100)",
    "feature1Price" to "Top 1 kun narxi",
    "feature3Price" to "Top 3 kun narxi",
    "feature7Price" to "Top 7 kun narxi",
)

private val FLAG_LABELS = mapOf(
    "maintenance" to ("Texnik ishlar rejimi" to "Hamma foydalanuvchiga to'xtash oynasi chiqadi"),
    "newApps" to ("Yangi ilova joylash" to "O'chirilsa, egalari yangi ilova qo'sha olmaydi"),
    "purchases" to ("Coin14 xaridlari" to "O'chirilsa, do'kon yopiladi"),
)

private val ECONOMY_GROUPS = listOf(
    "Joylash" to listOf("standardFee", "proFee"),
    "Mukofotlar" to listOf("standardRewardPerDay", "proRewardPerDay", "completionBonus", "feedbackReward", "welcomeBonus"),
    "Jarimalar" to listOf("failedPenalty", "leftPenalty"),
    "Top" to listOf("feature1Price", "feature3Price", "feature7Price"),
    "Limitlar" to listOf("maxActiveTests", "testersNeeded"),
)

@Composable
fun ConfigScreen(role: Role, onBack: () -> Unit, vm: ConfigViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val fields by vm.fields.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    val title = when (vm.doc) {
        "app_version" -> "Ilova versiyasi"
        "support" -> "Yordam va aloqa"
        "flags" -> "Flaglar"
        "economy" -> "Sozlamalar va iqtisodiyot"
        else -> "Sozlama"
    }
    val edit = role.canEditConfig
    Column(Modifier.fillMaxSize()) {
        AdminHeader(title, onBack = onBack, onRefresh = vm::load)
        StatusLine(s)
        Box(Modifier.weight(1f)) {
            ScrollBody {
                when (vm.doc) {
                    "app_version" -> {
                        Row(
                            Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Hozirgi tavsiya etilgan build", fontSize = 12.sp, color = AdminColors.Muted)
                                Text(fields["latestBuild"].orEmpty().ifBlank { "—" }, fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                            }
                            Pill("min ${fields["minBuild"].orEmpty().ifBlank { "—" }}", Tone.GREY)
                        }
                        Caps("Ixtiyoriy yangilanish")
                        ConfigCard { NumberField("latestBuild", fields, vm, edit) }
                        Caps("Majburiy yangilanish")
                        ConfigCard { NumberField("minBuild", fields, vm, edit) }
                        Caps("Nima yangi")
                        ConfigCard { AdminField(FIELD_LABELS["message"], fields["message"].orEmpty(), { vm.set("message", it.take(300)) }, minLines = 3, enabled = edit) }
                    }
                    "flags" -> {
                        Caps("Flaglar", top = 16.dp)
                        ConfigCard {
                            FLAG_LABELS.entries.forEachIndexed { i, (key, labels) ->
                                val on = fields[key]?.let { it == "true" } ?: (key != "maintenance")
                                SwitchRow(labels.first, labels.second, on, { vm.set(key, it.toString()) }, enabled = edit, divider = i < FLAG_LABELS.size - 1)
                            }
                        }
                        Caps("Texnik ishlar matni")
                        ConfigCard { AdminField(null, fields["maintenanceMessage"].orEmpty(), { vm.set("maintenanceMessage", it.take(300)) }, minLines = 3, enabled = edit) }
                    }
                    "economy" -> {
                        Text("Bo'sh maydon o'zgarmaydi (standart qiymat ishlaydi). Hammasi Coin14 da.", fontSize = 13.sp, color = AdminColors.Muted, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp))
                        ECONOMY_GROUPS.forEach { (group, keys) ->
                            Caps(group)
                            ConfigCard { keys.forEach { NumberField(it, fields, vm, edit) } }
                        }
                    }
                    "support" -> {
                        Caps("Bog'lanish", top = 16.dp)
                        ConfigCard {
                            listOf("telegram", "email", "groupUrl", "faqUrl").forEach { key ->
                                AdminField(FIELD_LABELS[key], fields[key].orEmpty(), { vm.set(key, it.take(300)) }, enabled = edit, keyboard = if (key == "email") KeyboardType.Email else KeyboardType.Uri)
                            }
                        }
                    }
                }
            }
        }
        if (edit) {
            Box(Modifier.fillMaxWidth().background(Color.White).padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp)) {
                AdminButton("Saqlash", vm::save, enabled = !s.busy, height = 54.dp)
            }
        }
    }
}

@Composable
private fun ConfigCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.padding(horizontal = 20.dp), padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), spacing = 14.dp, content = content)
}

@Composable
private fun NumberField(key: String, fields: Map<String, String>, vm: ConfigViewModel, edit: Boolean) {
    AdminField(FIELD_LABELS[key] ?: key, fields[key].orEmpty(), { v -> vm.set(key, v.filter(Char::isDigit).take(9)) }, keyboard = KeyboardType.Number, enabled = edit)
}

// ---------------------------------------------------------------- audit

@Composable
fun AuditScreen(onBack: () -> Unit, vm: AuditViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    Column(Modifier.fillMaxSize()) {
        AdminHeader("Audit log", onBack = onBack, onRefresh = vm::load)
        StatusLine(s)
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = Bottom)) {
            val list = s.data.orEmpty()
            itemsIndexedCard(list.size) { i ->
                val e = list[i]
                Column(Modifier.fillMaxWidth().hairline(i < list.lastIndex).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${auditLabel(e.action)} · ${e.target}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${e.actor} · ${dateTime(e.createdAtMillis)}", fontSize = 12.sp, color = AdminColors.Faint)
                }
            }
            if (s.data?.isEmpty() == true) item { Empty("Hali yozuv yo'q") }
        }
    }
}
