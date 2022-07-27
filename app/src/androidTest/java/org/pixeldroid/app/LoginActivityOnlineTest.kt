package org.pixeldroid.app

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.SharedPreferences
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.testUtility.clearData
import org.pixeldroid.app.testUtility.initDB
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.pixeldroid.app.testUtility.PACKAGE_ID
import org.pixeldroid.app.testUtility.waitForView

@RunWith(AndroidJUnit4::class)
class LoginActivityOnlineTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context
    private lateinit var pref: SharedPreferences

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context = ApplicationProvider.getApplicationContext()
        pref = context.getSharedPreferences("${PACKAGE_ID}.pref", Context.MODE_PRIVATE)
        pref.edit().clear().apply()
        db = initDB(context)
        db.clearAllTables()
        db.close()
    }
    @After
    fun after() {
        clearData()
    }

    @Test
    fun notPixelfedInstance() {
        ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.editText))
            .perform(replaceText("localhost"), closeSoftKeyboard())
        onView(withId(R.id.connect_instance_button)).perform(click())
        onView(withId(R.id.editText))
            .check(matches(hasErrorText(context.getString(R.string.registration_failed))))
    }

    @Test
    fun emptyStringNotAllowed() {
        ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.connect_instance_button)).perform(click())
        onView(withId(R.id.editText)).check(matches(
            hasErrorText(context.getString(R.string.invalid_domain))
        ))
    }

    @Test
    fun wrongIntentReturnInfoFailsTest() {
        pref.edit()
            .putString("domain", "https://invalid.pixeldroid.org")
            .putString("clientID", "iwndoiuqwnd")
            .putString("clientSecret", "wlifowed")
            .apply()
        val uri = Uri.parse("oauth2redirect://${PACKAGE_ID}?code=sdfdqsf")
        val intent = Intent(ACTION_VIEW, uri, context, LoginActivity::class.java)
        ActivityScenario.launch<LoginActivity>(intent)
        waitForView(R.id.editText)
        Thread.sleep(100)
        onView(withId(R.id.editText)).check(matches(
            hasErrorText(context.getString(R.string.token_error))
        ))
    }

    @Test
    fun incompleteIntentReturnInfoFailsTest() {
        val uri = Uri.parse("oauth2redirect://${PACKAGE_ID}?code=")
        val intent = Intent(ACTION_VIEW, uri, context, LoginActivity::class.java)
        ActivityScenario.launch<LoginActivity>(intent)
        onView(withId(R.id.editText)).check(matches(
            hasErrorText(context.getString(R.string.auth_failed))
        ))
    }

    /*
    @Test
    fun correctIntentReturnLoadsMainActivity() {
        context = ApplicationProvider.getApplicationContext()
        db = initDB(context)
        db.clearAllTables()

        db.instanceDao().insertInstance(
            testiTestoInstance
        )

        db.userDao().insertUser(testiTesto)
        db.close()
        pref.edit()
            .putString("domain", testiTestoInstance.uri)
            .putString("clientID", testiTesto.clientId)
            .putString("clientSecret", testiTesto.clientSecret)
            .apply()
        val uri = Uri.parse("oauth2redirect://org.pixeldroid.app?code=$testiTesto.")
        val intent = Intent(ACTION_VIEW, uri, context, LoginActivity::class.java)
        ActivityScenario.launch<LoginActivity>(intent)
        Thread.sleep(1000)
        onView(withId(R.id.main_activity_main_linear_layout)).check(matches(isDisplayed()))
    }
     */
}