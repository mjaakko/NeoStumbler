package xyz.malkki.neostumbler.data.restrictedarea

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

@Serializable
internal data class RestrictedAreaSettings(
    val restrictedAreas:
        Map<@Serializable(with = UUIDSerializer::class) UUID, RestrictedAreaGeometry>
) {
    @Serializable
    data class RestrictedAreaGeometry(
        val latitude: Double,
        val longitude: Double,
        /** Radius in meters */
        val radius: Double,
    )
}

internal val Context.restrictedAreasDataStore by
    dataStore("restricted_areas.json", RestrictedAreaSettingsSerializer)

internal object RestrictedAreaSettingsSerializer : Serializer<RestrictedAreaSettings> {
    override val defaultValue = RestrictedAreaSettings(emptyMap())

    override suspend fun readFrom(input: InputStream): RestrictedAreaSettings =
        try {
            Json.decodeFromStream<RestrictedAreaSettings>(input)
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read RestrictedAreaSettings", serialization)
        }

    override suspend fun writeTo(t: RestrictedAreaSettings, output: OutputStream) {
        Json.encodeToStream(t, output)
    }
}

private object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("java.util.UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}
