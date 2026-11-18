package uz.beko404.track14.domain.model

/** Ledger entry types; wire names match the server's TxType. */
enum class TxType(val wireName: String) {
    WELCOME("welcome"),
    PURCHASE("purchase"),
    EARN_DAY("earn_day"),
    BONUS("bonus"),
    FEEDBACK("feedback"),
    LISTING_FEE("listing_fee"),
    PENALTY("penalty"),
    FEATURE("feature"),
    REFUND("refund"),
    ADJUSTMENT("adjustment");

    companion object {
        fun fromWire(value: String?): TxType? = entries.firstOrNull { it.wireName == value }
    }
}

data class WalletTransaction(
    val id: String,
    val type: TxType,
    val amount: Coins,
    val refId: String,
    val createdAtMillis: Long,
) {
    val isIncome: Boolean get() = amount.value > 0
}

enum class TxFilter { ALL, INCOME, SPENT }

fun List<WalletTransaction>.filterBy(filter: TxFilter): List<WalletTransaction> = when (filter) {
    TxFilter.ALL -> this
    TxFilter.INCOME -> filter { it.isIncome }
    TxFilter.SPENT -> filter { !it.isIncome }
}

data class CoinPack(val productId: String, val coins: Coins, val popular: Boolean = false)

/** Same ids and amounts as COIN_PACKS in purchases.ts. Prices come from Play. */
object CoinPacks {
    val all = listOf(
        CoinPack("coins_500", Coins(500)),
        CoinPack("coins_1000", Coins(1_000)),
        CoinPack("coins_2000", Coins(2_000), popular = true),
        CoinPack("coins_5000", Coins(5_000)),
        CoinPack("coins_10000", Coins(10_000)),
    )

    fun byId(productId: String): CoinPack? = all.firstOrNull { it.productId == productId }
}
