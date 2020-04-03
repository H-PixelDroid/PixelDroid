package com.h.pixeldroid

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.android.material.tabs.TabLayout
import com.h.pixeldroid.testUtility.MockServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostCreationTest {

    private val mockServer: MockServer = MockServer()

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(20)
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity>
            = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        mockServer.start()
        val baseUrl = mockServer.getUrl()
        val preferences = getInstrumentation()
            .targetContext.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", baseUrl.toString()).apply()
        ActivityScenario.launch(MainActivity::class.java).onActivity {
            a -> a.findViewById<TabLayout>(R.id.tabs).getTabAt(2)?.select()
        }
    }

    // UI elements correctly displayed
    @Test
    fun testUIDisplayTitle() {
        Thread.sleep(1000)
        onView(withText(R.string.create_a_new_post))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testUIDisplayTakePictureButton() {
        Thread.sleep(1000)
        onView(withText(R.string.take_a_picture))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testUIDisplayUploadPictureButton() {
        Thread.sleep(1000)
        onView(withText(R.string.upload_a_picture))
            .check(matches(isDisplayed()))
    }
}