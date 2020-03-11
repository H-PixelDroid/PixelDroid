package com.h.pixeldroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.models.Post
import com.h.pixeldroid.models.Post.Companion.POST_FRAG_TAG
import com.h.pixeldroid.objects.*

class FeedActivity : AppCompatActivity() {
    private val status = Status(id="140364967936397312", uri="https://pixelfed.de/p/Miike/140364967936397312",
        created_at="2020-03-03T08:00:16.000000Z",
        account= Account(id="115114166443970560", username="Miike", acct="Miike",
            url="https://pixelfed.de/Miike", display_name="Miike Duart", note="",
            avatar="https://pixelfed.de/storage/avatars/011/511/416/644/397/056/0/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
            avatar_static="https://pixelfed.de/storage/avatars/011/511/416/644/397/056/0/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
            header="", header_static="", locked=false, emojis= emptyList(), discoverable=false,
            created_at="2019-12-24T15:42:35.000000Z", statuses_count=71, followers_count=14,
            following_count=0, moved=null, fields=null, bot=false, source=null),
        content="""Day 8 <a href="https://pixelfed.de/discover/tags/rotavicentina?src=hash" title="#rotavicentina" class="u-url hashtag" rel="external nofollow noopener">#rotavicentina</a> <a href="https://pixelfed.de/discover/tags/hiking?src=hash" title="#hiking" class="u-url hashtag" rel="external nofollow noopener">#hiking</a> <a href="https://pixelfed.de/discover/tags/nature?src=hash" title="#nature" class="u-url hashtag" rel="external nofollow noopener">#nature</a>""",
        visibility=Status.Visibility.public, sensitive=false, spoiler_text="",
        media_attachments= listOf(
            Attachment(id="15888", type= Attachment.AttachmentType.image, url="https://pixelfed.de/storage/m/113a3e2124a33b1f5511e531953f5ee48456e0c7/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB.jpeg",
                preview_url="https://pixelfed.de/storage/m/113a3e2124a33b1f5511e531953f5ee48456e0c7/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB_thumb.jpeg",
                remote_url=null, text_url=null, description=null, blurhash=null)
        ),
        application= Application(name="web", website=null, vapid_key=null), mentions=emptyList(),
        tags= listOf(Tag(name="hiking", url="https://pixelfed.de/discover/tags/hiking", history=null), Tag(name="nature", url="https://pixelfed.de/discover/tags/nature", history=null), Tag(name="rotavicentina", url="https://pixelfed.de/discover/tags/rotavicentina", history=null)),
        emojis= emptyList(), reblogs_count=21, favourites_count=7, replies_count=0, url="https://pixelfed.de/p/Miike/140364967936397312",
        in_reply_to_id=null, in_reply_to_account=null, reblog=null, poll=null, card=null, language=null, text=null, favourited=false, reblogged=false, muted=false, bookmarked=false, pinned=false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed)
        findViewById<TextView>(R.id.FeedText).text = "Feed"

        //Connect to PixelFed to retrieve the public timeline
        /*val api = PixelfedAPI.create("https://pixelfed.de")
        val statuses = api.timelinePublic().execute().body()*/

        val post = Post(status)

        val postFragment = PostFragment.newInstance(post)
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.postFragment, postFragment, POST_FRAG_TAG)
    }
}
