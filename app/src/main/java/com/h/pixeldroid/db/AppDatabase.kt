package com.h.pixeldroid.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PostEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    val MAX_NUMBER_OF_POSTS = 100

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null
        var TEST_MODE = false

        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {
                var instance: AppDatabase? = null

                // To be able to create a temporary database that flushes when tests are over
                instance = if (TEST_MODE) {
                    Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
                } else {
                    Room.databaseBuilder(
                        context.applicationContext, AppDatabase::class.java, "posts_database"
                    ).allowMainThreadQueries().build()
                }

                INSTANCE = instance
                return instance
            }
        }
    }
}