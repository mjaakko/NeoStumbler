package xyz.malkki.neostumbler.data.emitter

import android.bluetooth.BluetoothDevice
import org.altbeacon.beacon.BeaconParser as BeaconLibraryParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import xyz.malkki.neostumbler.beaconparser.BeaconLayout.Companion.parseBeaconLayout
import xyz.malkki.neostumbler.beaconparser.BeaconParser

class BeaconParserTest {
    private fun createParsers(layout: String): Pair<BeaconLibraryParser, BeaconParser> {
        val beaconLibraryParser = BeaconLibraryParser().apply { setBeaconLayout(layout) }

        val beaconParser = BeaconParser(layout.parseBeaconLayout())

        return beaconLibraryParser to beaconParser
    }

    private fun testParseResults(
        parsers: Pair<BeaconLibraryParser, BeaconParser>,
        scanData: ByteArray,
    ) {
        val device = mock<BluetoothDevice> { on { address } doReturn "ab:cd:01:01:01:01" }

        val beaconLibraryBeacon = parsers.first.fromScanData(scanData, -80, device, 0)

        val beacon = parsers.second.parseScanData(scanData)

        assertNotNull(beaconLibraryBeacon)
        assertNotNull(beacon)

        assertEquals(beaconLibraryBeacon.identifiers.size, beacon!!.identifiers.size)
        repeat(beaconLibraryBeacon.identifiers.size) { i ->
            assertEquals(
                beaconLibraryBeacon.identifiers[i].toString(),
                beacon.identifiers[i].toString(),
            )
        }

        assertEquals(beaconLibraryBeacon.beaconTypeCode, beacon.beaconType)

        assertEquals(beaconLibraryBeacon.dataFields.size, beacon.dataFields.size)
        repeat(beaconLibraryBeacon.dataFields.size) { i ->
            assertEquals(beaconLibraryBeacon.dataFields[i], beacon.dataFields[i])
        }
    }

    @Test
    fun `Compare iBeacon parse results`() {
        val parsers = createParsers("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")

        val scanData =
            "0201061AFF4C000215FB0B57A2822844CD913A94A122BA120600010002D100".hexToByteArray()

        testParseResults(parsers, scanData)
    }

    @Test
    fun `Compare AltBeacon parse results`() {
        val parsers = createParsers("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-254")

        val scanData =
            @Suppress("MaxLineLength")
            "1bff1801beac2f234454cf6d4a0fadf2f4911ba9ffa600010002c50000000000000000000000000000000000000000000000000000000000000000000000"
                .hexToByteArray()

        testParseResults(parsers, scanData)
    }

    @Test
    fun `Compare Eddystone-UID parse results`() {
        val parsers = createParsers("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19")

        val scanData = "0201060303aafe1516aafe001000000123456789123456789123456789".hexToByteArray()

        testParseResults(parsers, scanData)
    }

    @Test
    fun `Compare RuuviTag parse results`() {
        val parsers = createParsers("m:0-2=990405,i:20-25")

        val scanData =
            "0201041BFF99040512FC5394C37C0004FFFC040CAC364200CDCBB8334C884F".hexToByteArray()

        testParseResults(parsers, scanData)
    }
}
