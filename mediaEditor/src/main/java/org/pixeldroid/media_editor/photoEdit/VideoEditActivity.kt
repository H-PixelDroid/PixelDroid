package org.pixeldroid.media_editor.photoEdit

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.os.HandlerCompat
import androidx.core.view.isVisible
import androidx.media.AudioAttributesCompat
import androidx.media2.common.MediaMetadata
import androidx.media2.common.UriMediaItem
import androidx.media2.player.MediaPlayer
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformation
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Statistics
import com.bumptech.glide.Glide
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import org.pixeldroid.media_editor.R
import org.pixeldroid.media_editor.databinding.ActivityVideoEditBinding
import java.io.File
import java.io.Serializable
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

const val TAG = "VideoEditActivity"

class VideoEditActivity : AppCompatActivity() {

    data class RelativeCropPosition(
        // Width of the selected part of the video, relative to the width of the video
        val relativeWidth: Float = 1f,
        // Height of the selected part of the video, relative to the height of the video
        val relativeHeight: Float = 1f,
        // Distance of left corner of selected part, relative to the width of the video
        val relativeX: Float = 0f,
        // Distance of top of selected part, relative to the height of the video
        val relativeY: Float = 0f,
    ): Serializable {
        fun notCropped(): Boolean =
            (relativeWidth - 1f).absoluteValue < 0.001f
                    && (relativeHeight - 1f).absoluteValue < 0.001f
                    && relativeX.absoluteValue < 0.001f
                    && relativeY.absoluteValue < 0.001f

    }

    data class VideoEditArguments(
        val muted: Boolean,
        val videoStart: Float?,
        val videoEnd: Float? ,
        val speedIndex: Int,
        val videoCrop: RelativeCropPosition,
        val videoStabilize: Float
    ): Serializable

    private lateinit var videoUri: Uri
    private lateinit var mediaPlayer: MediaPlayer
    private var videoPosition: Int = -1

    private var cropRelativeDimensions: RelativeCropPosition = RelativeCropPosition()

    private var stabilization: Float = 0f
        set(value){
            field = value
            if(value > 0.01f && value <= 100f){
                // Stabilization requested, show UI
                binding.stabilisationSaved.isVisible = true
                val typedValue = TypedValue()
                val color: Int = if (binding.stabilizer.context.theme
                        .resolveAttribute(R.attr.colorSecondary, typedValue, true)
                ) typedValue.data else Color.TRANSPARENT

                binding.stabilizer.drawable.setTint(color)
            }
            else {
                binding.stabilisationSaved.isVisible = false
                binding.stabilizer.drawable.setTintList(null)
            }
        }

    private var speed: Int = 1
        set(value) {
            field = value

            mediaPlayer.playbackSpeed = speedChoices[value].toFloat()

            if(speed != 1) binding.muter.callOnClick()
        }

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

        videoUri = intent.getParcelableExtra(PhotoEditActivity.PICTURE_URI)!!

        videoPosition = intent.getIntExtra(PhotoEditActivity.PICTURE_POSITION, -1)

        val inputVideoPath = ffmpegCompliantUri(videoUri)
        val mediaInformation: MediaInformation? = FFprobeKit.getMediaInformation(inputVideoPath).mediaInformation

        //Duration in seconds, or null
        val duration: Float? = mediaInformation?.duration?.toFloatOrNull()

        binding.videoRangeSeekBar.valueFrom = 0f
        binding.videoRangeSeekBar.valueTo = duration ?: 100f
        binding.videoRangeSeekBar.values = listOf(0f,(duration?: 100f) / 2, duration ?: 100f)


