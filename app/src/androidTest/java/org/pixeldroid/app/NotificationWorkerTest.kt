package org.pixeldroid.app

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.pixeldroid.app.settings.AboutActivity
import org.pixeldroid.app.testUtility.*
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Notification
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.notificationsWorker.NotificationsWorker
import java.time.Instant

@RunWith(JUnit4::class)
class NotificationWorkerTest {
    private lateinit var context: Context
    private lateinit var activityScenario: ActivityScenario<AboutActivity>
    private val uiDevice by lazy { UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()) }

    private lateinit var db: AppDatabase

    private val secondToLatestNotification: Notification =
        Notification(
            id = "78",
            type = Notification.NotificationType.follow,
            created_at = Instant.parse("2022-07-25T16:23:45Z"),
            account = Account(
                id = "344399325768278017",
                username = "pixeldroid",
                acct = "pixeldroid",
                url = "${testiTesto.instance_uri}/pixeldroid",
                display_name = "PixelDroid",
                note = "",
                avatar = "${testiTesto.instance_uri}/storage/avatars/default.jpg?v=0",
                avatar_static = null,
                header = null,
                header_static = null,
                locked = false,
                emojis = null,
                discoverable = null,
                created_at = Instant.parse("1970-01-01T00:00:00Z"),
                statuses_count = 0,
                followers_count = 0,
                following_count = 1,
                moved = null,
                fields = null,
                bot = null,
                source = null
            ),
            status = null,
            user_id = testiTesto.user_id,
            instance_uri = testiTesto.instance_uri
        )
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        db = initDB(context)
        db.clearAllTables()
        db.instanceDao().insertInstance(
            testiTestoInstance
        )

        db.userDao().insertUser(
            testiTesto
        )

        runBlocking {
            db.notificationDao().insertAll(listOf(secondToLatestNotification))
        }

        db.close()

        activityScenario = ActivityScenario.launch(AboutActivity::class.java)
    }

    @Test
    fun testNotificationWorker() {
        val expectedAppName = context.getString(R.string.app_name)
        val expectedText = "admin liked your post"

        // Run the worker synchronously
        val worker = TestListenableWorkerBuilder<NotificationsWorker>(context).build()
        val result = worker.startWork().get()

        // Check worker returns success (which doesn't mean much, but is a good start)
        MatcherAssert.assertThat(result, CoreMatchers.`is`(ListenableWorker.Result.success()))

        // Open notification shade
        uiDevice.openNotification()
        uiDevice.wait(Until.hasObject(By.textStartsWith(expectedAppName)), 5000)

        val text: UiObject2 = uiDevice.findObject(By.textStartsWith(expectedText))
        text.click()

        uiDevice.wait(Until.hasObject(By.textStartsWith(expectedText)), 5000)
        waitForView(R.id.notification_type)
        onView(first(withId(R.id.notification_type)))
            .check(matches(withText(expectedText)))

    }
}