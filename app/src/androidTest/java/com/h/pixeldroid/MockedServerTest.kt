package com.h.pixeldroid


import android.content.Context
import android.graphics.ColorMatrix
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.tabs.TabLayout
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.InstanceDatabaseEntity
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.fragments.feeds.PostViewHolder
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.clickChildViewWithId
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.first
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.getText
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.second
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.slowSwipeUp
import com.h.pixeldroid.testUtility.CustomMatchers.Companion.typeTextInViewWithId
import com.h.pixeldroid.testUtility.MockServer
import com.h.pixeldroid.utils.DBUtils
import com.h.pixeldroid.utils.PostUtils.Companion.censorColorMatrix
import com.h.pixeldroid.utils.PostUtils.Companion.uncensorColorMatrix
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import kotlin.concurrent.thread


@RunWith(AndroidJUnit4::class)
class MockedServerTest {

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
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
    }

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
    @Test
    fun openDiscoverPost(){
        activityScenario.onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(1)?.select()
        }
        Thread.sleep(1000)
        onView(withId(R.id.discoverList)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.username)).check(matches(withText("machintuck")))

    }

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

    @Test
    fun clickFollowButton() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

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
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

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
        ActivityScenario.launch(MainActivity::class.java).onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }
        Thread.sleep(1000)

        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())
        Thread.sleep(1000)

        onView(withText("Dobios followed you")).perform(click())
        Thread.sleep(1000)
        onView(withText("Andrew Dobis")).check(matches(withId(R.id.accountNameTextView)))
    }

    @Test
    fun clickNotificationPost() {
        ActivityScenario.launch(MainActivity::class.java).onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }
        Thread.sleep(1000)

        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())
        Thread.sleep(1000)

        onView(withText("Dobios liked your post")).perform(click())
        Thread.sleep(1000)

        onView(withId(R.id.username)).perform(click())
        Thread.sleep(10000)
        onView(withText("Dante")).check(matches(withId(R.id.accountNameTextView)))
    }

    @Test
    fun clickNotificationRePost() {
        ActivityScenario.launch(MainActivity::class.java).onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }
        Thread.sleep(1000)

        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())
        Thread.sleep(1000)

        onView(withText("Clement shared your post")).perform(click())
        Thread.sleep(1000)

        onView(first(withText("Clement"))).check(matches(withId(R.id.username)))
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
        onView(withId(R.id.main_activity_main_linear_layout))
            .perform(ViewActions.swipeUp()) // notifications
            .perform(ViewActions.swipeUp()) // camera
            .perform(ViewActions.swipeUp()) // search
            .perform(ViewActions.swipeUp()) // homepage
            .perform(ViewActions.swipeUp()) // should stop at homepage
        onView(withId(R.id.list)).check(matches(isDisplayed()))
    }

    @Test
    fun clickingTabOnAlbumShowsNextPhoto() {
         ActivityScenario.launch(MainActivity::class.java).onActivity {
            a -> run {
                //Wait for the feed to load
                Thread.sleep(1000)
             a.findViewById<TextView>(R.id.sensitiveWarning).performClick()
             Thread.sleep(1000)
             //Pick the second photo
                a.findViewById<TabLayout>(R.id.postTabs).getTabAt(1)?.select()
            }
        }

        //Check that the tabs are shown
        onView(first(withId(R.id.postTabs))).check(matches(isDisplayed()))
    }

    @Test
    fun clickingLikeButtonWorks() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

        //Get initial like count
        val likes = getText(first(withId(R.id.nlikes)))

        //Like the post
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.liker)))
        Thread.sleep(100)
        //Unlike the post
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.liker)))
        //...
        Thread.sleep(100)

        //Profit
        onView(first(withId(R.id.nlikes))).check(matches((withText(likes))))
    }

    @Test
    fun clickingLikeButtonFails() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

        //Get initial like count
        val likes = getText(first(withId(R.id.nlikes)))

        //Like the post
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (2, clickChildViewWithId(R.id.liker)))
        Thread.sleep(100)

        //...
        Thread.sleep(100)

        //Profit
        onView((withId(R.id.list))).check(matches(isDisplayed()))
    }

    @Test
    fun clickingUsernameOpensProfile() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

        //Get initial like count
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
            (0, clickChildViewWithId(R.id.username)))

        Thread.sleep(1000)

        //Check that the Profile opened
        onView(withId(R.id.accountNameTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun clickingProfilePicOpensProfile() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

        //Get initial like count
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.profilePic)))

        Thread.sleep(1000)

        //Check that the Profile opened
        onView(withId(R.id.accountNameTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun clickingReblogButtonWorks() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

        //Get initial like count
        val shares = getText(first(withId(R.id.nshares)))

        //Reblog the post
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.reblogger)))
        Thread.sleep(100)

        //UnReblog the post
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.reblogger)))
        //...
        Thread.sleep(100)

        //Profit
        onView(first(withId(R.id.nshares))).check(matches((withText(shares))))
    }

    @Test
    fun clickingMentionOpensProfile() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

        //Click the mention
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.description)))

        //Wait a bit
        Thread.sleep(1000)

        //Check that the Profile is shown
        onView(first(withId(R.id.username))).check(matches(isDisplayed()))
    }

    @Test
    fun clickingHashTagsWorks() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

        //Click the hashtag
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (1, clickChildViewWithId(R.id.description)))

        //Wait a bit
        Thread.sleep(1000)

        //Check that the HashTag was indeed clicked
        //Doesn't do anything for now
        onView(withId(R.id.list)).check(matches(isDisplayed()))
    }


    @Test
    fun clickingCommentButtonOpensCommentSection() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

        //Click comment button 3 times and then try to see if the commenter exists
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.commenter)))
        Thread.sleep(100)
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.commenter)))
        Thread.sleep(100)
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.commenter)))

        onView(first(withId(R.id.commentIn)))
            .check(matches(hasDescendant(withId(R.id.editComment))))
    }

    @Test
    fun clickingViewCommentShowsTheComments() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)
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
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)
        //Open the comment section
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (2, clickChildViewWithId(R.id.ViewComments)))
        Thread.sleep(1000)
        onView(withId(R.id.list)).check(matches(isDisplayed()))
    }

    @Test
    fun postingACommentWorks() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

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
    fun censorMatrices() {
        // Doing these dummy checks as I can not get the matrix property from the ImageView
        val array: FloatArray = floatArrayOf(
            0.1f, 0f, 0f, 0f, 0f,  // red vector
            0f, 0.1f, 0f, 0f, 0f,  // green vector
            0f, 0f, 0.1f, 0f, 0f,  // blue vector
            0f, 0f, 0f, 1f, 0f   ) // alpha vector

        assert(censorColorMatrix().equals(array))
        assert(uncensorColorMatrix().equals(ColorMatrix()))
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
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

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
}

