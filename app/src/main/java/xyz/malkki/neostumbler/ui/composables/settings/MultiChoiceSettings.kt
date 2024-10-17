package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun <O> MultiChoiceSettings(
    title: String,
    options: Collection<O>,
    selectedOption: O,
    disabledOptions: Set<O> = emptySet(),
    titleProvider: (O) -> String,
    descriptionProvider: ((O) -> String)? = null,
    onValueSelected: suspend (O) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val dialogOpen = remember { mutableStateOf(false) }

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
            })
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable {
                dialogOpen.value = true
            }
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Text(text = title)
            Text(
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                text = titleProvider.invoke(selectedOption)
            )
        }
    }
}

@Composable
private fun <O> MultiChoiceSettingsDialog(
    title: String,
    options: Collection<O>,
    selectedOption: O,
    disabledOptions: Set<O>,
    titleProvider: (O) -> String,
    descriptionProvider: ((O) -> String)?,
    onValueSelected: (O) -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = { onValueSelected(selectedOption) }
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    style = MaterialTheme.typography.titleLarge,
                    text = title,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { option ->
                        val enabled = option !in disabledOptions

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .defaultMinSize(minHeight = 36.dp)
                                .selectable(
                                    enabled = enabled,
                                    selected = option == selectedOption,
                                    onClick = { onValueSelected(option) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                modifier = Modifier
                                    .align(Alignment.Top)
                                    .padding(top = 4.dp),
                                enabled = enabled,
                                selected = option == selectedOption,
                                onClick = null
                            )

                            val description = descriptionProvider?.invoke(option)

                            Column(
                                modifier = Modifier
                                    //Disabled -> alpha 0.38f https://developer.android.com/develop/ui/compose/designsystems/material2-material3#emphasis-and
                                    .alpha(
                                        if (enabled) {
                                            1f
                                        } else {
                                            0.38f
                                        }
                                    )
                                    .align(if (description != null) { Alignment.Top } else { Alignment.CenterVertically })
                                    .padding(start = 16.dp)
                            ) {
                                Text(
                                    text = titleProvider.invoke(option),
                                    style = MaterialTheme.typography.bodyMedium.merge()
                                )

                                if (description != null) {
                                    Text(
                                        text = description,
                                        fontWeight = FontWeight.Light,
                                        style = MaterialTheme.typography.bodySmall.merge()
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