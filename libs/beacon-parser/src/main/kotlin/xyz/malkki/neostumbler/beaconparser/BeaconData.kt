package xyz.malkki.neostumbler.beaconparser

import androidx.collection.LongList
import androidx.collection.ObjectList
import java.util.UUID

data class BeaconData(
    val beaconType: Int,
    val identifiers: ObjectList<Identifier>,
    val dataFields: LongList,
) {
    sealed interface Identifier {

        companion object {
            private const val NUMERIC_IDENTIFIER_MAX_SIZE = 2
            private const val UUID_IDENTIFIER_SIZE = 16

            private const val ALL_BYTE_BITS_MASK = 0xFF
            private const val BYTE_SIZE = 8

            fun ByteArray.toIdentifier(): Identifier {
                return if (size <= NUMERIC_IDENTIFIER_MAX_SIZE) {
                    var result = 0

                    for (i in 0 until size) {
                        result =
                            result or
                                ((this[i].toInt() and ALL_BYTE_BITS_MASK) shl
                                    ((size - i - 1) * BYTE_SIZE))
                    }

                    IntIdentifier(result)
                } else if (size == UUID_IDENTIFIER_SIZE) {
                    UuidIdentifier(UUID(toLong(), toLong(start = 8)))
                } else {
                    HexIdentifier("0x" + toHexString())
                }
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
        }
    }

    @JvmInline
    value class IntIdentifier(val identifier: Int) : Identifier {
        override fun toString(): String {
            return identifier.toString()
        }
    }

    @JvmInline
    value class UuidIdentifier(val identifier: UUID) : Identifier {
        override fun toString(): String {
            return identifier.toString()
        }
    }

    @JvmInline
    value class HexIdentifier(val identifier: String) : Identifier {
        override fun toString(): String {
            return identifier
        }
    }
}
