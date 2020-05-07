package com.h.pixeldroid

import android.Manifest
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.rule.GrantPermissionRule
import com.google.android.material.tabs.TabLayout
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

class CameraTest {

    private lateinit var activityScenario: ActivityScenario<MainActivity>

    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)
    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(30)
    @get:Rule
    var runtimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    @get:Rule
    var intentsTestRule: IntentsTestRule<MainActivity> =
        IntentsTestRule(MainActivity::class.java)
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
        activityScenario.onActivity {
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(2)?.select()
        }
        Thread.sleep(1000)
    }

    @Test
    fun flipCameraButton() {
        onView(withId(R.id.flip_button)).check(matches(isClickable()))
        onView(withId(R.id.flip_button)).perform(click())
    }

    @Test
    fun seekBar() {
        onView(withId(R.id.seekBar)).check(matches(isFocusable()))
        onView(withId(R.id.seekBar)).perform(swipeRight())
    }

  //  @Test
  //  fun takePictureButton() {
  //      onView(withId(R.id.capture_button)).perform(click())
  //      val expectedIntent: Matcher<Intent> = hasComponent(PostCreationActivity::class.simpleName)
  //      intending(expectedIntent)
  //      Thread.sleep(1000)
  //      intended(expectedIntent)
  //  }

    @Test
    fun uploadButtonLaunchesGalleryIntent() {
        val expectedIntent: Matcher<Intent> = hasAction(Intent.ACTION_CHOOSER)
        intending(expectedIntent)
        onView(withId(R.id.upload_button)).perform(click())
        Thread.sleep(1000)
        intended(expectedIntent)
    }
}