package xyz.malkki.neostumbler.data.emitter.internal.bluetooth

import java.util.UUID
import kotlinx.serialization.Serializable
import xyz.malkki.neostumbler.beaconparser.BeaconData

@Serializable
internal data class PassiveBluetoothBeaconScanResult(
    val address: String,
    val timestamp: Long,
    val signalStrength: Int,
    val beaconType: Int,
    val identifiers: List<Identifier>,
) {
    @Serializable
    internal sealed interface Identifier {
        companion object {
            fun BeaconData.Identifier.toStoreIdentifier(): Identifier {
                return when (this) {
                    is BeaconData.HexIdentifier -> HexIdentifier(identifier)
                    is BeaconData.IntIdentifier -> IntIdentifier(identifier)
                    is BeaconData.UuidIdentifier ->
                        UuidIdentifier(
                            identifier.mostSignificantBits,
                            identifier.leastSignificantBits,
                        )
                }
            }
        }

        @Serializable data class UuidIdentifier(val high: Long, val low: Long) : Identifier

        @Serializable data class IntIdentifier(val value: Int) : Identifier

        @Serializable data class HexIdentifier(val value: String) : Identifier

        fun toBeaconDataIdentifier(): BeaconData.Identifier {
            return when (this) {
                is HexIdentifier -> BeaconData.HexIdentifier(value)
                is IntIdentifier -> BeaconData.IntIdentifier(value)
                is UuidIdentifier -> BeaconData.UuidIdentifier(UUID(high, low))
            }
        }
    }
}
