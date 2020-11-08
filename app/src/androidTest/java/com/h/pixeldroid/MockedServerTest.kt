package com.h.pixeldroid


import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrix
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.tabs.TabLayout
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.InstanceDatabaseEntity
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.fragments.feeds.postFeeds.PostViewHolder
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.clickChildViewWithId
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.first
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.second
import com.h.pixeldroid.testUtility.MockServer
import com.h.pixeldroid.testUtility.clearData
import com.h.pixeldroid.testUtility.initDB
import com.h.pixeldroid.utils.PostUtils.Companion.censorColorMatrix
import com.h.pixeldroid.utils.PostUtils.Companion.uncensorColorMatrix
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
/*

@RunWith(AndroidJUnit4::class)
class MockedServerTest {

    private lateinit var mockServer: MockServer
    private lateinit var activityScenario: ActivityScenario<MainActivity>
    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun before(){
        mockServer = MockServer()
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
    @After
    fun after() {
        clearData()
        mockServer.stop()
    }
/*
    @Test
    fun searchPosts() {
        activityScenario.onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(1)?.select()
        }

        Thread.sleep(1000)
        onView(withId(R.id.searchEditText)).perform(ViewActions.replaceText("caturday"), ViewActions.closeSoftKeyboard())

        onView(withId(R.id.searchButton)).perform(click())
        Thread.sleep(3000)
        onView(first(withId(R.id.username))).check(matches(withText("memo")))
    }

    @Test
    fun searchHashtags() {
        activityScenario.onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(1)?.select()
        }

        Thread.sleep(1000)
        onView(withId(R.id.searchEditText)).perform(ViewActions.replaceText("#caturday"), ViewActions.closeSoftKeyboard())

        onView(withId(R.id.searchButton)).perform(click())
        Thread.sleep(3000)
        onView(first(withId(R.id.tag_name))).check(matches(withText("#caturday")))

    }

 */
    @Test
    fun openDiscoverPost(){
        activityScenario.onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(1)?.select()
        }
        Thread.sleep(1000)
        onView(first(withId(R.id.postPreview))).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.username)).check(matches(withText("Arthur")))

    }
