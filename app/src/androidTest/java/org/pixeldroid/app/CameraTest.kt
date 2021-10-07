package org.pixeldroid.app
/*
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_CHOOSER
import android.widget.ImageButton
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.rule.GrantPermissionRule
import org.pixeldroid.app.utils.db.entities.InstanceDatabaseEntity
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.postCreation.camera.CameraFragment
import org.pixeldroid.app.testUtility.clearData
import org.pixeldroid.app.testUtility.initDB
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CameraTest {

    private lateinit var context: Context

    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Before
    fun before(){
        context = ApplicationProvider.getApplicationContext()
        val db = initDB(context)
        db.clearAllTables()
        db.instanceDao().insertInstance(
            InstanceDatabaseEntity(
                uri = "http://localhost",
                title = "PixelTest"
            )
        )

        db.userDao().insertUser(
            UserDatabaseEntity(
                    user_id = "123",
                    instance_uri = "http://localhost",
                    username = "Testi",
                    display_name = "Testi Testo",
                    avatar_static = "some_avatar_url",
                    isActive = true,
                    accessToken = "token",
                    refreshToken = "refreshToken",
                    clientId = "clientId",
                    clientSecret = "clientSecret"
            )
        )
        db.close()
        Intents.init()
    }
    @After
    fun after(){
        Intents.release()
        clearData()
    }
    /*
    @Test
    fun takePictureButton() {
        val scenario = launchFragmentInContainer<CameraFragment>()

        Thread.sleep(1000)

        scenario.onFragment { fragment ->
            fragment.camera_capture_button.performClick()
        }
        Thread.sleep(3000)

        Intents.intended(hasComponent(PostCreationActivity::class.java.name))
    }

     */

    @Test
    fun uploadButton() {
        val expectedIntent: Matcher<Intent> = CoreMatchers.allOf(
            IntentMatchers.hasAction(ACTION_CHOOSER)
        )

        val scenario = launchFragmentInContainer<CameraFragment>()
        scenario.onFragment { fragment ->
            fragment.view?.findViewById<ImageButton>(R.id.photo_view_button)?.performClick()
        }
        Thread.sleep(1000)

        Intents.intended(expectedIntent)
    }

    @Test
    fun switchButton() {
        val scenario = launchFragmentInContainer<CameraFragment>()
        scenario.onFragment { fragment ->
            fragment.view?.findViewById<ImageButton>(R.id.camera_switch_button)?.performClick()
        }
        Thread.sleep(1000)

        //FIXME this assert doesn't actually do anything...
        // All this test really does is make sure it doesn't crash
        scenario.onFragment { fragment ->
            assert(!fragment.isHidden)
        }
    }
}*/