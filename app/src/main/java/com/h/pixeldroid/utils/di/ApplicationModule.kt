package com.h.pixeldroid.utils.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides

import javax.inject.Singleton


@Module
class ApplicationModule(app: Application) {
    private val mApplication: Application = app

    @Singleton
    @Provides
    fun provideContext(): Context {
        return mApplication
    }

    @Singleton
    @Provides
    fun provideApplication(): Application {
        return mApplication
    }

}