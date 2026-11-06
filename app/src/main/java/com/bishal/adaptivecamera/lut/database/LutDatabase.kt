package com.bishal.adaptivecamera.lut.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LutEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LutDatabase : RoomDatabase() {

    abstract fun lutDao(): LutDao

    companion object {
        @Volatile
        private var INSTANCE: LutDatabase? = null

        fun getInstance(context: Context): LutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LutDatabase::class.java,
                    "xamera_lut_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
