package org.pixeldroid.app.postCreation.photoEdit

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import com.arthenica.ffmpegkit.*
import com.arthenica.ffmpegkit.MediaInformation.KEY_DURATION
import com.bumptech.glide.Glide
import org.pixeldroid.app.databinding.ActivityVideoEditBinding
import org.pixeldroid.app.utils.BaseActivity
import java.io.File


class VideoEditActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityVideoEditBinding.inflate(layoutInflater)

        setContentView(binding.root)
        val uri = intent.getParcelableExtra(PhotoEditActivity.PICTURE_URI) as Uri?
        val videoPosition = intent.getIntExtra(PhotoEditActivity.PICTURE_POSITION, -1)

        val inputVideoPath =if(uri.toString().startsWith("content://")) FFmpegKitConfig.getSafParameterForRead(this, uri) else uri.toString()
        val inputVideoPath2 =if(uri.toString().startsWith("content://")) FFmpegKitConfig.getSafParameterForRead(this, uri) else uri.toString()
        val mediaInformation: MediaInformation? = FFprobeKit.getMediaInformation(inputVideoPath).mediaInformation

        val duration: Long? = mediaInformation?.getNumberProperty(KEY_DURATION)

        val file = File.createTempFile("temp_img", ".png").toUri()

        val outputImagePath =if(file.toString().startsWith("content://")) FFmpegKitConfig.getSafParameterForWrite(this, file) else file.toString()

        val session = FFmpegKit.execute(
            "-i $inputVideoPath2 -filter_complex \"select='not(mod(n,1000))',scale=240:-1,tile=layout=4x1\" -vframes 1 -q:v 2 -y $outputImagePath"
        )
        if (ReturnCode.isSuccess(session.returnCode)) {
            Glide.with(this).load(file).into(binding.thumbnails)
            // SUCCESS
        } else if (ReturnCode.isCancel(session.returnCode)) {

            // CANCEL
        } else {

            // FAILURE
            Log.d("VideoEditActivity",
                String.format("Command failed with state %s and rc %s.%s",
                    session.state,
                    session.returnCode,
                    session.failStackTrace))
        }

    }
    companion object {
        const val VIDEO_TAG = "VideoEditTag"
    }
}