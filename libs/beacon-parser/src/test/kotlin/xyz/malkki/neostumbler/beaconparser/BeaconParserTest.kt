package xyz.malkki.neostumbler.beaconparser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.malkki.neostumbler.beaconparser.BeaconLayout.Companion.parseBeaconLayout

class BeaconParserTest {
    @Test
    fun `Test parsing AltBeacon advertisement with no data fields`() {
        val parser =
            BeaconParser("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25".parseBeaconLayout())

        // Beacon data from Android Beacon Library tests
        val beaconData =
            parser.parseScanData(
                "02011a1aff1801beac2f234454cf6d4a0fadf2f4911ba9ffa600010002c5000000"
                    .hexToByteArray()
            )

        assertNotNull(beaconData)
        assertEquals(48812, beaconData?.beaconType)
        assertEquals(3, beaconData?.identifiers?.size)
        assertEquals("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6", beaconData?.identifiers[0].toString())
        assertEquals("1", beaconData?.identifiers[1].toString())
        assertEquals("2", beaconData?.identifiers[2].toString())
    }

    @Test
    fun `Test parsing AltBeacon advertisement`() {
        val parser =
            BeaconParser("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25".parseBeaconLayout())

        val beaconData =
            parser.parseScanData(
                @Suppress("MaxLineLength")
                "1bff1801beac2f234454cf6d4a0fadf2f4911ba9ffa600010002c50000000000000000000000000000000000000000000000000000000000000000000000"
                    .hexToByteArray()
            )

        assertNotNull(beaconData)
        assertEquals(48812, beaconData?.beaconType)
        assertEquals(3, beaconData?.identifiers?.size)
    }

    @Test
    fun `Test parsing Eddystone-UID advertisement`() {
        val parser =
            BeaconParser("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19".parseBeaconLayout())

        val beaconData =
            parser.parseScanData(
                "0201060303aafe1516aafe001000000123456789123456789123456789".hexToByteArray()
            )

        assertNotNull(beaconData)
        assertEquals(0, beaconData!!.beaconType)
        assertEquals(2, beaconData.identifiers.size)
        assertEquals("0x00000123456789123456", beaconData.identifiers[0].toString())
        assertEquals("0x789123456789", beaconData.identifiers[1].toString())
    }

    @Test
    fun `Test parsing iBeacon advertisement`() {
        val parser = BeaconParser("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24".parseBeaconLayout())

        // Beacon data from Wikipedia examples
        val beaconData =
            parser.parseScanData(
                "0201061AFF4C000215FB0B57A2822844CD913A94A122BA120600010002D100".hexToByteArray()
            )

        assertNotNull(beaconData)
        assertEquals(533, beaconData?.beaconType)
        assertEquals(3, beaconData?.identifiers?.size)
        assertEquals("fb0b57a2-8228-44cd-913a-94a122ba1206", beaconData?.identifiers[0].toString())
        assertEquals("1", beaconData?.identifiers[1].toString())
        assertEquals("2", beaconData?.identifiers[2].toString())
    }

    @Test
    fun `Test parsing iBeacon advertisement with zeroes at the end`() {
        val parser = BeaconParser("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24".parseBeaconLayout())

        val beaconData =
            parser.parseScanData(
                @Suppress("MaxLineLength")
                "1aff4c0002152f234454cf6d4a0fadf2f4911ba9ffa60032006fc50000000000000000000000000000000000000000000000000000000000000000000000"
                    .hexToByteArray()
            )

        assertNotNull(beaconData)
        assertEquals(533, beaconData?.beaconType)
        assertEquals(3, beaconData?.identifiers?.size)
        assertEquals("50", beaconData?.identifiers[1].toString())
        assertEquals("111", beaconData?.identifiers[2].toString())
    }

    @Test
    fun `Test parsing RuuviTag data format version 5`() {
        val parser = BeaconParser("m:0-2=990405,i:20-25".parseBeaconLayout())

        val beaconData =
            parser.parseScanData(
                "0201041BFF99040512FC5394C37C0004FFFC040CAC364200CDCBB8334C884F".hexToByteArray()
            )

        assertNotNull(beaconData)
        assertEquals(1, beaconData?.identifiers?.size)
        assertEquals("0xcbb8334c884f", beaconData?.identifiers?.firstOrNull()?.toString())
    }

    @Test
    fun `Test parsing bytes not containing a beacon advertisement`() {
        val parser = BeaconParser("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24".parseBeaconLayout())

        assertNull(parser.parseScanData(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)))
    }
}
