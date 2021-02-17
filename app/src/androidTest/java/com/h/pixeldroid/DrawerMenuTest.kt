package com.h.pixeldroid

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.h.pixeldroid.testUtility.*
import com.h.pixeldroid.utils.db.AppDatabase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawerMenuTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context


    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(30)

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

        // Open Drawer to click on navigation.
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.drawer_layout))
            .check(matches(DrawerMatchers.isClosed())) // Left Drawer should be closed.
            .perform(DrawerActions.open()) // Open Drawer
    }

    @After
    fun after() {
        clearData()
    }

    @Test
    fun testDrawerSettingsButton() {
        // Start the screen of your activity.
        onView(withText(R.string.menu_settings)).perform(click())
        // Check that settings activity was opened.
        onView(withText(R.string.theme_title)).check(matches(isDisplayed()))
    }

    @Test
    fun testThemeSettings() {
        // Start the screen of your activity.
        onView(withText(R.string.menu_settings)).perform(click())
        val themes = getInstrumentation().targetContext.resources.getStringArray(R.array.theme_entries)
        //select theme modes
        onView(withText(R.string.theme_title)).perform(click())
        onView(withText(themes[2])).perform(click())

        //Select an other theme
        onView(withText(R.string.theme_title)).perform(click())
        onView(withText(themes[0])).perform(click())

        //Select the last theme
        onView(withText(R.string.theme_title)).perform(click())
        onView(withText(themes[1])).perform(click())

        //Check that we are back in the settings page
        onView(withText(R.string.theme_header)).check(matches(isDisplayed()))
    }

    @Test
    fun testDrawerLogoutButton() {
        // Start the screen of your activity.
        onView(withText(R.string.logout)).perform(click())
        // Check that login activity was opened.
        onView(withId(R.id.mascotImage)).check(matches(isDisplayed()))
    }

    @Test
    fun testDrawerProfileButton() {
        // Start the screen of your activity.
        onView(withText(R.string.menu_account)).perform(click())
        // Check that profile activity was opened.
        onView(withId(R.id.profilePictureImageView)).check(matches(isDisplayed()))
    }

    @Test
    fun testDrawerOwnProfileFollowers() {
        // Start the screen of your activity.
        onView(withText(R.string.menu_account)).perform(click())
        // Check that profile activity was opened.
        onView(withId(R.id.editButton)).check(matches(isDisplayed()))
        val followersText = context.resources.getQuantityString(R.plurals.nb_followers, 2, 2)
        onView(withText(followersText)).perform(click())

        waitForView(R.id.account_entry_avatar)
        onView(withText("PixelDroid Developer")).check(matches(isDisplayed()))
    }

    @Test
    fun testDrawerOwnProfileFollowing() {
        // Start the screen of your activity.
        onView(withText(R.string.menu_account)).perform(click())
        // Check that profile activity was opened.
        onView(withId(R.id.editButton)).check(matches(isDisplayed()))
        val followingText = context.resources.getQuantityString(R.plurals.nb_following, 3, 3)
        onView(withText(followingText)).perform(click())

        waitForView(R.id.account_entry_avatar)
        onView(withText("@User 1")).check(matches(isDisplayed()))
    }

    /*@Test
    fun testDrawerAccountNameClick() {
        // Start the screen of your activity.
        onView(withText("Testi")).perform(click())
        // Check that profile activity was opened.
        onView(withText("Add Account")).check(matches(isDisplayed()))
    }*/

    @Test
    fun clickFollowers() {
        // Open My Profile from drawer
        onView(withText(R.string.menu_account)).perform(click())

        waitForView(R.id.nbFollowersTextView)

        // Open followers list
        onView(withId(R.id.nbFollowersTextView)).perform(click())

        waitForView(R.id.account_entry_avatar)

        // Open follower's profile
        onView(withText("@pixeldroid")).perform(click())

        waitForView(R.id.profilePictureImageView)

        onView(withId(R.id.accountNameTextView)).check(matches(withText("PixelDroid Developer")))
    }

    @Test
    fun clickFollowing() {
        // Open My Profile from drawer
        onView(withText(R.string.menu_account)).perform(click())
        waitForView(R.id.nbFollowersTextView)

        // Open followers list
        onView(withId(R.id.nbFollowingTextView)).perform(click())

        waitForView(R.id.account_entry_avatar)

        // Open following's profile
        onView(withText("@user2")).perform(click())
        waitForView(R.id.profilePictureImageView)

        onView(withId(R.id.accountNameTextView)).check(matches(withText("User 2")))
    }

    @Test
    fun onBackPressedClosesDrawer() {
        UiDevice.getInstance(getInstrumentation()).pressBack()
        Thread.sleep(1000)
        onView(withId(R.id.drawer_layout)).check(matches(DrawerMatchers.isClosed()))
    }
}