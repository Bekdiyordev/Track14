package uz.beko404.track14.admin.data

import uz.beko404.track14.domain.model.AppStatus
import uz.beko404.track14.domain.model.TesterStats

data class DashboardCounts(
    val users: Long = 0,
    val activeCampaigns: Long = 0,
    val openReports: Long = 0,
    val scheduledPushes: Long = 0,
)

data class AdminUser(
    val uid: String,
    val name: String,
    val email: String,
    val blocked: Boolean,
    val blockedReason: String,
    val lang: String,
    val stats: TesterStats,
    val createdAtMillis: Long,
)

data class LedgerRow(val id: String, val type: String, val amount: Long, val createdAtMillis: Long)

data class UserDetail(val user: AdminUser, val balance: Long, val ledger: List<LedgerRow>)

data class AdminApp(
    val id: String,
    val name: String,
    val packageName: String,
    val ownerUid: String,
    val status: AppStatus?,
    val category: String,
    val createdAtMillis: Long,
)

data class AppDetailAdmin(
    val app: AdminApp,
    val campaignStatus: String?,
    val plan: String?,
    val feePaid: Long,
    val joined: Int,
    val completed: Int,
    val failed: Int,
    val suspendedReason: String,
)

data class Report(
    val id: String,
    val targetType: String,
    val targetId: String,
    val reason: String,
    val comment: String,
    val reporterUid: String,
    val status: String,
    val createdAtMillis: Long,
)

data class PushJob(
    val id: String,
    val title: String,
    val body: String,
    val target: String,
    val status: String,
    val sendAtMillis: Long,
)

data class AuditEntry(val id: String, val actor: String, val action: String, val target: String, val createdAtMillis: Long)
