package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.enums.enumEntries
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.ichnaeaupload.AutoUploadMode
import xyz.malkki.neostumbler.ichnaeaupload.IchnaeaAutoUploadToggler

private val AUTO_UPLOAD_LABELS: Map<AutoUploadMode, Int> =
    mapOf(
        AutoUploadMode.NEVER to R.string.send_reports_automatically_never,
        AutoUploadMode.ANY_NETWORK to R.string.send_reports_automatically_on_any_network,
        AutoUploadMode.UNMETERED_NETWORK to R.string.send_reports_automatically_on_unmetered_network,
    )

@Composable
fun AutoUploadToggle(ichnaeaAutoUploadToggler: IchnaeaAutoUploadToggler = koinInject()) {
    val context = LocalContext.current

    val autoUploadMode by
        ichnaeaAutoUploadToggler
            .getAutoUploadMode()
            .collectAsStateWithLifecycle(initialValue = null)

    MultiChoiceSettings(
        title = stringResource(R.string.send_reports_automatically_title),
        options = enumEntries<AutoUploadMode>(),
        selectedOption = autoUploadMode,
        titleProvider = {
            AUTO_UPLOAD_LABELS[autoUploadMode]?.let { ContextCompat.getString(context, it) } ?: ""
        },
        onValueSelected = { autoUploadMode ->
            if (autoUploadMode != null) {
                ichnaeaAutoUploadToggler.setAutoUploadMode(autoUploadMode)
            }
        },
    )
}
