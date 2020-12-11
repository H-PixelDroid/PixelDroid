package com.h.pixeldroid.di

import android.app.Application
import android.content.Context
import com.h.pixeldroid.*
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.fragments.BaseFragment
import com.h.pixeldroid.fragments.PostFragment
import com.h.pixeldroid.fragments.SearchDiscoverFragment
import com.h.pixeldroid.fragments.feeds.cachedFeeds.notifications.NotificationsFragment
import dagger.Component

import javax.inject.Singleton


@Singleton
@Component(modules = [ApplicationModule::class, DatabaseModule::class, APIModule::class])
interface ApplicationComponent {
    fun inject(application: Pixeldroid?)
    fun inject(activity: LoginActivity?)
    fun inject(activity: PostActivity?)
    fun inject(activity: PostCreationActivity?)
    fun inject(activity: ProfileActivity?)
    fun inject(mainActivity: MainActivity?)
    fun inject(activity: ReportActivity?)

    fun inject(feedFragment: BaseFragment)
    fun inject(followsActivity: FollowsActivity)


    val context: Context?
    val application: Application?
    val database: AppDatabase
}