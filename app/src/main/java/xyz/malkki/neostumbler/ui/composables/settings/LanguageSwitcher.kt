package xyz.malkki.neostumbler.ui.composables.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale
import xyz.malkki.neostumbler.BuildConfig
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.scanner.quicksettings.ScannerTileService

// Place English first, otherwise sort by language code
private val ENGLISH_FIRST_COMPARATOR: Comparator<String> =
    Comparator<String> { a, b ->
        if (a == "en") {
            -1
        } else if (b == "en") {
            1
        } else {
            a.compareTo(b)
        }
    }

private val SUPPORTED_LOCALES_BCP47 =
    BuildConfig.SUPPORTED_LOCALES.split(',')
        .map { languageTag ->
            val subtags = languageTag.split('-')

            if (subtags.size == 1) {
                subtags.first()
            } else {
                "${subtags[0]}-${subtags[1].replaceFirst("r", "")}"
            }
        }
        .sortedWith(ENGLISH_FIRST_COMPARATOR)
        .toTypedArray()

private fun getCurrentLocale(context: Context): Locale {
    val locale =
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            // Per-app language was not set, use system language
            LocaleManagerCompat.getSystemLocales(context).getFirstMatch(SUPPORTED_LOCALES_BCP47)!!
        } else {
            AppCompatDelegate.getApplicationLocales()[0]!!
        }

    return if ("${locale.language}-${locale.country}" in SUPPORTED_LOCALES_BCP47) {
        locale
    } else {
        // If the user chooses an unsupported country-variant in the system settings, just use the
        // language tag
        Locale(locale.language)
    }
}

@Composable
fun LanguageSwitcher() {
    val context = LocalContext.current

    val localeList = remember {
        LocaleListCompat.forLanguageTags(SUPPORTED_LOCALES_BCP47.joinToString(","))
    }
    val locales = remember(localeList) { (0 until localeList.size()).map { localeList.get(it)!! } }

    val selectedLanguage = getCurrentLocale(context)

    MultiChoiceSettings(
        title = stringResource(id = R.string.app_language),
        options = locales,
        selectedOption = selectedLanguage,
        titleProvider = { it.getDisplayName(selectedLanguage) },
        descriptionProvider = { it.getDisplayName(it) },
        onValueSelected = { newLocale ->
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(newLocale))

            // Update QS tile so that it will use the selected language
            ScannerTileService.updateTile(context)
        },
    )
}
