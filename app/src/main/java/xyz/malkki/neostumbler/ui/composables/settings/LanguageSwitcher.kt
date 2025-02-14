package xyz.malkki.neostumbler.ui.composables.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale
import xyz.malkki.neostumbler.BuildConfig
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.extensions.defaultLocale
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

private fun getCurrentLocale(): Locale? {
    if (AppCompatDelegate.getApplicationLocales().isEmpty) {
        // Per-app language was not set, use system default language
        return null
    }

    return AppCompatDelegate.getApplicationLocales()[0]!!
}

@Composable
fun LanguageSwitcher() {
    val context = LocalContext.current

    val locales = remember {
        listOf(null) + SUPPORTED_LOCALES_BCP47.map { Locale.forLanguageTag(it) }
    }

    val selectedLanguage = getCurrentLocale()

    MultiChoiceSettings(
        title = stringResource(id = R.string.app_language),
        options = locales,
        selectedOption = selectedLanguage,
        titleProvider = {
            it?.getDisplayName(selectedLanguage ?: context.defaultLocale)
                ?: ContextCompat.getString(context, R.string.system_default_language)
        },
        descriptionProvider = { it?.getDisplayName(it) },
        onValueSelected = { newLocale ->
            if (newLocale != null) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(newLocale))
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }

            // Update QS tile so that it will use the selected language
            ScannerTileService.updateTile(context)
        },
    )
}
