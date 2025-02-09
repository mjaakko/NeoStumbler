package xyz.malkki.neostumbler.ui.composables

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import xyz.malkki.neostumbler.BuildConfig
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.extensions.defaultLocale

@Composable
fun AboutNeoStumbler() {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    val showDialog = rememberSaveable { mutableStateOf(false) }

    if (showDialog.value) {
        BasicAlertDialog(onDismissRequest = { showDialog.value = false }) {
            Surface(
                modifier = Modifier.sizeIn(maxWidth = 400.dp).fillMaxWidth().wrapContentHeight(),
                shape = AlertDialogDefaults.shape,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(modifier = Modifier.padding(all = 24.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Column(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
                        Text(
                            text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.app_variant, BuildConfig.FLAVOR),
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = getAuthorsText(),
                            fontStyle = FontStyle.Italic,
                            style = MaterialTheme.typography.bodySmall,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = { openUrl(getBugReportUrl()) }) {
                            Text(text = stringResource(R.string.bug_report_button))
                        }

                        Button(onClick = { openUrl(getTranslationsUrl(context)) }) {
                            Text(text = stringResource(R.string.update_translations_button))
                        }
                    }
                }
            }
        }
    }

    Text(
        modifier =
            Modifier.fillMaxWidth().clickable(onClick = { showDialog.value = true }).padding(8.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium,
        text = stringResource(R.string.about_app),
    )
}

@Composable
private fun getAuthorsText(): AnnotatedString {
    val author = stringResource(R.string.author)
    val contributors = stringResource(R.string.contributors)

    val raw = stringResource(R.string.author_text, author, contributors)

    return buildAnnotatedString {
        append(raw)

        addStyle(
            style = SpanStyle(fontWeight = FontWeight.SemiBold),
            start = raw.indexOf(author),
            end = raw.indexOf(author) + author.length,
        )

        addLink(
            url = LinkAnnotation.Url("https://github.com/mjaakko/NeoStumbler/graphs/contributors"),
            start = raw.indexOf(contributors),
            end = raw.indexOf(contributors) + contributors.length,
        )
        addStyle(
            style = SpanStyle(color = MaterialTheme.colorScheme.primary),
            start = raw.indexOf(contributors),
            end = raw.indexOf(contributors) + contributors.length,
        )
    }
}

private fun String.urlEncode(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8.name())
}

private fun getBugReportUrl(): String {
    return buildString {
        val device =
            "${Build.BRAND.takeIf { !it.isNullOrBlank() }?.replaceFirstChar { it.uppercaseChar() } ?: Build.MANUFACTURER} ${Build.MODEL}"

        append(
            "https://github.com/mjaakko/NeoStumbler/issues/new?labels=bug&template=1-bug_report.yml"
        )
        append("&version=${BuildConfig.VERSION_NAME.urlEncode()}")
        append("&variant=${BuildConfig.FLAVOR.urlEncode()}")
        append("&android-version=${Build.VERSION.RELEASE.urlEncode()}")
        append("&device=${device.urlEncode()}")
    }
}

private fun getTranslationsUrl(context: Context): String {
    return "https://hosted.weblate.org/projects/neostumbler/-/${context.defaultLocale.language}/"
}
