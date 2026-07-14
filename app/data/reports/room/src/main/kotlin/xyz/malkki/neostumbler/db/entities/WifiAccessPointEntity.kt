package xyz.malkki.neostumbler.db.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.values.SignalStrength
import xyz.malkki.neostumbler.db.entities.embeddables.EstimatedDistanceEmbeddable

@Entity(
    foreignKeys =
        [
            ForeignKey(
                entity = Report::class,
                parentColumns = ["id"],
                childColumns = ["reportId"],
                onDelete = ForeignKey.CASCADE,
            )
        ]
)
internal data class WifiAccessPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    val macAddress: String,
    val radioType: String?,
    val age: Long,
    val channel: Int?,
    val frequency: Int?,
    val signalStrength: Int?,
    val ssid: String?,
    @Embedded("distance_") val estimatedDistanceEmbeddable: EstimatedDistanceEmbeddable?,
    @ColumnInfo(index = true) val reportId: Long?,
) {
    companion object {
        fun EmitterObservation<WifiAccessPoint, MacAddress>.toEntity(
            reportTimestamp: Long,
            reportId: Long,
        ): WifiAccessPointEntity {
            val wifiAccessPoint = emitter

            val age = reportTimestamp - timestamp

            return WifiAccessPointEntity(
                id = null,
                macAddress = wifiAccessPoint.macAddress.value,
                radioType = wifiAccessPoint.radioType?.to802String(),
                age = age,
                channel = wifiAccessPoint.channel,
                frequency = wifiAccessPoint.frequency,
                signalStrength = wifiAccessPoint.signalStrength?.dbm,
                ssid = wifiAccessPoint.ssid,
                estimatedDistanceEmbeddable =
                    estimatedDistance?.let {
                        EstimatedDistanceEmbeddable(
                            meters = it.distance.meters,
                            accuracy = it.accuracy?.meters,
                            rangingType = it.rangingType,
                        )
                    },
                reportId = reportId,
            )
        }
    }
}

internal fun WifiAccessPointEntity.toWifiAccessPoint(): WifiAccessPoint {
    return WifiAccessPoint(
        macAddress = MacAddress(macAddress),
        radioType = radioType?.let { WifiAccessPoint.RadioType.from802String(it) },
        channel = channel,
        frequency = frequency,
        ssid = ssid,
        signalStrength = signalStrength?.let { SignalStrength(it) },
    )
}
