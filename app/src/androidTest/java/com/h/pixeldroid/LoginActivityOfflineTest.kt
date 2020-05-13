package com.h.pixeldroid

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.InstanceDatabaseEntity
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.utils.DBUtils
import kotlinx.android.synthetic.main.activity_login.login_activity_instance_chooser_button
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class LoginActivityOfflineTest {

    private lateinit var db: AppDatabase
    private lateinit var device: UiDevice

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @Before
    fun before() {
        device = UiDevice.getInstance(getInstrumentation())
        device.openQuickSettings()
        device.findObject(UiSelector().textContains("airplane")).click()
        device.pressHome()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = DBUtils.initDB(context)
        db.clearAllTables()
    }

    @Test
    fun emptyDBandOfflineModeDisplayCorrectMessage() {
        ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.login_activity_connection_required_text)).check(matches(isDisplayed()))
    }

    @Test
    fun offlineModeSelectAvailabeLaunchesMainActivityWithStoredAccountInstance() {
        db.instanceDao().insertInstance(
            InstanceDatabaseEntity(
            uri = "some_uri",
            title = "PixelTest"
        ))
        db.userDao().insertUser(
            UserDatabaseEntity(
            user_id = "some_user_id",
            instance_uri = "some_uri",
            username = "Testi",
            display_name = "Testi Testo",
            avatar_static = "some_avatar_url"
        ))
        ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.login_activity_instance_chooser_button)).perform(click())
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.drawer_account_name)).check(matches(withText("Testi Testo")))
    }

    @After
    fun after() {
        device.openQuickSettings()
        device.findObject(UiSelector().textContains("airplane")).click()
        device.pressHome()
    }
}