package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasDataString
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.h.pixeldroid.testUtility.clearData
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginCheckIntent {
    private lateinit var context: Context


    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)
    @get:Rule
    val intentsTestRule = ActivityTestRule(LoginActivity::class.java)

    @Before
    fun before() {
        Intents.init()
    }

    @Test
    fun launchesOAuthIntent() {
        ActivityScenario.launch(LoginActivity::class.java)
        val expectedIntent: Matcher<Intent> = allOf(
            hasAction(ACTION_VIEW),
            hasDataString(containsString("pixelfed.de"))
        )
        Thread.sleep(1000)

        onView(withId(R.id.editText)).perform(scrollTo()).perform(ViewActions.replaceText("pixelfed.de"), ViewActions.closeSoftKeyboard())
        onView(withId(R.id.connect_instance_button)).perform(scrollTo()).perform(click())

        Thread.sleep(3000)

        intended(expectedIntent)

    }
    @Test
    fun launchesInstanceInfo() {
        ActivityScenario.launch(LoginActivity::class.java)
        val expectedIntent: Matcher<Intent> = allOf(
            hasAction(ACTION_VIEW),
            hasDataString(containsString("pixelfed.org/join"))
        )

        onView(withId(R.id.whatsAnInstanceTextView)).perform(scrollTo()).perform(click())

        Thread.sleep(3000)

        intended(expectedIntent)

    }

    @After
    fun after() {
        Intents.release()
        clearData()
    }
}
