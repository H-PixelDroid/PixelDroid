package org.pixeldroid.app

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.pixeldroid.app.BuildConfig.INSTANCE_URI
import org.pixeldroid.app.posts.PostActivity
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.api.objects.*
import org.pixeldroid.app.testUtility.clearData
import org.pixeldroid.app.testUtility.initDB
import org.pixeldroid.app.testUtility.testiTesto
import org.pixeldroid.app.testUtility.testiTestoInstance
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.Matcher
import org.junit.*
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


@RunWith(AndroidJUnit4::class)
class PostTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @Before
    fun before(){
        context = getInstrumentation().targetContext
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
    fun saveToGalleryTestSimplePost() {
        val attachment = Attachment(
            id = "12",
            url = "$INSTANCE_URI/storage/avatars/default.jpg?v=0",
                meta = null
        )
        val post = Status(
            id = "12",
            account = Account(
                id = "12",
                username = "SQDFSQDF",
                url = "$INSTANCE_URI/pixeldroid",
            ),
            media_attachments = listOf(attachment),
            created_at = Instant.now().minusSeconds(3600)
        )
        val intent = Intent(context, PostActivity::class.java)
        intent.putExtra(Status.POST_TAG, post)
        ActivityScenario.launch<PostActivity>(intent)
        onView(withId(R.id.status_more)).perform(click())
        onView(withText(R.string.save_to_gallery)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        Thread.sleep(300)
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(anyOf(withText(R.string.image_download_downloading),
                withText(R.string.image_download_success),
                withText(R.string.image_download_failed)
            )
            )
            )
    }

    @Test
    fun saveToGalleryTestAlbum() {
        val attachment1 = Attachment(
            id = "12",
            url = "$INSTANCE_URI/storage/avatars/default.jpg?v=0",
                meta = null
        )
        val attachment2 = Attachment(
            id = "13",
            url = "$INSTANCE_URI/storage/avatars/default.jpg?v=0",
                meta = null
        )
        val post = Status(
            id = "12",
            account = Account(
                id = "12",
                username = "douze",
                    url = "$INSTANCE_URI/pixeldroid",
            ),
            media_attachments = listOf(attachment1, attachment2),
            created_at = Instant.now().minusSeconds(3600)
        )
        val intent = Intent(context, PostActivity::class.java)
        intent.putExtra(Status.POST_TAG, post)
        ActivityScenario.launch<PostActivity>(intent)
        onView(withId(R.id.status_more)).perform(click())
        onView(withText(R.string.save_to_gallery)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        Thread.sleep(300)
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(anyOf(withText(R.string.image_download_downloading),
                                 withText(R.string.image_download_success),
                                 withText(R.string.image_download_failed)
                                )
                            )
                   )
    }

    @Test
    fun shareTestSimplePost() {
        val expectedIntent: Matcher<Intent> = IntentMatchers.hasAction(Intent.ACTION_CHOOSER)
        val attachment = Attachment(
                id = "12",
                url = "$INSTANCE_URI/storage/avatars/default.jpg?v=0",
                meta = null
        )
        val post = Status(
                id = "12",
                account = Account(
                        id = "12",
                        username = "douze",
                        url = "$INSTANCE_URI/pixeldroid",
                ),
                media_attachments = listOf(attachment),
            created_at = Instant.now().minusSeconds(3600)
        )
        val intent = Intent(context, PostActivity::class.java)
        intent.putExtra(Status.POST_TAG, post)
        ActivityScenario.launch<PostActivity>(intent)
        onView(withId(R.id.status_more)).perform(click())
        onView(withText(R.string.share_picture)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        Thread.sleep(2000)
        Intents.intended(expectedIntent)
    }

    @Test
    fun shareIntentAlbumTest() {
        val expectedIntent: Matcher<Intent> = IntentMatchers.hasAction(Intent.ACTION_CHOOSER)
        val attachment1 = Attachment(
            id = "12",
                url = "$INSTANCE_URI/storage/avatars/default.jpg?v=0",
                meta = null
        )
        val attachment2 = Attachment(
                id = "13",
                url = "$INSTANCE_URI/storage/avatars/default.jpg?v=0",
                meta = null
        )
        val post = Status(
                id = "12",
                account = Account(
                        id = "12",
                        username = "douze",
                        url = "$INSTANCE_URI/pixeldroid",
                ),
                media_attachments = listOf(attachment1, attachment2),
            created_at = Instant.now().minusSeconds(3600)
        )
        val intent = Intent(context, PostActivity::class.java)
        intent.putExtra(Status.POST_TAG, post)
        ActivityScenario.launch<PostActivity>(intent)
        onView(withId(R.id.status_more)).perform(click())
        onView(withText(R.string.share_picture)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        Thread.sleep(2000)
        Intents.intended(expectedIntent)
    }



    @Test
    fun getNLikesReturnsCorrectFormat() {
        val status = Status(id="140364967936397312", uri="https://pixelfed.de/p/Miike/140364967936397312",
            created_at= OffsetDateTime.parse("2020-03-03T08:00:16+00:00").toInstant(),
            account= Account(id="115114166443970560", username="Miike", acct="Miike",
                url="https://pixelfed.de/Miike", display_name="Miike Duart", note="",
                avatar="https://pixelfed.de/storage/avatars/011/511/416/644/397/056/0/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
                avatar_static="https://pixelfed.de/storage/avatars/011/511/416/644/397/056/0/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
                header="", header_static="", locked=false, emojis= emptyList(), discoverable=false,
                created_at=Instant.parse("2019-12-24T15:42:35.000000Z"), statuses_count=71, followers_count=14,
                following_count=0, moved=null, fields=null, bot=false, source=null),
            content="""Day 8 <a href="https://pixelfed.de/discover/tags/rotavicentina?src=hash" title="#rotavicentina" class="u-url hashtag" rel="external nofollow noopener">#rotavicentina</a> <a href="https://pixelfed.de/discover/tags/hiking?src=hash" title="#hiking" class="u-url hashtag" rel="external nofollow noopener">#hiking</a> <a href="https://pixelfed.de/discover/tags/nature?src=hash" title="#nature" class="u-url hashtag" rel="external nofollow noopener">#nature</a>""",
            visibility=Status.Visibility.public, sensitive=false, spoiler_text="",
            media_attachments= listOf(
                Attachment(id="15888", type= Attachment.AttachmentType.image, url="https://pixelfed.de/storage/m/113a3e2124a33b1f5511e531953f5ee48456e0c7/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB.jpeg",
                    preview_url="https://pixelfed.de/storage/m/113a3e2124a33b1f5511e531953f5ee48456e0c7/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB_thumb.jpeg",
                    remote_url=null, text_url=null, description=null, blurhash=null, meta = null)
            ),
            application= Application(name="web", website=null, vapid_key=null), mentions=emptyList(),
            tags= listOf(Tag(name="hiking", url="https://pixelfed.de/discover/tags/hiking", history=null), Tag(name="nature", url="https://pixelfed.de/discover/tags/nature", history=null), Tag(name="rotavicentina", url="https://pixelfed.de/discover/tags/rotavicentina", history=null)),
            emojis= emptyList(), reblogs_count=0, favourites_count=0, replies_count=0, url="https://pixelfed.de/p/Miike/140364967936397312",
            in_reply_to_id=null, in_reply_to_account=null, reblog=null, poll=null, card=null, language=null, text=null, favourited=false, reblogged=false, muted=false, bookmarked=false, pinned=false)

        Assert.assertEquals("${status.favourites_count} Likes",
            status.getNLikes(getInstrumentation().targetContext))
    }

    @Test
    fun getNSharesReturnsCorrectFormat() {
        val status = Status(id="140364967936397312", uri="https://pixelfed.de/p/Miike/140364967936397312",
            created_at= Instant.parse("2020-03-03T08:00:16.00Z"),
            account= Account(id="115114166443970560", username="Miike", acct="Miike",
                url="https://pixelfed.de/Miike", display_name="Miike Duart", note="",
                avatar="https://pixelfed.de/storage/avatars/011/511/416/644/397/056/0/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
                avatar_static="https://pixelfed.de/storage/avatars/011/511/416/644/397/056/0/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
                header="", header_static="", locked=false, emojis= emptyList(), discoverable=false,
                created_at=Instant.parse("2019-12-24T15:42:35.000000Z"), statuses_count=71, followers_count=14,
                following_count=0, moved=null, fields=null, bot=false, source=null),
            content="""Day 8 <a href="https://pixelfed.de/discover/tags/rotavicentina?src=hash" title="#rotavicentina" class="u-url hashtag" rel="external nofollow noopener">#rotavicentina</a> <a href="https://pixelfed.de/discover/tags/hiking?src=hash" title="#hiking" class="u-url hashtag" rel="external nofollow noopener">#hiking</a> <a href="https://pixelfed.de/discover/tags/nature?src=hash" title="#nature" class="u-url hashtag" rel="external nofollow noopener">#nature</a>""",
            visibility=Status.Visibility.public, sensitive=false, spoiler_text="",
            media_attachments= listOf(
                Attachment(id="15888", type= Attachment.AttachmentType.image, url="https://pixelfed.de/storage/m/113a3e2124a33b1f5511e531953f5ee48456e0c7/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB.jpeg",
                    preview_url="https://pixelfed.de/storage/m/113a3e2124a33b1f5511e531953f5ee48456e0c7/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB_thumb.jpeg",
                    remote_url=null, text_url=null, description=null, blurhash=null, meta = null)
            ),
            application= Application(name="web", website=null, vapid_key=null), mentions=emptyList(),
            tags= listOf(Tag(name="hiking", url="https://pixelfed.de/discover/tags/hiking", history=null), Tag(name="nature", url="https://pixelfed.de/discover/tags/nature", history=null), Tag(name="rotavicentina", url="https://pixelfed.de/discover/tags/rotavicentina", history=null)),
            emojis= emptyList(), reblogs_count=0, favourites_count=0, replies_count=0, url="https://pixelfed.de/p/Miike/140364967936397312",
            in_reply_to_id=null, in_reply_to_account=null, reblog=null, poll=null, card=null, language=null, text=null, favourited=false, reblogged=false, muted=false, bookmarked=false, pinned=false)

        Assert.assertEquals("${status.reblogs_count} Shares",
            status.getNShares(context))
    }

    @After
    fun after() {
        Intents.release()
        clearData()
    }

}
