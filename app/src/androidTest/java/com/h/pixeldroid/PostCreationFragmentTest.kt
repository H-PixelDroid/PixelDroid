package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.entities.InstanceDatabaseEntity
import com.h.pixeldroid.db.entities.UserDatabaseEntity
import com.h.pixeldroid.testUtility.MockServer
import com.h.pixeldroid.testUtility.clearData
import com.h.pixeldroid.testUtility.initDB
import kotlinx.android.synthetic.main.activity_main.*
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostCreationFragmentTest {

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(30)
    @get:Rule
    var runtimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    @get:Rule
    var intentsTestRule: IntentsTestRule<MainActivity> =
        IntentsTestRule(MainActivity::class.java)

    @Before
    fun setup() {
        onView(withId(R.id.drawer_layout))
            .perform(swipeLeft())
            .perform(swipeLeft())
        Thread.sleep(300)
    }

    // upload intent
    @Test
    fun uploadButtonLaunchesGalleryIntent() {
        val expectedIntent: Matcher<Intent> = hasAction(Intent.ACTION_CHOOSER)
        intending(expectedIntent)
        onView(withId(R.id.photo_view_button)).perform(click())
        Thread.sleep(1000)
        intended(expectedIntent)
    }
}

@RunWith(AndroidJUnit4::class)
class PostFragmentUITests {
    private lateinit var mockServer: MockServer
    private lateinit var context: Context

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(30)
    private lateinit var db: AppDatabase


    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        mockServer = MockServer()
        mockServer.start()
        val baseUrl = mockServer.getUrl()
        db = initDB(context)
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
        Thread.sleep(300)
    }

    @After
    fun after() {
        clearData()
        mockServer.stop()
    }

    @Test
    fun newPostUiTest() {
        ActivityScenario.launch(MainActivity::class.java).onActivity {
                a -> a.tabs.getTabAt(2)!!.select()
        }
        Thread.sleep(1500)
        onView(withId(R.id.photo_view_button)).check(matches(isDisplayed()))
        onView(withId(R.id.camera_capture_button)).check(matches(isDisplayed()))
    }
}