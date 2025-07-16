package xyz.malkki.neostumbler.core

data class WifiAccessPoint(
    val macAddress: String,
    val radioType: RadioType? = null,
    val channel: Int? = null,
    val frequency: Int? = null,
    val signalStrength: Int? = null,
    val ssid: String? = null,
    override val timestamp: Long,
) : ObservedDevice<String> {
    override val uniqueKey: String
        get() = macAddress

    enum class RadioType {
        G,
        N,
        AC,
        AX,
        BE,
    }
}
