package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun <O> MultiChoiceSettings(
    title: String,
    options: Collection<O>,
    selectedOption: O,
    disabledOptions: Set<O> = emptySet(),
    titleProvider: (O) -> String,
    descriptionProvider: ((O) -> String)? = null,
    onValueSelected: suspend (O) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val dialogOpen = rememberSaveable { mutableStateOf(false) }

    if (dialogOpen.value) {
        MultiChoiceSettingsDialog(
            title = title,
            options = options,
            selectedOption = selectedOption,
            disabledOptions = disabledOptions,
            titleProvider = titleProvider,
            descriptionProvider = descriptionProvider,
            onValueSelected = {
                coroutineScope.launch {
                    onValueSelected(it)

                    dialogOpen.value = false
                }
            },
        )
    }

    SettingsItem(
        title = title,
        description = titleProvider.invoke(selectedOption),
        onClick = { dialogOpen.value = true },
    )
}

// Disabled -> alpha 0.38f
// https://developer.android.com/develop/ui/compose/designsystems/material2-material3#emphasis-and
private const val ALPHA_DISABLED = 0.38f

@Composable
private fun <O> MultiChoiceSettingsDialog(
    title: String,
    options: Collection<O>,
    selectedOption: O,
    disabledOptions: Set<O> = emptySet(),
    titleProvider: (O) -> String,
    descriptionProvider: ((O) -> String)? = null,
    onValueSelected: (O) -> Unit,
) {
    BasicAlertDialog(onDismissRequest = { onValueSelected(selectedOption) }) {
        Surface(
            modifier = Modifier.wrapContentWidth().wrapContentHeight().sizeIn(maxHeight = 380.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(style = MaterialTheme.typography.titleLarge, text = title)

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier =
                        Modifier.selectableGroup()
                            .padding(bottom = 8.dp)
                            .verticalScroll(state = rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEach { option ->
                        val enabled = option !in disabledOptions

                        Row(
                            Modifier.fillMaxWidth()
                                .wrapContentHeight()
                                .defaultMinSize(minHeight = 36.dp)
                                .selectable(
                                    enabled = enabled,
                                    selected = option == selectedOption,
                                    onClick = { onValueSelected(option) },
                                    role = Role.RadioButton,
                                )
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                modifier = Modifier.align(Alignment.Top).padding(top = 4.dp),
                                enabled = enabled,
                                selected = option == selectedOption,
                                onClick = null,
                            )

                            val description = descriptionProvider?.invoke(option)

                            Column(
                                modifier =
                                    Modifier.alpha(
                                            if (enabled) {
                                                1f
                                            } else {
                                                ALPHA_DISABLED
                                            }
                                        )
                                        .align(
                                            if (description != null) {
                                                Alignment.Top
                                            } else {
                                                Alignment.CenterVertically
                                            }
                                        )
                                        .padding(start = 16.dp)
                            ) {
                                Text(
                                    text = titleProvider.invoke(option),
                                    style = MaterialTheme.typography.bodyMedium.merge(),
                                )

                                if (description != null) {
                                    Text(
                                        text = description,
                                        fontWeight = FontWeight.Light,
                                        style = MaterialTheme.typography.bodySmall.merge(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
