package xyz.malkki.neostumbler.ui.composables

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.rememberLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.ui.composables.shared.Dialog

@Composable
private fun LicensesDialog(onClose: () -> Unit) {
    val libraries by rememberLibraries(R.raw.aboutlibraries)

    Dialog(onDismissRequest = onClose, title = stringResource(R.string.third_party_licenses)) {
        LibrariesContainer(
            libraries = libraries,
            textStyles =
                LibraryDefaults.libraryTextStyles(
                    nameTextStyle = MaterialTheme.typography.titleMedium,
                    authorTextStyle = MaterialTheme.typography.bodySmall,
                    versionTextStyle = MaterialTheme.typography.labelSmall,
                    licensesTextStyle = MaterialTheme.typography.labelSmall,
                ),
        )
    }
}

@Composable
fun LicensesButton() {
    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    if (dialogOpen) {
        LicensesDialog(onClose = { dialogOpen = false })
    }

    Button(onClick = { dialogOpen = true }) {
        Text(text = stringResource(R.string.third_party_licenses))
    }
}
