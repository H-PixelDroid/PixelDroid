package com.h.pixeldroid.utils.di

import android.content.Context
import androidx.room.Room
import com.h.pixeldroid.utils.db.AppDatabase
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
        ).allowMainThreadQueries().build()
    }
}