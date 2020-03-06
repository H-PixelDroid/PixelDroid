package com.h.pixeldroid

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.not

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileTest {
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testFollowersTextView() {
        onView(withId(R.id.activity_main_account_btn)).perform(click())
        onView(withId(R.id.followers)).check(matches(withText("Followers")))
    }

//    @Test
//    fun testAccountNameTextView() {
//        onView(withId(R.id.activity_main_account_btn)).perform(click())
//        onView(withId(R.id.accountName)).check(matches(withText("No Username")))
//    }
}