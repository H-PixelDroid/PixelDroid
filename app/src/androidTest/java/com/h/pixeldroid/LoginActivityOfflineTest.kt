package com.h.pixeldroid
/*
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.h.pixeldroid.testUtility.clearData
import com.h.pixeldroid.testUtility.initDB
import com.h.pixeldroid.utils.db.AppDatabase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityOfflineTest {

    private lateinit var context: Context

    companion object {
        fun switchAirplaneMode() {
            val device = UiDevice.getInstance(getInstrumentation())
            device.openQuickSettings()
            device.findObject(UiSelector().textContains("Airplane")).click()
            device.pressHome()
        }
    }

    private lateinit var db: AppDatabase

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @Before
    fun before() {
        switchAirplaneMode()
        context = ApplicationProvider.getApplicationContext<Context>()
        db = initDB(context)
        db.clearAllTables()
        ActivityScenario.launch(LoginActivity::class.java)
    }

    @Test
    fun emptyDBandOfflineModeDisplayCorrectMessage() {
        onView(withId(R.id.login_activity_connection_required)).check(matches(isDisplayed()))
    }

    @Test
    fun retryButtonReloadsLoginActivity() {
        onView(withId(R.id.login_activity_connection_required_button)).perform(click())
        onView(withId(R.id.login_activity_connection_required)).check(matches(isDisplayed()))
    }

    @After
    fun after() {
        switchAirplaneMode()
        db.close()
        clearData()
    }
}*/