package com.h.pixeldroid

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MyProfileTest {
    private val accountJson = "{\n" +
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
            "    }"
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)
    private val dispatcher: Dispatcher = object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
            when (request.path) {
                "/api/v1/accounts/verify_credentials" -> return MockResponse().addHeader("Content-Type", "application/json; charset=utf-8").setResponseCode(200).setBody(accountJson)
            }
            return MockResponse().setResponseCode(404)
        }
    }

    @Before
    fun before(){
        val server = MockWebServer()
        server.dispatcher = dispatcher
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
        onView(withId(R.id.nbFollowersTextView)).check(matches(withText("68\nFollowers")))
        onView(withId(R.id.accountNameTextView)).check(matches(withText("deerbard_photo")))
    }
}
