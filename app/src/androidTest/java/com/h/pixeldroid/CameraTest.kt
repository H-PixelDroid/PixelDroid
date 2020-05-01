package com.h.pixeldroid

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.h.pixeldroid.CameraActivity
import com.h.pixeldroid.PostCreationActivity
import com.h.pixeldroid.R

class CameraTest {

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(30)
    @get:Rule
    var activityRule = ActivityScenarioRule(CameraActivity::class.java)


    @Test
    fun takePictureButton() {
        onView(withId(R.id.capture_button)).perform(click())
        Thread.sleep(300)
        assert(activityRule.scenario.state == Lifecycle.State.DESTROYED)
    }

    @Test
    fun rotateAndTakePictureButton() {
        val device = UiDevice.getInstance(getInstrumentation())
        device.setOrientationLeft()
        onView(withId(R.id.capture_button)).perform(click())
        Thread.sleep(300)
        assert(activityRule.scenario.state == Lifecycle.State.DESTROYED)
    }
}