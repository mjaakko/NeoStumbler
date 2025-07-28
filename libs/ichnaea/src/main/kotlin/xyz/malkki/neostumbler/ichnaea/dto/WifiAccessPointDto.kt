package xyz.malkki.neostumbler.ichnaea.dto

import kotlinx.serialization.Serializable

@Serializable
data class WifiAccessPointDto(
    val macAddress: String,
    val radioType: String? = null,
    val age: Long? = null,
    val channel: Int? = null,
    val frequency: Int? = null,
    val signalStrength: Int? = null,
    val signalToNoiseRatio: Int? = null,
    val ssid: String? = null,
)
