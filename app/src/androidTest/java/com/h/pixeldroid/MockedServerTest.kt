package com.h.pixeldroid

import android.content.Context
import android.service.autofill.Validators.not
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.tabs.TabLayout
import com.h.pixeldroid.fragments.feeds.PostViewHolder
import com.h.pixeldroid.testUtility.MockServer
import org.hamcrest.BaseMatcher
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MockedServerTest {
    private fun <T> first(matcher: Matcher<T>): Matcher<T>? {
        return object : BaseMatcher<T>() {
            var isFirst = true
            override fun describeTo(description: org.hamcrest.Description?) {
                description?.appendText("first matching item")
            }

            override fun matches(item: Any?): Boolean {
                if (isFirst && matcher.matches(item)) {
                    isFirst = false
                    return true
                }
                return false
            }

        }
    }

    /**
     * @param percent can be 1 or 0
     * 1: swipes all the way up
     * 0: swipes half way up
     */
    private fun slowSwipeUp(percent: Boolean) : ViewAction {
        return ViewActions.actionWithAssertions(
                GeneralSwipeAction(
                    Swipe.SLOW,
                    GeneralLocation.BOTTOM_CENTER,
                    if(percent) GeneralLocation.TOP_CENTER else GeneralLocation.CENTER,
                    Press.FINGER)
                )
    }

    fun getText(matcher: Matcher<View?>?): String? {
        val stringHolder = arrayOf<String?>(null)
        onView(matcher).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(TextView::class.java)
            }

            override fun getDescription(): String {
                return "getting text from a TextView"
            }

            override fun perform(
                uiController: UiController,
                view: View
            ) {
                val tv = view as TextView //Save, because of check in getConstraints()
                stringHolder[0] = tv.text.toString()
            }
        })
        return stringHolder[0]
    }

    private fun clickChildViewWithId(id: Int) = object : ViewAction {

        override fun getConstraints() = null

        override fun getDescription() = "click child view with id $id"

        override fun perform(uiController: UiController, view: View) {
            val v = view.findViewById<View>(id)
            v.performClick()
        }
    }

    private fun typeTextInViewWithId(id: Int, text: String) = object : ViewAction {

        override fun getConstraints() = null

        override fun getDescription() = "click child view with id $id"

        override fun perform(uiController: UiController, view: View) {
            val v = view.findViewById<EditText>(id)
            v.text.append(text)
        }
    }

    val mockServer = MockServer()


    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun before(){
        mockServer.start()
        val baseUrl = mockServer.getUrl()
        val preferences = InstrumentationRegistry.getInstrumentation()
            .targetContext.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", baseUrl.toString()).apply()
    }

    @Test
    fun testFollowersTextView() {
        ActivityScenario.launch(MainActivity::class.java).onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(4)?.select()
        }
        Thread.sleep(1000)
        onView(withId(R.id.nbFollowersTextView)).check(matches(withText("68\nFollowers")))
        onView(withId(R.id.accountNameTextView)).check(matches(withText("deerbard_photo")))
    }

    @Test
    fun testNotificationsList() {
        ActivityScenario.launch(MainActivity::class.java).onActivity{
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
        ActivityScenario.launch(MainActivity::class.java).onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }
        Thread.sleep(1000)

        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())
        Thread.sleep(1000)

        onView(withText("Dobios liked your post")).perform(ViewActions.click())
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

        onView(withText("Dobios followed you")).perform(ViewActions.click())
        Thread.sleep(1000)
        onView(withText("Dobios")).check(matches(withId(R.id.accountNameTextView)))
    }

    @Test
    fun clickNotificationPost() {
        ActivityScenario.launch(MainActivity::class.java).onActivity{
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(3)?.select()
        }
        Thread.sleep(1000)

        onView(withId(R.id.view_pager)).perform(ViewActions.swipeUp()).perform(ViewActions.swipeDown())
        Thread.sleep(1000)

        onView(withText("Dobios liked your post")).perform(ViewActions.click())
        Thread.sleep(1000)

        onView(withId(R.id.username)).perform(ViewActions.click())
        Thread.sleep(10000)
        onView(withText("Dante")).check(matches(withId(R.id.accountNameTextView)))
    }

    @Test
    fun testDrawerSettingsButton() {
        // Open Drawer to click on navigation.
        onView(withId(R.id.drawer_layout))
            .check(matches(DrawerMatchers.isClosed(Gravity.LEFT))) // Left Drawer should be closed.
            .perform(DrawerActions.open()) // Open Drawer

        // Start the screen of your activity.
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_settings))

        // Check that settings activity was opened.
        onView(withText(R.string.signature_title)).check(matches(isDisplayed()))
    }

    @Test
    fun swipingLeftStopsAtProfile() {
        onView(withId(R.id.main_activity_main_linear_layout))
            .perform(ViewActions.swipeLeft()) // search
            .perform(ViewActions.swipeLeft()) // camera
            .perform(ViewActions.swipeLeft()) // notifications
            .perform(ViewActions.swipeLeft()) // profile
            .perform(ViewActions.swipeLeft()) // should stop at profile
        onView(withId(R.id.nbFollowersTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun swipingRightStopsAtHomepage() {
        ActivityScenario.launch(MainActivity::class.java).onActivity {
                a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(4)?.select()
        } // go to the last tab
        onView(withId(R.id.main_activity_main_linear_layout))
            .perform(ViewActions.swipeRight()) // notifications
            .perform(ViewActions.swipeRight()) // camera
            .perform(ViewActions.swipeRight()) // search
            .perform(ViewActions.swipeRight()) // homepage
            .perform(ViewActions.swipeRight()) // should stop at homepage
        onView(withId(R.id.list)).check(matches(isDisplayed()))
    }

    @Test
    fun clickingLikeButtonWorks() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

        //Get initial like count
        val likes = getText(withId(R.id.nlikes))

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
        val shares = getText(withId(R.id.nshares))

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
        //Click comment button and then try to see if the commenter exists
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.commenter)))
        Thread.sleep(1000)
        onView(withId(R.id.commentIn))
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
        onView(withId(R.id.commentContainer))
            .check(matches(hasDescendant(withId(R.id.comment))))
    }

    @Test
    fun postingACommentWorks() {
        ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)

        //Open the comment section
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.commenter)))

        onView(withId(R.id.list)).perform(slowSwipeUp(true))
        onView(withId(R.id.list)).perform(slowSwipeUp(false))
        onView(withId(R.id.list)).perform(slowSwipeUp(false))
        Thread.sleep(1000)

        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, typeTextInViewWithId(R.id.editComment, "test")))
        onView(withId(R.id.list))
            .perform(actionOnItemAtPosition<PostViewHolder>
                (0, clickChildViewWithId(R.id.submitComment)))

        Thread.sleep(1000)
        onView(withId(R.id.commentContainer))
            .check(matches(hasDescendant(withId(R.id.comment))))
    }
}

