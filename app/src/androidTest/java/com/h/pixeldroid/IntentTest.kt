package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.h.pixeldroid.utils.db.AppDatabase
import com.h.pixeldroid.utils.db.entities.InstanceDatabaseEntity
import com.h.pixeldroid.utils.db.entities.UserDatabaseEntity
import com.h.pixeldroid.posts.StatusViewHolder
import com.h.pixeldroid.utils.api.objects.Account
import com.h.pixeldroid.utils.api.objects.Account.Companion.ACCOUNT_TAG
import com.h.pixeldroid.settings.AboutActivity
import com.h.pixeldroid.testUtility.*
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class IntentTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @get:Rule
    var mLoginActivityActivityTestRule =
        ActivityTestRule(
            AboutActivity::class.java
        )

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
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
    fun clickingMentionOpensProfile() {
        ActivityScenario.launch(MainActivity::class.java)

        val account = Account("265626292148375552", "user2", "user2",
            "https://testing2.pixeldroid.org/user2", "User 2",
            "",
            "https://testing2.pixeldroid.org/storage/avatars/default.jpg?v=0",
            "https://testing2.pixeldroid.org/storage/avatars/default.jpg?v=0",
            "", "", false, emptyList(), null,
            "2021-02-11T23:44:03.000000Z", 0, 1, 2,
            null, null, false, null)
        val expectedIntent: Matcher<Intent> = CoreMatchers.allOf(
            IntentMatchers.hasExtra(ACCOUNT_TAG, account)
        )

        waitForView(R.id.description)

        //Click the mention
        Espresso.onView(ViewMatchers.withId(R.id.list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<StatusViewHolder>
                (0, clickClickableSpanInDescription("@user2")))

        //Wait a bit
        Thread.sleep(1000)

        //Check that the right intent was launched
        intended(expectedIntent)
    }


    @Test
    fun clickEditProfileMakesIntent() {
        ActivityScenario.launch(MainActivity::class.java)

        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout))
            .check(ViewAssertions.matches(DrawerMatchers.isClosed())) // Left Drawer should be closed.
            .perform(DrawerActions.open()) // Open Drawer

        val expectedIntent: Matcher<Intent> = CoreMatchers.allOf(
            IntentMatchers.hasAction(Intent.ACTION_VIEW),
            IntentMatchers.hasDataString(CoreMatchers.containsString("settings/home"))
        )

        // Start the screen of your activity.
        Espresso.onView(ViewMatchers.withText(R.string.menu_account)).perform(ViewActions.click())
        // Check that profile activity was opened.
        Espresso.onView(ViewMatchers.withId(R.id.editButton))
            .perform(ViewActions.click())
        intended(expectedIntent)

    }

    @After
    fun after() {
        Intents.release()
        clearData()
    }
}