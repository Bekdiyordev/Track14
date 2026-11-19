package uz.beko404.track14.admin.logic

import java.time.Instant
import java.time.ZoneId

/** Small calculations behind the admin dashboard and app screen; kept pure for tests. */
object DashboardMath {
    /** Play list prices (USD) of the Coin14 packs, by coins credited. Before Google's fee. */
    val PACK_PRICE_USD: Map<Long, Double> = mapOf(
        500L to 1.99,
        1_000L to 3.99,
        2_000L to 6.99,
        5_000L to 14.99,
        10_000L to 27.99,
    )

    /** Gross revenue of the given purchase transactions; unknown amounts count as 0. */
    fun revenueUsd(purchaseAmounts: List<Long>): Double =
        purchaseAmounts.sumOf { PACK_PRICE_USD[it] ?: 0.0 }

    /** Local midnight of [nowMillis] in [zone]. */
    fun startOfDay(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    /** Report reasons by count, most frequent first (bars on the app screen). */
    fun reasonCounts(reasons: List<String>): List<Pair<String, Int>> =
        reasons.groupingBy { it }.eachCount().entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key to it.value }

    /** Block reason chips; the chosen chip fills the user-visible note, which stays editable. */
    val BLOCK_REASONS = listOf(
        "Soxta akkaunt" to "Soxta akkauntlar ishlatilgani aniqlandi.",
        "Spam" to "Spam va keraksiz harakatlar aniqlandi.",
        "Aldov" to "Test natijalarini aldash aniqlandi.",
        "Boshqa" to "",
    )

    /** Allowed block lengths in days; null is permanent. Mirrors BlockUser in admin.ts. */
    val BLOCK_DAYS: List<Int?> = listOf(7, 30, null)
}
