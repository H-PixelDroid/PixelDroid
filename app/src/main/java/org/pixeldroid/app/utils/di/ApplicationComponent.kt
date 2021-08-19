package org.pixeldroid.app.utils.di

import android.app.Application
import android.content.Context
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.PixelDroidApplication
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.BaseFragment
import dagger.Component
import org.pixeldroid.app.utils.notificationsWorker.NotificationsWorker
import javax.inject.Singleton


@Singleton
@Component(modules = [ApplicationModule::class, DatabaseModule::class, APIModule::class])
interface ApplicationComponent {
    fun inject(application: PixelDroidApplication?)
    fun inject(activity: BaseActivity?)
    fun inject(feedFragment: BaseFragment)
    fun inject(notificationsWorker: NotificationsWorker)

    val context: Context?
    val application: Application?
    val database: AppDatabase
}