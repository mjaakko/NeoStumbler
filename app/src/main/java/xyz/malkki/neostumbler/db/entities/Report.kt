package xyz.malkki.neostumbler.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity()
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    @ColumnInfo(index = true) val timestamp: Instant,
    @ColumnInfo(index = true) val uploaded: Boolean,
    val uploadTimestamp: Instant?
)
