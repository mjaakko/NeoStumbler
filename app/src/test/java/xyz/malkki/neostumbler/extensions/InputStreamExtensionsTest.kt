package xyz.malkki.neostumbler.extensions

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputStreamExtensionsTest {
    @Test
    fun `Test Gzipped data is detected and read correctly`() {
        val output = ByteArrayOutputStream()

        GZIPOutputStream(output).bufferedWriter().use { it.write("test 123") }

        val input = ByteArrayInputStream(output.toByteArray())

        assertTrue(input.isGzipped())

        // Test reading the stream
        val inputAsText = GZIPInputStream(input).use { it.readBytes().decodeToString() }
        assertEquals("test 123", inputAsText)
    }

    @Test
    fun `Test non-Gzipped data is not detected as Gzipped`() {
        val inputStream = ByteArrayInputStream("test".encodeToByteArray())

        assertFalse(inputStream.isGzipped())
    }
}
