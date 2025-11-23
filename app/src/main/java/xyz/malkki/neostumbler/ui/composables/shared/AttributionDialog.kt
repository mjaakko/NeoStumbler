package xyz.malkki.neostumbler.ui.composables.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.maplibre.android.attribution.Attribution
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.utils.openUrl

@Composable
fun AttributionDialog(attributions: List<Attribution>, onDialogClose: () -> Unit) {
    val context = LocalContext.current

    Dialog(
        title = stringResource(id = R.string.map_data_sources),
        onDismissRequest = onDialogClose,
    ) {
        Column {
            attributions.forEach { attribution ->
                Box(
                    modifier =
                        Modifier.height(48.dp)
                            .fillMaxWidth()
                            .clickable { context.startActivity(openUrl(attribution.url)) }
                            .padding(8.dp)
                ) {
                    Text(modifier = Modifier.align(Alignment.CenterStart), text = attribution.title)
                }
            }
        }
    }
}
