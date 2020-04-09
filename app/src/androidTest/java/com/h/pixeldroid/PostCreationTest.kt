package com.h.pixeldroid

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
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
    fun cameraFragmentMainLayoutTest() {
        Thread.sleep(1000)
        onView(withId(R.id.camera_fragment_main_linear_layout)).check(matches(isDisplayed()))
    }
}