package xyz.malkki.neostumbler.beaconparser

import androidx.collection.LongList
import androidx.collection.ObjectList
import androidx.collection.mutableLongListOf
import androidx.collection.mutableObjectListOf
import xyz.malkki.neostumbler.beaconparser.BeaconData.Identifier.Companion.toIdentifier

private const val HEX_RADIX = 16

class BeaconParser(private val beaconLayout: BeaconLayout) {

    private val allowPduOverflow: Boolean = true

    private fun isBeacon(
        pdu: Pdu,
        scanData: ByteArray,
        typeCodeBytes: ByteArray,
        serviceUuidBytes: ByteArray?,
    ): Boolean {
        return if (serviceUuidBytes == null) {
            scanData.contentMatches(
                typeCodeBytes,
                pdu.startIndex + beaconLayout.typeCode.startOffset,
            )
        } else {
            val pduType = Pdu.PduType.fromType(type = pdu.type)
            val correctLength =
                pduType != Pdu.PduType.MANUFACTURER_DATA_AD &&
                    pduType?.expectedLength == serviceUuidBytes.size

            if (
                correctLength &&
                    scanData.contentMatches(
                        serviceUuidBytes,
                        pdu.startIndex + beaconLayout.serviceUuid!!.startOffset,
                    )
            ) {
                scanData.contentMatches(
                    typeCodeBytes,
                    pdu.startIndex + beaconLayout.typeCode.endOffset,
                )
            } else {
                false
            }
        }
    }

    private fun parseIdentifiers(pdu: Pdu, scanData: ByteArray): ObjectList<BeaconData.Identifier> {
        val identifiers = mutableObjectListOf<BeaconData.Identifier>()

        for (identifier in beaconLayout.identifiers) {
            val endIndex = identifier.endOffset + pdu.startIndex

            if (endIndex > pdu.endIndex && identifier.variableLength) {
                val start = identifier.startOffset + pdu.startIndex
                val end = pdu.endIndex + 1

                if (end > start) {
                    identifiers.add(
                        scanData
                            .extractIdentifier(start, end, identifier.littleEndian)
                            .toIdentifier()
                    )
                }
            } else if (endIndex > pdu.endIndex && !allowPduOverflow) {
                // can't parse
            } else {
                identifiers.add(
                    scanData
                        .extractIdentifier(
                            identifier.startOffset + pdu.startIndex,
                            endIndex + 1,
                            identifier.littleEndian,
                        )
                        .toIdentifier()
                )
            }
        }

        return identifiers
    }

    private fun parseDataFields(pdu: Pdu, scanData: ByteArray): LongList {
        val dataFields = mutableLongListOf()

        for (data in beaconLayout.datas) {
            val endIndex = data.endOffset + pdu.startIndex
            if (endIndex > pdu.endIndex && !allowPduOverflow) {
                dataFields.add(0L)
            } else {
                dataFields.add(
                    scanData
                        .let {
                            if (data.littleEndian) {
                                it.reversedArray()
                            } else {
                                it
                            }
                        }
                        .copyOfRange(data.startOffset + pdu.startIndex, endIndex)
                        .toHexString()
                        .takeIf { it.isNotEmpty() }
                        ?.toLong(HEX_RADIX) ?: 0L
                )
            }
        }

        return dataFields
    }

    fun parseScanData(scanData: ByteArray): BeaconData? {
        val pdus = Pdu.parseFromBleAdvertisement(scanData)

        val parsablePdus = mutableObjectListOf<Pdu>()
        pdus.forEach { pdu ->
            val pduType = Pdu.PduType.fromType(pdu.type)

            @Suppress("ComplexCondition")
            if (
                pduType != null &&
                    pduType.expectedLength == beaconLayout.serviceUuid?.serviceUuid128?.size ||
                    (pduType == Pdu.PduType.MANUFACTURER_DATA_AD) ||
                    (pduType == Pdu.PduType.GATT_SERVICE_DATA_UUID_16_BIT_AD &&
                        beaconLayout.serviceUuid?.serviceUuid != null)
            ) {
                parsablePdus.add(pdu)
            }
        }

        parsablePdus.forEach { pdu ->
            val typeCodeBytes =
                beaconLayout.typeCode.typeCode.toLong().toBytes(beaconLayout.typeCode.size)

            val serviceUuidBytes =
                beaconLayout.serviceUuid?.serviceUuid128
                    ?: beaconLayout.serviceUuid
                        ?.serviceUuid
                        ?.toBytes(beaconLayout.serviceUuid.size, bigEndian = false)

            val isBeacon =
                isBeacon(
                    pdu = pdu,
                    scanData = scanData,
                    typeCodeBytes = typeCodeBytes,
                    serviceUuidBytes = serviceUuidBytes,
                )

            if (isBeacon) {
                val bytesWithOverflow =
                    if (
                        scanData.size <= pdu.startIndex + beaconLayout.layoutSize &&
                            allowPduOverflow
                    ) {
                        scanData.copyOf(pdu.startIndex + beaconLayout.layoutSize)
                    } else {
                        scanData
                    }

                val identifiers = parseIdentifiers(pdu = pdu, scanData = bytesWithOverflow)
                val dataFields = parseDataFields(pdu = pdu, scanData = bytesWithOverflow)

                // Transmitted power is currently not parsed

                return BeaconData(
                    beaconType = beaconLayout.typeCode.typeCode,
                    identifiers = identifiers,
                    dataFields = dataFields,
                )
            }
        }

        return null
    }
}

private const val BYTE_LENGTH = 8
private const val ALL_BYTE_BITS_MASK = 0xFFL

private fun Long.toBytes(length: Int = 8, bigEndian: Boolean = true): ByteArray {
    val bytes = ByteArray(length)

    for (i in 0 until length) {
        val indexWithEndianess =
            if (bigEndian) {
                i
            } else {
                length - i - 1
            }

        val shift = (length - indexWithEndianess - 1) * BYTE_LENGTH

        val mask = ALL_BYTE_BITS_MASK shl shift

        bytes[i] = ((this and mask) shr shift).toByte()
    }

    return bytes
}

private fun ByteArray.contentMatches(other: ByteArray, offset: Int = 0): Boolean {
    if (size - offset < other.size) {
        return false
    }

    for (i in 0 until other.size) {
        if (this[offset + i] != other[i]) {
            return false
        }
    }

    return true
}

private fun ByteArray.extractIdentifier(
    startIndex: Int,
    endIndex: Int,
    littleEndian: Boolean,
): ByteArray {
    val identifier = copyOfRange(startIndex, endIndex)

    return if (littleEndian) {
        identifier.reversedArray()
    } else {
        identifier
    }
}
