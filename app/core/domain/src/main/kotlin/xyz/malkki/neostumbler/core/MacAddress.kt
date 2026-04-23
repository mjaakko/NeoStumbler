package xyz.malkki.neostumbler.core

private const val MAX = 281474976710655

private const val GROUP_COUNT = 6
private const val GROUP_SIZE = 2

@JvmInline
value class MacAddress private constructor(val raw: Long) {
    constructor(macAddress: String) : this(macAddress.replace(":", "").uppercase().hexToLong())

    init {
        require(raw <= MAX) { "Invalid MAC address: $value" }
    }

    val value: String
        get() {
            return raw.toHexString()
                .windowed(GROUP_SIZE, GROUP_SIZE)
                .takeLast(GROUP_COUNT)
                .joinToString(":")
        }

    override fun toString(): String {
        return value
    }
}
