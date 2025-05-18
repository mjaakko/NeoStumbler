package xyz.malkki.neostumbler.utils.io

import java.io.FilterOutputStream
import java.io.OutputStream

class CloseShieldOutputStream(outputStream: OutputStream) : FilterOutputStream(outputStream) {
    override fun close() {
        // no-op
    }
}

fun OutputStream.closeShielded(): OutputStream = CloseShieldOutputStream(this)