/*
    @Test
    fun searchAccounts() {
        activityScenario.onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(1)?.select()
        }

        Thread.sleep(1000)
        onView(withId(R.id.searchEditText)).perform(ViewActions.replaceText("@dansup"), ViewActions.closeSoftKeyboard())

        onView(withId(R.id.searchButton)).perform(click())
        Thread.sleep(3000)
        onView(first(withId(R.id.account_entry_username))).check(matches(withText("dansup")))

    }

 */

    @Test
    fun clickFollowButton() {
        //Get initial like count
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.username)))

        Thread.sleep(1000)

        // Unfollow
        onView(withId(R.id.followButton)).perform((click()))
        Thread.sleep(1000)
        onView(withId(R.id.followButton)).check(matches(withText("Follow")))

        // Follow
        onView(withId(R.id.followButton)).perform((click()))
        Thread.sleep(1000)
        onView(withId(R.id.followButton)).check(matches(withText("Unfollow")))
    }

    @Test
    fun clickOtherUserFollowers() {
        //Get initial like count
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.username)))

        Thread.sleep(1000)

        // Open followers list
        onView(withId(R.id.nbFollowersTextView)).perform((click()))
        Thread.sleep(1000)
        // Open follower's profile
        onView(withText("ete2")).perform((click()))
        Thread.sleep(1000)

        onView(withId(R.id.accountNameTextView)).check(matches(withText("Christian")))
    }

    @Test
    fun testNotificationsList() {
        activityScenario.onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }

        Thread.sleep(1000)
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())
        onView(withText("Dobios liked your post")).check(matches(withId(R.id.notification_type)))
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeDown())

        Thread.sleep(1000)
        onView(withText("Dobios followed you")).check(matches(withId(R.id.notification_type)))

    }
    @Test
    fun clickNotification() {
        activityScenario.onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }

        Thread.sleep(1000)
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())

        Thread.sleep(1000)
        onView(withText("Dobios liked your post")).perform(click())

        Thread.sleep(1000)
        onView(withText("6 Likes")).check(matches(withId(R.id.nlikes)))
    }

    @Test
    fun clickNotificationUser() {
        activityScenario.onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }
        Thread.sleep(1000)

        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())
        Thread.sleep(1000)

        onView(withText("Dobios followed you")).perform(click())
        Thread.sleep(1000)
        onView(second(withText("Andrew Dobis"))).check(matches(withId(R.id.accountNameTextView)))
    }

    @Test
    fun clickNotificationPost() {
        activityScenario.onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }
        Thread.sleep(1000)

        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())
        Thread.sleep(1000)

        onView(withText("Dobios liked your post")).perform(click())
        Thread.sleep(1000)

        onView(withId(R.id.username)).perform(click())
        Thread.sleep(1000)
        onView(second(withText("Dante"))).check(matches(withId(R.id.accountNameTextView)))
    }

    @Test
    fun clickNotificationRePost() {
        activityScenario.onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }
        Thread.sleep(1000)

        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())
        Thread.sleep(1000)

        onView(withText("Clement shared your post")).perform(click())
        Thread.sleep(1000)

        onView(first(withText("Andrea"))).check(matches(withId(R.id.username)))
    }

    @Test
    fun swipingRightStopsAtHomepage() {
        activityScenario.onActivity {
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(4)?.select()
        } // go to the last tab

        Thread.sleep(1000)
        onView(withId(R.id.main_activity_main_linear_layout))
            .perform(ViewActions.swipeRight()) // notifications
            .perform(ViewActions.swipeRight()) // camera
            .perform(ViewActions.swipeRight()) // search
            .perform(ViewActions.swipeRight()) // homepage
            .perform(ViewActions.swipeRight()) // should stop at homepage
        onView(withId(R.id.list)).check(matches(isDisplayed()))
    }

    @Test
    fun swipingLeftStopsAtPublicTimeline() {
        activityScenario.onActivity {
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(0)?.select()
        }

        Thread.sleep(1000)
        onView(withId(R.id.main_activity_main_linear_layout))
            .perform(ViewActions.swipeLeft()) // notifications
            .perform(ViewActions.swipeLeft()) // camera
            .perform(ViewActions.swipeLeft()) // search
            .perform(ViewActions.swipeLeft()) // homepage
            .perform(ViewActions.swipeLeft()) // should stop at homepage
        onView(withId(R.id.list)).check(matches(isDisplayed()))
    }

    @Test
    fun swipingPublicTimelineWorks() {
        activityScenario.onActivity {
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(4)?.select()
        } // go to the last tab

        Thread.sleep(1000)
        onView(withId(R.id.view_pager))
            .perform(ViewActions.swipeRight()) // notifications
            .perform(ViewActions.swipeRight()) // camera
            .perform(ViewActions.swipeRight()) // search
            .perform(ViewActions.swipeRight()) // homepage
            .perform(ViewActions.swipeRight()) // should stop at homepage

        onView(withId(R.id.list)).check(matches(isDisplayed()))

        activityScenario.onActivity {
                a -> assert(a.findViewById<TabLayout>(R.id.tabs).getTabAt(0)?.isSelected ?: false)
        }
    }

    @Test
    fun censorMatrices() {
        val array: FloatArray = floatArrayOf(
            0f, 0f, 0f, 0f, 0f,  // red vector
            0f, 0f, 0f, 0f, 0f,  // green vector
            0f, 0f, 0f, 0f, 0f,  // blue vector
            0f, 0f, 0f, 1f, 0f   ) // alpha vector

        assert(censorColorMatrix().equals(ColorMatrix(array)))
        assert(uncensorColorMatrix().equals(ColorMatrix()))
    }
}

*/