package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.view.View.VISIBLE
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.InstanceDatabaseEntity
import com.h.pixeldroid.db.UserDatabaseEntity
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
        val intent = Intent(context, PostCreationActivity::class.java).putExtra("picture_uri", uri)
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
}