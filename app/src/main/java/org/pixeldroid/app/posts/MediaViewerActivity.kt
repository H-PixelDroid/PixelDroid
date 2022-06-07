package org.pixeldroid.app.posts

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager.STREAM_MUSIC
import android.os.Bundle
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media.AudioAttributesCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import org.pixeldroid.app.databinding.ActivityMediaviewerBinding
import org.pixeldroid.app.utils.BaseActivity
import kotlin.random.Random

class MediaViewerActivity : BaseActivity() {

    private lateinit var mediaPlayer: ExoPlayer
    private lateinit var session: MediaSession
    private lateinit var binding: ActivityMediaviewerBinding


    companion object {
        const val VIDEO_URL_TAG = "video_url_mediavieweractivity"
        const val VIDEO_DESCRIPTION_TAG = "video_description_mediavieweractivity"

        fun openActivity(context: Context, url: String?, description: String?){
            val intent = Intent(context, MediaViewerActivity::class.java)
            intent.putExtra(VIDEO_URL_TAG, url)
            intent.putExtra(VIDEO_DESCRIPTION_TAG, description)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaviewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uri: String = intent.getStringExtra(VIDEO_URL_TAG).orEmpty()
        val description: String? = intent.getStringExtra(VIDEO_DESCRIPTION_TAG)

        val mediaItem: MediaItem = MediaItem.Builder().setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(description).build()).build()

        mediaPlayer = ExoPlayer.Builder(this).build()

        mediaPlayer.setMediaItem(mediaItem)

        binding.videoView.setControllerOnFullScreenModeChangedListener { fullscreen ->
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

        mediaPlayer.prepare()

        binding.videoView.player = mediaPlayer

        // Start actually playing the video
        mediaPlayer.play()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.pause()
        binding.videoView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.videoView.onResume()
        mediaPlayer.play()
    }
    override fun onStop() {
        super.onStop()
        binding.videoView.player = null
    }

    override fun onStart() {
        super.onStart()
        binding.videoView.player = mediaPlayer
    }
}