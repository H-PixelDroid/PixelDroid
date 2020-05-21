package com.h.pixeldroid

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_CHOOSER
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.h.pixeldroid.fragments.CameraFragment
import kotlinx.android.synthetic.main.camera_ui_container.*
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File


class CameraTest {

    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun File.writeBitmap(bitmap: Bitmap) {
        outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
            out.flush()
        }
    }

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
        var scenario = launchFragmentInContainer<CameraFragment>()

        scenario.onFragment {
            val image = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
            image.eraseColor(Color.GREEN)
            val folder =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            if (!folder.exists()) {
                folder.mkdir()
            }
            val file = File.createTempFile("temp_img", ".png", folder)
            file.writeBitmap(image)

            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(file.extension)
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType)){_, _ ->
            }

        }
        scenario = launchFragmentInContainer<CameraFragment>()

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