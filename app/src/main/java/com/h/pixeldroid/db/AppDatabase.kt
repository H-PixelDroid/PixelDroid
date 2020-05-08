package com.h.pixeldroid.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Instance::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        const val MAX_NUMBER_OF_POSTS = 100
    }

    abstract fun instanceDao(): InstanceDao
}