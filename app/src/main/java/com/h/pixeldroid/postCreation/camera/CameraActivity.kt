package com.h.pixeldroid.postCreation.camera

import android.os.Bundle
import com.h.pixeldroid.utils.BaseActivity
import com.h.pixeldroid.R

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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}