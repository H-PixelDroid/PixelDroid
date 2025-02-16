package org.pixeldroid.app.postCreation.camera

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import org.pixeldroid.app.main.MainActivity
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityCameraBinding
import org.pixeldroid.app.postCreation.camera.CameraFragment.Companion.CAMERA_ACTIVITY
import org.pixeldroid.app.postCreation.camera.CameraFragment.Companion.CAMERA_ACTIVITY_STORY
import org.pixeldroid.app.utils.BaseActivity


class CameraActivity : BaseActivity() {
    private lateinit var binding: ActivityCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val cameraFragment = CameraFragment()

        val story: Boolean = intent.getBooleanExtra(CAMERA_ACTIVITY_STORY, false)

        if(story) supportActionBar?.setTitle(R.string.add_story)
        else supportActionBar?.setTitle(R.string.add_photo)

        // If this CameraActivity wasn't started from the shortcut,
        // tell the fragment it's in an activity (so that it sends back the result instead of
        // starting a new post creation process)
        if (intent.action != Intent.ACTION_VIEW) {
            val arguments = Bundle()
            arguments.putBoolean(CAMERA_ACTIVITY, true)
            arguments.putBoolean(CAMERA_ACTIVITY_STORY, story)
            cameraFragment.arguments = arguments
        } else {
            supportActionBar?.setTitle(R.string.new_post_shortcut_long)
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.camera_activity_fragment, cameraFragment).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // If this CameraActivity wasn't started from the shortcut, behave as usual
        if (intent.action != Intent.ACTION_VIEW) return super.onOptionsItemSelected(item)

        // Else, start a new MainActivity when "going back" on this activity
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