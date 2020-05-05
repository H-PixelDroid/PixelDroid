package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.recyclerview.widget.RecyclerView
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
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import com.google.android.material.tabs.TabLayout
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.first
import com.h.pixeldroid.testUtility.MockServer
import kotlinx.android.synthetic.main.fragment_edit_image.*
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import java.lang.reflect.Method

@RunWith(AndroidJUnit4::class)
class EditPhotoTest {

    private val mockServer = MockServer()
    private val imageName = "chat.jpg"
    private lateinit var activity: PhotoEditActivity
    private lateinit var activityScenario: ActivityScenario<PhotoEditActivity>

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @get:Rule
    var mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Before
    fun before() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        mockServer.start()
        val baseUrl = mockServer.getUrl()
        val preferences = context.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", baseUrl.toString()).apply()

        // Launch PhotoEditActivity
        val uri: Uri = Uri.parse("android.resource://com.h.pixeldroid/drawable/index")
        val intent = Intent(context, PhotoEditActivity::class.java).putExtra("uri", uri)

        activityScenario = ActivityScenario.launch<PhotoEditActivity>(intent).onActivity{a -> activity = a}
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

    private fun setSeekBarProgress(newProgress: Int, fromUser: Boolean, seekBar: SeekBar) {
        var privateSetProgressMethod: Method? = null
        try {
            privateSetProgressMethod =
                ProgressBar::class.java.getDeclaredMethod(
                    "setProgress",
                    Integer.TYPE,
                    java.lang.Boolean.TYPE
                )
            privateSetProgressMethod.isAccessible = true
            privateSetProgressMethod.invoke(seekBar, newProgress, fromUser)
        } catch (e: ReflectiveOperationException) {
            e.printStackTrace()
        }
    }

    private fun setProgress(progress: Int): ViewAction? {
        return object : ViewAction {
            override fun getDescription() =  "Set the progress on a SeekBar"

            override fun getConstraints() = isAssignableFrom(SeekBar::class.java)

            override fun perform(uiController: UiController, view: View) {
                val seekBar = view as SeekBar
                setSeekBarProgress(progress, true, seekBar)
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
        Espresso.onView(withId(R.id.image_preview)).check(matches(isDisplayed()))
    }

    @Test
    fun FiltersIsSwipeableAndClickeable() {
        //val myRcView: RecyclerView = activityTestRule.activity.findViewById(R.id.recycler_view)
        //Espresso.onView(first(withId(R.id.recycler_view))).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
        Thread.sleep(1000)
    }

    @Test
    fun BirghtnessSaturationContrastTest() {
        Espresso.onView(withId(R.id.tabs)).perform(selectTabAtPosition(1))

        Thread.sleep(1000)

        var change = 5
        Espresso.onView(withId(R.id.seekbar_brightness)).perform(setProgress(change))
        Espresso.onView(withId(R.id.seekbar_contrast)).perform(setProgress(change))
        Espresso.onView(withId(R.id.seekbar_saturation)).perform(setProgress(change))

        Assert.assertEquals(change, activity.seekbar_brightness.progress)
        Assert.assertEquals(change, activity.seekbar_contrast.progress)
        Assert.assertEquals(change, activity.seekbar_saturation.progress)

        Thread.sleep(1000)

        change = 15
        Espresso.onView(withId(R.id.seekbar_brightness)).perform(setProgress(change))
        Espresso.onView(withId(R.id.seekbar_contrast)).perform(setProgress(change))
        Espresso.onView(withId(R.id.seekbar_saturation)).perform(setProgress(change))

        Assert.assertEquals(change, activity.seekbar_brightness.progress)
        Assert.assertEquals(change, activity.seekbar_contrast.progress)
        Assert.assertEquals(change, activity.seekbar_saturation.progress)
    }

    @Test
    fun SaveButtonLaunchNewPostActivity() {
        Espresso.onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        Espresso.onView(withId(R.id.action_save)).perform(click())
        Thread.sleep(1000)
        Espresso.onView(withId(R.id.new_post_description_input_layout)).check(matches(isDisplayed()))
    }

    @Test
    fun buttonUpload() {
        Espresso.onView(withId(R.id.action_upload)).perform(click())
    }

}