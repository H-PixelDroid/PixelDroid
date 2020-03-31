package com.h.pixeldroid

import android.Manifest
import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.h.pixeldroid.fragments.NewPostFragment
import kotlinx.android.synthetic.main.fragment_new_post.*
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
    fun  testTakePictureButton() {
        val scenario = launchFragmentInContainer<NewPostFragment>()
        scenario.onFragment { fragment ->
        fragment.uploadPictureButton.performClick()
        }
    }

    @Test
    fun testUploadPictureButton() {
        val scenario = launchFragmentInContainer<NewPostFragment>()
        scenario.onFragment { fragment ->
            fragment.uploadPictureButton.performClick()
        }
    }
}