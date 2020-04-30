package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.objects.Attachment
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.testUtility.MockServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class PostTest {

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @Before
    fun before(){
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mockServer = MockServer()
        mockServer.start()
        val baseUrl = mockServer.getUrl()
        val preferences = context.getSharedPreferences(
            "com.h.pixeldroid.pref",
            Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", baseUrl.toString()).apply()
        val attachment = Attachment(
            id = "12",
            url = "https://wiki.gnugen.ch/lib/tpl/gnugen/images/logo_web.png"
        )
        val post = Status(
            id = "12",
            account = Account(
                id = "12",
                username = "douze"
            ),
            media_attachments = listOf(attachment)
        )
        val intent = Intent(context, PostActivity::class.java)
        intent.putExtra(Status.POST_TAG, post)
        ActivityScenario.launch<PostActivity>(intent)
    }

    @Test
    fun saveToGalleryTest() {
        onView(withId(R.id.postPicture)).perform(longClick())
        onView(withText(R.string.save_to_gallery)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        Thread.sleep(300)
        onView(withText(R.string.image_download_downloading)).inRoot(
            RootMatchers.hasWindowLayoutParams()
        ).check(matches(isDisplayed()))
    }
}