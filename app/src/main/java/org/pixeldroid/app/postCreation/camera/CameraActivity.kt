package org.pixeldroid.app.postCreation.camera

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import org.pixeldroid.app.MainActivity
import org.pixeldroid.app.R
import org.pixeldroid.app.postCreation.camera.CameraFragment.Companion.CAMERA_ACTIVITY
import org.pixeldroid.app.postCreation.camera.CameraFragment.Companion.CAMERA_ACTIVITY_STORY
import org.pixeldroid.app.utils.BaseThemedWithBarActivity


class CameraActivity : BaseThemedWithBarActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val cameraFragment = CameraFragment()

        val story: Boolean = intent.getBooleanExtra(CAMERA_ACTIVITY_STORY, false)

        if(story) supportActionBar?.setTitle(R.string.add_story)
        else supportActionBar?.setTitle(R.string.add_photo)

        val arguments = Bundle()
        arguments.putBoolean(CAMERA_ACTIVITY, true)
        arguments.putBoolean(CAMERA_ACTIVITY_STORY, story)
        cameraFragment.arguments = arguments

        supportFragmentManager.beginTransaction()
            .add(R.id.camera_activity_fragment, cameraFragment).commit()
    }
}

/**
 * Launch without arguments so that it will open the
 * [org.pixeldroid.app.postCreation.PostCreationActivity] instead of "returning" to a non-existent
 * [org.pixeldroid.app.postCreation.PostCreationActivity]
 */
class CameraActivityShortcut : BaseThemedWithBarActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.new_post_shortcut_long)

        val cameraFragment = CameraFragment()

        supportFragmentManager.beginTransaction()
            .add(R.id.camera_activity_fragment, cameraFragment).commit()
    }

    //Start a new MainActivity when "going back" on this activity
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->  {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                super.startActivity(intent)
            }
        }
        return true
    }

}