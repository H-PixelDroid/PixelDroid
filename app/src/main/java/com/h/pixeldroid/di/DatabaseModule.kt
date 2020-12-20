package com.h.pixeldroid.di

import android.content.Context
import androidx.room.Room
import com.h.pixeldroid.db.AppDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DatabaseModule(private val context: Context) {

    @Provides
    @Singleton
    fun providesDatabase(): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java, "pixeldroid"
        ).allowMainThreadQueries()
            //TODO remove this for 1.0 release
            .fallbackToDestructiveMigration()
            .build()
    }
}