package xyz.malkki.neostumbler.data.emitter.internal.bluetooth

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import java.util.UUID
import xyz.malkki.neostumbler.beaconparser.BeaconLayout

private const val IBEACON_TYPE_CODE = 0x0215

private const val ALL_BITS_MASK: Byte = 0xFF.toByte()

// UUID -> 16 bytes
private const val UUID_BYTE_LENGTH = 16

// Byte -> 8 bits
private const val BYTE_BITS = 8

/**
 * @param manufacturerIds Bluetooth manufacturer IDs to use in the scan filters
 * @param identifiers List of beacon identifiers to use in the scan filter. If empty, filter does
 *   not include identifiers
 */
internal fun BeaconLayout.createScanFilters(
    manufacturerIds: List<Int>,
    identifiers: List<ByteArray> = emptyList(),
): List<ScanFilter> {
    val scanFilters = mutableListOf<ScanFilter>()

    if (typeCode.typeCode == IBEACON_TYPE_CODE) {
        scanFilters += createIBeaconScanFilters(manufacturerIds, identifiers)
    }

    val typeCodeBytes = typeCode.typeCode.toBytes(typeCode.size)

    val offset = serviceUuid?.serviceUuid128?.size ?: 2

    val filterSize = typeCode.endOffset + 1 - offset

    if (filterSize > 0) {
        val filter = ByteArray(filterSize)
        val mask = ByteArray(filterSize)

        for (i in offset..typeCode.endOffset) {
            val index = i - offset
            if (index < typeCode.startOffset) {
                filter[index] = 0
                mask[index] = 0
            } else {
                filter[index] = typeCodeBytes[i - typeCode.startOffset]
                mask[index] = ALL_BITS_MASK
            }
        }

        manufacturerIds.forEach { manufacturerId ->
            scanFilters +=
                createScanFilter(
                    serviceUuid = serviceUuid,
                    manufacturerId = manufacturerId,
                    filter = filter,
                    mask = mask,
                    hasIdentifiers = identifiers.isNotEmpty(),
                )
        }
    }

    return scanFilters
}

private fun createScanFilter(
    serviceUuid: BeaconLayout.ServiceUuid?,
    manufacturerId: Int,
    filter: ByteArray,
    mask: ByteArray,
    hasIdentifiers: Boolean,
): ScanFilter {
    return if (serviceUuid?.serviceUuid != null) {
        val serviceUuid =
            ParcelUuid.fromString(
                "0000%04X-0000-1000-8000-00805f9b34fb".format(serviceUuid.serviceUuid)
            )

        ScanFilter.Builder().setServiceData(serviceUuid, filter, mask).build()
    } else if (serviceUuid?.serviceUuid128?.size == UUID_BYTE_LENGTH) {
        val serviceUuid =
            ParcelUuid(
                UUID(
                    serviceUuid!!.serviceUuid128!!.toLong(),
                    serviceUuid!!.serviceUuid128!!.toLong(start = UUID_BYTE_LENGTH / 2),
                )
            )

        if (hasIdentifiers) {
            ScanFilter.Builder().setServiceData(serviceUuid, filter, mask).build()
        } else {
            ScanFilter.Builder()
                .setServiceUuid(
                    serviceUuid,
                    ParcelUuid.fromString("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"),
                )
                .build()
        }
    } else {
        ScanFilter.Builder().setManufacturerData(manufacturerId, filter, mask).build()
    }
}

private fun BeaconLayout.createIBeaconScanFilters(
    manufacturerIds: List<Int>,
    identifiers: List<ByteArray>,
): List<ScanFilter> {
    val filterSize = 2 + identifiers.sumOf { it.size }

    return manufacturerIds.map { manufacturerId ->
        val filter = ByteArray(filterSize)
        val typeCodeBytes = typeCode.typeCode.toBytes(typeCode.size)

        filter[0] = typeCodeBytes[0]
        filter[1] = typeCodeBytes[1]

        var offset = 2

        identifiers.forEach { identifier ->
            identifier.copyInto(filter, offset)

            offset += identifier.size
        }

        val mask = ByteArray(filterSize) { ALL_BITS_MASK }
        ScanFilter.Builder().setManufacturerData(manufacturerId, filter, mask).build()
    }
}

private fun Int.toBytes(length: Int): ByteArray {
    val bytes = ByteArray(length)

    repeat(length) { i ->
        val shift = (length - i - 1) * BYTE_BITS

        val mask = ALL_BITS_MASK.toInt() shl shift
        bytes[i] = ((this and mask) shr shift).toByte()
    }

    return bytes
}

@Suppress("MagicNumber")
private fun ByteArray.toLong(start: Int = 0): Long {
    return (((this[start].toLong() and 0xFFL) shl 56) or
        ((this[start + 1].toLong() and 0xFFL) shl 48) or
        ((this[start + 2].toLong() and 0xFFL) shl 40) or
        ((this[start + 3].toLong() and 0xFFL) shl 32) or
        ((this[start + 4].toLong() and 0xFFL) shl 24) or
        ((this[start + 5].toLong() and 0xFFL) shl 16) or
        ((this[start + 6].toLong() and 0xFFL) shl 8) or
        ((this[start + 7].toLong() and 0xFFL) shl 0))
}
