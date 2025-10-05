package xyz.malkki.neostumbler.beaconparser

private const val UUID_16_BIT_LENGTH = 2
private const val UUID_32_BIT_LENGTH = 4
private const val UUID_128_BIT_LENGTH = 16

internal data class Pdu(
    val type: Byte,
    val declaredLength: Int,
    val startIndex: Int,
    val endIndex: Int,
) {
    val actualLength: Int
        get() = this.endIndex - this.startIndex + 1

    companion object {
        private const val PDU_MAX_LENGTH = 31

        /**
         * Parse a PDU from a byte array looking offset by startIndex
         *
         * @param bytes
         * @param startIndex
         * @return
         */
        fun parse(bytes: ByteArray, startIndex: Int): Pdu? {
            if (bytes.size - startIndex >= 2) {
                val length = bytes[startIndex]
                if (length > 0) {
                    val type = bytes[startIndex + 1]
                    val firstIndex = startIndex + 2
                    if (firstIndex < bytes.size) {
                        val endIndex = (startIndex + length).coerceAtMost(bytes.size - 1)

                        return Pdu(
                            type = type,
                            declaredLength = length.toInt(),
                            startIndex = firstIndex,
                            endIndex = endIndex,
                        )
                    }
                }
            }

            return null
        }

        fun parseFromBleAdvertisement(bytes: ByteArray): List<Pdu> {
            fun parsePdus(startIndex: Int, endIndex: Int): List<Pdu> {
                return buildList {
                    var index = startIndex
                    var pdu: Pdu?

                    do {
                        pdu = parse(bytes, index)

                        if (pdu != null) {
                            index += pdu.declaredLength + 1
                            add(pdu)
                        }
                    } while (pdu != null && index < endIndex)
                }
            }

            return buildList {
                addAll(parsePdus(0, bytes.size.coerceAtMost(PDU_MAX_LENGTH)))

                if (bytes.size > PDU_MAX_LENGTH) {
                    addAll(parsePdus(PDU_MAX_LENGTH, bytes.size))
                }
            }
        }
    }

    /** @property expectedLength `null` if not specified */
    enum class PduType(val type: Byte, val expectedLength: Int? = null) {
        MANUFACTURER_DATA_AD(0xFF.toByte(), null),
        GATT_SERVICE_DATA_UUID_16_BIT_AD(0x16.toByte(), UUID_16_BIT_LENGTH),
        GATT_SERVICE_DATA_UUID_32_BIT_AD(0x20.toByte(), UUID_32_BIT_LENGTH),
        GATT_SERVICE_DATA_UUID_128_BIT_AD(0x21.toByte(), UUID_128_BIT_LENGTH),
        GATT_SERVICE_COMPLETE_UUID_128_BIT_AD(0x07.toByte(), UUID_128_BIT_LENGTH);

        companion object {
            internal fun fromType(type: Byte): PduType? {
                return entries.find { it.type == type }
            }
        }
    }
}
