package uz.beko404.track14.domain.time

/** Injected time source, so rules that depend on "now" stay testable. */
fun interface Clock {
    fun nowMillis(): Long
}
