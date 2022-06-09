package org.pixeldroid.app


import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import org.hamcrest.CoreMatchers.*
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.posts.StatusViewHolder
import org.pixeldroid.app.testUtility.*
import org.hamcrest.core.IsInstanceOf
import org.hamcrest.core.StringContains.containsString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class HomeFeedTest {

    private lateinit var activityScenario: ActivityScenario<MainActivity>
    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Rule @JvmField
    var repeatRule: RepeatRule = RepeatRule()


    @Before
    fun before(){
        context = ApplicationProvider.getApplicationContext()
        db = initDB(context)
        db.clearAllTables()
        db.instanceDao().insertInstance(
            testiTestoInstance
        )
        db.userDao().insertUser(
            testiTesto
        )
        db.close()
        activityScenario = ActivityScenario.launch(MainActivity::class.java)

        waitForView(R.id.username)
        onView(withId(R.id.list)).perform(scrollToPosition<StatusViewHolder>(0))

    }
    @After
    fun after() {
        clearData()
    }

    @Test
    @RepeatTest
    fun clickingTabOnAlbumShowsNextPhoto() {
        //Wait for the feed to load
        waitForView(R.id.albumPager)

        activityScenario.onActivity {
            a -> run {
                //Pick the second photo
                a.findViewById<ViewPager2>(R.id.albumPager).currentItem = 2
            }
        }
        onView(first(withId(R.id.albumPager))).check(matches(isDisplayed()))
    }

    @Test
    @RepeatTest
    fun tabReClickScrollUp() {
        //Wait for the feed to load
        waitForView(R.id.albumPager)

        onView(withId(R.id.list)).perform(scrollToPosition<StatusViewHolder>(4))

        onView(first(IsInstanceOf.instanceOf(TabLayout.TabView::class.java))).perform(ViewActions.click())


        onView(first(withId(R.id.description))).check(matches(withText(containsString("@user2"))));
    }

    @Test
    @RepeatTest
    fun hashtag() {
        //Wait for the feed to load
        waitForView(R.id.albumPager)

        onView(allOf(withClassName(endsWith("RecyclerView")), not(withId(R.id.material_drawer_recycler_view))))
            .perform(
                scrollToPosition<StatusViewHolder>(3)
            )

        onView(allOf(withText(containsString("randomNoise"))))
            .perform(clickClickableSpan("#randomNoise"))

        waitForView(R.id.action_bar, allOf(withText("#randomNoise"), not(withId(R.id.description))))

        onView(withId(R.id.action_bar)).check(matches(isDisplayed()));
        onView(allOf(withText("#randomNoise"), not(withId(R.id.description)))).check(matches(withParent(withId(R.id.action_bar))));
    }
/*
    @Test
    fun clickingReblogButtonWorks() {
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<StatusViewHolder>
                (0, clickChildViewWithId(R.id.reblogger)))
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<StatusViewHolder>
                (0, clickChildViewWithId(R.id.reblogger)))
        onView(first(withId(R.id.nshares)))
            .check(matches(withText(getText(first(withId(R.id.nshares))))))
    }

    @Test
    fun doubleTapLikerWorks() {
        Thread.sleep(1000)
        //Get initial like count
        val likes = getText(first(withId(R.id.nlikes)))
        val nLikes = likes!!.split(" ")[0].toInt()

        //Remove sensitive media warning
        onView(withId(R.id.list))
                .perform(actionOnItemAtPosition<StatusViewHolder>
                (0, clickChildViewWithId(R.id.sensitiveWarning)))
        Thread.sleep(100)

        //Like the post
        onView(withId(R.id.list))
                .perform(actionOnItemAtPosition<StatusViewHolder>
                (0, clickChildViewWithId(R.id.postPicture)))
        onView(withId(R.id.list))
                .perform(actionOnItemAtPosition<StatusViewHolder >
                (0, clickChildViewWithId(R.id.postPicture)))
        //...
        Thread.sleep(100)

        //Profit
        onView(first(withId(R.id.nlikes))).check(matches((withText("${nLikes + 1} Likes"))))
    }

    @Test
    fun clickingLikeButtonWorks() {
        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<StatusViewHolder>(0, clickChildViewWithId(R.id.liker))
        )
        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<StatusViewHolder>(0, clickChildViewWithId(R.id.liker))
        )
        onView(first(withId(R.id.nlikes)))
            .check(matches(withText(getText(first(withId(R.id.nlikes))))))
    }

    @Test
    fun clickingLikeButtonFails() {
        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<StatusViewHolder>(2, clickChildViewWithId(R.id.liker))
        )
        onView((withId(R.id.list))).check(matches(isDisplayed()))
    }*/

    @Test
    @RepeatTest
    fun clickingUsernameOpensProfile() {
        waitForView(R.id.username)

        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<StatusViewHolder>(0, clickChildViewWithId(R.id.username))
        )
        onView(withId(R.id.accountNameTextView)).check(matches(isDisplayed()))
    }

    @Test
    @RepeatTest
    fun clickingProfilePicOpensProfile() {
        waitForView(R.id.profilePic)

        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<StatusViewHolder>(0, clickChildViewWithId(R.id.profilePic))
        )
        onView(withId(R.id.accountNameTextView)).check(matches(isDisplayed()))
    }

    @Test
    @RepeatTest
    fun clickingMentionOpensProfile() {
        waitForView(R.id.description)

        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<StatusViewHolder>(0, clickChildViewWithId(R.id.description))
        )
        onView(first(withId(R.id.username))).check(matches(isDisplayed()))
    }
