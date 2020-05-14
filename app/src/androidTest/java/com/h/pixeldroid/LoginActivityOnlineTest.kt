package com.h.pixeldroid

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
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.InstanceDatabaseEntity
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.testUtility.MockServer
import com.h.pixeldroid.utils.DBUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityOnlineTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context
    private lateinit var pref: SharedPreferences
    private lateinit var server: MockServer

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @Before
    fun setup() {
        server = MockServer()
        server.start()
        context = ApplicationProvider.getApplicationContext()
        pref = context.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        pref.edit().clear().apply()
        db = DBUtils.initDB(context)
        db.clearAllTables()
    }

    @Test
    fun connectToSavedAccount() {
        db.instanceDao().insertInstance(
            InstanceDatabaseEntity(
                uri = "some_uri",
                title = "PixelTest"
            )
        )
        db.userDao().insertUser(
            UserDatabaseEntity(
                user_id = "some_user_id",
                instance_uri = "some_uri",
                username = "Testi",
                display_name = "Testi Testo",
                avatar_static = "some_avatar_url"
            )
        )
        ActivityScenario.launch(LoginActivity::class.java)
        onView(withId(R.id.login_activity_instance_chooser_button)).perform(click())
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
            hasErrorText(context.getString(R.string.login_empty_string_error))
        ))
    }

    @Test
    fun wrongIntentReturnInfoFailsTest() {
        pref.edit()
            .putString("domain", "https://dhbfnhgbdbbet")
            .putString("clientID", "iwndoiuqwnd")
            .putString("clientSecret", "wlifowed")
            .apply()
        val uri = Uri.parse("oauth2redirect://com.h.pixeldroid?code=sdfdqsf")
        val intent = Intent(ACTION_VIEW, uri, context, LoginActivity::class.java)
        ActivityScenario.launch<LoginActivity>(intent)
        onView(withId(R.id.editText)).check(matches(
            hasErrorText(context.getString(R.string.token_error))
        ))
    }

    @Test
    fun incompleteIntentReturnInfoFailsTest() {
        val uri = Uri.parse("oauth2redirect://com.h.pixeldroid?code=")
        val intent = Intent(ACTION_VIEW, uri, context, LoginActivity::class.java)
        ActivityScenario.launch<LoginActivity>(intent)
        onView(withId(R.id.editText)).check(matches(
            hasErrorText(context.getString(R.string.auth_failed))
        ))
    }

    @Test
    fun correctIntentReturnLoadsMainActivity() {
        pref.edit()
            .putString("accessToken", "azerty")
            .putString("domain", server.getUrl().toString())
            .putString("clientID", "test_id")
            .putString("clientSecret", "test_secret")
            .apply()
        val uri = Uri.parse("oauth2redirect://com.h.pixeldroid?code=test_code")
        val intent = Intent(ACTION_VIEW, uri, context, LoginActivity::class.java)
        ActivityScenario.launch<LoginActivity>(intent)
        onView(withId(R.id.main_activity_main_linear_layout)).check(matches(isDisplayed()))
    }
}