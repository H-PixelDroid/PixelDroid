package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
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
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)
    @Before
    fun before(){
        val preferences = getInstrumentation()
            .targetContext.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", "http://localhost").apply()
        ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun swipingRightOnHomepageShowsSettings() {
        onView(withId(R.id.view_pager)).perform(swipeLeft()).perform(swipeLeft()).perform(swipeLeft()).perform(swipeLeft())
        onView(withId(R.id.nbFollowersTextView)).check(matches(isDisplayed()))
    }
}