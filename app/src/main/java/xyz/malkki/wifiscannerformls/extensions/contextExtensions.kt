package xyz.malkki.wifiscannerformls.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Checks which permissions the application is missing
 *
 * @return List of missing permissions
 */
fun Context.checkMissingPermissions(vararg permissions: String): List<String> = permissions.filter { checkSelfPermission(it) == PackageManager.PERMISSION_DENIED }

/**
 * Returns activity of the context or null when the context is not an activity
 *
 * @return Activity or null
 */
fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

val Context.localeList: LocaleListCompat
    get() = LocaleListCompat.wrap(resources.configuration.locales)

val Context.defaultLocale: Locale
    get() = localeList[0]!!
