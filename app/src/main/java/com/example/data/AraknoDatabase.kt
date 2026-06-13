package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Usuario::class, EspecieArana::class, Avistamiento::class], version = 1, exportSchema = false)
abstract class AraknoDatabase : RoomDatabase() {
    abstract val dao: AraknoDao

    companion object {
        @Volatile
        private var INSTANCE: AraknoDatabase? = null

        fun getDatabase(context: Context): AraknoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AraknoDatabase::class.java,
                    "arakno_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
