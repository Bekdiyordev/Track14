package uz.beko404.track14.domain.model

/**
 * Coin14 amount. Signed on purpose: after a penalty the balance may go negative
 * until the user earns it back (see plan, section 5).
 */
@JvmInline
value class Coins(val value: Long) : Comparable<Coins> {
    operator fun plus(other: Coins) = Coins(value + other.value)
    operator fun minus(other: Coins) = Coins(value - other.value)
    operator fun unaryMinus() = Coins(-value)
    override fun compareTo(other: Coins): Int = value.compareTo(other.value)

    val isNegative: Boolean get() = value < 0

    companion object {
        val ZERO = Coins(0)
    }
}

val Int.coins: Coins get() = Coins(toLong())
