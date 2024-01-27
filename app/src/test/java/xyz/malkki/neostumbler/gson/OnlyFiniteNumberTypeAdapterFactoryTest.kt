package xyz.malkki.neostumbler.gson

import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class OnlyFiniteNumberTypeAdapterFactoryTest {
    private val GSON = GsonBuilder()
        .registerTypeAdapterFactory(OnlyFiniteNumberTypeAdapterFactory())
        .create()

    @Test
    fun `Test writing an object with infinite doubles`() {
        val obj = mapOf("a" to Double.POSITIVE_INFINITY, "b" to Double.NEGATIVE_INFINITY, "c" to 1.23)

        val json = GSON.toJson(obj)

        val objFromJson = GSON.fromJson(json, Map::class.java)

        assertEquals(1, objFromJson.size)
        assertEquals(1.0, 0.01, objFromJson["c"] as Double)
    }
}