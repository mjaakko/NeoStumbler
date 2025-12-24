package xyz.malkki.neostumbler.core.emitter

import org.junit.Assert.assertEquals
import org.junit.Test

class WifiAccessPointTest {
    @Test
    fun `Test mapping radio types to IEEE802 standards`() {
        WifiAccessPoint.RadioType.entries.forEach { radioType ->
            assertEquals(
                radioType,
                WifiAccessPoint.RadioType.from802String(radioType.to802String()),
            )
        }
    }
}
