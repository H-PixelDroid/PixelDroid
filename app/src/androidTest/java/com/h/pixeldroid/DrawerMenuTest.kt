package com.h.pixeldroid

import android.content.Context
import android.view.Gravity
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.h.pixeldroid.testUtility.MockServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawerMenuTest {

    private val mockServer = MockServer()

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(30)
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
        // Open Drawer to click on navigation.
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.drawer_layout))
            .check(matches(DrawerMatchers.isClosed())) // Left Drawer should be closed.
            .perform(DrawerActions.open()) // Open Drawer
    }

   /* @Test
    fun testDrawerSettingsButton() {
        // Start the screen of your activity.
        onView(withId(R.id.drawer)).perform(NavigationViewActions.navigateTo(R.id.nav_settings))
        // Check that settings activity was opened.
        onView(withText(R.string.theme_title)).check(matches(isDisplayed()))
    }

    @Test
    fun testThemeSettings() {
        // Start the screen of your activity.
        onView(withId(R.id.drawer)).perform(NavigationViewActions.navigateTo(R.id.nav_settings))
        val themes = getInstrumentation().targetContext.resources.getStringArray(R.array.theme_entries)
        //select theme modes
        onView(withText(R.string.theme_title)).perform(click())
        onView(withText(themes[2])).perform(click())

        //Select an other theme
        onView(withText(R.string.theme_title)).perform(click())
        onView(withText(themes[0])).perform(click())

        //Select the last theme
        onView(withText(R.string.theme_title)).perform(click())
        onView(withText(themes[1])).perform(click())

        //Check that we are back in the settings page
        onView(withText(R.string.theme_header)).check(matches(isDisplayed()))
    }

    @Test
    fun testDrawerLogoutButton() {
        // Start the screen of your activity.
        onView(withId(R.id.drawer)).perform(NavigationViewActions.navigateTo(R.id.nav_logout))
        // Check that settings activity was opened.
        onView(withId(R.id.connect_instance_button)).check(matches(isDisplayed()))
    }

    @Test
    fun testDrawerProfileButton() {
        // Start the screen of your activity.
        onView(withId(R.id.drawer)).perform(NavigationViewActions.navigateTo(R.id.nav_account))
        // Check that profile activity was opened.
        onView(withId(R.id.profilePictureImageView)).check(matches(isDisplayed()))
    }

    @Test
    fun testDrawerAvatarClick() {
        // Start the screen of your activity.
        onView(withId(R.id.drawer_avatar)).perform(click())
        // Check that profile activity was opened.
        onView(withId(R.id.profilePictureImageView)).check(matches(isDisplayed()))
    }

    @Test
    fun testDrawerAccountNameClick() {
        // Start the screen of your activity.
        onView(withId(R.id.drawer_account_name)).perform(click())
        // Check that profile activity was opened.
        onView(withId(R.id.profilePictureImageView)).check(matches(isDisplayed()))
    }

    @Test
    fun clickFollowers() {
        // Open My Profile from drawer
        onView(withId(R.id.drawer)).perform(NavigationViewActions.navigateTo(R.id.nav_account))
        Thread.sleep(1000)

        // Open followers list
        onView(withId(R.id.nbFollowersTextView)).perform(click())
        Thread.sleep(1000)
        // Open follower's profile
        onView(withText("ete2")).perform(click())
        Thread.sleep(1000)

        onView(withId(R.id.accountNameTextView)).check(matches(withText("Christian")))
    }

    @Test
    fun clickFollowing() {
        // Open My Profile from drawer
        onView(withId(R.id.drawer)).perform(NavigationViewActions.navigateTo(R.id.nav_account))
        Thread.sleep(1000)
        // Open followers list
        onView(withId(R.id.nbFollowingTextView)).perform(click())
        Thread.sleep(1000)
        // Open following's profile
        onView(withText("Dobios")).perform(click())
        Thread.sleep(1000)

        onView(withId(R.id.accountNameTextView)).check(matches(withText("Andrew Dobis")))
    }

    @Test
    fun onBackPressedClosesDrawer() {
        UiDevice.getInstance(getInstrumentation()).pressBack()
        Thread.sleep(1000)
        onView(withId(R.id.drawer_layout)).check(matches(DrawerMatchers.isClosed()))
    }*/
}