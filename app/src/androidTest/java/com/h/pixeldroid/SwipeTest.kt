package com.h.pixeldroid

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SwipeTest {
    @get:Rule
    var activityRule: ActivityTestRule<MainActivity>
            = ActivityTestRule(MainActivity::class.java)
    @Before
    fun before(){
        val preferences = getInstrumentation()
            .targetContext.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", "http://localhost").apply()
        ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun swipingLeftStopsAtProfile() {
        onView(withId(R.id.main_activity_main_linear_layout))
            .perform(swipeLeft()) // search
            .perform(swipeLeft()) // camera
            .perform(swipeLeft()) // notifications
            .perform(swipeLeft()) // profile
            .perform(swipeLeft()) // should stop at profile
        onView(withId(R.id.nbFollowersTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun swipingRightStopsAtHomepage() {
        onView(withId(R.id.main_activity_main_linear_layout))
            .perform(swipeLeft()) // search
            .perform(swipeLeft()) // camera
            .perform(swipeLeft()) // notifications
            .perform(swipeLeft()) // profile
            .perform(swipeRight()) // notifications
            .perform(swipeRight()) // camera
            .perform(swipeRight()) // search
            .perform(swipeRight()) // homepage
            .perform(swipeRight()) // should stop at homepage
        onView(withId(R.id.feedList)).check(matches(isDisplayed()))
    }
}