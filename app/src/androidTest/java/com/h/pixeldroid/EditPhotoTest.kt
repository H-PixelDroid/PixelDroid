package com.h.pixeldroid

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.SeekBar
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.android.material.tabs.TabLayout
import com.h.pixeldroid.adapters.ThumbnailAdapter
import com.h.pixeldroid.testUtility.CustomMatchers
import junit.framework.Assert.assertTrue
import kotlinx.android.synthetic.main.fragment_edit_image.*
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import java.io.File
import java.net.URI

@RunWith(AndroidJUnit4::class)
class EditPhotoTest {

    private lateinit var activity: PhotoEditActivity
    private lateinit var activityScenario: ActivityScenario<PhotoEditActivity>

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @get:Rule
    var mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun File.writeBitmap(bitmap: Bitmap) {
        outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
            out.flush()
        }
    }

    @Before
    fun before() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var uri: Uri = "".toUri()
        val scenario = ActivityScenario.launch(ProfileActivity::class.java)
        scenario.onActivity {
            val image = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
            image.eraseColor(Color.GREEN)
            val folder =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            if (!folder.exists()) {
                folder.mkdir()
            }
            val file = File.createTempFile("temp_img", ".png", folder)
            file.writeBitmap(image)
            uri = file.toUri()
        }
        val intent = Intent(context, PhotoEditActivity::class.java).putExtra("picture_uri", uri)

        activityScenario = ActivityScenario.launch<PhotoEditActivity>(intent).onActivity{a -> activity = a}

        Thread.sleep(1000)
    }

    private fun selectTabAtPosition(tabIndex: Int): ViewAction {
        return object : ViewAction {
            override fun getDescription() = "with tab at index $tabIndex"

            override fun getConstraints() =
                allOf(isDisplayed(), isAssignableFrom(TabLayout::class.java))

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

    @Test
    fun PhotoEditActivityHasAnImagePreview() {
        Espresso.onView(withId(R.id.image_preview)).check(matches(isDisplayed()))
    }

    @Test
    fun FiltersIsSwipeableAndClickeable() {
        Espresso.onView(withId(R.id.recycler_view))
            .perform(actionOnItemAtPosition<ThumbnailAdapter.MyViewHolder>(1, CustomMatchers.clickChildViewWithId(R.id.thumbnail)))
        Thread.sleep(1000)
        Espresso.onView(withId(R.id.recycler_view))
            .perform(actionOnItemAtPosition<ThumbnailAdapter.MyViewHolder>(1, CustomMatchers.slowSwipeLeft(false)))
        Thread.sleep(1000)
        Espresso.onView(withId(R.id.recycler_view))
            .perform(actionOnItemAtPosition<ThumbnailAdapter.MyViewHolder>(5, CustomMatchers.clickChildViewWithId(R.id.thumbnail)))
        Espresso.onView(withId(R.id.image_preview)).check(matches(isDisplayed()))
    }

    @Test
    fun BrightnessSaturationContrastTest() {
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
    fun SaveButton() {
        Espresso.onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        Espresso.onView(withId(R.id.action_save)).perform(click())
        Espresso.onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.save_image_success)))
    }

    @Test
    fun backButton() {
        Espresso.onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        Espresso.onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click());
        assertTrue(activityScenario.state == Lifecycle.State.DESTROYED)    }

    @Test
    fun buttonUploadLaunchNewPostActivity() {
        Espresso.onView(withId(R.id.action_upload)).perform(click())
        Thread.sleep(1000)
        Espresso.onView(withId(R.id.post_creation_picture_frame)).check(matches(isDisplayed()))
    }

    @Test
    fun modifiedUploadLaunchesNewPostActivity() {
        Espresso.onView(withId(R.id.recycler_view))
            .perform(actionOnItemAtPosition<ThumbnailAdapter.MyViewHolder>(2, CustomMatchers.clickChildViewWithId(R.id.thumbnail)))
        Thread.sleep(1000)

        Espresso.onView(withId(R.id.tabs)).perform(selectTabAtPosition(1))
        Espresso.onView(withId(R.id.seekbar_brightness)).perform(setProgress(5))
        Thread.sleep(1000)

        Espresso.onView(withId(R.id.action_upload)).perform(click())
        Thread.sleep(1000)


        Espresso.onView(withId(R.id.post_creation_picture_frame)).check(matches(isDisplayed()))
    }

    @Test
    fun croppingIsPossible() {
        Espresso.onView(withId(R.id.cropImageButton)).perform(click())
        Thread.sleep(1000)
        Espresso.onView(withId(R.id.menu_crop)).perform(click())
        Espresso.onView(withId(R.id.image_preview)).check(matches(isDisplayed()))
    }

    @Test
    fun alreadyUploadingDialog() {
        activityScenario.onActivity { a -> a.saving = true }
        Espresso.onView(withId(R.id.action_upload)).perform(click())
        Thread.sleep(1000)
        Espresso.onView(withText(R.string.busy_dialog_text)).check(matches(isDisplayed()))
    }
}