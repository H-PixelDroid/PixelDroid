package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.android.material.tabs.TabLayout
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SwipeTest {
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)
    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)
    @Before
    fun before(){
        val preferences = getInstrumentation()
            .targetContext.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", "http://localhost").apply()
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
        ActivityScenario.launch(MainActivity::class.java).onActivity {
            a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(4)?.select()
        } // go to the last tab
        onView(withId(R.id.main_activity_main_linear_layout))
            .perform(swipeRight()) // notifications
            .perform(swipeRight()) // camera
            .perform(swipeRight()) // search
            .perform(swipeRight()) // homepage
            .perform(swipeRight()) // should stop at homepage
        onView(withId(R.id.feedList)).check(matches(isDisplayed()))
    }
}