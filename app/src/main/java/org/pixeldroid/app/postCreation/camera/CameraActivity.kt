package org.pixeldroid.app.postCreation.camera

import android.os.Bundle
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.R

class CameraActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.add_photo)

        val cameraFragment = CameraFragment()

        val arguments = Bundle()
        arguments.putBoolean("CameraActivity", true)
        cameraFragment.arguments = arguments

        supportFragmentManager.beginTransaction()
            .add(R.id.camera_activity_fragment, cameraFragment).commit()
    }
}