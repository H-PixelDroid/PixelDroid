package org.pixeldroid.app


import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import org.hamcrest.CoreMatchers.anyOf
import org.pixeldroid.app.testUtility.*
import org.pixeldroid.app.utils.db.AppDatabase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MockedServerTest {

    private lateinit var activityScenario: ActivityScenario<MainActivity>
    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun before(){
        context = ApplicationProvider.getApplicationContext()
        db = initDB(context)
        db.clearAllTables()
        db.instanceDao().insertInstance(testiTestoInstance)
        db.userDao().insertUser(testiTesto)
        db.close()
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
    }
    @After
    fun after() {
        clearData()
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
*/
    @Test
    fun searchHashtags() {
    activityScenario.onActivity {
        it.findViewById<BottomNavigationView>(R.id.tabs).selectedItemId = R.id.page_2
    }

        onView(withId(R.id.search)).perform(typeSearchViewText("#randomnoise"))

        waitForView(R.id.tag_name)

        onView(first(withId(R.id.tag_name))).check(matches(withText("#randomnoise")))

    }

    @Test
    fun openDiscoverPost(){
        activityScenario.onActivity {
            it.findViewById<BottomNavigationView>(R.id.tabs).selectedItemId = R.id.page_2
        }

        waitForView(R.id.postPreview)

        onView(first(withId(R.id.postPreview))).perform(click())

        waitForView(R.id.username)

        onView(withId(R.id.username)).check(matches(anyOf(
            withSubstring("ros_testing"),
            withSubstring("PixelDroid Developer"),
            withSubstring("admin")
        )))
    }

    @Test
    fun searchAccounts() {
        activityScenario.onActivity {
            it.findViewById<BottomNavigationView>(R.id.tabs).selectedItemId = R.id.page_2
        }

        waitForView(R.id.search)

        onView(withId(R.id.search)).perform(typeSearchViewText("@pixeldroid"))

        waitForView(R.id.account_entry_username)

        onView(first(withId(R.id.account_entry_username))).check(matches(withText("PixelDroid Developer")))

    }

/*TODO test notifications (harder since they disappear after 6 months...

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
        onView(withText("user2 followed you")).check(matches(withId(R.id.notification_type)))

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
    }*/


    @Test
    fun swipingLeftStopsAtPublicTimeline() {
        activityScenario.onActivity {
            it.findViewById<BottomNavigationView>(R.id.tabs).selectedItemId = R.id.page_1
        }

        waitForView(R.id.view_pager)

        onView(withId(R.id.view_pager))
            .perform(ViewActions.swipeLeft()) // Notifications
            .perform(ViewActions.swipeLeft()) // Camera
            .perform(ViewActions.swipeLeft()) // Search
            .perform(ViewActions.swipeLeft()) // Homepage
            .perform(ViewActions.swipeLeft()) // Should stop at homepage
        activityScenario.onActivity {
            assert(it.findViewById<BottomNavigationView>(R.id.tabs).selectedItemId == R.id.page_5)
        }
    }

    @Test
    fun swipingPublicTimelineWorks() {
        activityScenario.onActivity {
            it.findViewById<BottomNavigationView>(R.id.tabs).selectedItemId = R.id.page_5
        } // Go to the last tab

        waitForView(R.id.view_pager)

        onView(withId(R.id.view_pager))
            .perform(ViewActions.swipeRight()) // Notifications
            .perform(ViewActions.swipeRight()) // Camera
            .perform(ViewActions.swipeRight()) // Search
            .perform(ViewActions.swipeRight()) // Homepage
            .perform(ViewActions.swipeRight()) // Should stop at homepage

        activityScenario.onActivity {
            assert(it.findViewById<BottomNavigationView>(R.id.tabs).selectedItemId == R.id.page_1)
        }
    }
/*
    @Test
    fun censorMatrices() {
        val array: FloatArray = floatArrayOf(
            0f, 0f, 0f, 0f, 0f,  // red vector
            0f, 0f, 0f, 0f, 0f,  // green vector
            0f, 0f, 0f, 0f, 0f,  // blue vector
            0f, 0f, 0f, 1f, 0f   ) // alpha vector

        assert(censorColorMatrix().equals(ColorMatrix(array)))
        assert(uncensorColorMatrix().equals(ColorMatrix()))
    }*/
}