        val mediaItem: UriMediaItem = UriMediaItem.Builder(videoUri).build()
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
            else {
                mediaPlayer.playerVolume = 1f
                speed = 1
            }
            binding.muter.isSelected = !binding.muter.isSelected
        }

        binding.cropper.setOnClickListener {
            showCropInterface(show = true, uri = videoUri)
        }

        binding.saveCropButton.setOnClickListener {
            // This is the rectangle selected by the crop
            val cropRect = binding.cropImageView.cropWindowRect

            // This is the rectangle of the whole image
            val fullImageRect: Rect = binding.cropImageView.getInitialCropWindowRect()

            // x, y are coordinates of top left, in the ImageView
            val x = cropRect.left - fullImageRect.left
            val y = cropRect.top - fullImageRect.top

            // width and height selected by the crop
            val width = cropRect.width()
            val height = cropRect.height()

            // To avoid having to calculate the dimensions of the video here, we pass
            // relative width, height and x, y back to be treated in FFmpeg
            cropRelativeDimensions = RelativeCropPosition(
                relativeWidth = width/fullImageRect.width(),
                relativeHeight = height/fullImageRect.height(),
                relativeX = x/fullImageRect.width(),
                relativeY = y/fullImageRect.height()
            )

            // If a crop was saved, change the color of the crop button to give a visual indication
            if(!cropRelativeDimensions.notCropped()){
                val typedValue = TypedValue()
                val color: Int = if (binding.checkMarkCropped.context.theme
                        .resolveAttribute(R.attr.colorSecondary, typedValue, true)
                ) typedValue.data else Color.TRANSPARENT

                binding.cropper.drawable.setTint(color)
            } else {
                // Else reset the tint
                binding.cropper.drawable.setTintList(null)
            }

            showCropInterface(show = false)
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



        binding.speeder.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setIcon(R.drawable.speed)
                setTitle(R.string.video_speed)
                setSingleChoiceItems(speedChoices.map { it.toString() + "x" }.toTypedArray(), speed) { dialog, which ->
                    // update the selected item which is selected by the user so that it should be selected
                    // when user opens the dialog next time and pass the instance to setSingleChoiceItems method
                    speed = which

                    // when selected an item the dialog should be closed with the dismiss method
                    dialog.dismiss()
                }
                setNegativeButton(android.R.string.cancel) { _, _ -> }
            }.show()
        }

        binding.stabilizer.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setIcon(R.drawable.video_stable)
                setTitle(R.string.stabilize_video_intensity)
                val slider = Slider(context).apply {
                    valueFrom = 0f
                    valueTo = 100f
                    value = stabilization
                }
                setView(slider)
                setNegativeButton(android.R.string.cancel) { _, _ -> }
                setPositiveButton(android.R.string.ok) { _, _ -> stabilization = slider.value}
            }.show()
        }


        val thumbInterval: Float? = duration?.div(7)

        thumbInterval?.let {
            thumbnail(videoUri, resultHandler, binding.thumbnail1, it)
            thumbnail(videoUri, resultHandler, binding.thumbnail2, it.times(2))
            thumbnail(videoUri, resultHandler, binding.thumbnail3, it.times(3))
            thumbnail(videoUri, resultHandler, binding.thumbnail4, it.times(4))
            thumbnail(videoUri, resultHandler, binding.thumbnail5, it.times(5))
            thumbnail(videoUri, resultHandler, binding.thumbnail6, it.times(6))
            thumbnail(videoUri, resultHandler, binding.thumbnail7, it.times(7))
        }

        resetControls()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
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
        if(binding.cropImageView.isVisible) {
            showCropInterface(false)
        } else if (noEdits()) super.onBackPressed()
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
        val speedUnchanged = speed == 1

        val stabilizationUnchanged = stabilization <= 0.01f || stabilization > 100.5f

        return !muted && videoPositions && speedUnchanged && cropRelativeDimensions.notCropped() && stabilizationUnchanged
    }

    private fun showCropInterface(show: Boolean, uri: Uri? = null){
        val visibilityOfOthers = if(show) View.GONE else View.VISIBLE
        val visibilityOfCrop = if(show) View.VISIBLE else View.GONE

        if(show) mediaPlayer.pause()

        if(show) binding.cropSavedCard.visibility = View.GONE
        else if(!cropRelativeDimensions.notCropped()) binding.cropSavedCard.visibility = View.VISIBLE

        binding.stabilisationSaved.visibility =
            if(!show && stabilization > 0.01f && stabilization <= 100f) View.VISIBLE
            else View.GONE

        binding.muter.visibility = visibilityOfOthers
        binding.speeder.visibility = visibilityOfOthers
        binding.cropper.visibility = visibilityOfOthers
        binding.stabilizer.visibility = visibilityOfOthers
        binding.videoRangeSeekBar.visibility = visibilityOfOthers
        binding.videoView.visibility = visibilityOfOthers
        binding.thumbnail1.visibility = visibilityOfOthers
        binding.thumbnail2.visibility = visibilityOfOthers
        binding.thumbnail3.visibility = visibilityOfOthers
        binding.thumbnail4.visibility = visibilityOfOthers
        binding.thumbnail5.visibility = visibilityOfOthers
        binding.thumbnail6.visibility = visibilityOfOthers
        binding.thumbnail7.visibility = visibilityOfOthers


        binding.cropImageView.visibility = visibilityOfCrop
        binding.saveCropButton.visibility = visibilityOfCrop

        if(show && uri != null) binding.cropImageView.setImageUriAsync(uri, cropRelativeDimensions)
    }

    private fun returnWithValues() {
        //TODO Check if some of these should be null to indicate no changes in that category? Ex start/end
        val intent = Intent()
            .apply {
                putExtra(PhotoEditActivity.PICTURE_POSITION, videoPosition)
                putExtra(VIDEO_ARGUMENTS_TAG, VideoEditArguments(
                    binding.muter.isSelected, binding.videoRangeSeekBar.values.first(),
                    binding.videoRangeSeekBar.values[2],
                    speed,
                    cropRelativeDimensions,
                    stabilization
                )
                )
                putExtra(MODIFIED, !noEdits())
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun resetControls() {
        binding.videoRangeSeekBar.values = listOf(0f, binding.videoRangeSeekBar.valueTo/2, binding.videoRangeSeekBar.valueTo)
        binding.muter.isSelected = false

        binding.cropImageView.resetCropRect()
        cropRelativeDimensions = RelativeCropPosition()
        binding.cropper.drawable.setTintList(null)
        binding.stabilizer.drawable.setTintList(null)
        binding.cropSavedCard.visibility = View.GONE
        stabilization = 0f
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
        val file = File.createTempFile("temp_img", ".bmp", cacheDir)
        tempFiles.add(file)
        val fileUri = file.toUri()
        val ffmpegCompliantUri = ffmpegCompliantUri(inputUri)

        val outputImagePath =
            if(fileUri.toString().startsWith("content://"))
                FFmpegKitConfig.getSafParameterForWrite(this, fileUri)
            else fileUri.toString()
        val session = FFmpegKit.executeWithArgumentsAsync(arrayOf(
            "-noaccurate_seek", "-ss", "$thumbTime", "-i", ffmpegCompliantUri, "-vf",
            "scale=${thumbnail.width}:${thumbnail.height}",
            "-frames:v", "1", "-f", "image2", "-y", outputImagePath), { session ->
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
            {/* CALLED WHEN SESSION PRINTS LOGS */ }, { /*CALLED WHEN SESSION GENERATES STATISTICS*/ })
        sessionList.add(session.sessionId)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.pause()
    }

    companion object {
        const val VIDEO_ARGUMENTS_TAG = "org.pixeldroid.media_editor.VideoEditTag"
        // List of choices of speeds
        val speedChoices: List<Number> = listOf(0.5, 1, 2, 4, 8)
        const val MODIFIED = "VideoEditModifiedTag"

        /**
         * @param muted should audio tracks be removed in the output
         * @param videoStart when we want to start the video, in seconds, or null if we
         * don't want to remove the start
         * @param videoEnd when we want to end the video, in seconds, or null if we
         * don't want to remove the end
         */
        fun startEncoding(
            originalUri: Uri,
            arguments: VideoEditArguments,
            context: Context,
            //TODO make interfaces for these callbacks, or something more explicit
            registerNewFFmpegSession: (Uri, Long) -> Unit,
            trackTempFile: (File) -> Unit,
            videoEncodeProgress: (Uri, Int, Boolean, Uri?, Boolean) -> Unit,
        ) {

            // Having a meaningful suffix is necessary so that ffmpeg knows what to put in output
            val suffix = originalUri.fileExtension(context.contentResolver)
            val file = File.createTempFile("temp_video", ".$suffix", context.cacheDir)
            //val file = File.createTempFile("temp_video", ".webm", cacheDir)
            trackTempFile(file)
            val fileUri = file.toUri()
            val outputVideoPath = context.ffmpegCompliantUri(fileUri)

            val ffmpegCompliantUri: String = context.ffmpegCompliantUri(originalUri)

            val mediaInformation: MediaInformation? = FFprobeKit.getMediaInformation(context.ffmpegCompliantUri(originalUri)).mediaInformation
            val totalVideoDuration = mediaInformation?.duration?.toFloatOrNull()

            fun secondPass(stabilizeString: String = ""){
                val speed = speedChoices[arguments.speedIndex]

                val mutedString = if(arguments.muted || arguments.speedIndex != 1) "-an" else null
                val startString: List<String?> = if(arguments.videoStart != null) listOf("-ss", "${arguments.videoStart/speed.toFloat()}") else listOf(null, null)

                val endString: List<String?> = if(arguments.videoEnd != null) listOf("-to", "${arguments.videoEnd/speed.toFloat() - (arguments.videoStart ?: 0f)/speed.toFloat()}") else listOf(null, null)

                // iw and ih are variables for the original width and height values, FFmpeg will know them
                val cropString = if(arguments.videoCrop.notCropped()) "" else "crop=${arguments.videoCrop.relativeWidth}*iw:${arguments.videoCrop.relativeHeight}*ih:${arguments.videoCrop.relativeX}*iw:${arguments.videoCrop.relativeY}*ih"
                val separator = if(arguments.speedIndex != 1 && !arguments.videoCrop.notCropped()) "," else ""
                val speedString = if(arguments.speedIndex != 1) "setpts=PTS/${speed}" else ""

                val separatorStabilize = if(stabilizeString == "" || (speedString == "" && cropString == "")) "" else ","

                val speedAndCropString: List<String?> = if(arguments.speedIndex!= 1 || !arguments.videoCrop.notCropped() || stabilizeString.isNotEmpty())
                    listOf("-filter:v", stabilizeString + separatorStabilize + speedString + separator + cropString)
                // Stream copy is not compatible with filter, but when not filtering we can copy the stream without re-encoding
                else listOf("-c", "copy")

                // This should be set when re-encoding is required (otherwise it defaults to mpeg which then doesn't play)
                val encodePreset: List<String?> = if(arguments.speedIndex != 1 && !arguments.videoCrop.notCropped()) listOf("-c:v", "libx264", "-preset", "ultrafast") else listOf(null, null, null, null)

                val session: FFmpegSession =
                    FFmpegKit.executeWithArgumentsAsync(listOfNotNull(
                        startString[0], startString[1],
                        "-i", ffmpegCompliantUri,
                        speedAndCropString[0], speedAndCropString[1],
                        endString[0], endString[1],
                        mutedString, "-y",
                        encodePreset[0], encodePreset[1], encodePreset[2], encodePreset[3],
                        outputVideoPath,
                    ).toTypedArray(),
                        //val session: FFmpegSession = FFmpegKit.executeAsync("$startString -i $inputSafePath $endString -c:v libvpx-vp9 -c:a copy -an -y $outputVideoPath",
                        { session ->
                            val returnCode = session.returnCode
                            if (ReturnCode.isSuccess(returnCode)) {

                                videoEncodeProgress(originalUri, 100, false, outputVideoPath.toUri(), false)

                                Log.d(TAG, "Encode completed successfully in ${session.duration} milliseconds")
                            } else {
                                videoEncodeProgress(originalUri, 0, false, outputVideoPath.toUri(), true)
                                Log.e(TAG, "Encode failed with state ${session.state} and rc $returnCode.${session.failStackTrace}")
                            }
                        },
                        { log -> Log.d("PostCreationActivityEncoding", log.message) }
                    ) { statistics: Statistics? ->

                        val timeInMilliseconds: Int? = statistics?.time
                        timeInMilliseconds?.let {
                            if (timeInMilliseconds > 0) {
                                val completePercentage = totalVideoDuration?.let {
                                    val speedupDurationModifier = speedChoices[arguments.speedIndex].toFloat()

                                    val newTotalDuration = (it - (arguments.videoStart ?: 0f) - (it - (arguments.videoEnd ?: it)))/speedupDurationModifier
                                    timeInMilliseconds / (10*newTotalDuration)
                                }
                                completePercentage?.let {
                                    val rounded: Int = it.roundToInt()
                                    videoEncodeProgress(originalUri, rounded, false, null, false)
                                }
                                Log.d(TAG, "Encoding video: %$completePercentage.")
                            }
                        }
                    }
                registerNewFFmpegSession(originalUri, session.sessionId)
            }

            fun stabilizationFirstPass(){

                val shakeResultsFile = File.createTempFile("temp_shake_results", ".trf", context.cacheDir)
                trackTempFile(shakeResultsFile)
                val shakeResultsFileUri = shakeResultsFile.toUri()
                val shakeResultsFileSafeUri = context.ffmpegCompliantUri(shakeResultsFileUri).removePrefix("file://")

                val inputSafeUri: String = context.ffmpegCompliantUri(originalUri)

                // Map chosen "stabilization force" to shakiness, from 3 to 10
                val shakiness = (0f..100f).convert(arguments.videoStabilize, 3f..10f).roundToInt()

                val analyzeVideoCommandList = listOf(
                    "-y", "-i", inputSafeUri,
                    "-vf", "vidstabdetect=shakiness=$shakiness:accuracy=15:result=$shakeResultsFileSafeUri",
                    "-f", "null", "-"
                ).toTypedArray()

                val session: FFmpegSession =
                    FFmpegKit.executeWithArgumentsAsync(analyzeVideoCommandList,
                    { firstPass ->
                        if (ReturnCode.isSuccess(firstPass.returnCode)) {
                            // Map chosen "stabilization force" to shakiness, from 8 to 40
                            val smoothing = (0f..100f).convert(arguments.videoStabilize, 8f..40f).roundToInt()

                            val stabilizeVideoCommand =
                                "vidstabtransform=smoothing=$smoothing:input=${context.ffmpegCompliantUri(shakeResultsFileUri).removePrefix("file://")}"
                            secondPass(stabilizeVideoCommand)
                        } else {
                            Log.e(
                                "PostCreationActivityEncoding",
                                "Video stabilization first pass failed!"
                            )
                        }
                    },
                    { log -> Log.d("PostCreationActivityEncoding", log.message) },
                    { statistics: Statistics? ->

                        val timeInMilliseconds: Int? = statistics?.time
                        timeInMilliseconds?.let {
                            if (timeInMilliseconds > 0) {
                                val completePercentage = totalVideoDuration?.let {
                                    // At this stage, we didn't change speed or start/end of the video
                                    timeInMilliseconds / (10 * it)
                                }
                                completePercentage?.let {
                                    val rounded: Int = it.roundToInt()
                                    videoEncodeProgress(originalUri, rounded, true, null, false)
                                }

                                Log.d(TAG, "Stabilization pass: %$completePercentage.")
                            }
                        }
                    })
                registerNewFFmpegSession(originalUri, session.sessionId)
            }

            if(arguments.videoStabilize > 0.01f) {
                // Stabilization was requested: we need an additional first pass to get stabilization data
                stabilizationFirstPass()
            } else {
                // Immediately call the second pass, no stabilization needed
                secondPass()
            }

        }

        fun cancelEncoding(){
            FFmpegKit.cancel()
        }
        fun cancelEncoding(sessionId: Long){
            FFmpegKit.cancel(sessionId)
        }
    }
}