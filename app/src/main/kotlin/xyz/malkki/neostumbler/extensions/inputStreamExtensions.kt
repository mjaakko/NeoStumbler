package xyz.malkki.neostumbler.extensions

import java.io.InputStream
import java.util.zip.GZIPInputStream

fun InputStream.isGzipped(): Boolean {
    require(markSupported()) {
        "InputStream does not support mark(), cannot check for Gzip magic number"
    }

    mark(2)

    val a = read()
    val b = read()

    val isGzipped = ((b shl Byte.SIZE_BITS) or a) == GZIPInputStream.GZIP_MAGIC

    reset()

    return isGzipped
}
