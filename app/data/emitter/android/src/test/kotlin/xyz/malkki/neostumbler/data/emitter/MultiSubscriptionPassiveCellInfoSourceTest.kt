package xyz.malkki.neostumbler.data.emitter

import android.telephony.CellIdentityLte
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellSignalStrengthLte
import android.telephony.ServiceState
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MultiSubscriptionPassiveCellInfoSourceTest {
    private val cellInfo =
        listOf(createCellInfo(mcc = "001", mnc = "01"), createCellInfo(mcc = "001", mnc = "02"))

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

    private fun createTelephonyManager(mcc: String, mnc: String): TelephonyManager {
        val telephonyManager = mock<TelephonyManager>()
        val serviceState = mock<ServiceState> { on { operatorNumeric } doReturn "$mcc$mnc" }

        whenever(telephonyManager.serviceState).thenReturn(serviceState)
        whenever(telephonyManager.allCellInfo).thenReturn(cellInfo)

        return telephonyManager
    }

    @Test
    fun `Duplicate cell towers are filtered`() {
        val telephonyManagerSub1 = createTelephonyManager(mcc = "001", mnc = "01")
        val telephonyManagerSub2 = createTelephonyManager(mcc = "001", mnc = "02")

        val subscription1 = mock<SubscriptionInfo> { on { subscriptionId } doReturn 1 }
        val subscription2 = mock<SubscriptionInfo> { on { subscriptionId } doReturn 1 }

        val subscriptionManager = mock<SubscriptionManager>()
        whenever(subscriptionManager.activeSubscriptionInfoList)
            .doReturn(listOf(subscription1, subscription2))

        val telephonyManager = mock<TelephonyManager>()
        whenever(telephonyManager.createForSubscriptionId(1)).doReturn(telephonyManagerSub1)
        whenever(telephonyManager.createForSubscriptionId(2)).doReturn(telephonyManagerSub2)

        val multiSubscriptionPassiveCellInfoSource =
            MultiSubscriptionPassiveCellInfoSource(subscriptionManager, telephonyManager)

        val cellTowers = multiSubscriptionPassiveCellInfoSource.getCellTowers()

        assertEquals(2, cellTowers.size)
        assertEquals(
            listOf("001" to "01", "001" to "02"),
            cellTowers.map { it.emitter.mobileCountryCode to it.emitter.mobileNetworkCode },
        )
    }
}
