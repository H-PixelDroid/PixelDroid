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
import androidx.media2.common.MediaMetadata
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import org.pixeldroid.app.databinding.ActivityMediaviewerBinding
import org.pixeldroid.app.utils.BaseActivity

class MediaViewerActivity : BaseActivity() {

    private lateinit var mediaPlayer: MediaPlayer
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

        val mediaItem: UriMediaItem = UriMediaItem.Builder(uri.toUri()).build()
        mediaItem.metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, description ?: "")
            .build()

        mediaPlayer = MediaPlayer(this)
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
        mediaPlayer.setAudioAttributes(AudioAttributesCompat.Builder()
                    .setLegacyStreamType(STREAM_MUSIC)
                    .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE)
                    .build()
        )

        mediaPlayer.prepare()

        binding.videoView.setPlayer(mediaPlayer)

        // Start actually playing the video
        mediaPlayer.play()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.close()
    }
}