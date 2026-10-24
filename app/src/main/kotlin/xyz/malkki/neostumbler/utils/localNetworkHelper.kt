@file:Suppress("MagicNumber") // IP addresses

package xyz.malkki.neostumbler.utils

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Checks if the given IP address or hostname is within the local network.
 *
 * Implementation note: This can do a DNS query if needed
 */
suspend fun String.isLocalNetwork(): Boolean {
    val inetAddresses: Array<InetAddress> =
        try {
            withContext(Dispatchers.IO) { InetAddress.getAllByName(this@isLocalNetwork) }
        } catch (_: UnknownHostException) {
            return false
        }

    return inetAddresses.any { it.isAndroidLocalAddress() }
}

/** See https://developer.android.com/privacy-and-security/local-network-definition */
private fun InetAddress.isAndroidLocalAddress(): Boolean {
    val bytes = address

    when (this) {
        is Inet4Address -> {
            val ipInt = bytes.toInt()
            return when {
                // 10.0.0.0/8
                isInCidr(ipInt, 0x0A000000.toInt(), 8) -> true
                // 172.16.0.0/12
                isInCidr(ipInt, 0xAC100000.toInt(), 12) -> true
                // 192.168.0.0/16
                isInCidr(ipInt, 0xC0A80000.toInt(), 16) -> true
                // 100.64.0.0/10
                isInCidr(ipInt, 0x64400000.toInt(), 10) -> true
                // 169.254.0.0/16
                isInCidr(ipInt, 0xA9FE0000.toInt(), 16) -> true
                // 255.255.255.255
                ipInt == 0xFFFFFFFF.toInt() -> true
                // 224.0.0.0/4
                isInCidr(ipInt, 0xE0000000.toInt(), 4) -> true
                else -> false
            }
        }
        is Inet6Address -> {
            // IPv6 Multicast address
            if (bytes.isNotEmpty() && (bytes[0].toInt() and 0xFF) == 0xFF) {
                return true
            }
            if (isLinkLocalAddress) {
                return true
            }
        }
    }

    return false
}

private fun ByteArray.toInt(): Int {
    var result = 0
    for (b in this) {
        result = (result shl 8) or (b.toInt() and 0xFF)
    }
    return result
}

private fun isInCidr(ipInt: Int, networkInt: Int, prefixLength: Int): Boolean {
    val mask = if (prefixLength == 0) 0 else (0xFFFFFFFF.toInt() shl (32 - prefixLength))
    return (ipInt and mask) == (networkInt and mask)
}
