package xyz.malkki.wifiscannerformls.geosubmit

import java.time.Instant

data class Report(
    val timestamp: Instant,
    val position: Position,
    val wifiAccessPoints: List<WifiAccessPoint>
) {
    data class Position(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Double?,
        val age: Long,
        val altitude: Double?,
        val altitudeAccuracy: Double?,
        val heading: Double?,
        val pressure: Double?,
        val speed: Double?,
        val source: String
    ) {
        companion object {
            fun fromDbEntity(position: xyz.malkki.wifiscannerformls.db.entities.Position): Position {
                return Position(
                    position.latitude,
                    position.longitude,
                    position.accuracy,
                    position.age,
                    position.altitude,
                    position.altitudeAccuracy,
                    position.heading,
                    position.pressure,
                    position.speed,
                    position.source
                )
            }
        }
    }

    data class WifiAccessPoint(
        val macAddress: String,
        val radioType: String?,
        val age: Long,
        val channel: Int?,
        val frequency: Int?,
        val signalStrength: Int?,
        val signalToNoiseRatio: Int?,
        val ssid: String?
    ) {
        companion object {
            fun fromDbEntity(wifiAccessPoint: xyz.malkki.wifiscannerformls.db.entities.WifiAccessPoint): WifiAccessPoint {
                return WifiAccessPoint(
                    wifiAccessPoint.macAddress,
                    wifiAccessPoint.radioType,
                    wifiAccessPoint.age,
                    wifiAccessPoint.channel,
                    wifiAccessPoint.frequency,
                    wifiAccessPoint.signalStrength,
                    wifiAccessPoint.signalToNoiseRatio,
                    wifiAccessPoint.ssid
                )
            }
        }
    }
}
