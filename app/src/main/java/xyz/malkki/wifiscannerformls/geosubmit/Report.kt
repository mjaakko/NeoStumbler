package xyz.malkki.wifiscannerformls.geosubmit

import java.time.Instant

data class Report(
    val timestamp: Instant,
    val position: Position,
    val wifiAccessPoints: List<WifiAccessPoint>?,
    val cellTowers: List<CellTower>?,
    val bluetoothBeacons: List<BluetoothBeacon>?
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

    data class CellTower(
        val radioType: String,
        val mobileCountryCode: Int?,
        val mobileNetworkCode: Int?,
        val locationAreaCode: Int?,
        val cellId: Int?,
        val age: Long,
        val asu: Int?,
        val primaryScramblingCode: Int?,
        val serving: Int?,
        val signalStrength: Int?,
        val timingAdvance: Int?
    ) {
        companion object {
            fun fromDbEntity(cellTower: xyz.malkki.wifiscannerformls.db.entities.CellTower): CellTower {
                return CellTower(
                    cellTower.radioType,
                    cellTower.mobileCountryCode,
                    cellTower.mobileNetworkCode,
                    cellTower.locationAreaCode,
                    cellTower.cellId,
                    cellTower.age,
                    cellTower.asu,
                    cellTower.primaryScramblingCode,
                    cellTower.serving,
                    cellTower.signalStrength,
                    cellTower.timingAdvance
                )
            }
        }
    }

    data class BluetoothBeacon(
        val macAddress: String,
        val name: String?,
        val age: Long,
        val signalStrength: Int?
    ) {
        companion object {
            fun fromDbEntity(beacon: xyz.malkki.wifiscannerformls.db.entities.BluetoothBeacon): BluetoothBeacon {
                return BluetoothBeacon(
                    beacon.macAddress,
                    beacon.name,
                    beacon.age,
                    beacon.signalStrength
                )
            }
        }
    }
}
