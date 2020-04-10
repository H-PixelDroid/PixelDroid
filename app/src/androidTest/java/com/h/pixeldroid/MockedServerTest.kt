package com.h.pixeldroid

import android.content.Context
import android.view.Gravity
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.tabs.TabLayout
import com.h.pixeldroid.testUtility.MockServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MockedServerTest {

    val mockServer = MockServer()

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun before(){
        mockServer.start()
        val baseUrl = mockServer.getUrl()
        val preferences = InstrumentationRegistry.getInstrumentation()
            .targetContext.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", baseUrl.toString()).apply()
    }

    @Test
    fun testFollowersTextView() {
        ActivityScenario.launch(MainActivity::class.java).onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(4)?.select()
        }
        Thread.sleep(1000)
        onView(withId(R.id.nbFollowersTextView)).check(matches(withText("68\nFollowers")))
        onView(withId(R.id.accountNameTextView)).check(matches(withText("deerbard_photo")))
    }

    @Test
    fun testNotificationsList() {
        ActivityScenario.launch(MainActivity::class.java).onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }
        Thread.sleep(1000)

        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())
        onView(withText("Dobios liked your post")).check(matches(withId(R.id.notification_type)))
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeDown())
        Thread.sleep(1000)
        onView(withText("Dobios followed you")).check(matches(withId(R.id.notification_type)))

    }
    @Test
    fun clickNotification() {
        ActivityScenario.launch(MainActivity::class.java).onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }
        Thread.sleep(1000)

        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())
        Thread.sleep(1000)

        onView(withText("Dobios liked your post")).perform(ViewActions.click())
        Thread.sleep(1000)
        onView(withText("6 Likes")).check(matches(withId(R.id.nlikes)))
    }

    @Test
    fun clickNotificationUser() {
        ActivityScenario.launch(MainActivity::class.java).onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }
        Thread.sleep(1000)

        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())
        Thread.sleep(1000)

        onView(withText("Dobios followed you")).perform(ViewActions.click())
        Thread.sleep(1000)
        onView(withText("Dobios")).check(matches(withId(R.id.accountNameTextView)))
    }

    @Test
    fun clickNotificationPost() {
        ActivityScenario.launch(MainActivity::class.java).onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }
        Thread.sleep(1000)

        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())
        Thread.sleep(1000)

        onView(withText("Dobios liked your post")).perform(ViewActions.click())
        Thread.sleep(1000)

        onView(withId(R.id.username)).perform(ViewActions.click())
        Thread.sleep(10000)
        onView(withText("Dante")).check(matches(withId(R.id.accountNameTextView)))
    }

    @Test
    fun testDrawerSettingsButton() {
        // Open Drawer to click on navigation.
        onView(withId(R.id.drawer_layout))
            .check(matches(DrawerMatchers.isClosed(Gravity.LEFT))) // Left Drawer should be closed.
            .perform(DrawerActions.open()); // Open Drawer

        // Start the screen of your activity.
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_settings))

        // Check that settings activity was opened.
        onView(withText(R.string.signature_title)).check(matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun swipingLeftStopsAtProfile() {
        onView(withId(R.id.main_activity_main_linear_layout))
            .perform(ViewActions.swipeLeft()) // search
            .perform(ViewActions.swipeLeft()) // camera
            .perform(ViewActions.swipeLeft()) // notifications
            .perform(ViewActions.swipeLeft()) // profile
            .perform(ViewActions.swipeLeft()) // should stop at profile
        onView(withId(R.id.nbFollowersTextView)).check(matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun swipingRightStopsAtHomepage() {
        ActivityScenario.launch(MainActivity::class.java).onActivity {
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(4)?.select()
        } // go to the last tab
        onView(withId(R.id.main_activity_main_linear_layout))
            .perform(ViewActions.swipeRight()) // notifications
            .perform(ViewActions.swipeRight()) // camera
            .perform(ViewActions.swipeRight()) // search
            .perform(ViewActions.swipeRight()) // homepage
            .perform(ViewActions.swipeRight()) // should stop at homepage
        onView(withId(R.id.list)).check(matches(ViewMatchers.isDisplayed()))
    }
}
