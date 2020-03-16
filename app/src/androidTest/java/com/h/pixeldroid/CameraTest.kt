package com.h.pixeldroid

import androidx.fragment.app.testing.launchFragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.h.pixeldroid.fragments.CameraFragment
import kotlinx.android.synthetic.main.fragment_camera.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraTest {
    @Test
    fun testFragment() {
        val scenario = launchFragment<CameraFragment>()
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