package com.h.pixeldroid

import android.content.Context
import androidx.core.view.get
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.h.pixeldroid.objects.Account
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ProfileTest {

    val account = Account(id="115114166443970560", username="Miike", acct="Miike",
        url="https://pixelfed.de/Miike", display_name="Miike Duart", note="",
        avatar="https://pixelfed.de/storage/avatars/011/511/416/644/397/056/0/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
        avatar_static="https://pixelfed.de/storage/avatars/011/511/416/644/397/056/0/ZhaopLJWTWJ3hsVCS5pS_avatar.png?v=d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35",
        header="", header_static="", locked=false, emojis= emptyList(), discoverable=false,
        created_at="2019-12-24T15:42:35.000000Z", statuses_count=71, followers_count=14,
        following_count=0, moved=null, fields=null, bot=false, source=null)
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)


    @Before
    fun before(){
        val server = MockWebServer()
        server.enqueue(MockResponse().addHeader("Content-Type", "application/json; charset=utf-8").setBody("{\n" +
                "      \"id\": \"1450\",\n" +
                "      \"username\": \"deerbard_photo\",\n" +
                "      \"acct\": \"deerbard_photo\",\n" +
                "      \"display_name\": \"deerbard photography\",\n" +
                "      \"locked\": false,\n" +
                "      \"created_at\": \"2018-08-01T12:58:21.000000Z\",\n" +
                "      \"followers_count\": 68,\n" +
                "      \"following_count\": 27,\n" +
                "      \"statuses_count\": 72,\n" +
                "      \"note\": \"\",\n" +
                "      \"url\": \"https://pixelfed.social/deerbard_photo\",\n" +
                "      \"avatar\": \"https://pixelfed.social/storage/avatars/000/000/001/450/SMSep5NoabDam1W8UDMh_avatar.png?v=4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a\",\n" +
                "      \"avatar_static\": \"https://pixelfed.social/storage/avatars/000/000/001/450/SMSep5NoabDam1W8UDMh_avatar.png?v=4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a\",\n" +
                "      \"header\": \"\",\n" +
                "      \"header_static\": \"\",\n" +
                "      \"emojis\": [],\n" +
                "      \"moved\": null,\n" +
                "      \"fields\": null,\n" +
                "      \"bot\": false,\n" +
                "      \"software\": \"pixelfed\",\n" +
                "      \"is_admin\": false\n" +
                "    }"))
        server.start()
        val baseUrl = server.url("")
        val preferences = InstrumentationRegistry.getInstrumentation()
            .targetContext.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", baseUrl.toString()).apply()
        ActivityScenario.launch(MainActivity::class.java)
    }
    @Test
    fun testFollowersTextView() {
        onView(withId(R.id.view_pager)).perform(ViewActions.swipeLeft()).perform(ViewActions.swipeLeft()).perform(
            ViewActions.swipeLeft()
        ).perform(ViewActions.swipeLeft())
        Thread.sleep(1000)
        onView(withId(R.id.followers)).check(matches(withText("Followers")))
        onView(withId(R.id.accountName)).check(matches(withText("deerbard_photo")))

    }

}
