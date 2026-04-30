package com.autodoc.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            removeDuplicateCarsByPlate(db)
            createUniquePlateIndex(db)
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            removeDuplicateCarsByPlate(db)
            createUniquePlateIndex(db)
        }
    }

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "autodoc_database"
            )
                .addMigrations(
                    MIGRATION_2_3,
                    MIGRATION_3_4
                )
                .build()
                .also { INSTANCE = it }
        }
    }

    private fun removeDuplicateCarsByPlate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            DELETE FROM cars
            WHERE id NOT IN (
                SELECT MIN(id)
                FROM cars
                GROUP BY plate
            )
            """.trimIndent()
        )
    }

    private fun createUniquePlateIndex(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_cars_plate
            ON cars(plate)
            """.trimIndent()
        )
    }
}