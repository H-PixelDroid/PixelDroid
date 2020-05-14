package com.h.pixeldroid

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_CHOOSER
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import org.junit.Rule
import org.junit.Test
import androidx.test.rule.GrantPermissionRule
import com.h.pixeldroid.fragments.CameraFragment
import kotlinx.android.synthetic.main.camera_ui_container.*
import kotlinx.android.synthetic.main.fragment_camera.*
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before

class CameraTest {

    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)

    @Before
    fun before(){
        Intents.init()
    }
    @After
    fun after(){
        Intents.release()
    }
    @Test
    fun takePictureButton() {
        val scenario = launchFragmentInContainer<CameraFragment>()
        Thread.sleep(2000)
        scenario.onFragment { fragment ->
            fragment.camera_capture_button.performClick()
        }
        scenario.onFragment { fragment ->
            assert(fragment.isHidden)
        }

    }

    @Test
    fun uploadButton() {
        val expectedIntent: Matcher<Intent> = CoreMatchers.allOf(
            IntentMatchers.hasAction(ACTION_CHOOSER)
        )

        val scenario = launchFragmentInContainer<CameraFragment>()
        scenario.onFragment { fragment ->
            fragment.photo_view_button.performClick()
        }
        Thread.sleep(1000)

        Intents.intended(expectedIntent)



    }

    @Test
    fun switchButton() {
        val scenario = launchFragmentInContainer<CameraFragment>()
        scenario.onFragment { fragment ->
            fragment.camera_switch_button.performClick()
        }
        Thread.sleep(1000)
        scenario.onFragment { fragment ->
            assert(!fragment.isHidden)
        }
    }
}