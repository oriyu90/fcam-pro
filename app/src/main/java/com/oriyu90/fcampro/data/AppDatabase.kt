package com.oriyu90.fcampro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CameraProfile::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cameraProfileDao(): CameraProfileDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** v1 -> v2: nullable-safe add of the exposure-compensation column. */
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE camera_profiles " +
                            "ADD COLUMN exposureCompensation INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }

        /** v2 -> v3: profile color and dock order; all existing rows remain valid. */
        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE camera_profiles " +
                            "ADD COLUMN colorArgb INTEGER NOT NULL DEFAULT -24822"
                    )
                    db.execSQL(
                        "ALTER TABLE camera_profiles " +
                            "ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }

        fun get(context: Context): AppDatabase =
            instance
                ?: synchronized(this) {
                    instance
                        ?: Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "camera-profiles.db",
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            // Profiles are non-critical convenience data. If a future
                            // schema bump ships without a migration, recreate the table
                            // instead of crashing on upgrade.
                            .fallbackToDestructiveMigration(dropAllTables = true)
                            .build()
                            .also { instance = it }
                }
    }
}
