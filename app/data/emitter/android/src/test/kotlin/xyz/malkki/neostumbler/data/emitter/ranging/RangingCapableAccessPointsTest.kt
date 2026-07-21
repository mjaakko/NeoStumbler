package xyz.malkki.neostumbler.data.emitter.ranging

import android.net.wifi.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class RangingCapableAccessPointsTest {
    @Test
    fun `Access points supporting 2-way ranging are prioritized`() {
        val ap1 = mock<ScanResult> { on { is80211mcResponder } doReturn false }
        val ap2 =
            mock<ScanResult> {
                on { is80211mcResponder } doReturn true
                on { is80211azNtbResponder } doReturn false
            }
        val ap3 =
            mock<ScanResult> {
                on { is80211mcResponder } doReturn true
                on { is80211azNtbResponder } doReturn true
            }
        val ap4 = mock<ScanResult> { on { is80211mcResponder } doReturn false }

        val scanResults = listOf(ap1, ap2, ap3, ap4)

        assertEquals(
            listOf(ap3, ap2, ap1, ap4),
            scanResults.getRttRangingCapableAccessPoints(
                maxResults = scanResults.size,
                isAtLeastAndroid12 = true,
            ),
        )
    }

    @Test
    fun `Access points with highest signal strength are prioritized`() {
        val ap1 = mock<ScanResult> { on { is80211mcResponder } doReturn false }
        ap1.level = -80
        val ap2 = mock<ScanResult> { on { is80211mcResponder } doReturn false }
        ap2.level = -60
        val ap3 = mock<ScanResult> { on { is80211mcResponder } doReturn false }
        ap3.level = -70

        val scanResults = listOf(ap1, ap2, ap3)

        assertEquals(
            listOf(ap2, ap3, ap1),
            scanResults.getRttRangingCapableAccessPoints(
                maxResults = scanResults.size,
                isAtLeastAndroid12 = true,
            ),
        )
    }
}
