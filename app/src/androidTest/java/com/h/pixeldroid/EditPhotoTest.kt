package com.h.pixeldroid

import android.content.Context
import android.view.View
import android.widget.SeekBar
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.android.material.tabs.TabLayout
import com.h.pixeldroid.testUtility.MockServer
import kotlinx.android.synthetic.main.fragment_edit_image.*
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class EditPhotoTest {

    private val mockServer = MockServer()
    private val imageName = "chat.jpg"

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @get:Rule
    var mActivityTestRule = IntentsTestRule(PhotoEditActivity::class.java)

    @get:Rule
    var mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Before
    fun before() {
        mockServer.start()
        val baseUrl = mockServer.getUrl()
        val preferences = InstrumentationRegistry.getInstrumentation()
            .targetContext.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", baseUrl.toString()).apply()

        // Launch PhotoEditActivity
        ActivityScenario.launch(MainActivity::class.java).onActivity { a ->
            a.findViewById<TabLayout>(R.id.tabs).getTabAt(2)?.select()
        }
        Thread.sleep(1000)
        Espresso.onView(withId(R.id.edit_picture_button)).perform(click())
    }

    private fun selectTabAtPosition(tabIndex: Int): ViewAction {
        return object : ViewAction {
            override fun getDescription() = "with tab at index $tabIndex"

            override fun getConstraints() = allOf(isDisplayed(), isAssignableFrom(TabLayout::class.java))

            override fun perform(uiController: UiController, view: View) {
                val tabLayout = view as TabLayout
                val tabAtIndex: TabLayout.Tab = tabLayout.getTabAt(tabIndex)
                    ?: throw PerformException.Builder()
                        .withCause(Throwable("No tab at index $tabIndex"))
                        .build()

                tabAtIndex.select()
            }
        }
    }

    private fun setProgress(progress: Int): ViewAction? {
        return object : ViewAction {
            override fun getDescription() =  "Set the progress on a SeekBar"

            override fun getConstraints() = isAssignableFrom(SeekBar::class.java)

            override fun perform(uiController: UiController, view: View) {
                val seekBar = view as SeekBar
                seekBar.progress = progress
            }
        }
    }

    private fun swipeSlowLeft(): ViewAction? {
        return GeneralSwipeAction(
            Swipe.SLOW, GeneralLocation.CENTER_RIGHT,
            GeneralLocation.CENTER_LEFT, Press.FINGER
        )
    }

    @Test
    fun PhotoEditActivityHasAnImagePreview() {
        Espresso.onView(withId(R.id.image_preview)).check(ViewAssertions.matches(isDisplayed()))
    }

    /*
    @Test
    fun FiltersIsSwipeableAndClickeable() {
        Espresso.onView(withId(R.id.viewPager)).perform(swipeSlowLeft())
        Thread.sleep(1000)
        Espresso.onView(withId(R.id.tabs)).perform(selectTabAtPosition(1))
    }*/

    @Test
    fun BirghtnessSaturationContrastTest() {
        Espresso.onView(withId(R.id.tabs)).perform(selectTabAtPosition(1))

        Thread.sleep(1000)

        val change = 5
        Espresso.onView(withId(R.id.seekbar_brightness)).perform(setProgress(change))
        Espresso.onView(withId(R.id.seekbar_contrast)).perform(setProgress(change))
        Espresso.onView(withId(R.id.seekbar_saturation)).perform(setProgress(change))

        Assert.assertEquals(change, mActivityTestRule.activity.seekbar_brightness.progress)
        Assert.assertEquals(change, mActivityTestRule.activity.seekbar_contrast.progress)
        Assert.assertEquals(change, mActivityTestRule.activity.seekbar_saturation.progress)
    }

    @Test
    fun SaveButtonSavesToGallery() {

    }

    /*
    @Test
    fun buttonReturnGoesToTheLastIntent() {
        Espresso.onView(withId(R.id.home)).perform(click())

        Espresso.onView(withId(R.id.edit_picture_button)).check(ViewAssertions.matches(isDisplayed()))
    }
    */
}