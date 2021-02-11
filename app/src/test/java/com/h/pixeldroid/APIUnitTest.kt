package com.h.pixeldroid

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.utils.api.objects.*
import io.reactivex.Single
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class APIUnitTest {
    private val referenceFirstStatus = Status(id="140364967936397312", uri="https://pixelfed.de/p/Miike/140364967936397312",
        created_at= SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.Z").parse("2020-03-03T08:00:16.+0000"),
        account=Account(id="115114166443970560", username="Miike", acct="Miike",
            url="https://pixelfed.de/Miike", display_name="Miike Duart", note="",
            avatar="https://pixelfed.de/storage/avatars/011/511/416/644/397/056/0/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
            avatar_static="https://pixelfed.de/storage/avatars/011/511/416/644/397/056/0/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
            header="", header_static="", locked=false, emojis= emptyList(), discoverable=null,
            created_at="2019-12-24T15:42:35.000000Z", statuses_count=71, followers_count=14,
            following_count=0, moved=null, fields=null, bot=false, source=null),
        content="""Day 8 <a href="https://pixelfed.de/discover/tags/rotavicentina?src=hash" title="#rotavicentina" class="u-url hashtag" rel="external nofollow noopener">#rotavicentina</a> <a href="https://pixelfed.de/discover/tags/hiking?src=hash" title="#hiking" class="u-url hashtag" rel="external nofollow noopener">#hiking</a> <a href="https://pixelfed.de/discover/tags/nature?src=hash" title="#nature" class="u-url hashtag" rel="external nofollow noopener">#nature</a>""",
        visibility=Status.Visibility.public, sensitive=false, spoiler_text="",
        media_attachments= listOf(Attachment(id="15888", type= Attachment.AttachmentType.image, url="https://pixelfed.de/storage/m/113a3e2124a33b1f5511e531953f5ee48456e0c7/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB.jpeg",
            preview_url="https://pixelfed.de/storage/m/113a3e2124a33b1f5511e531953f5ee48456e0c7/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB_thumb.jpeg",
            remote_url=null, text_url=null, description=null, blurhash=null, meta = null)),
        application= Application(name="web", website=null, vapid_key=null), mentions=emptyList(),
        tags= listOf(Tag(name="hiking", url="https://pixelfed.de/discover/tags/hiking", history=null), Tag(name="nature", url="https://pixelfed.de/discover/tags/nature", history=null), Tag(name="rotavicentina", url="https://pixelfed.de/discover/tags/rotavicentina", history=null)),
        emojis= emptyList(), reblogs_count=0, favourites_count=0, replies_count=0, url="https://pixelfed.de/p/Miike/140364967936397312",
        in_reply_to_id=null, in_reply_to_account=null, reblog=null, poll=null, card=null, language=null, text=null, favourited=null, reblogged=null, muted=null, bookmarked=null, pinned=null)
    val sampleNotification = Notification("45723", Notification.NotificationType.favourite,
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+hh:mm").parse("2020-03-14T15:01:49+00:00")!!,
        Account("79574199701737472", "Spaziergaenger",
            "Spaziergaenger", "https://pixelfed.de/Spaziergaenger",
            "anonymous", "", "https://pixelfed.de/storage/avatars/007/957/419/970/173/747/2/KEg4YgCgsmzdgyVztszz_avatar.jpeg?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
            "https://pixelfed.de/storage/avatars/007/957/419/970/173/747/2/KEg4YgCgsmzdgyVztszz_avatar.jpeg?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
            locked=false, followers_count = 40, following_count = 0, statuses_count = 891, created_at = "1568728767", header = "", discoverable = true, emojis = emptyList(), header_static = ""),
        Status("144456497894658048","https://pixelfed.de/p/dante/144456497894658048",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.hhmmss'Z'").parse("2020-03-03T08:00:16.000000Z"), in_reply_to_id = null,
            in_reply_to_account = null, reblog = null,content = "Saturn V launch", emojis = emptyList(), reblogs_count = 0,
            favourites_count = 1, reblogged = false, favourited = false, muted = false, sensitive = false,
            spoiler_text = "", visibility = Status.Visibility.public, application = Application("web", null),
            language = null, pinned = false, mentions = emptyList(), tags = emptyList(), replies_count = 0,
            account = Account("136453537340198912", "dante", "dante", locked = false, following_count = 3,
                followers_count = 1,statuses_count = 1, note = "", url = "https://pixelfed.de/dante",
                avatar = "https://pixelfed.de/storage/avatars/default.png?v=5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9",
                emojis = emptyList(), header_static = "", header = "", created_at = "1582289858", avatar_static = "https://pixelfed.de/storage/avatars/default.png?v=5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9",
                discoverable = true, display_name = "Dante"), media_attachments = listOf(
                Attachment("16583",Attachment.AttachmentType.image, "https://pixelfed.de/storage/m/113a3e2124a33b1f5511e531953f5ee48456e0c7/0fa8bbe19cc23442034913a7c97fbe4527c1d63a/vs2vouJ86OvzxhK9ewhPlfPf4Y9IoQ5CHfiBIqad.jpeg",
                    "https://pixelfed.de/storage/m/113a3e2124a33b1f5511e531953f5ee48456e0c7/0fa8bbe19cc23442034913a7c97fbe4527c1d63a/vs2vouJ86OvzxhK9ewhPlfPf4Y9IoQ5CHfiBIqad_thumb.jpeg",
                    null, null, null, null)
            )
            , bookmarked = false, card = null, poll = null, text= null,url= "https://pixelfed.de/p/dante/144456497894658048")
    , user_id = "", instance_uri = ""
    )
    @get:Rule
    var wireMockRule = WireMockRule(8089)

    @Test
    fun api_correctly_translated_data_class() {
        stubFor(
            get(urlEqualTo("/api/v1/timelines/public"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                        """[{"id":"140364967936397312","uri":"https:\/\/pixelfed.de\/p\/Miike\/140364967936397312","url":"https:\/\/pixelfed.de\/p\/Miike\/140364967936397312","in_reply_to_id":null,"in_reply_to_account_id":null,"reblog":null,"content":"Day 8 <a href=\"https:\/\/pixelfed.de\/discover\/tags\/rotavicentina?src=hash\" title=\"#rotavicentina\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#rotavicentina<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/hiking?src=hash\" title=\"#hiking\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#hiking<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/nature?src=hash\" title=\"#nature\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#nature<\/a>","created_at":"2020-03-03T08:00:16.000000Z","emojis":[],"replies_count":0,"reblogs_count":0,"favourites_count":0,"reblogged":null,"favourited":null,"muted":null,"sensitive":false,"spoiler_text":"","visibility":"public","mentions":[],"tags":[{"name":"hiking","url":"https:\/\/pixelfed.de\/discover\/tags\/hiking"},{"name":"nature","url":"https:\/\/pixelfed.de\/discover\/tags\/nature"},{"name":"rotavicentina","url":"https:\/\/pixelfed.de\/discover\/tags\/rotavicentina"}],"card":null,"poll":null,"application":{"name":"web","website":null},"language":null,"pinned":null,"account":{"id":"115114166443970560","username":"Miike","acct":"Miike","display_name":"Miike Duart","locked":false,"created_at":"2019-12-24T15:42:35.000000Z","followers_count":14,"following_count":0,"statuses_count":71,"note":"","url":"https:\/\/pixelfed.de\/Miike","avatar":"https:\/\/pixelfed.de\/storage\/avatars\/011\/511\/416\/644\/397\/056\/0\/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","avatar_static":"https:\/\/pixelfed.de\/storage\/avatars\/011\/511\/416\/644\/397\/056\/0\/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","header":"","header_static":"","emojis":[],"moved":null,"fields":null,"bot":false,"software":"pixelfed","is_admin":false},"media_attachments":[{"id":"15888","type":"image","url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f\/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB.jpeg","remote_url":null,"preview_url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f\/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB_thumb.jpeg","text_url":null,"meta":null,"description":null}]},{"id":"140349785193451520","uri":"https:\/\/pixelfed.de\/p\/stephan\/140349785193451520","url":"https:\/\/pixelfed.de\/p\/stephan\/140349785193451520","in_reply_to_id":null,"in_reply_to_account_id":null,"reblog":null,"content":"","created_at":"2020-03-03T06:59:56.000000Z","emojis":[],"replies_count":0,"reblogs_count":0,"favourites_count":2,"reblogged":null,"favourited":null,"muted":null,"sensitive":false,"spoiler_text":"","visibility":"public","mentions":[],"tags":[],"card":null,"poll":null,"application":{"name":"web","website":null},"language":null,"pinned":null,"account":{"id":"908","username":"stephan","acct":"stephan","display_name":"Stephan","locked":false,"created_at":"2019-03-17T07:46:33.000000Z","followers_count":136,"following_count":25,"statuses_count":136,"note":"Musician, software developer &amp; hobby photographer.","url":"https:\/\/pixelfed.de\/stephan","avatar":"https:\/\/pixelfed.de\/storage\/avatars\/000\/000\/000\/908\/5nQzzsB1mkwKaUqQ9GNN_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","avatar_static":"https:\/\/pixelfed.de\/storage\/avatars\/000\/000\/000\/908\/5nQzzsB1mkwKaUqQ9GNN_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","header":"","header_static":"","emojis":[],"moved":null,"fields":null,"bot":false,"software":"pixelfed","is_admin":false},"media_attachments":[{"id":"15887","type":"image","url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/a1349f5183c2bac7d52880e8f5188df0f3b2d62a\/mvT3nYV6Wdu42Xh56Ny4VYaWU0OzbnC3wjxiqnKS.jpeg","remote_url":null,"preview_url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/a1349f5183c2bac7d52880e8f5188df0f3b2d62a\/mvT3nYV6Wdu42Xh56Ny4VYaWU0OzbnC3wjxiqnKS_thumb.jpeg","text_url":null,"meta":null,"description":null}]},{"id":"140276879742603264","uri":"https:\/\/pixelfed.de\/p\/fegrimaldi\/140276879742603264","url":"https:\/\/pixelfed.de\/p\/fegrimaldi\/140276879742603264","in_reply_to_id":null,"in_reply_to_account_id":null,"reblog":null,"content":"february 2 is the day to give flowers to Iemanj\u00e1. <a href=\"https:\/\/pixelfed.de\/discover\/tags\/salvador?src=hash\" title=\"#salvador\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#salvador<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/bahia?src=hash\" title=\"#bahia\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#bahia<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/brazil?src=hash\" title=\"#brazil\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#brazil<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/iemanja?src=hash\" title=\"#iemanja\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#iemanja<\/a>","created_at":"2020-03-03T02:10:14.000000Z","emojis":[],"replies_count":0,"reblogs_count":0,"favourites_count":1,"reblogged":null,"favourited":null,"muted":null,"sensitive":false,"spoiler_text":"","visibility":"public","mentions":[],"tags":[{"name":"salvador","url":"https:\/\/pixelfed.de\/discover\/tags\/salvador"},{"name":"bahia","url":"https:\/\/pixelfed.de\/discover\/tags\/bahia"},{"name":"brazil","url":"https:\/\/pixelfed.de\/discover\/tags\/brazil"},{"name":"iemanja","url":"https:\/\/pixelfed.de\/discover\/tags\/iemanja"}],"card":null,"poll":null,"application":{"name":"web","website":null},"language":null,"pinned":null,"account":{"id":"137257212828585984","username":"fegrimaldi","acct":"fegrimaldi","display_name":"Fernanda Grimaldi","locked":false,"created_at":"2020-02-23T18:11:09.000000Z","followers_count":2,"following_count":7,"statuses_count":2,"note":"a little piece of Bahia in the fediverse.","url":"https:\/\/pixelfed.de\/fegrimaldi","avatar":"https:\/\/pixelfed.de\/storage\/avatars\/013\/725\/721\/282\/858\/598\/4\/oUPBit0TJso1xNhJfFqg_avatar.jpeg?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","avatar_static":"https:\/\/pixelfed.de\/storage\/avatars\/013\/725\/721\/282\/858\/598\/4\/oUPBit0TJso1xNhJfFqg_avatar.jpeg?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","header":"","header_static":"","emojis":[],"moved":null,"fields":null,"bot":false,"software":"pixelfed","is_admin":false},"media_attachments":[{"id":"15886","type":"image","url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/feb878b4bd60b85ac840670c6b9c809fd76b628b\/lYMrx0WF8LDqn0vTRgNJaRs7stMKtAXrgzpMrWEr.jpeg","remote_url":null,"preview_url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/feb878b4bd60b85ac840670c6b9c809fd76b628b\/lYMrx0WF8LDqn0vTRgNJaRs7stMKtAXrgzpMrWEr_thumb.jpeg","text_url":null,"meta":null,"description":null}]}]""" )
        ))
        stubFor(
            get(urlEqualTo("/api/v1/timelines/home"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """[{"id":"140364967936397312","uri":"https:\/\/pixelfed.de\/p\/Miike\/140364967936397312","url":"https:\/\/pixelfed.de\/p\/Miike\/140364967936397312","in_reply_to_id":null,"in_reply_to_account_id":null,"reblog":null,"content":"Day 8 <a href=\"https:\/\/pixelfed.de\/discover\/tags\/rotavicentina?src=hash\" title=\"#rotavicentina\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#rotavicentina<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/hiking?src=hash\" title=\"#hiking\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#hiking<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/nature?src=hash\" title=\"#nature\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#nature<\/a>","created_at":"2020-03-03T08:00:16.000000Z","emojis":[],"replies_count":0,"reblogs_count":0,"favourites_count":0,"reblogged":null,"favourited":null,"muted":null,"sensitive":false,"spoiler_text":"","visibility":"public","mentions":[],"tags":[{"name":"hiking","url":"https:\/\/pixelfed.de\/discover\/tags\/hiking"},{"name":"nature","url":"https:\/\/pixelfed.de\/discover\/tags\/nature"},{"name":"rotavicentina","url":"https:\/\/pixelfed.de\/discover\/tags\/rotavicentina"}],"card":null,"poll":null,"application":{"name":"web","website":null},"language":null,"pinned":null,"account":{"id":"115114166443970560","username":"Miike","acct":"Miike","display_name":"Miike Duart","locked":false,"created_at":"2019-12-24T15:42:35.000000Z","followers_count":14,"following_count":0,"statuses_count":71,"note":"","url":"https:\/\/pixelfed.de\/Miike","avatar":"https:\/\/pixelfed.de\/storage\/avatars\/011\/511\/416\/644\/397\/056\/0\/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","avatar_static":"https:\/\/pixelfed.de\/storage\/avatars\/011\/511\/416\/644\/397\/056\/0\/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","header":"","header_static":"","emojis":[],"moved":null,"fields":null,"bot":false,"software":"pixelfed","is_admin":false},"media_attachments":[{"id":"15888","type":"image","url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f\/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB.jpeg","remote_url":null,"preview_url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f\/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB_thumb.jpeg","text_url":null,"meta":null,"description":null}]},{"id":"140349785193451520","uri":"https:\/\/pixelfed.de\/p\/stephan\/140349785193451520","url":"https:\/\/pixelfed.de\/p\/stephan\/140349785193451520","in_reply_to_id":null,"in_reply_to_account_id":null,"reblog":null,"content":"","created_at":"2020-03-03T06:59:56.000000Z","emojis":[],"replies_count":0,"reblogs_count":0,"favourites_count":2,"reblogged":null,"favourited":null,"muted":null,"sensitive":false,"spoiler_text":"","visibility":"public","mentions":[],"tags":[],"card":null,"poll":null,"application":{"name":"web","website":null},"language":null,"pinned":null,"account":{"id":"908","username":"stephan","acct":"stephan","display_name":"Stephan","locked":false,"created_at":"2019-03-17T07:46:33.000000Z","followers_count":136,"following_count":25,"statuses_count":136,"note":"Musician, software developer &amp; hobby photographer.","url":"https:\/\/pixelfed.de\/stephan","avatar":"https:\/\/pixelfed.de\/storage\/avatars\/000\/000\/000\/908\/5nQzzsB1mkwKaUqQ9GNN_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","avatar_static":"https:\/\/pixelfed.de\/storage\/avatars\/000\/000\/000\/908\/5nQzzsB1mkwKaUqQ9GNN_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","header":"","header_static":"","emojis":[],"moved":null,"fields":null,"bot":false,"software":"pixelfed","is_admin":false},"media_attachments":[{"id":"15887","type":"image","url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/a1349f5183c2bac7d52880e8f5188df0f3b2d62a\/mvT3nYV6Wdu42Xh56Ny4VYaWU0OzbnC3wjxiqnKS.jpeg","remote_url":null,"preview_url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/a1349f5183c2bac7d52880e8f5188df0f3b2d62a\/mvT3nYV6Wdu42Xh56Ny4VYaWU0OzbnC3wjxiqnKS_thumb.jpeg","text_url":null,"meta":null,"description":null}]},{"id":"140276879742603264","uri":"https:\/\/pixelfed.de\/p\/fegrimaldi\/140276879742603264","url":"https:\/\/pixelfed.de\/p\/fegrimaldi\/140276879742603264","in_reply_to_id":null,"in_reply_to_account_id":null,"reblog":null,"content":"february 2 is the day to give flowers to Iemanj\u00e1. <a href=\"https:\/\/pixelfed.de\/discover\/tags\/salvador?src=hash\" title=\"#salvador\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#salvador<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/bahia?src=hash\" title=\"#bahia\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#bahia<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/brazil?src=hash\" title=\"#brazil\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#brazil<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/iemanja?src=hash\" title=\"#iemanja\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#iemanja<\/a>","created_at":"2020-03-03T02:10:14.000000Z","emojis":[],"replies_count":0,"reblogs_count":0,"favourites_count":1,"reblogged":null,"favourited":null,"muted":null,"sensitive":false,"spoiler_text":"","visibility":"public","mentions":[],"tags":[{"name":"salvador","url":"https:\/\/pixelfed.de\/discover\/tags\/salvador"},{"name":"bahia","url":"https:\/\/pixelfed.de\/discover\/tags\/bahia"},{"name":"brazil","url":"https:\/\/pixelfed.de\/discover\/tags\/brazil"},{"name":"iemanja","url":"https:\/\/pixelfed.de\/discover\/tags\/iemanja"}],"card":null,"poll":null,"application":{"name":"web","website":null},"language":null,"pinned":null,"account":{"id":"137257212828585984","username":"fegrimaldi","acct":"fegrimaldi","display_name":"Fernanda Grimaldi","locked":false,"created_at":"2020-02-23T18:11:09.000000Z","followers_count":2,"following_count":7,"statuses_count":2,"note":"a little piece of Bahia in the fediverse.","url":"https:\/\/pixelfed.de\/fegrimaldi","avatar":"https:\/\/pixelfed.de\/storage\/avatars\/013\/725\/721\/282\/858\/598\/4\/oUPBit0TJso1xNhJfFqg_avatar.jpeg?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","avatar_static":"https:\/\/pixelfed.de\/storage\/avatars\/013\/725\/721\/282\/858\/598\/4\/oUPBit0TJso1xNhJfFqg_avatar.jpeg?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","header":"","header_static":"","emojis":[],"moved":null,"fields":null,"bot":false,"software":"pixelfed","is_admin":false},"media_attachments":[{"id":"15886","type":"image","url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/feb878b4bd60b85ac840670c6b9c809fd76b628b\/lYMrx0WF8LDqn0vTRgNJaRs7stMKtAXrgzpMrWEr.jpeg","remote_url":null,"preview_url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/feb878b4bd60b85ac840670c6b9c809fd76b628b\/lYMrx0WF8LDqn0vTRgNJaRs7stMKtAXrgzpMrWEr_thumb.jpeg","text_url":null,"meta":null,"description":null}]}]""" )
                ))
        val statuses: List<Status>
        val statusesHome: List<Status>

        runBlocking {
            statuses = PixelfedAPI.createFromUrl("http://localhost:8089")
                    .timelinePublic(null, null, null, null, null)
            statusesHome = PixelfedAPI.createFromUrl("http://localhost:8089")
                .timelineHome("abc", null, null, null,null, null)
        }


        val f = statuses[0]
        val g = statuses[0]

        assertStatusEqualsToReference(f)
        assertStatusEqualsToReference(g)
             }

    @Test
    fun register_application(){
        stubFor(
            post(urlEqualTo("/api/v1/apps"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(""" {"id":3197,"name":"Pixeldroid","website":null,"redirect_uri":"urn:ietf:wg:oauth:2.0:oob","client_id":3197,"client_secret":"hhRwLupqUJPghKsZzpZtxNV67g5DBdPYCqW6XE3m","vapid_key":null}"""
                        )))
        val application: Application = runBlocking {
            PixelfedAPI.createFromUrl("http://localhost:8089")
                .registerApplication("Pixeldroid", "urn:ietf:wg:oauth:2.0:oob", "read write follow")
        }

        assertEquals("3197", application.client_id)
        assertEquals("hhRwLupqUJPghKsZzpZtxNV67g5DBdPYCqW6XE3m", application.client_secret)
        assertEquals("Pixeldroid", application.name)
        assertEquals(null, application.website)
        assertEquals(null, application.vapid_key)
    }
    @Test
    fun obtainToken(){
        stubFor(
            post(urlEqualTo("/oauth/token"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{
  "access_token": "ZA-Yj3aBD8U8Cm7lKUp-lm9O9BmDgdhHzDeqsY8tlL0",
  "refresh_token": "ZA-Yj3aBD8U8Cm7lKUp-sqfdsqfdqfsdfqds",
  "token_type": "Bearer",
  "scope": "read write follow push",
  "created_at": 1573979017
}"""
                        )))
        val OAUTH_SCHEME = "oauth2redirect"
        val SCOPE = "read write follow"
        val PACKAGE_ID = "com.h.pixeldroid"

        val token: Token =  runBlocking {
            PixelfedAPI.createFromUrl("http://localhost:8089")
                .obtainToken(
                    "123", "ssqdfqsdfqds", "$OAUTH_SCHEME://$PACKAGE_ID", SCOPE, "abc",
                    "authorization_code"
                )
        }
        assertEquals("ZA-Yj3aBD8U8Cm7lKUp-lm9O9BmDgdhHzDeqsY8tlL0", token.access_token)
        assertEquals("Bearer", token.token_type)
        assertEquals("read write follow push", token.scope)
        assertEquals(1573979017, token.created_at)
        assertEquals(Token("ZA-Yj3aBD8U8Cm7lKUp-lm9O9BmDgdhHzDeqsY8tlL0", "ZA-Yj3aBD8U8Cm7lKUp-sqfdsqfdqfsdfqds","Bearer", "read write follow push",1573979017), token)


    }
}

fun assertStatusEqualsToReference(actual: Status){
    assert(
        ((actual.id=="140364967936397312"
                && actual.uri=="https://pixelfed.de/p/Miike/140364967936397312"
                && actual.created_at==SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.Z").parse("2020-03-03T08:00:16.+0000")
                && actual.account!!.id=="115114166443970560"&& actual.account!!.username=="Miike"&& actual.account!!.acct=="Miike" &&
                actual.account!!.url=="https://pixelfed.de/Miike"&& actual.account!!.display_name=="Miike Duart"&& actual.account!!.note==""&&
                actual.account!!.avatar=="https://pixelfed.de/storage/avatars/011/511/416/644/397/056/0/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35"&&
                actual.account!!.avatar_static=="https://pixelfed.de/storage/avatars/011/511/416/644/397/056/0/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35"&&
                actual.account!!.header==""&& actual.account!!.header_static=="") && !actual.account!!.locked!! && actual.account!!.emojis== emptyList<Emoji>() && actual.account!!.discoverable == null && actual.account!!.created_at=="2019-12-24T15:42:35.000000Z" && actual.account!!.statuses_count==71 && actual.account!!.followers_count==14 && actual.account!!.following_count==0 && actual.account!!.moved==null && actual.account!!.fields==null && !actual.account!!.bot!! && actual.account!!.source==null && actual.content == """Day 8 <a href="https://pixelfed.de/discover/tags/rotavicentina?src=hash" title="#rotavicentina" class="u-url hashtag" rel="external nofollow noopener">#rotavicentina</a> <a href="https://pixelfed.de/discover/tags/hiking?src=hash" title="#hiking" class="u-url hashtag" rel="external nofollow noopener">#hiking</a> <a href="https://pixelfed.de/discover/tags/nature?src=hash" title="#nature" class="u-url hashtag" rel="external nofollow noopener">#nature</a>""" && actual.visibility==Status.Visibility.public) && !actual.sensitive!! && actual.spoiler_text==""
    )
    val attchmnt = actual.media_attachments!![0]
    assert(attchmnt.id == "15888" && attchmnt.type == Attachment.AttachmentType.image && attchmnt.url=="https://pixelfed.de/storage/m/113a3e2124a33b1f5511e531953f5ee48456e0c7/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB.jpeg" &&
            attchmnt.preview_url =="https://pixelfed.de/storage/m/113a3e2124a33b1f5511e531953f5ee48456e0c7/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB_thumb.jpeg" &&
            attchmnt.remote_url ==null && attchmnt.text_url==null && attchmnt.description==null && attchmnt.blurhash==null )
    assert( actual.application!!.name=="web" && actual.application!!.website==null && actual.application!!.vapid_key==null && actual.mentions==emptyList<Mention>())

    val firstTag =actual.tags!![0]

    assert(firstTag.name=="hiking" && firstTag.url=="https://pixelfed.de/discover/tags/hiking" && firstTag.history==null &&
            actual.emojis== emptyList<Emoji>() && actual.reblogs_count==0 && actual.favourites_count==0&& actual.replies_count==0 && actual.url=="https://pixelfed.de/p/Miike/140364967936397312")
//    assert(actual.in_reply_to_id==null && actual.in_reply_to_account==null && actual.reblog==null && actual.poll==null && actual.card==null && actual.language==null && actual.text==null && !actual.favourited!! && !actual.reblogged!! && !actual.muted!! && !actual.bookmarked!! && !actual.pinned!!)


}