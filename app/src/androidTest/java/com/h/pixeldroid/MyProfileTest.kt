package com.h.pixeldroid

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyProfileTest {
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

   /* @Before
    fun openProfileFragment() {

    }

    @Test
    fun testFollowersTextView() {
        onView(withId(R.id.editButton)).check(matches(withText("Edit profile")))
    }*/
}
/*
@RunWith(AndroidJUnit4::class)
class LoginCheckIntent {
    @get:Rule
    val intentsTestRule = IntentsTestRule(LoginActivity::class.java)

    @Test
    fun launchesIntent() {
        val expectedIntent: Matcher<Intent> = allOf(
            hasAction(ACTION_VIEW)
        )

        onView(withId(R.id.editText)).perform(ViewActions.replaceText("pixelfed.social"), ViewActions.closeSoftKeyboard())
        onView(withId(R.id.connect_instance_button)).perform(click())

        Thread.sleep(5000)

        intended(expectedIntent)
    }
}*/