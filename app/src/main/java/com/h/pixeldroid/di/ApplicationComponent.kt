package com.h.pixeldroid.di

import android.app.Application
import android.content.Context
import com.h.pixeldroid.BaseActivity
import com.h.pixeldroid.Pixeldroid
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.fragments.BaseFragment
import dagger.Component
import javax.inject.Singleton


@Singleton
@Component(modules = [ApplicationModule::class, DatabaseModule::class, APIModule::class])
interface ApplicationComponent {
    fun inject(application: Pixeldroid?)
    fun inject(activity: BaseActivity?)
    fun inject(feedFragment: BaseFragment)

    val context: Context?
    val application: Application?
    val database: AppDatabase
}