package xyz.malkki.neostumbler.ui.composables.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    dialogProperties: DialogProperties = DialogProperties(),
    title: String,
    primaryActions: @Composable (RowScope.() -> Unit)? = null,
    secondaryActions: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable (() -> Unit),
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest, properties = dialogProperties) {
        Surface(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = AlertDialogDefaults.shape,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(all = 16.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Box(modifier = Modifier.sizeIn(maxHeight = 360.dp)) { content() }

                    if (primaryActions != null || secondaryActions != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row {
                            if (secondaryActions != null) {
                                Row(
                                    modifier = Modifier.wrapContentWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp, Alignment.Start),
                                ) {
                                    secondaryActions()
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            if (primaryActions != null) {
                                Row(
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp, Alignment.End)
                                ) {
                                    primaryActions()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
