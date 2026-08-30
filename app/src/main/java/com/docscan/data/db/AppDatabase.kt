package com.docscan.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.docscan.data.model.DocumentEntity
import com.docscan.data.model.PageEntity
import com.docscan.data.model.QrHistoryEntity

@Database(
    entities = [DocumentEntity::class, PageEntity::class, QrHistoryEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao
    abstract fun qrHistoryDao(): QrHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure all new columns exist safely without data loss
                try {
                    db.execSQL("ALTER TABLE documents ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Migration 1_2 step failed: ${e.message}", e)
                }
                try {
                    db.execSQL("ALTER TABLE documents ADD COLUMN isStarred INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Migration 1_2 step failed: ${e.message}", e)
                }
                try {
                    db.execSQL("ALTER TABLE pages ADD COLUMN watermarkText TEXT")
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Migration 1_2 step failed: ${e.message}", e)
                }
                try {
                    db.execSQL("ALTER TABLE pages ADD COLUMN watermarkOpacity REAL NOT NULL DEFAULT 0.35")
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Migration 1_2 step failed: ${e.message}", e)
                }
                try {
                    db.execSQL("ALTER TABLE pages ADD COLUMN watermarkColor INTEGER NOT NULL DEFAULT -7829368")
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Migration 1_2 step failed: ${e.message}", e)
                }
                try {
                    db.execSQL("ALTER TABLE pages ADD COLUMN signatureImagePath TEXT")
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Migration 1_2 step failed: ${e.message}", e)
                }
                try {
                    db.execSQL("ALTER TABLE pages ADD COLUMN notes TEXT")
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Migration 1_2 step failed: ${e.message}", e)
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS qr_scan_history (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            rawValue TEXT NOT NULL,
                            displayValue TEXT NOT NULL,
                            format INTEGER NOT NULL,
                            formatName TEXT NOT NULL,
                            valueType TEXT NOT NULL,
                            title TEXT NOT NULL,
                            subtitle TEXT NOT NULL DEFAULT '',
                            timestamp INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Migration 2_3 step failed: ${e.message}", e)
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Lets a document remember its own exported Word/Excel file, so the app's
                // built-in Word/Excel readers can reopen it later — like CamScanner's readers.
                try {
                    db.execSQL("ALTER TABLE documents ADD COLUMN wordFilePath TEXT")
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Migration 3_4 step failed: ${e.message}", e)
                }
                try {
                    db.execSQL("ALTER TABLE documents ADD COLUMN excelFilePath TEXT")
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Migration 3_4 step failed: ${e.message}", e)
                }
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "doc_scanner.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                 .fallbackToDestructiveMigrationOnDowngrade()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
