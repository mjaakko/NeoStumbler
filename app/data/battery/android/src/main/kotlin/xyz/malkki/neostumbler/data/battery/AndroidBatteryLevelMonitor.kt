package xyz.malkki.neostumbler.data.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.broadcastreceiverflow.broadcastReceiverFlow

class AndroidBatteryLevelMonitor(private val context: Context) : BatteryLevelMonitor {
    override fun getBatteryLevelFlow(): Flow<Float> {
        return context.broadcastReceiverFlow(IntentFilter(Intent.ACTION_BATTERY_CHANGED)).map {
            intent ->
            intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0).toFloat() /
                intent.getIntExtra(BatteryManager.EXTRA_SCALE, 1)
        }
    }
}
