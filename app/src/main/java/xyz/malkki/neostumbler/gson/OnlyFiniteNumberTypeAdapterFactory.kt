package xyz.malkki.neostumbler.gson

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * GSON type adapter factory which writes non-finite (i.e. NaN and infinite) numbers as null
 */
class OnlyFiniteNumberTypeAdapterFactory : TypeAdapterFactory {
    companion object {
        private val DOUBLE_TYPE = TypeToken.get(Double::class.java)
        private val FLOAT_TYPE = TypeToken.get(Float::class.java)
    }

    override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val delegate = when (type.rawType) {
            Double::class.javaObjectType -> gson.getDelegateAdapter(this, DOUBLE_TYPE)
            Float::class.javaObjectType -> gson.getDelegateAdapter(this, FLOAT_TYPE)
            else -> null
        }

        return delegate?.let { typeAdapter ->
            FiniteNumberTypeAdapter(typeAdapter).nullSafe() as TypeAdapter<T>
        }
    }

    private class FiniteNumberTypeAdapter<T : Number>(private val delegate: TypeAdapter<T>) : TypeAdapter<T>() {
        override fun write(out: JsonWriter, value: T) {
            if (value.isFiniteCompat()) {
                delegate.write(out, value)
            } else {
                out.nullValue()
            }
        }

        override fun read(`in`: JsonReader): T {
            return delegate.read(`in`)
        }
    }
}

private fun Number.isFiniteCompat(): Boolean {
    return when (this) {
        is Float -> this.isFinite()
        is Double -> this.isFinite()
        else -> true
    }
}