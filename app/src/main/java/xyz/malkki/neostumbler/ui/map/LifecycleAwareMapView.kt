package xyz.malkki.neostumbler.ui.map

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Locale
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.SymbolLayer
import xyz.malkki.neostumbler.extensions.defaultLocale

class LifecycleAwareMapView(context: Context) : MapView(context) {
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
    val preferredLanguage =
        if (locale.language == "zh") {
            // Handle different Chinese scripts by preferring the simplified script if it's chosen
            // by the user or if the country is China
            if (locale.script == "Hans" || (locale.script.isEmpty() && locale.country == "CN")) {
                arrayOf(
                    Expression.get("name:zh-Hans"),
                    Expression.get("name:zh-Hant"),
                    Expression.get("name:zh"),
                )
            } else {
                arrayOf(
                    Expression.get("name:zh-Hant"),
                    Expression.get("name:zh-Hans"),
                    Expression.get("name:zh"),
                )
            }
        } else {
            arrayOf(Expression.get("name:${locale.language}"))
        }

    return Expression.format(
        Expression.formatEntry(
            Expression.coalesce(
                *preferredLanguage,
                Expression.get("name:en"),
                // VersaTiles does not seem to support localized labels and uses underscore instead
                // in the expression
                Expression.get("name_en"),
                Expression.get("name"),
            )
        )
    )
}
