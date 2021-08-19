package org.pixeldroid.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.pixeldroid.app.utils.notificationsWorker.NotificationsWorker

//TODO actual test here
@RunWith(JUnit4::class)
class NotificationWorkerTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testNotificationWorker() {
        // Get the ListenableWorker
        val worker =
            TestListenableWorkerBuilder<NotificationsWorker>(context).build()        // Run the worker synchronously
        val result = worker.startWork().get()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(ListenableWorker.Result.success()))
    }
}