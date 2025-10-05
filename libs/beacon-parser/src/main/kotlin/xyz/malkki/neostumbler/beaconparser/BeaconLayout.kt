package xyz.malkki.neostumbler.beaconparser

import xyz.malkki.neostumbler.beaconparser.BeaconLayout.Data.Companion.parseData
import xyz.malkki.neostumbler.beaconparser.BeaconLayout.Identifier.Companion.parseIdentifier
import xyz.malkki.neostumbler.beaconparser.BeaconLayout.Power.Companion.parsePower
import xyz.malkki.neostumbler.beaconparser.BeaconLayout.ServiceUuid.Companion.parseServiceUuid
import xyz.malkki.neostumbler.beaconparser.BeaconLayout.TypeCode.Companion.parseTypeCode

private const val HEX_RADIX = 16

data class BeaconLayout(
    val identifiers: List<Identifier>,
    val datas: List<Data>,
    val typeCode: TypeCode,
    val serviceUuid: ServiceUuid?,
    val power: Power?,
) {
    val layoutSize: Int
        get() {
            return maxOf(
                identifiers.maxOfOrNull { it.endOffset } ?: 0,
                datas.maxOfOrNull { it.endOffset } ?: 0,
                serviceUuid?.endOffset ?: 0,
                power?.endOffset ?: 0,
            )
        }

    companion object {
        private const val LITTLE_ENDIAN_FLAG = 'l'
        private const val VARIABLE_LENGTH_FLAG = 'v'

        /**
         * Parses beacon layout from the string. Uses a format compatible with Android Beacon
         * Library
         *
         * @throws IllegalArgumentException When the beacon layout is invalid
         */
        @Throws(IllegalArgumentException::class)
        fun String.parseBeaconLayout(): BeaconLayout {
            val terms = split(',')

            val identifiers = mutableListOf<Identifier>()
            val datas = mutableListOf<Data>()
            var typeCode: TypeCode? = null
            var serviceUuid: ServiceUuid? = null
            var power: Power? = null

            terms.forEach { term ->
                term.parseIdentifier()?.let { identifiers.add(it) }
                term.parseData()?.let { datas.add(it) }

                if (typeCode == null) {
                    typeCode = term.parseTypeCode()
                }

                if (serviceUuid == null) {
                    serviceUuid = term.parseServiceUuid()
                }

                if (power == null) {
                    power = term.parsePower()
                }
            }

            return BeaconLayout(
                identifiers = identifiers,
                datas = datas,
                typeCode = requireNotNull(typeCode) { "No type code found in the beacon layout" },
                serviceUuid = serviceUuid,
                power = power,
            )
        }
    }

    sealed interface Segment {
        val startOffset: Int
        val endOffset: Int

        val size: Int
            get() = endOffset - startOffset + 1
    }

    data class Identifier(
        override val startOffset: Int,
        override val endOffset: Int,
        val littleEndian: Boolean,
        val variableLength: Boolean,
    ) : Segment {
        companion object {
            private val PATTERN = Regex("i:(\\d+)-(\\d+)([blv]*)?")

            private const val START_OFFSET_GROUP = 1
            private const val END_OFFSET_GROUP = 2
            private const val FLAGS_GROUP = 3

            internal fun String.parseIdentifier(): Identifier? {
                return PATTERN.find(this)?.let {
                    Identifier(
                        startOffset = it.groupValues[START_OFFSET_GROUP].toInt(),
                        endOffset = it.groupValues[END_OFFSET_GROUP].toInt(),
                        littleEndian = it.groupValues[FLAGS_GROUP].contains(LITTLE_ENDIAN_FLAG),
                        variableLength = it.groupValues[FLAGS_GROUP].contains(VARIABLE_LENGTH_FLAG),
                    )
                }
            }
        }
    }

    data class Data(
        override val startOffset: Int,
        override val endOffset: Int,
        val littleEndian: Boolean,
    ) : Segment {
        companion object {
            private val PATTERN = Regex("d:(\\d+)-(\\d+)([bl]*)?")

            private const val START_OFFSET_GROUP = 1
            private const val END_OFFSET_GROUP = 2
            private const val FLAGS_GROUP = 3

            internal fun String.parseData(): Data? {
                return PATTERN.find(this)?.let {
                    Data(
                        startOffset = it.groupValues[START_OFFSET_GROUP].toInt(),
                        endOffset = it.groupValues[END_OFFSET_GROUP].toInt(),
                        littleEndian = it.groupValues[FLAGS_GROUP].contains(LITTLE_ENDIAN_FLAG),
                    )
                }
            }
        }
    }

    data class TypeCode(
        override val startOffset: Int,
        override val endOffset: Int,
        val typeCode: Int,
    ) : Segment {
        companion object {
            private val PATTERN = Regex("m:(\\d+)-(\\d+)=([0-9A-Fa-f]+)")

            private const val START_OFFSET_GROUP = 1
            private const val END_OFFSET_GROUP = 2
            private const val TYPE_CODE_GROUP = 3

            internal fun String.parseTypeCode(): TypeCode? {
                return PATTERN.find(this)?.let {
                    TypeCode(
                        startOffset = it.groupValues[START_OFFSET_GROUP].toInt(),
                        endOffset = it.groupValues[END_OFFSET_GROUP].toInt(),
                        typeCode = it.groupValues[TYPE_CODE_GROUP].toInt(HEX_RADIX),
                    )
                }
            }
        }
    }

    data class ServiceUuid(
        override val startOffset: Int,
        override val endOffset: Int,
        val serviceUuid: Long?,
        val serviceUuid128: ByteArray?,
    ) : Segment {
        companion object {
            private val PATTERN = Regex("s:(\\d+)-(\\d+)=([0-9A-Fa-f\\-]+)")

            private const val START_OFFSET_GROUP = 1
            private const val END_OFFSET_GROUP = 2
            private const val SERVICE_UUID_GROUP = 3

            private const val NUMERIC_SERVICE_UUID_LENGTH = 2

            private const val SHORT_HEX_SERVICE_UUID_LENGTH = 4
            private const val LONG_HEX_SERVICE_UUID_LENGTH = 16

            internal fun String.parseServiceUuid(): ServiceUuid? {
                return PATTERN.find(this)?.let {
                    val startOffset = it.groupValues[START_OFFSET_GROUP].toInt()
                    val endOffset = it.groupValues[END_OFFSET_GROUP].toInt()

                    val serviceUuidString = it.groupValues[SERVICE_UUID_GROUP]
                    val serviceUuidByteLength = endOffset - startOffset + 1

                    val serviceUuid =
                        if (serviceUuidByteLength == NUMERIC_SERVICE_UUID_LENGTH) {
                            serviceUuidString.toLong(HEX_RADIX)
                        } else {
                            null
                        }

                    val serviceUuid128 =
                        if (
                            serviceUuidByteLength == LONG_HEX_SERVICE_UUID_LENGTH ||
                                serviceUuidByteLength == SHORT_HEX_SERVICE_UUID_LENGTH
                        ) {
                            val serviceUuidStringWithoutDash = serviceUuidString.replace("-", "")

                            ByteArray(serviceUuidByteLength) { index ->
                                val strIndex =
                                    serviceUuidStringWithoutDash.length - ((index + 1) * 2)

                                serviceUuidStringWithoutDash
                                    .substring(strIndex, strIndex + 2)
                                    .hexToByte()
                            }
                        } else {
                            null
                        }

                    ServiceUuid(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        serviceUuid = serviceUuid,
                        serviceUuid128 = serviceUuid128,
                    )
                }
            }
        }
    }

    data class Power(
        override val startOffset: Int,
        override val endOffset: Int,
        val dbmCorrection: Int?,
    ) : Segment {
        companion object {
            private val PATTERN = Regex("p:(\\d+)-(\\d+):?([\\-\\d]+)?")

            internal fun String.parsePower(): Power? {
                return PATTERN.find(this)?.let {
                    Power(
                        startOffset = it.groupValues[1].toInt(),
                        endOffset = it.groupValues[2].toInt(),
                        dbmCorrection =
                            if (it.groupValues.size >= 4) {
                                it.groupValues[3].toIntOrNull()
                            } else {
                                null
                            },
                    )
                }
            }
        }
    }
}
