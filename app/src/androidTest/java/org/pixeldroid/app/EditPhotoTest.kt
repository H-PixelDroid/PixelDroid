package org.pixeldroid.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.view.View
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
import org.pixeldroid.app.postCreation.photoEdit.PhotoEditActivity
import org.pixeldroid.app.postCreation.photoEdit.ThumbnailAdapter
import org.pixeldroid.app.settings.AboutActivity
import org.pixeldroid.app.testUtility.clearData
import org.pixeldroid.app.testUtility.clickChildViewWithId
import org.pixeldroid.app.testUtility.slowSwipeLeft
import org.pixeldroid.app.testUtility.waitForView
import org.hamcrest.CoreMatchers.allOf
import org.junit.*
import org.junit.Assert.assertTrue
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EditPhotoTest {

    private lateinit var activity: PhotoEditActivity
    private lateinit var activityScenario: ActivityScenario<PhotoEditActivity>
    private lateinit var context: Context


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
        context = InstrumentationRegistry.getInstrumentation().targetContext
        var uri: Uri = "".toUri()
        val scenario = ActivityScenario.launch(AboutActivity::class.java)
        scenario.onActivity {
            val image = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
            image.eraseColor(Color.GREEN)

            val file = File.createTempFile("temp_img", ".png")
            file.writeBitmap(image)
            uri = file.toUri()
        }
        val intent = Intent(context, PhotoEditActivity::class.java)
            .putExtra(PhotoEditActivity.PICTURE_URI, uri)
            .putExtra(PhotoEditActivity.PICTURE_POSITION, 0)

        activityScenario = ActivityScenario.launch<PhotoEditActivity>(intent).onActivity{a -> activity = a}

        waitForView(R.id.coordinator_edit)
    }

    @After
    fun after() {
        clearData()
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
        waitForView(R.id.thumbnail)
        Espresso.onView(withId(R.id.recycler_view))
            .perform(actionOnItemAtPosition<ThumbnailAdapter.MyViewHolder>(1, clickChildViewWithId(R.id.thumbnail)))
        Thread.sleep(1000)
        Espresso.onView(withId(R.id.recycler_view))
            .perform(actionOnItemAtPosition<ThumbnailAdapter.MyViewHolder>(1, slowSwipeLeft(false)))
        Thread.sleep(1000)
        Espresso.onView(withId(R.id.recycler_view))
            .perform(actionOnItemAtPosition<ThumbnailAdapter.MyViewHolder>(5, clickChildViewWithId(R.id.thumbnail)))
        Espresso.onView(withId(R.id.image_preview)).check(matches(isDisplayed()))
    }

    @Test
    fun BrightnessSaturationContrastTest() {
        Espresso.onView(withId(R.id.tabs)).perform(selectTabAtPosition(1))

        waitForView(R.id.seekbar_brightness)

        var change = 5
        Espresso.onView(withId(R.id.seekbar_brightness)).perform(setProgress(change))
        Espresso.onView(withId(R.id.seekbar_contrast)).perform(setProgress(change))
        Espresso.onView(withId(R.id.seekbar_saturation)).perform(setProgress(change))

        Assert.assertEquals(change, activity.findViewById<SeekBar>(R.id.seekbar_brightness).progress)
        Assert.assertEquals(change, activity.findViewById<SeekBar>(R.id.seekbar_contrast).progress)
        Assert.assertEquals(change, activity.findViewById<SeekBar>(R.id.seekbar_saturation).progress)

        Thread.sleep(1000)

        change = 15
        Espresso.onView(withId(R.id.seekbar_brightness)).perform(setProgress(change))
        Espresso.onView(withId(R.id.seekbar_contrast)).perform(setProgress(change))
        Espresso.onView(withId(R.id.seekbar_saturation)).perform(setProgress(change))

        Assert.assertEquals(change, activity.findViewById<SeekBar>(R.id.seekbar_brightness).progress)
        Assert.assertEquals(change, activity.findViewById<SeekBar>(R.id.seekbar_contrast).progress)
        Assert.assertEquals(change, activity.findViewById<SeekBar>(R.id.seekbar_saturation).progress)
    }

    @Test
    fun saveButton() {
        // The save button saves the edits and goes back to the post creation activity.
        Espresso.onView(withId(R.id.action_save)).perform(click())
        Thread.sleep(1000)
        assertTrue(activityScenario.state == Lifecycle.State.DESTROYED)
    }

    @Test
    fun backButton() {
        Espresso.onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click())
        Thread.sleep(1000)
        assertTrue(activityScenario.state == Lifecycle.State.DESTROYED)
    }

    @Test
    fun croppingIsPossible() {
        Espresso.onView(withId(R.id.cropImageButton)).perform(click())

        waitForView(R.id.menu_crop)

        Espresso.onView(withId(R.id.menu_crop)).perform(click())
        Espresso.onView(withId(R.id.image_preview)).check(matches(isDisplayed()))
    }

    @Test
    fun alreadyUploadingDialog() {
        activityScenario.onActivity { a -> a.saving = true }
        Espresso.onView(withId(R.id.action_save)).perform(click())
        Thread.sleep(1000)
        Espresso.onView(withText(R.string.busy_dialog_text)).check(matches(isDisplayed()))
    }
}