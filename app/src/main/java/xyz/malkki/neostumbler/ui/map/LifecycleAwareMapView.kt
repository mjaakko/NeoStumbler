package xyz.malkki.neostumbler.ui.map

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Locale
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.SymbolLayer
import xyz.malkki.neostumbler.extensions.defaultLocale

class LifecycleAwareMapView(context: Context) : MapView(context) {
    val componentCallback: ComponentCallbacks2 =
        object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                @Suppress("DEPRECATION")
                // Android 14+ never notifies of this level, but we can ignore the deprecation,
                // because we are doing an inequality comparison
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                    this@LifecycleAwareMapView.onLowMemory()
                }
            }

            override fun onConfigurationChanged(newConfig: Configuration) {}

            override fun onLowMemory() {}
        }

    var lifecycle: Lifecycle? = null
        set(value) {
            if (value != field) {
                field?.removeObserver(observer)
                value?.addObserver(observer)
                field = value

                // When the lifecycle is set to null, the map view is removed from the Compose
                // tree -> call onDestroy to release any resources
                if (value == null) {
                    onDestroy()
                }
            }
        }

    private val observer = LifecycleEventObserver { source, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                onResume()
            }
            Lifecycle.Event.ON_PAUSE -> {
                onPause()
            }
            Lifecycle.Event.ON_START -> {
                onStart()
            }
            Lifecycle.Event.ON_STOP -> {
                onStop()
            }
            Lifecycle.Event.ON_DESTROY -> {
                onDestroy()
            }
            else -> {}
        }
    }

    fun localizeLabelNames() {
        addOnDidFinishLoadingStyleListener {
            getMapAsync { map ->
                map.getStyle { style ->
                    style.layers.forEach { layer ->
                        if (layer is SymbolLayer) {
                            if (
                                layer.textField.isExpression &&
                                    layer.textField.expression?.toString()?.contains("name") == true
                            ) {
                                layer.setProperties(
                                    textField(getLocalizedLabelExpression(context.defaultLocale))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getLocalizedLabelExpression(locale: Locale): Expression {
    val preferredLanguages: MutableList<String> =
        if (locale.language == "zh") {
            /**
             * Handle different Chinese scripts by preferring the simplified script in any of the
             * following cases:
             * * it's chosen by the user
             * * if the country is China and the user has not selected the traditional script
             * * if there's no preferred script and no specific country variant (Android OS uses
             *   simplified script in this case, see
             *   https://github.com/mjaakko/NeoStumbler/issues/790)
             */
            @Suppress("ComplexCondition")
            if (
                locale.script == "Hans" ||
                    (locale.country == "CN" && locale.script != "Hant") ||
                    (locale.country.isEmpty() && locale.script.isEmpty())
            ) {
                mutableListOf("name:zh-Hans", "name:zh-Hant", "name:zh")
            } else {
                mutableListOf("name:zh-Hant", "name:zh-Hans", "name:zh")
            }
        } else {
            mutableListOf("name:${locale.language}")
        }

    preferredLanguages +=
        listOf(
            // Fallback to English if the preferred language is not supported
            "name:en",
            // VersaTiles does not seem to support localized labels and uses underscore instead
            // in the expression
            "name_en",
            "name",
        )

    fun createExpressionForLanguage(languages: List<String>): Expression {
        if (languages.isEmpty()) {
            return Expression.get("name")
        }

        val language = languages.first()

        return Expression.switchCase(
            Expression.all(
                // has -> get rid of nulls
                Expression.has(language),
                // neq -> get rid of empty strings
                Expression.neq(Expression.get(language), ""),
            ),
            Expression.get(language),
            createExpressionForLanguage(languages.subList(1, languages.size)),
        )
    }

    return Expression.format(
        Expression.formatEntry(createExpressionForLanguage(preferredLanguages.distinct()))
    )
}
