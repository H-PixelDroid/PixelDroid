package com.h.pixeldroid

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions

import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class SettingsTest {
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun openDrawer() {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
    }

    @Test
    fun myProfileButtonTest() {
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_account))
        onView(withId(R.id.profile_main_container)).check(matches(isDisplayed()))
    }


    @Test
    fun settingsButtonTest() {
        // Start the screen of the activity.

        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_settings))
        onView(withId(R.id.settings)).check(matches(isDisplayed()))
    }


    fun testDrawerProfileButton() {
        // Open Drawer to click on navigation.
        onView(withId(R.id.drawer_layout))
            .check(matches(isClosed(Gravity.LEFT))) // Left Drawer should be closed.
            .perform(DrawerActions.open()); // Open Drawer

        // Start the screen of the activity.
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_account))

        // Check that profile activity was opened.
        onView(withId(R.id.posts)).check(matches(withText("Posts")))
    }

    @Test
    fun testOnBackPressed() {
        // Open Drawer to click on navigation.
        onView(withId(R.id.drawer_layout))
            .check(matches(isClosed(Gravity.LEFT))) // Left Drawer should be closed.
            .perform(DrawerActions.open()); // Open Drawer

        // Start the screen of your activity.
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_settings))

        onView(withId(R.id.settings)).perform(ViewActions.pressBack())

        // Check that profile activity was opened.
        onView(withId(R.id.button_start_login)).check(matches(withText("start login")))
    }
}