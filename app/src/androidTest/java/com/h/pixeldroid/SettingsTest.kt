package com.h.pixeldroid

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
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
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_settings))
        onView(withId(R.id.settings)).check(matches(isDisplayed()))
    }

    @Test
    fun accessibilityButtonTest() {
    }

//    @After
//    fun resetState() {
//        onView(withId(R.id.drawer_layout)).perform(DrawerActions.close())
//    }
}