package com.h.pixeldroid

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.InstanceDatabaseEntity
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.utils.DBUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class LoginActivityOfflineTest {

    private lateinit var db: AppDatabase

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @Before
    fun before() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openQuickSettings()
        device.findObject(UiSelector().textContains("airplane")).click()
        device.pressHome()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = DBUtils.initDB(context)
    }

    @Test
    fun emptyDBandOfflineModeDisplayCorrectMessage() {
        db.clearAllTables()
        ActivityScenario.launch(LoginActivity::class.java)
        Espresso.onView(ViewMatchers.withId(R.id.login_activity_connection_required_text))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun offlineModeShowsAvailabeInstances() {
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
        Espresso.onView(ViewMatchers.withText("Testi@PixelTest"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @After
    fun after() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openQuickSettings()
        device.findObject(UiSelector().textContains("airplane")).click()
        device.pressHome()
    }
}