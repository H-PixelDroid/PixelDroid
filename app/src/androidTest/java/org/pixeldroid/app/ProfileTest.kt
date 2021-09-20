package org.pixeldroid.app

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.pixeldroid.app.profile.ProfileActivity
import org.pixeldroid.app.testUtility.*
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.db.AppDatabase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ProfileTest {
    private lateinit var activityScenario: ActivityScenario<ProfileActivity>
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

        val intent = Intent(context, ProfileActivity::class.java)
        val account = Account(id="344399325768278017", username="pixeldroid", acct="pixeldroid", url="https://testing.pixeldroid.org/pixeldroid", display_name="PixelDroid Developer", note="", avatar="https://testing.pixeldroid.org/storage/avatars/default.jpg?v=0", avatar_static="https://testing.pixeldroid.org/storage/avatars/default.jpg?v=0", header="", header_static="", locked=false, emojis= emptyList(), discoverable=null, created_at=Instant.parse("2021-09-17T08:39:57Z"), statuses_count=0, followers_count=1, following_count=1, moved=null, fields=null, bot=false, source=null)
        intent.putExtra(Account.ACCOUNT_TAG, account)
        activityScenario = ActivityScenario.launch(intent)
        onView(withId(R.id.profileRefreshLayout)).perform(swipeDown())
        Thread.sleep(2000)
    }
    @After
    fun after() {
        clearData()
    }


    @Test
    fun clickFollowButton() {
        if (onView(ViewMatchers.withText("Unfollow")).isDisplayed()) {
            //Currently following

            // Unfollow
            follow("Follow")

            // Follow
            follow("Unfollow")
        } else {
            //Currently not following

            // Follow
            follow("Unfollow")

            // Unfollow
            follow("Follow")
        }
    }

    private fun follow(follow_or_unfollow: String){
        onView(withId(R.id.followButton)).perform((ViewActions.click()))
        Thread.sleep(1000)
        onView(withId(R.id.followButton)).check(ViewAssertions.matches(ViewMatchers.withText(follow_or_unfollow)))
    }



    @Test
    fun clickOtherUserFollowers() {
        // Open followers list
        onView(withId(R.id.nbFollowersTextView)).perform((ViewActions.click()))

        waitForView(R.id.account_entry_username)

        // Open follower's profile
        onView(ViewMatchers.withText("Testi Testo")).perform((ViewActions.click()))

        waitForView(R.id.editButton)

        //Check that our own profile opened
        onView(withId(R.id.editButton)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

}