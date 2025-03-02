package xyz.malkki.neostumbler.ichnaea.dto

import kotlinx.serialization.Serializable
import xyz.malkki.neostumbler.db.entities.WifiAccessPointEntity

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
) {
    companion object {
        fun fromDbEntity(wifiAccessPointEntity: WifiAccessPointEntity): WifiAccessPointDto {
            return WifiAccessPointDto(
                wifiAccessPointEntity.macAddress,
                wifiAccessPointEntity.radioType,
                wifiAccessPointEntity.age,
                wifiAccessPointEntity.channel,
                wifiAccessPointEntity.frequency,
                wifiAccessPointEntity.signalStrength,
                wifiAccessPointEntity.signalToNoiseRatio,
                wifiAccessPointEntity.ssid,
            )
        }
    }
}
