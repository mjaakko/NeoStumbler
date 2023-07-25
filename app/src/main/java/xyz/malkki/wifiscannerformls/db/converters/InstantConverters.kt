package xyz.malkki.wifiscannerformls.db.converters

import androidx.room.TypeConverter
import java.time.Instant

class InstantConverters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}