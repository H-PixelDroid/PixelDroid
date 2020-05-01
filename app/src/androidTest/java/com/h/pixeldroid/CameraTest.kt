package com.h.pixeldroid

import android.Manifest
import androidx.fragment.app.testing.launchFragmentInContainer
import org.junit.Rule
import org.junit.Test
import androidx.test.rule.GrantPermissionRule
import com.h.pixeldroid.fragments.CameraFragment
import kotlinx.android.synthetic.main.fragment_camera.*

class CameraTest {

    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Test
    fun takePictureButton() {
        val scenario = launchFragmentInContainer<CameraFragment>()
        scenario.onFragment { fragment ->
            fragment.capture_button.performClick()
            Thread.sleep(300)
            assert(fragment.isHidden)
        }
    }

    @Test
    fun uploadButton() {
        val scenario = launchFragmentInContainer<CameraFragment>()
        scenario.onFragment { fragment ->
            fragment.upload_button.performClick()
            Thread.sleep(300)
            assert(fragment.isHidden)
        }
    }
}