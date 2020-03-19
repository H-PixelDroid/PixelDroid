package com.h.pixeldroid

import android.Manifest
import android.os.Build
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.InstrumentationRegistry.getTargetContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.h.pixeldroid.fragments.CameraFragment
import kotlinx.android.synthetic.main.fragment_camera.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.rule.GrantPermissionRule;

@RunWith(AndroidJUnit4::class)
class CameraTest {

    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)


    @Test
    fun testFragment() {
        val scenario = launchFragmentInContainer<CameraFragment>()
        scenario.recreate()

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)


        scenario.onFragment { fragment ->
            assert(fragment.isAdded)
            assert(fragment.isResumed)
            assert(fragment.isVisible)
            assert(fragment.textureView.isAvailable)
        }

        scenario.moveToState(Lifecycle.State.DESTROYED)
    }
}