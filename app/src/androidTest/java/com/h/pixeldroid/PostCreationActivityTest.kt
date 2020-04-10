package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.h.pixeldroid.testUtility.MockServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostCreationActivityTest {

    val mockServer = MockServer()

    @get:Rule
    val globalTimeout: Timeout = Timeout.seconds(30)
    @get:Rule
    val rule = ActivityScenarioRule(PostCreationActivity::class.java)

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        mockServer.start()
        val baseUrl = mockServer.getUrl()
        val preferences = context.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", baseUrl.toString()).apply()

        val intent = Intent(context, PostCreationActivity::class.java)
        ActivityScenario.launch<PostCreationActivity>(intent)
    }

    @Test
    fun checkUiComponents() {
        onView(withId(R.id.new_post_description_input_field)).check(matches(isDisplayed()))
    }
}