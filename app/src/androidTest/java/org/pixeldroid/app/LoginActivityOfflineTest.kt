package org.pixeldroid.app

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.pixeldroid.app.testUtility.clearData
import org.pixeldroid.app.testUtility.initDB
import org.pixeldroid.app.utils.db.AppDatabase
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.pixeldroid.app.testUtility.waitForView

@RunWith(AndroidJUnit4::class)
@Ignore("Ignore until we can get TestButler to work on CI")
class LoginActivityOfflineTest {

    private lateinit var context: Context

    private lateinit var db: AppDatabase

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @Before
    fun before() {
        //TestButler.setWifiState(false)
        //TestButler.setGsmState(false)
        context = ApplicationProvider.getApplicationContext<Context>()
        db = initDB(context)
        db.clearAllTables()
        ActivityScenario.launch(LoginActivity::class.java)
    }

    @Test
    fun emptyDBandOfflineModeDisplayCorrectMessage() {
        waitForView(R.id.login_activity_connection_required)
        onView(withId(R.id.login_activity_connection_required)).check(matches(isDisplayed()))
    }

    @Test
    fun retryButtonReloadsLoginActivity() {
        waitForView(R.id.login_activity_connection_required_button)
        onView(withId(R.id.login_activity_connection_required_button)).perform(click())
        onView(withId(R.id.login_activity_connection_required)).check(matches(isDisplayed()))
    }

    @After
    fun after() {
        //TestButler.setWifiState(true)
        //TestButler.setGsmState(true)
        db.close()
        clearData()
    }
}