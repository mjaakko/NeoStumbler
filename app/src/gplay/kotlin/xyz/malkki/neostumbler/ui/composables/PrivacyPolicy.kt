package xyz.malkki.neostumbler.ui.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHeader
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.ui.composables.shared.Dialog

@Composable
fun PrivacyPolicyButton() {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    if (showDialog) {
        PrivacyPolicyDialog(onDialogClosed = { showDialog = false })
    }

    Button(onClick = { showDialog = true }) { Text(text = stringResource(R.string.privacy_policy)) }
}

@Composable
fun PrivacyPolicyDialog(onDialogClosed: () -> Unit) {
    val context = LocalContext.current

    val markdownState = rememberMarkdownState {
        withContext(Dispatchers.IO) {
            context.assets.open("privacy_policy.md").use { it.reader().readText() }
        }
    }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDialogClosed, title = stringResource(R.string.privacy_policy)) {
        Markdown(
            modifier = Modifier.verticalScroll(state = scrollState),
            markdownState = markdownState,
            components =
                markdownComponents(
                    paragraph = {
                        MarkdownParagraph(
                            modifier = Modifier.padding(vertical = 4.dp),
                            node = it.node,
                            content = it.content,
                        )
                    },
                    heading1 = {
                        MarkdownHeader(
                            style = MaterialTheme.typography.titleLarge,
                            node = it.node,
                            content = it.content,
                        )
                    },
                    heading2 = {
                        MarkdownHeader(
                            style = MaterialTheme.typography.titleMedium,
                            node = it.node,
                            content = it.content,
                        )
                    },
                ),
        )
    }
}
