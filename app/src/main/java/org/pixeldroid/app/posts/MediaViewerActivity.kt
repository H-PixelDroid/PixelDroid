package org.pixeldroid.app.posts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media2.common.MediaMetadata
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import org.pixeldroid.app.databinding.ActivityMediaviewerBinding
import org.pixeldroid.app.utils.BaseActivity

class MediaViewerActivity : BaseActivity() {

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

        val mediaPlayer = MediaPlayer(this)
        mediaPlayer.setMediaItem(mediaItem)
        mediaPlayer.prepare()

        binding.videoView.setPlayer(mediaPlayer)

        // Start actually playing the video
        mediaPlayer.play()
    }
}