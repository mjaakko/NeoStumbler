package xyz.malkki.neostumbler.data.emitter

import android.Manifest
import android.content.Context
import android.net.wifi.WifiManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import xyz.malkki.neostumbler.core.WifiAccessPoint
import xyz.malkki.neostumbler.data.emitter.mapper.toWifiAccessPoint

class WifiManagerPassiveWifiAccessPointSource(context: Context) : PassiveWifiAccessPointSource {
    private val wifiManager = context.applicationContext.getSystemService<WifiManager>()!!

    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE]
    )
    override fun getWifiAccessPoints(): List<WifiAccessPoint> {
        return wifiManager.scanResults.map { it.toWifiAccessPoint() }
    }
}
