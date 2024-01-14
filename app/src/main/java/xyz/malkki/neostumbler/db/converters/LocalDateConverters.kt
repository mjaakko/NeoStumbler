package xyz.malkki.neostumbler.db.converters

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateConverters {
    @TypeConverter
    fun fromInstant(value: LocalDate?): String? = value?.format(DateTimeFormatter.ISO_LOCAL_DATE)

    @TypeConverter
    fun toInstant(value: String?): LocalDate? = value?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }
}