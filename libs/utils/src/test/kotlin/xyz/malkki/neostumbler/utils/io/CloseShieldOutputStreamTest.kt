package xyz.malkki.neostumbler.utils.io

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CloseShieldOutputStreamTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `Closing CloseShieldOutputStream does not close the underlying stream`() {
        val file = temporaryFolder.newFile("test.txt")

        val outputStream = file.outputStream()

        val closeShielded = outputStream.closeShielded()
        closeShielded.write("test".encodeToByteArray())
        closeShielded.close()

        outputStream.write("test".encodeToByteArray())
        outputStream.close()

        assertEquals("testtest", file.readText())
    }
}
