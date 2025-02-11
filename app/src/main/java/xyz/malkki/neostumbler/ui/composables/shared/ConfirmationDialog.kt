package xyz.malkki.neostumbler.ui.composables.shared

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

@Composable
fun ConfirmationDialog(
    title: String,
    description: String,
    positiveButtonText: String,
    negativeButtonText: String,
    onPositiveAction: () -> Unit,
    onNegativeAction: () -> Unit,
) {
    AlertDialog(
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        onDismissRequest = onNegativeAction,
        title = { Text(text = title) },
        text = { Text(text = description) },
        confirmButton = {
            TextButton(onClick = onPositiveAction) { Text(text = positiveButtonText) }
        },
        dismissButton = {
            TextButton(onClick = onNegativeAction) { Text(text = negativeButtonText) }
        },
    )
}
