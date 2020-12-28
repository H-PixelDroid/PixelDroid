package com.h.pixeldroid.utils.di

import android.app.Application
import android.content.Context
import com.h.pixeldroid.utils.BaseActivity
import com.h.pixeldroid.utils.PixelDroidApplication
import com.h.pixeldroid.utils.db.AppDatabase
import com.h.pixeldroid.utils.BaseFragment
import dagger.Component
import javax.inject.Singleton


@Singleton
@Component(modules = [ApplicationModule::class, DatabaseModule::class, APIModule::class])
interface ApplicationComponent {
    fun inject(application: PixelDroidApplication?)
    fun inject(activity: BaseActivity?)
    fun inject(feedFragment: BaseFragment)

    val context: Context?
    val application: Application?
    val database: AppDatabase
}