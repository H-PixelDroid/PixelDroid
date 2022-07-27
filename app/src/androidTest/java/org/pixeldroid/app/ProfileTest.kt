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
        val account = Account(id="448138207202832386", username="admin", acct="admin", url="https://testing.pixeldroid.org/admin", display_name="admin", note="", avatar="https://testing.pixeldroid.org/storage/avatars/default.jpg?v=0", avatar_static="https://testing.pixeldroid.org/storage/avatars/default.jpg?v=0", header="https://testing.pixeldroid.org/storage/headers/missing.png", header_static="https://testing.pixeldroid.org/storage/headers/missing.png", locked=false, emojis= emptyList(), discoverable=true, created_at=Instant.parse("2022-06-30T15:01:14Z"), statuses_count=1, followers_count=0, following_count=3, moved=null, fields= emptyList(), bot=false, source=null, suspended=null, mute_expires_at=null)
        intent.putExtra(Account.ACCOUNT_TAG, account)
        activityScenario = ActivityScenario.launch(intent)
        waitForView(R.id.followButton)
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
        } else if (onView(ViewMatchers.withText("Follow")).isDisplayed()){
            //Currently not following

            // Follow
            follow("Unfollow")

            // Unfollow
            follow("Follow")
        } else check(false)
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
        onView(ViewMatchers.withText("PixelDroid Developer")).perform((ViewActions.click()))

        waitForView(R.id.editButton)

        //Check that our own profile opened
        onView(withId(R.id.editButton)).isDisplayed()
    }

}