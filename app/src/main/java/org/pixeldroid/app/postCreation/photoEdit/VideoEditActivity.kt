package org.pixeldroid.app.postCreation.photoEdit

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.core.os.HandlerCompat
import androidx.media.AudioAttributesCompat
import androidx.media2.common.MediaMetadata
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import com.arthenica.ffmpegkit.*
import com.bumptech.glide.Glide
import com.google.android.material.slider.RangeSlider
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivityVideoEditBinding
import org.pixeldroid.app.postCreation.PostCreationActivity
import org.pixeldroid.app.postCreation.carousel.dpToPx
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.ffmpegSafeUri
import java.io.File


class VideoEditActivity : BaseActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private var videoPosition: Int = -1
    private lateinit var binding: ActivityVideoEditBinding
    // Map photoData indexes to FFmpeg Session IDs
    private val sessionList: ArrayList<Long> = arrayListOf()
    private val tempFiles: ArrayList<File> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setTitle(R.string.toolbar_title_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)


        binding.videoRangeSeekBar.setCustomThumbDrawablesForValues(R.drawable.thumb_left,R.drawable.double_circle,R.drawable.thumb_right)
        binding.videoRangeSeekBar.thumbRadius = 20.dpToPx(this)


        val resultHandler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())

        val uri = intent.getParcelableExtra<Uri>(PhotoEditActivity.PICTURE_URI)!!
        videoPosition = intent.getIntExtra(PhotoEditActivity.PICTURE_POSITION, -1)

        val inputVideoPath = ffmpegSafeUri(uri)
        val mediaInformation: MediaInformation? = FFprobeKit.getMediaInformation(inputVideoPath).mediaInformation

        binding.muter.setOnClickListener {
            binding.muter.isSelected = !binding.muter.isSelected
        }

        //Duration in seconds, or null
        val duration: Float? = mediaInformation?.duration?.toFloatOrNull()

        binding.videoRangeSeekBar.valueFrom = 0f
        binding.videoRangeSeekBar.valueTo = duration ?: 100f
        binding.videoRangeSeekBar.values = listOf(0f,(duration?: 100f) / 2, duration ?: 100f)


        val mediaItem: UriMediaItem = UriMediaItem.Builder(uri).build()
        mediaItem.metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, "")
            .build()

        mediaPlayer = MediaPlayer(this)
        mediaPlayer.setMediaItem(mediaItem)

        //binding.videoView.mediaControlView?.setMediaController()

        // Configure audio
        mediaPlayer.setAudioAttributes(AudioAttributesCompat.Builder()
            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE)
            .build()
        )

        findViewById<FrameLayout?>(R.id.progress_bar)?.visibility = View.GONE

        mediaPlayer.prepare()

        binding.muter.setOnClickListener {
            if(!binding.muter.isSelected) mediaPlayer.playerVolume = 0f
            else mediaPlayer.playerVolume = 1f
            binding.muter.isSelected = !binding.muter.isSelected
        }

        binding.videoView.setPlayer(mediaPlayer)

        mediaPlayer.seekTo((binding.videoRangeSeekBar.values[1]*1000).toLong())

        object : Runnable {
            override fun run() {
                val getCurrent = mediaPlayer.currentPosition / 1000f
                if(getCurrent >= binding.videoRangeSeekBar.values[0] && getCurrent <= binding.videoRangeSeekBar.values[2] ) {
                    binding.videoRangeSeekBar.values = listOf(binding.videoRangeSeekBar.values[0],getCurrent, binding.videoRangeSeekBar.values[2])
                }
                Handler(Looper.getMainLooper()).postDelayed(this, 1000)
            }
        }.run()

        binding.videoRangeSeekBar.addOnChangeListener { rangeSlider: RangeSlider, value, fromUser ->
            // Responds to when the middle slider's value is changed
            if(fromUser && value != rangeSlider.values[0] && value != rangeSlider.values[2]) {
                mediaPlayer.seekTo((rangeSlider.values[1]*1000).toLong())
            }
        }

        binding.videoRangeSeekBar.setLabelFormatter { value: Float ->
            DateUtils.formatElapsedTime(value.toLong())
        }


        val thumbInterval: Float? = duration?.div(7)

        thumbInterval?.let {
            thumbnail(uri, resultHandler, binding.thumbnail1, it)
            thumbnail(uri, resultHandler, binding.thumbnail2, it.times(2))
            thumbnail(uri, resultHandler, binding.thumbnail3, it.times(3))
            thumbnail(uri, resultHandler, binding.thumbnail4, it.times(4))
            thumbnail(uri, resultHandler, binding.thumbnail5, it.times(5))
            thumbnail(uri, resultHandler, binding.thumbnail6, it.times(6))
            thumbnail(uri, resultHandler, binding.thumbnail7, it.times(7))
        }


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_save -> {
               returnWithValues()
            }
            R.id.action_reset -> {
                resetControls()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (noEdits()) super.onBackPressed()
        else {
            val builder = AlertDialog.Builder(this)
            builder.apply {
                setMessage(R.string.save_before_returning)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    returnWithValues()
                }
                setNegativeButton(R.string.no_cancel_edit) { _, _ ->
                    super.onBackPressed()
                }
            }
            // Create the AlertDialog
            builder.show()
        }
    }

    private fun noEdits(): Boolean {
        val videoPositions = binding.videoRangeSeekBar.values.let {
            it[0] == 0f && it[2] == binding.videoRangeSeekBar.valueTo
        }
        val muted = binding.muter.isSelected
        return !muted && videoPositions
    }


    private fun returnWithValues() {
        val intent = Intent(this, PostCreationActivity::class.java)
            .apply {
                putExtra(PhotoEditActivity.PICTURE_POSITION, videoPosition)
                putExtra(MUTED, binding.muter.isSelected)
                putExtra(MODIFIED, !noEdits())
                putExtra(VIDEO_START, binding.videoRangeSeekBar.values.first())
                putExtra(VIDEO_END, binding.videoRangeSeekBar.values[2])
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }

        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun resetControls() {
        binding.videoRangeSeekBar.values = listOf(0f, binding.videoRangeSeekBar.valueTo/2, binding.videoRangeSeekBar.valueTo)
        binding.muter.isSelected = false
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionList.forEach {
            FFmpegKit.cancel(it)
        }
        tempFiles.forEach{
            it.delete()
        }
        mediaPlayer.close()
    }

    private fun thumbnail(
        inputUri: Uri?,
        resultHandler: Handler,
        thumbnail: ImageView,
        thumbTime: Float,
    ) {
        val file = File.createTempFile("temp_img", ".bmp")
        tempFiles.add(file)
        val fileUri = file.toUri()
        val inputSafePath = ffmpegSafeUri(inputUri)

        val outputImagePath =
            if(fileUri.toString().startsWith("content://"))
                FFmpegKitConfig.getSafParameterForWrite(this, fileUri)
            else fileUri.toString()
        val session = FFmpegKit.executeAsync(
            "-noaccurate_seek -ss $thumbTime -i $inputSafePath -vf scale=${thumbnail.width}:${thumbnail.height} -frames:v 1 -f image2 -y $outputImagePath",
            { session ->
                val state = session.state
                val returnCode = session.returnCode

                if (ReturnCode.isSuccess(returnCode)) {
                    // SUCCESS
                    resultHandler.post {
                        if(!this.isFinishing)
                            Glide.with(this).load(outputImagePath).centerCrop().into(thumbnail)
                    }
                }
                // CALLED WHEN SESSION IS EXECUTED
                Log.d("VideoEditActivity", "FFmpeg process exited with state $state and rc $returnCode.${session.failStackTrace}")
            },
            {/* CALLED WHEN SESSION PRINTS LOGS */ }) { /*CALLED WHEN SESSION GENERATES STATISTICS*/ }
        sessionList.add(session.sessionId)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.pause()
    }

    companion object {
        const val VIDEO_TAG = "VideoEditTag"
        const val MUTED = "VideoEditMutedTag"
        const val VIDEO_START = "VideoEditVideoStartTag"
        const val VIDEO_END = "VideoEditVideoEndTag"
        const val MODIFIED = "VideoEditModifiedTag"
    }
}