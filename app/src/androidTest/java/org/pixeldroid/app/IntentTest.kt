package org.pixeldroid.app

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.posts.StatusViewHolder
import org.pixeldroid.app.utils.api.objects.Account
import org.pixeldroid.app.utils.api.objects.Account.Companion.ACCOUNT_TAG
import org.pixeldroid.app.settings.AboutActivity
import org.pixeldroid.app.testUtility.*
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.pixeldroid.app.posts.PostActivity
import org.pixeldroid.app.utils.api.objects.Attachment
import org.pixeldroid.app.utils.api.objects.Mention
import org.pixeldroid.app.utils.api.objects.Status
import java.time.Instant


@RunWith(AndroidJUnit4::class)
class IntentTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @get:Rule
    var mLoginActivityActivityTestRule =
        ActivityTestRule(
            AboutActivity::class.java
        )

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        db = initDB(context)
        db.clearAllTables()
        db.instanceDao().insertInstance(
            testiTestoInstance
        )

        db.userDao().insertUser(testiTesto)
        db.close()

        Intents.init()
    }


    @Test
    fun clickingMentionOpensProfile() {
        val accountExpected = Account(id="448137467259420673", username="ros_testing", acct="ros_testing", url="https://testing.pixeldroid.org/ros_testing", display_name="ros_testing", note="", avatar="https://testing.pixeldroid.org/storage/avatars/default.jpg?v=0", avatar_static="https://testing.pixeldroid.org/storage/avatars/default.jpg?v=0", header="https://testing.pixeldroid.org/storage/headers/missing.png", header_static="https://testing.pixeldroid.org/storage/headers/missing.png", locked=false, emojis=emptyList(), discoverable=true, created_at=Instant.parse("2022-06-30T14:58:18Z"), statuses_count=1, followers_count=2, following_count=0, moved=null, fields=emptyList(), bot=false, source=null, suspended=null, mute_expires_at=null)
        val accountPoster = Account(id="457218336143343773", username="pixeldroid", acct="pixeldroid", url="https://testing.pixeldroid.org/pixeldroid", display_name="PixelDroid Developer", note="", avatar="https://testing.pixeldroid.org/storage/avatars/default.jpg?v=0", avatar_static="https://testing.pixeldroid.org/storage/avatars/default.jpg?v=0", header="https://testing.pixeldroid.org/storage/headers/missing.png", header_static="https://testing.pixeldroid.org/storage/headers/missing.png", locked=false, emojis=emptyList(), discoverable=true, created_at=Instant.parse("2022-07-25T16:22:26Z"), statuses_count=3, followers_count=1, following_count=2, moved=null, fields=emptyList(), bot=false, source=null, suspended=null, mute_expires_at=null)
        val expectedIntent: Matcher<Intent> = CoreMatchers.allOf(
            IntentMatchers.hasExtra(ACCOUNT_TAG, accountExpected)
        )


        val attachment = Attachment(id="31", type=Attachment.AttachmentType.image, url="https://testing.pixeldroid.org/storage/m/_v2/457218336143343773/7a6475c83-a44db4/RcuV81RiDorC/RCtbr01ttKfqATIA9TYL7MOatlYuxdkm3CsNYydB.jpg", preview_url="https://testing.pixeldroid.org/storage/m/_v2/457218336143343773/7a6475c83-a44db4/RcuV81RiDorC/RCtbr01ttKfqATIA9TYL7MOatlYuxdkm3CsNYydB_thumb.jpg", remote_url=null, meta= Attachment.Meta(
            focus = Attachment.Meta.Focus(x = 0.0, y = 0.0),
            original = Attachment.Meta.Image(width = 720,
                height = 576,
                size = "720x576",
                aspect = 1.25)), description=null, blurhash="U4HoQs014-~UyD4rRit200~mIe034s-*srIZ", text_url=null)
        val post = Status(
            id = "457277566298267808",
            content = "<a class=\"u-url mention\" href=\"https://testing.pixeldroid.org/ros_testing\" rel=\"external nofollow noopener\" target=\"_blank\">@ros_testing</a> nice I have this too",
            account = accountPoster,
            media_attachments = listOf(attachment),
            created_at = Instant.parse("2022-07-25T20:17:47Z"),
            mentions = listOf(Mention(id="448137467259420673", username="ros_testing", acct="ros_testing", url="https://testing.pixeldroid.org/ros_testing")),
            uri = "https://testing.pixeldroid.org/p/pixeldroid/457277566298267808",
            url = "https://testing.pixeldroid.org/p/pixeldroid/457277566298267808",
        )


        val intent = Intent(context, PostActivity::class.java)
        intent.putExtra(Status.POST_TAG, post)
        ActivityScenario.launch<PostActivity>(intent)

        waitForView(R.id.description)

        //Click the mention
        Espresso.onView(ViewMatchers.withId(R.id.description))
            .perform(clickClickableSpanInDescription("@ros_testing"))

        //Wait a bit
        Thread.sleep(1000)

        //Check that the right intent was launched
        intended(expectedIntent)
    }


    @Test
    fun clickEditProfileMakesIntent() {
        ActivityScenario.launch(MainActivity::class.java)

        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout))
            .check(ViewAssertions.matches(DrawerMatchers.isClosed())) // Left Drawer should be closed.
            .perform(DrawerActions.open()) // Open Drawer

        val expectedIntent: Matcher<Intent> = CoreMatchers.allOf(
            IntentMatchers.hasAction(Intent.ACTION_VIEW),
            IntentMatchers.hasDataString(CoreMatchers.containsString("settings/home"))
        )

        // Start the screen of your activity.
        Espresso.onView(ViewMatchers.withText(R.string.menu_account)).perform(ViewActions.click())
        // Check that profile activity was opened.
        Espresso.onView(ViewMatchers.withId(R.id.editButton))
            .perform(ViewActions.click())
        intended(expectedIntent)

    }

    @After
    fun after() {
        Intents.release()
        clearData()
    }
}