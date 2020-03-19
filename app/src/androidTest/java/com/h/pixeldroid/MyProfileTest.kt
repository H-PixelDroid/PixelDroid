package com.h.pixeldroid

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MyProfileTest {
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun before(){
        val preferences = InstrumentationRegistry.getInstrumentation()
            .targetContext.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", "http://localhost").apply()
        ActivityScenario.launch(MainActivity::class.java)
    }
    @Test
    fun testFollowersTextView() {
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()).perform(ViewActions.swipeLeft()).perform(
            ViewActions.swipeLeft()
        ).perform(ViewActions.swipeLeft())
        Thread.sleep(1000)

        onView(withId(R.id.nbFollowersTextView)).check(matches(withText("-\nFollowers")))
        onView(withId(R.id.editButton)).check(matches(withText("Edit profile")))
        onView(withId(R.id.accountNameTextView)).check(matches(withText("No Username")))
    }
}