/*


    @Test
    fun clickingCommentButtonOpensCommentSection() {
        onView(withId(R.id.list)).perform(
            actionOnItemAtPosition<StatusViewHolder>(0, clickChildViewWithId(R.id.commenter))
        )
        onView(first(withId(R.id.commentIn)))
            .check(matches(hasDescendant(withId(R.id.editComment))))
    }

    @Test
    fun clickingViewCommentShowsTheComments() {
        //Open the comment section
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<StatusViewHolder>
                (0, clickChildViewWithId(R.id.viewComments)))
        Thread.sleep(1000)
        onView(first(withId(R.id.commentContainer)))
            .check(matches(hasDescendant(withId(R.id.comment))))
    }

    @Test
    fun clickingViewCommentFails() {
        //Open the comment section
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<StatusViewHolder>
                (2, clickChildViewWithId(R.id.viewComments)))
        Thread.sleep(1000)
        onView(withId(R.id.list)).check(matches(isDisplayed()))
    }

    @Test
    fun postingACommentWorks() {
        //Open the comment section
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<StatusViewHolder>
                (0, clickChildViewWithId(R.id.commenter)))

        onView(withId(R.id.list)).perform(slowSwipeUp(false))
        Thread.sleep(1000)

        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<StatusViewHolder>
                (0, typeTextInViewWithId(R.id.editComment, "test")))
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<StatusViewHolder>
                (0, clickChildViewWithId(R.id.submitComment)))

        Thread.sleep(1000)
        onView(first(withId(R.id.commentContainer)))
            .check(matches(hasDescendant(withId(R.id.comment))))
    }*/

    @RepeatTest
    @Test
    fun performClickOnSensitiveWarning() {
        waitForView(R.id.username)
        
        onView(second(withId(R.id.sensitiveWarning))).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<StatusViewHolder>
                (1, clickChildViewWithId(R.id.sensitiveWarning)))

        onView(withId(R.id.list))
            .check(matches(atPosition(1, not(withId(R.id.sensitiveWarning)))))
    }

    @Test
    @RepeatTest
    fun performClickOnSensitiveWarningTabs() {
        waitForView(R.id.username)

        onView(first(withId(R.id.sensitiveWarning))).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<StatusViewHolder>
                (0, clickChildViewWithId(R.id.sensitiveWarning)))

        onView(first(withId(R.id.sensitiveWarning))).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

/*
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

 */
}

