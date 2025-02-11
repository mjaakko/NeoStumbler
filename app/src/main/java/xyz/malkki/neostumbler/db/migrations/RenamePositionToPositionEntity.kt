package xyz.malkki.neostumbler.db.migrations

import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec

@RenameTable.Entries(RenameTable(fromTableName = "Position", toTableName = "PositionEntity"))
class RenamePositionToPositionEntity : AutoMigrationSpec
