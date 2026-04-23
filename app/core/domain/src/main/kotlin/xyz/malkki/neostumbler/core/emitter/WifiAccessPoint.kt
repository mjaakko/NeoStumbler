package xyz.malkki.neostumbler.core.emitter

import xyz.malkki.neostumbler.core.MacAddress

data class WifiAccessPoint(
    val macAddress: MacAddress,
    val radioType: RadioType? = null,
    val channel: Int? = null,
    val frequency: Int? = null,
    val ssid: String? = null,
    override val signalStrength: Int? = null,
) : Emitter<MacAddress> {
    override val uniqueKey: MacAddress
        get() = macAddress

    enum class RadioType {
        G,
        N,
        AC,
        AX,
        BE;

        fun to802String(): String {
            return "802.11" + name.lowercase()
        }

        companion object {
            fun from802String(value: String): RadioType? {
                return try {
                    RadioType.valueOf(value.trim().replaceFirst("802.11", "").uppercase())
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        }
    }
}
