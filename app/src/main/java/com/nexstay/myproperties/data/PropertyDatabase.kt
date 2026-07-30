package com.nexstay.myproperties.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Property::class, PropertyMedia::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PropertyDatabase : RoomDatabase() {

    abstract fun propertyDao(): PropertyDao
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: PropertyDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `property_media` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`propertyId` INTEGER NOT NULL, " +
                        "`fileName` TEXT NOT NULL, " +
                        "`isVideo` INTEGER NOT NULL, " +
                        "`addedAt` INTEGER NOT NULL)"
                )
            }
        }

        fun getInstance(context: Context): PropertyDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PropertyDatabase::class.java,
                    "my_properties.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
