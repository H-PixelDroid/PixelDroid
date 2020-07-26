package com.h.pixeldroid


import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.tabs.TabLayout
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.InstanceDatabaseEntity
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.fragments.feeds.postFeeds.PostViewHolder
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.clickChildViewWithId
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.first
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.getText
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.second
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.slowSwipeUp
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.typeTextInViewWithId
import com.h.pixeldroid.testUtility.MockServer
import com.h.pixeldroid.testUtility.initDB
import com.h.pixeldroid.utils.DBUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class HomeFeedTest {

    private val mockServer = MockServer()
    private lateinit var activityScenario: ActivityScenario<MainActivity>
    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @Before
    fun before(){
        mockServer.start()
        val baseUrl = mockServer.getUrl()
        context = ApplicationProvider.getApplicationContext()
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
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun clickingTabOnAlbumShowsNextPhoto() {
        activityScenario.onActivity {
            a -> run {
                //Wait for the feed to load
                Thread.sleep(1000)
                a.findViewById<TextView>(R.id.sensitiveWarning).performClick()
                Thread.sleep(1000)
                //Pick the second photo
                a.findViewById<TabLayout>(R.id.postTabs).getTabAt(1)?.select()
            }
        }
        onView(first(withId(R.id.postTabs))).check(matches(isDisplayed()))
    }

    @Test
    fun clickingLikeButtonWorks() {
        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<PostViewHolder>(0, clickChildViewWithId(R.id.liker))
        )
        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<PostViewHolder>(0, clickChildViewWithId(R.id.liker))
        )
        onView(first(withId(R.id.nlikes)))
            .check(matches(withText(getText(first(withId(R.id.nlikes))))))
    }

    @Test
    fun clickingLikeButtonFails() {
        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<PostViewHolder>(2, clickChildViewWithId(R.id.liker))
        )
        onView((withId(R.id.list))).check(matches(isDisplayed()))
    }

    @Test
    fun clickingUsernameOpensProfile() {
        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<PostViewHolder>(0, clickChildViewWithId(R.id.username))
        )
        onView(withId(R.id.accountNameTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun clickingProfilePicOpensProfile() {
        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<PostViewHolder>(0, clickChildViewWithId(R.id.profilePic))
        )
        onView(withId(R.id.accountNameTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun clickingReblogButtonWorks() {
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.reblogger)))
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.reblogger)))
        onView(first(withId(R.id.nshares)))
            .check(matches(withText(getText(first(withId(R.id.nshares))))))
    }

    @Test
    fun clickingMentionOpensProfile() {
        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<PostViewHolder>(0, clickChildViewWithId(R.id.description))
        )
        onView(first(withId(R.id.username))).check(matches(isDisplayed()))
    }

    @Test
    fun clickingHashTagsWorks() {
        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<PostViewHolder>(1, clickChildViewWithId(R.id.description))
        )
        onView(withId(R.id.list)).check(matches(isDisplayed()))
    }


    @Test
    fun clickingCommentButtonOpensCommentSection() {
        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<PostViewHolder>(0, clickChildViewWithId(R.id.commenter))
        )
        onView(first(withId(R.id.commentIn)))
            .check(matches(hasDescendant(withId(R.id.editComment))))
    }

    @Test
    fun clickingViewCommentShowsTheComments() {
        //Open the comment section
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.ViewComments)))
        Thread.sleep(1000)
        onView(first(withId(R.id.commentContainer)))
            .check(matches(hasDescendant(withId(R.id.comment))))
    }

    @Test
    fun clickingViewCommentFails() {
        //Open the comment section
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (2, clickChildViewWithId(R.id.ViewComments)))
        Thread.sleep(1000)
        onView(withId(R.id.list)).check(matches(isDisplayed()))
    }

    @Test
    fun postingACommentWorks() {
        //Open the comment section
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.commenter)))

        onView(withId(R.id.list)).perform(slowSwipeUp(false))
        Thread.sleep(1000)

        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, typeTextInViewWithId(R.id.editComment, "test")))
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.submitComment)))

        Thread.sleep(1000)
        onView(first(withId(R.id.commentContainer)))
            .check(matches(hasDescendant(withId(R.id.comment))))
    }

    @Test
    fun performClickOnSensitiveWarning() {
        onView(withId(R.id.list)).perform(scrollToPosition<PostViewHolder>(1))
        Thread.sleep(1000)

        onView(second(withId(R.id.sensitiveWarning))).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        Thread.sleep(1000)

        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (1, clickChildViewWithId(R.id.sensitiveWarning)))
        Thread.sleep(1000)

        onView(second(withId(R.id.sensitiveWarning))).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun performClickOnSensitiveWarningTabs() {
        onView(withId(R.id.list)).perform(scrollToPosition<PostViewHolder>(0))
        Thread.sleep(1000)

        onView(first(withId(R.id.sensitiveWarning))).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        Thread.sleep(1000)

        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.sensitiveWarning)))
        Thread.sleep(1000)

        onView(first(withId(R.id.sensitiveWarning))).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun doubleTapLikerWorks() {
        //Get initial like count
        val likes = getText(first(withId(R.id.nlikes)))
        val nlikes = likes!!.split(" ")[0].toInt()

        //Remove sensitive media warning
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.sensitiveWarning)))
        Thread.sleep(100)

        //Like the post
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.postPicture)))
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.postPicture)))
        //...
        Thread.sleep(100)

        //Profit
        onView(first(withId(R.id.nlikes))).check(matches((withText("${nlikes + 1} Likes"))))
    }

    @Test
    fun goOfflineShowsPosts() {
        // show some posts to populate DB
        onView(withId(R.id.main_activity_main_linear_layout)).perform(swipeUp())
        Thread.sleep(1000)
        onView(withId(R.id.main_activity_main_linear_layout)).perform(swipeUp())
        Thread.sleep(1000)
        // offline section
        LoginActivityOfflineTest.switchAirplaneMode()
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.offline_feed_recyclerview)).check(matches(isDisplayed()))
        // back online
        LoginActivityOfflineTest.switchAirplaneMode()
    }
}

