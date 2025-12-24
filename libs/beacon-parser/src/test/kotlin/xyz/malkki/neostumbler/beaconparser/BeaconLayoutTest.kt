package xyz.malkki.neostumbler.beaconparser

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.malkki.neostumbler.beaconparser.BeaconLayout.Companion.parseBeaconLayout

class BeaconLayoutTest {
    @Test
    fun `Test parsing AltBeacon layout`() {
        val layout = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25".parseBeaconLayout()

        assertEquals(3, layout.identifiers.size)
        assertEquals(1, layout.datas.size)
        assertEquals(48812, layout.typeCode.typeCode)
        assertEquals(24, layout.power?.startOffset)
        assertEquals(24, layout.power?.endOffset)
    }

    @Test
    fun `Test parsing Eddystone UID layout`() {
        val layout = "s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19".parseBeaconLayout()

        assertEquals(0xfeaaL, layout.serviceUuid?.serviceUuid)
        assertEquals(2, layout.identifiers.size)
        assertEquals(0, layout.typeCode.typeCode)
    }

    @Test
    fun `Test parsing iBeacon layout`() {
        val layout = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24".parseBeaconLayout()

        assertEquals(3, layout.identifiers.size)
        assertEquals(0, layout.datas.size)
        assertEquals(533, layout.typeCode.typeCode)
    }

    @Test
    fun `Test parsing layout with a service UUID`() {
        val serviceUuid = UUID.randomUUID()

        val layout = "s:0-15=${serviceUuid},m:16-16=00,i:17-18".parseBeaconLayout()

        val serviceUuidStr = serviceUuid.toString().replace("-", "")
        assertEquals(
            serviceUuidStr,
            layout.serviceUuid
                ?.serviceUuid128
                // Service UUID is little endian -> reverse
                ?.reversedArray()
                ?.toHexString(),
        )
    }
}
