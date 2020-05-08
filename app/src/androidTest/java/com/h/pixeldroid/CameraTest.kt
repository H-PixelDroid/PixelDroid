package com.h.pixeldroid

import android.Manifest
import android.content.Context
import androidx.core.view.isVisible
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.h.pixeldroid.fragments.CameraFragment
import kotlinx.android.synthetic.main.fragment_camera.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class CameraTest {

    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Before
    fun before(){
        val preferences = InstrumentationRegistry.getInstrumentation()
            .targetContext.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", "http://localhost").apply()
    }

    @Test
    fun  takePictureButton() {
        val scenario = launchFragmentInContainer<CameraFragment>()
        scenario.onFragment { fragment ->
        fragment.capture_button.performClick()
        }
    }

    @Test
    fun uploadPictureButton() {
        val scenario = launchFragmentInContainer<CameraFragment>()
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onFragment { fragment ->
            fragment.upload_button.performClick()
            assert(!fragment.isVisible)
            assert(fragment.requireActivity().window.decorView.rootView.isVisible)
        }
    }

    @Test
    fun openCameraAndTakePicture() {
        val scenario = launchFragmentInContainer<CameraFragment>()
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onFragment { fragment ->
            val activity = fragment.requireActivity()
            fragment.onCreateView(activity.layoutInflater, activity.findViewById(android.R.id.content), null)
        }
    }

    @Test
    fun switchCameraButton() {
        val scenario = launchFragmentInContainer<CameraFragment>()
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onFragment { fragment ->
            fragment.flip_button.performClick()
        }
    }

}