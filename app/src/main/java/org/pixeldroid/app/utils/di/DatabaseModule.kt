package org.pixeldroid.app.utils.di

import android.content.Context
import androidx.room.Room
import org.pixeldroid.app.utils.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.pixeldroid.app.utils.db.MIGRATION_3_4
import org.pixeldroid.app.utils.db.MIGRATION_4_5
import org.pixeldroid.app.utils.db.MIGRATION_5_6
import org.pixeldroid.app.utils.db.MIGRATION_9_10
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun providesDatabase(
        @ApplicationContext applicationContext: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "pixeldroid"
        ).addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_9_10)
            .allowMainThreadQueries().build()
    }
}