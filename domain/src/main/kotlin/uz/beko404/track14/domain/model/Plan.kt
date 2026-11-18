package uz.beko404.track14.domain.model

/** Listing tier chosen by the app owner. Server value: `standard` / `pro`. */
enum class Plan(val wireName: String) {
    STANDARD("standard"),
    PRO("pro");

    companion object {
        fun fromWire(value: String?): Plan? = entries.firstOrNull { it.wireName == value }
    }
}
