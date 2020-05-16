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
        db.close()
    }

    @Test
    fun emptyDBandOfflineModeDisplayCorrectMessage() {
        ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.login_activity_connection_required_text)).check(matches(isDisplayed()))
    }


    @After
    fun after() {
        device.openQuickSettings()
        device.findObject(UiSelector().textContains("airplane")).click()
        device.pressHome()
    }
}