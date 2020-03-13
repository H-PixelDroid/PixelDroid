package com.h.pixeldroid

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedInstrumentedTest {
    private val feedJson = """[{"id":"140364967936397312","uri":"https:\/\/pixelfed.de\/p\/Miike\/140364967936397312","url":"https:\/\/pixelfed.de\/p\/Miike\/140364967936397312","in_reply_to_id":null,"in_reply_to_account_id":null,"reblog":null,"content":"Day 8 <a href=\"https:\/\/pixelfed.de\/discover\/tags\/rotavicentina?src=hash\" title=\"#rotavicentina\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#rotavicentina<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/hiking?src=hash\" title=\"#hiking\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#hiking<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/nature?src=hash\" title=\"#nature\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#nature<\/a>","created_at":"2020-03-03T08:00:16.000000Z","emojis":[],"replies_count":0,"reblogs_count":0,"favourites_count":0,"reblogged":null,"favourited":null,"muted":null,"sensitive":false,"spoiler_text":"","visibility":"public","mentions":[],"tags":[{"name":"hiking","url":"https:\/\/pixelfed.de\/discover\/tags\/hiking"},{"name":"nature","url":"https:\/\/pixelfed.de\/discover\/tags\/nature"},{"name":"rotavicentina","url":"https:\/\/pixelfed.de\/discover\/tags\/rotavicentina"}],"card":null,"poll":null,"application":{"name":"web","website":null},"language":null,"pinned":null,"account":{"id":"115114166443970560","username":"Miike","acct":"Miike","display_name":"Miike Duart","locked":false,"created_at":"2019-12-24T15:42:35.000000Z","followers_count":14,"following_count":0,"statuses_count":71,"note":"","url":"https:\/\/pixelfed.de\/Miike","avatar":"https:\/\/pixelfed.de\/storage\/avatars\/011\/511\/416\/644\/397\/056\/0\/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","avatar_static":"https:\/\/pixelfed.de\/storage\/avatars\/011\/511\/416\/644\/397\/056\/0\/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","header":"","header_static":"","emojis":[],"moved":null,"fields":null,"bot":false,"software":"pixelfed","is_admin":false},"media_attachments":[{"id":"15888","type":"image","url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f\/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB.jpeg","remote_url":null,"preview_url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/34dd6d9fb1762dac8c7ddeeaf789d2d8fa083c9f\/JtjO0eAbELpgO1UZqF5ydrKbCKRVyJUM1WAaqIeB_thumb.jpeg","text_url":null,"meta":null,"description":null}]},{"id":"140349785193451520","uri":"https:\/\/pixelfed.de\/p\/stephan\/140349785193451520","url":"https:\/\/pixelfed.de\/p\/stephan\/140349785193451520","in_reply_to_id":null,"in_reply_to_account_id":null,"reblog":null,"content":"","created_at":"2020-03-03T06:59:56.000000Z","emojis":[],"replies_count":0,"reblogs_count":0,"favourites_count":2,"reblogged":null,"favourited":null,"muted":null,"sensitive":false,"spoiler_text":"","visibility":"public","mentions":[],"tags":[],"card":null,"poll":null,"application":{"name":"web","website":null},"language":null,"pinned":null,"account":{"id":"908","username":"stephan","acct":"stephan","display_name":"Stephan","locked":false,"created_at":"2019-03-17T07:46:33.000000Z","followers_count":136,"following_count":25,"statuses_count":136,"note":"Musician, software developer &amp; hobby photographer.","url":"https:\/\/pixelfed.de\/stephan","avatar":"https:\/\/pixelfed.de\/storage\/avatars\/000\/000\/000\/908\/5nQzzsB1mkwKaUqQ9GNN_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","avatar_static":"https:\/\/pixelfed.de\/storage\/avatars\/000\/000\/000\/908\/5nQzzsB1mkwKaUqQ9GNN_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","header":"","header_static":"","emojis":[],"moved":null,"fields":null,"bot":false,"software":"pixelfed","is_admin":false},"media_attachments":[{"id":"15887","type":"image","url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/a1349f5183c2bac7d52880e8f5188df0f3b2d62a\/mvT3nYV6Wdu42Xh56Ny4VYaWU0OzbnC3wjxiqnKS.jpeg","remote_url":null,"preview_url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/a1349f5183c2bac7d52880e8f5188df0f3b2d62a\/mvT3nYV6Wdu42Xh56Ny4VYaWU0OzbnC3wjxiqnKS_thumb.jpeg","text_url":null,"meta":null,"description":null}]},{"id":"140276879742603264","uri":"https:\/\/pixelfed.de\/p\/fegrimaldi\/140276879742603264","url":"https:\/\/pixelfed.de\/p\/fegrimaldi\/140276879742603264","in_reply_to_id":null,"in_reply_to_account_id":null,"reblog":null,"content":"february 2 is the day to give flowers to Iemanj\u00e1. <a href=\"https:\/\/pixelfed.de\/discover\/tags\/salvador?src=hash\" title=\"#salvador\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#salvador<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/bahia?src=hash\" title=\"#bahia\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#bahia<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/brazil?src=hash\" title=\"#brazil\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#brazil<\/a> <a href=\"https:\/\/pixelfed.de\/discover\/tags\/iemanja?src=hash\" title=\"#iemanja\" class=\"u-url hashtag\" rel=\"external nofollow noopener\">#iemanja<\/a>","created_at":"2020-03-03T02:10:14.000000Z","emojis":[],"replies_count":0,"reblogs_count":0,"favourites_count":1,"reblogged":null,"favourited":null,"muted":null,"sensitive":false,"spoiler_text":"","visibility":"public","mentions":[],"tags":[{"name":"salvador","url":"https:\/\/pixelfed.de\/discover\/tags\/salvador"},{"name":"bahia","url":"https:\/\/pixelfed.de\/discover\/tags\/bahia"},{"name":"brazil","url":"https:\/\/pixelfed.de\/discover\/tags\/brazil"},{"name":"iemanja","url":"https:\/\/pixelfed.de\/discover\/tags\/iemanja"}],"card":null,"poll":null,"application":{"name":"web","website":null},"language":null,"pinned":null,"account":{"id":"137257212828585984","username":"fegrimaldi","acct":"fegrimaldi","display_name":"Fernanda Grimaldi","locked":false,"created_at":"2020-02-23T18:11:09.000000Z","followers_count":2,"following_count":7,"statuses_count":2,"note":"a little piece of Bahia in the fediverse.","url":"https:\/\/pixelfed.de\/fegrimaldi","avatar":"https:\/\/pixelfed.de\/storage\/avatars\/013\/725\/721\/282\/858\/598\/4\/oUPBit0TJso1xNhJfFqg_avatar.jpeg?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","avatar_static":"https:\/\/pixelfed.de\/storage\/avatars\/013\/725\/721\/282\/858\/598\/4\/oUPBit0TJso1xNhJfFqg_avatar.jpeg?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35","header":"","header_static":"","emojis":[],"moved":null,"fields":null,"bot":false,"software":"pixelfed","is_admin":false},"media_attachments":[{"id":"15886","type":"image","url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/feb878b4bd60b85ac840670c6b9c809fd76b628b\/lYMrx0WF8LDqn0vTRgNJaRs7stMKtAXrgzpMrWEr.jpeg","remote_url":null,"preview_url":"https:\/\/pixelfed.de\/storage\/m\/113a3e2124a33b1f5511e531953f5ee48456e0c7\/feb878b4bd60b85ac840670c6b9c809fd76b628b\/lYMrx0WF8LDqn0vTRgNJaRs7stMKtAXrgzpMrWEr_thumb.jpeg","text_url":null,"meta":null,"description":null}]}]"""
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)
    @Before
    fun before(){
        val server = MockWebServer()
        server.enqueue(MockResponse().addHeader("Content-Type", "application/json; charset=utf-8").setBody(feedJson))
        server.start()
        val baseUrl = server.url("")
        val preferences = InstrumentationRegistry.getInstrumentation()
            .targetContext.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", baseUrl.toString()).apply()
        ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun swipingDownOnHomepageShowsMorePosts() {
        Thread.sleep(5000)

        val firstDesc = withId(R.id.description)
        onView(withId(R.id.view_pager)).perform(swipeUp()).perform(swipeDown()).perform(swipeDown())
        onView(withId(R.id.description)).check(matches(firstDesc))
    }
}