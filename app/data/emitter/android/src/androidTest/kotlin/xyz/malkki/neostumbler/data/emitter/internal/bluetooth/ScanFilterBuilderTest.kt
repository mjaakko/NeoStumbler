package xyz.malkki.neostumbler.data.emitter.internal.bluetooth

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import xyz.malkki.neostumbler.beaconparser.BeaconLayout.Companion.parseBeaconLayout

@RunWith(AndroidJUnit4::class)
class ScanFilterBuilderTest {
    @Test
    fun testBuildingScanFilters() {
        val manufacturerIds = listOf(0x1111)

        val layouts =
            listOf(
                    "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25",
                    "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24",
                    "s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19",
                    "m:0-2=990405,i:20-25",
                )
                .map { it.parseBeaconLayout() }

        layouts.forEach { layout ->
            assertTrue(layout.createScanFilters(manufacturerIds = manufacturerIds).isNotEmpty())
        }
    }
}
