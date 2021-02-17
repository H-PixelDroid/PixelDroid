package com.h.pixeldroid

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.View.VISIBLE
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.h.pixeldroid.postCreation.PostCreationActivity
import com.h.pixeldroid.postCreation.photoEdit.ThumbnailAdapter
import com.h.pixeldroid.settings.AboutActivity
import com.h.pixeldroid.testUtility.*
import com.h.pixeldroid.utils.db.AppDatabase
import org.hamcrest.CoreMatchers.not
import org.junit.*
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PostCreationActivityTest {

    private var testScenario: ActivityScenario<PostCreationActivity>? = null
    private lateinit var db: AppDatabase
    private lateinit var context: Context


    @get:Rule
    val globalTimeout: Timeout = Timeout.seconds(30)

    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun File.writeBitmap(bitmap: Bitmap) {
        outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
            out.flush()
        }
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = initDB(context)
        db.clearAllTables()
        db.instanceDao().insertInstance(
            testiTestoInstance
        )

        db.userDao().insertUser(
            testiTesto
        )
        db.close()

        var uri1: Uri? = null
        var uri2: Uri? = null
        val scenario = ActivityScenario.launch(AboutActivity::class.java)
        scenario.onActivity {
            val image1 = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
            image1.eraseColor(Color.GREEN)
            val image2 = Bitmap.createBitmap(270, 270, Bitmap.Config.ARGB_8888)
            image2.eraseColor(Color.RED)

            val file1 = File.createTempFile("temp_img1", ".png")
            val file2 = File.createTempFile("temp_img2", ".png")
            file1.writeBitmap(image1)
            uri1 = file1.toUri()
            file2.writeBitmap(image2)
            uri2 = file2.toUri()
        }
        val intent = Intent(context, PostCreationActivity::class.java)

        intent.clipData = ClipData("", emptyArray(), ClipData.Item(uri1))
        intent.clipData!!.addItem(ClipData.Item(uri2))

        testScenario = ActivityScenario.launch(intent)
    }

    @After
    fun after() {
        clearData()
    }

    @Test
    @Ignore("Annoying to deal with and also sometimes the intent is not working as it should")
    fun createPost() {
        onView(withId(R.id.post_creation_send_button)).perform(click())
        // should send on main activity
        Thread.sleep(3000)
        onView(withId(R.id.list)).check(matches(isDisplayed()))
    }
/*
    @Test
    fun errorShown() {
        testScenario!!.onActivity { a -> a.upload_error.visibility = VISIBLE }
        onView(withId(R.id.retry_upload_button)).perform(click())
        // should send on main activity
        onView(withId(R.id.retry_upload_button)).check(matches(not(isDisplayed())))
    }

    @Test
    fun editImage() {
        Thread.sleep(1000)

        onView(withId(R.id.image_grid)).perform(
            RecyclerViewActions.actionOnItemAtPosition<PostCreationActivity.PostCreationAdapter.ViewHolder>(
                0,
                CustomMatchers.clickChildViewWithId(R.id.galleryImage)
            )
        )
        Thread.sleep(1000)

        onView(withId(R.id.recycler_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<ThumbnailAdapter.MyViewHolder>(
                    2,
                    CustomMatchers.clickChildViewWithId(R.id.thumbnail)
                )
            )
        Thread.sleep(1000)
        onView(withId(R.id.action_save)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.carousel)).check(matches(isDisplayed()))
    }

    @Test
    fun cancelEdit() {
        onView(withId(R.id.image_grid)).perform(
            RecyclerViewActions.actionOnItemAtPosition<PostCreationActivity.PostCreationAdapter.ViewHolder>(
                0,
                CustomMatchers.clickChildViewWithId(R.id.galleryImage)
            )
        )
        Thread.sleep(1000)
    }*/
}