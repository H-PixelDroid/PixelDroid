package com.h.pixeldroid

import android.Manifest
import androidx.fragment.app.testing.launchFragmentInContainer
import org.junit.Rule
import org.junit.Test
import androidx.test.rule.GrantPermissionRule
import com.h.pixeldroid.fragments.CameraFragment
import kotlinx.android.synthetic.main.camera_ui_container.*
import kotlinx.android.synthetic.main.fragment_camera.*

class CameraTest {

    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)

    @Test
    fun takePictureButton() {
        val scenario = launchFragmentInContainer<CameraFragment>()
        scenario.onFragment { fragment ->
            fragment.camera_capture_button.performClick()
            Thread.sleep(3000)
            assert(fragment.isHidden)
        }
    }

    @Test
    fun uploadButton() {
        val scenario = launchFragmentInContainer<CameraFragment>()
        scenario.onFragment { fragment ->
            fragment.photo_view_button.performClick()
            Thread.sleep(300)
            assert(fragment.isHidden)
        }
    }
}