package com.nexstay.myproperties.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Property::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PropertyDatabase : RoomDatabase() {

    abstract fun propertyDao(): PropertyDao

    companion object {
        @Volatile
        private var INSTANCE: PropertyDatabase? = null

        fun getInstance(context: Context): PropertyDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PropertyDatabase::class.java,
                    "my_properties.db"
                ).build().also { INSTANCE = it }
            }
    }
}
