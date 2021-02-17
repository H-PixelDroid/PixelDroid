package com.h.pixeldroid

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasDataString
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import com.h.pixeldroid.BuildConfig.INSTANCE_URI
import com.h.pixeldroid.testUtility.clearData
import com.h.pixeldroid.testUtility.waitForView
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

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    private lateinit var activityScenario: ActivityScenario<LoginActivity>

    @Before
    fun before() {
        Intents.init()
        activityScenario = ActivityScenario.launch(LoginActivity::class.java)
    }

    @Test
    fun launchesOAuthIntent() {
        ActivityScenario.launch(LoginActivity::class.java)
        val expectedIntent: Matcher<Intent> = allOf(
            hasAction(ACTION_VIEW),
            hasDataString(containsString(INSTANCE_URI))
        )

        waitForView(R.id.editText)

        onView(withId(R.id.editText)).perform(scrollTo()).perform(
            ViewActions.replaceText(
                INSTANCE_URI
            ), ViewActions.closeSoftKeyboard()
        )
        onView(withId(R.id.connect_instance_button)).perform(scrollTo()).perform(click())

        Thread.sleep(3000)

        intended(expectedIntent)

    }
    @Test
    fun launchesInstanceInfo() {
        ActivityScenario.launch(LoginActivity::class.java)

        onView(withId(R.id.whatsAnInstanceTextView)).perform(scrollTo()).perform(click())

        waitForView(R.id.whats_an_instance_explanation)

        onView(withText(R.string.whats_an_instance_explanation))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
    }

    @After
    fun after() {
        Intents.release()
        clearData()
        activityScenario.close()
    }
}
