package com.h.pixeldroid

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class LoginInstrumentedTest {
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)
    @Test
    fun clickConnect() {
        onView(withId(R.id.button_start_login)).perform(click())
        onView(withId(R.id.connect_instance_button)).check(matches(withText("Connect")))
    }
    @Test
    fun invalidURL() {
        onView(withId(R.id.button_start_login)).perform(click())
        onView(withId(R.id.editText)).perform(ViewActions.replaceText("/jdi"), ViewActions.closeSoftKeyboard())
        onView(withId(R.id.connect_instance_button)).perform(click())
        onView(withId(R.id.editText)).check(matches(hasErrorText("Invalid domain")))

    }
    @Test
    fun notPixelfedInstance() {
        onView(withId(R.id.button_start_login)).perform(click())
        onView(withId(R.id.editText)).perform(ViewActions.replaceText("localhost"), ViewActions.closeSoftKeyboard())
        onView(withId(R.id.connect_instance_button)).perform(click())
        onView(withId(R.id.editText)).check(matches(hasErrorText("Could not register the application with this server")))

    }
}
