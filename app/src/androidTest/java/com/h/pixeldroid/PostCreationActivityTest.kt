package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostCreationActivityTest {

    private val mockServer = MockServer()
    private lateinit var db: AppDatabase


    @get:Rule
    val globalTimeout: Timeout = Timeout.seconds(30)

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
        val uri: Uri = Uri.parse("android.resource://com.h.pixeldroid/drawable/index")
        val intent = Intent(context, PostCreationActivity::class.java)
            .putExtra("picture_uri", uri)

        ActivityScenario.launch<PostCreationActivity>(intent)
    }

    @Test
    fun createPost() {
        onView(withId(R.id.post_creation_send_button)).perform(click())
        // should send on main activity
        onView(withId(R.id.main_activity_main_linear_layout)).check(matches(isDisplayed()))
    }
}