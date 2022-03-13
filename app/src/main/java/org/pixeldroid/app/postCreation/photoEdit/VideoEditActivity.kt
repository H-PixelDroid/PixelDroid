package org.pixeldroid.app.postCreation.photoEdit

import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media.AudioAttributesCompat
import androidx.media2.common.MediaMetadata
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import com.bumptech.glide.Glide
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityVideoEditBinding
import org.pixeldroid.app.postCreation.PostCreationActivity
import org.pixeldroid.app.posts.MediaViewerActivity
import org.pixeldroid.app.utils.BaseActivity


class VideoEditActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityVideoEditBinding.inflate(layoutInflater)

        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.edit)

        val videoUri: Uri? = intent.getParcelableExtra(PostCreationActivity.PICTURE_URI)
        val videoPosition: Int = intent.getIntExtra(PostCreationActivity.PICTURE_POSITION, 0)

        val description: String? = intent.getStringExtra(MediaViewerActivity.VIDEO_DESCRIPTION_TAG)

        val mediaItem: UriMediaItem = UriMediaItem.Builder(videoUri!!).build() // TODO: remove !!
        mediaItem.metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, description ?: "")
            .build()

        val mediaPlayer = MediaPlayer(this)
        mediaPlayer.setMediaItem(mediaItem)

        binding.videoView.mediaControlView?.setOnFullScreenListener{ view, fullscreen ->
            val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView)
            if (!fullscreen) {
                // Configure the behavior of the hidden system bars
                windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                // Hide both the status bar and the navigation bar
                windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
                supportActionBar?.show()
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                // Configure the behavior of the hidden system bars
                windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                // Hide both the status bar and the navigation bar
                windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())

                requestedOrientation =
                    if (mediaPlayer.videoSize.height < mediaPlayer.videoSize.width) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                supportActionBar?.hide()
            }
        }

        // Configure audio
        mediaPlayer.setAudioAttributes(
            AudioAttributesCompat.Builder()
            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE)
            .build()
        )

        mediaPlayer.prepare()

        binding.videoView.setPlayer(mediaPlayer)

    }
}