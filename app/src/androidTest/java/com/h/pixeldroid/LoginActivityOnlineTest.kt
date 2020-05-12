package com.h.pixeldroid

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityOnlineTest {
    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)
    @get:Rule
    var activityRule: ActivityScenarioRule<LoginActivity>
            = ActivityScenarioRule(LoginActivity::class.java)

    @Test
    fun notPixelfedInstance() {
        Espresso.onView(ViewMatchers.withId(R.id.editText))
            .perform(ViewActions.replaceText("localhost"), ViewActions.closeSoftKeyboard())
        Espresso.onView(ViewMatchers.withId(R.id.connect_instance_button))
            .perform(ViewActions.scrollTo()).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.editText))
            .check(ViewAssertions.matches(ViewMatchers.hasErrorText("Could not register the application with this server")))
    }
}