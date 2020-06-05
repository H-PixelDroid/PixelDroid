package com.h.pixeldroid

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.util.Log
import android.view.View.VISIBLE
import androidx.core.net.toUri
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
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.InstanceDatabaseEntity
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.testUtility.CustomMatchers
import com.h.pixeldroid.testUtility.MockServer
import com.h.pixeldroid.utils.DBUtils
import kotlinx.android.synthetic.main.activity_post_creation.*
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PostCreationActivityTest {

    private var testScenario: ActivityScenario<PostCreationActivity>? = null
    private val mockServer = MockServer()
    private lateinit var db: AppDatabase


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
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        mockServer.start()
        val baseUrl = mockServer.getUrl()
        db = DBUtils.initDB(context)
        db.clearAllTables()
        db.instanceDao().insertInstance(
            InstanceDatabaseEntity(
                uri = baseUrl.toString(),
                title = "PixelTest"
            )
        )

        db.userDao().insertUser(
            UserDatabaseEntity(
                user_id = "123",
                instance_uri = baseUrl.toString(),
                username = "Testi",
                display_name = "Testi Testo",
                avatar_static = "some_avatar_url",
                isActive = true,
                accessToken = "token"
            )
        )
        db.close()

        var uri1: String = ""
        var uri2: String = ""
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            val image1 = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
            image1.eraseColor(Color.GREEN)
            val image2 = Bitmap.createBitmap(270, 270, Bitmap.Config.ARGB_8888)
            image2.eraseColor(Color.RED)
            val folder =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            if (!folder.exists()) {
                folder.mkdir()
            }
            val file1 = File.createTempFile("temp_img1", ".png", folder)
            val file2 = File.createTempFile("temp_img2", ".png", folder)
            file1.writeBitmap(image1)
            uri1 = file1.toUri().toString()
            file2.writeBitmap(image2)
            uri2 = file2.toUri().toString()
            Log.d("test", uri1+"\n"+uri2)
        }
        val intent = Intent(context, PostCreationActivity::class.java).putExtra("pictures_uri", arrayListOf(uri1, uri2))
        testScenario = ActivityScenario.launch(intent)
    }

    @Test
    fun createPost() {
        onView(withId(R.id.post_creation_send_button)).perform(click())
        // should send on main activity
        onView(withId(R.id.main_activity_main_linear_layout)).check(matches(isDisplayed()))
    }

    @Test
    fun errorShown() {
        testScenario!!.onActivity { a -> a.upload_error.visibility = VISIBLE }
        onView(withId(R.id.retry_upload_button)).perform(click())
        // should send on main activity
        onView(withId(R.id.retry_upload_button)).check(matches(not(isDisplayed())))
    }

    @Test
    fun editImage() {
        onView(withId(R.id.image_grid)).perform(
            RecyclerViewActions.actionOnItemAtPosition<PostCreationActivity.PostCreationAdapter.ViewHolder>(
                0,
                CustomMatchers.clickChildViewWithId(R.id.galleryImage)
            )
        )
        Thread.sleep(1000)
        onView(withId(R.id.action_upload)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.image_grid)).check(matches(isDisplayed()))
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
    }
}