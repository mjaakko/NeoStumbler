package xyz.malkki.neostumbler.data.emitter

import android.telephony.CellIdentityLte
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellSignalStrengthLte
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TelephonyManagerPassiveCellInfoSourceTest {
    private fun createCellInfo(mcc: String, mnc: String): CellInfo {
        val cellIdentity =
            mock<CellIdentityLte> {
                on { mccString } doReturn mcc
                on { mncString } doReturn mnc
                on { ci } doReturn 10000
                on { tac } doReturn 1000
                on { pci } doReturn CellInfo.UNAVAILABLE
                on { earfcn } doReturn CellInfo.UNAVAILABLE
            }

        val cellSignalStrength =
            mock<CellSignalStrengthLte> {
                on { asuLevel } doReturn CellInfo.UNAVAILABLE
                on { timingAdvance } doReturn CellInfo.UNAVAILABLE
                on { rssi } doReturn CellInfo.UNAVAILABLE
            }

        val cellInfo = mock<CellInfoLte>()
        whenever(cellInfo.cellIdentity).thenReturn(cellIdentity)
        whenever(cellInfo.cellSignalStrength).thenReturn(cellSignalStrength)
        whenever(cellInfo.cellConnectionStatus).thenReturn(CellInfo.CONNECTION_UNKNOWN)

        return cellInfo
    }

    @Test
    fun `Cell towers for other subscriptions are filtered`() {
        val cellInfo =
            listOf(createCellInfo(mcc = "001", mnc = "01"), createCellInfo(mcc = "001", mnc = "02"))
        val serviceState = mock<ServiceState> { on { operatorNumeric } doReturn "00101" }

        val telephonyManager = mock<TelephonyManager>()
        whenever(telephonyManager.serviceState).thenReturn(serviceState)
        whenever(telephonyManager.allCellInfo).thenReturn(cellInfo)

        val passiveCellInfoSource = TelephonyManagerPassiveCellInfoSource(telephonyManager)

        val cellTowers = passiveCellInfoSource.getCellTowers()
        assertEquals(1, cellTowers.size)
        assertEquals("001", cellTowers.first().emitter.mobileCountryCode)
        assertEquals("01", cellTowers.first().emitter.mobileNetworkCode)
    }
}
