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
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
            .check(matches(DrawerMatchers.isClosed(Gravity.LEFT))) // Left Drawer should be closed.
            .perform(DrawerActions.open()) // Open Drawer
    }

    @Test
    fun testDrawerSettingsButton() {
        // Start the screen of your activity.
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_settings))
        // Check that settings activity was opened.
        onView(withText(R.string.signature_title)).check(matches(isDisplayed()))
    }

    @Test
    fun testDrawerLogoutButton() {
        // Start the screen of your activity.
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_logout))
        // Check that settings activity was opened.
        onView(withId(R.id.connect_instance_button)).check(matches(isDisplayed()))
    }

    @Test
    fun testDrawerProfileButton() {
        // Start the screen of your activity.
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_account))
        // Check that profile activity was opened.
        onView(withId(R.id.profilePictureImageView)).check(matches(isDisplayed()))
    }

    @Test
    fun testDrawerAvatarClick() {
        // Start the screen of your activity.
        onView(withId(R.id.drawer_avatar)).perform(ViewActions.click())
        // Check that profile activity was opened.
        onView(withId(R.id.profilePictureImageView)).check(matches(isDisplayed()))
    }

    @Test
    fun testDrawerAccountNameClick() {
        // Start the screen of your activity.
        onView(withId(R.id.drawer_account_name)).perform(ViewActions.click())
        // Check that profile activity was opened.
        onView(withId(R.id.profilePictureImageView)).check(matches(isDisplayed()))
    }

    @Test
    fun clickFollowers() {
        // Open My Profile from drawer
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_account))
        Thread.sleep(1000)

        // Open followers list
        onView(withId(R.id.nbFollowersTextView)).perform(ViewActions.click())
        Thread.sleep(1000)
        // Open follower's profile
        onView(withText("ete2")).perform(ViewActions.click())
        Thread.sleep(1000)

        onView(withId(R.id.accountNameTextView)).check(matches(withText("ete2")))
    }

    @Test
    fun clickFollowing() {
        // Open My Profile from drawer
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_account))
        Thread.sleep(1000)
        // Open followers list
        onView(withId(R.id.nbFollowingTextView)).perform(ViewActions.click())
        Thread.sleep(1000)
        // Open following's profile
        onView(withText("Dobios")).perform(ViewActions.click())
        Thread.sleep(1000)

        onView(withId(R.id.accountNameTextView)).check(matches(withText("Dobios")))
    }
}