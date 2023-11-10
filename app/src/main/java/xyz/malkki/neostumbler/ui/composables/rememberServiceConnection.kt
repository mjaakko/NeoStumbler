package xyz.malkki.neostumbler.ui.composables

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
inline fun <reified BoundService : Service, reified BoundServiceBinder : Binder> rememberServiceConnection(
    crossinline getService: @DisallowComposableCalls BoundServiceBinder.() -> BoundService,
): MutableState<BoundService?> {
    val context: Context = LocalContext.current

    val boundService = remember(context) { mutableStateOf<BoundService?>(null) }
    val serviceConnection: ServiceConnection = remember(context) {
        object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
                boundService.value = (service as BoundServiceBinder).getService()
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                boundService.value = null
            }
        }
    }

    DisposableEffect(context, serviceConnection) {
        context.bindService(Intent(context, BoundService::class.java), serviceConnection, 0)

        onDispose {
            context.unbindService(serviceConnection)
        }
    }

    return boundService
}