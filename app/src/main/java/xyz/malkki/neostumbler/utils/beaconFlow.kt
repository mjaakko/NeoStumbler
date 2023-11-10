package xyz.malkki.neostumbler.utils

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region

fun getBeaconFlow(context: Context): Flow<List<Beacon>> = callbackFlow {
    val beaconManager = BeaconManager.getInstanceForApplication(context)

    val rangeNotifier: (Collection<Beacon>, Region) -> Unit = { beacons: Collection<Beacon>, _: Region ->
        trySendBlocking(beacons.toList())
    }

    beaconManager.addRangeNotifier(rangeNotifier)

    val region = Region("all_beacons", null, null, null)

    beaconManager.startRangingBeacons(region)

    awaitClose {
        beaconManager.stopRangingBeacons(region)

        beaconManager.removeRangeNotifier(rangeNotifier)
    }
}