package com.h.pixeldroid

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.android.material.textfield.TextInputEditText
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.objects.Instance
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.DBUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_post_creation.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*


class PostCreationActivity : AppCompatActivity(){

    private val TAG = "Post Creation Activity"

    private lateinit var accessToken: String
    private lateinit var pixelfedAPI: PixelfedAPI
    private lateinit var pictureFrame: ImageView
    private lateinit var image: File
    private var user: UserDatabaseEntity? = null

    private var listOfIds: List<String> = emptyList()

    private var maxLength: Int = Instance.DEFAULT_MAX_TOOT_CHARS

    private var description: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_creation)

        val imageUri: Uri = intent.getParcelableExtra("picture_uri")!!

        saveImage(imageUri)

        pictureFrame = findViewById(R.id.post_creation_picture_frame)
        pictureFrame.setImageURI(image.toUri())

        val db = DBUtils.initDB(applicationContext)
        user = db.userDao().getActiveUser()

        val instances = db.instanceDao().getAll()
        db.close()
        maxLength = if (user!=null){
            val thisInstances =
                instances.filter { instanceDatabaseEntity ->
                    instanceDatabaseEntity.uri.contains(user!!.instance_uri)
                }
            thisInstances.first().max_toot_chars
        } else {
            Instance.DEFAULT_MAX_TOOT_CHARS
        }

        val domain = user?.instance_uri.orEmpty()
        accessToken = user?.accessToken.orEmpty()
        pixelfedAPI = PixelfedAPI.create(domain)

        //upload the picture and display progress while doing so
        upload()

        // get the description and send the post
        findViewById<Button>(R.id.post_creation_send_button).setOnClickListener {
            if (setDescription() && listOfIds.isNotEmpty()) post()
        }
        
        // Button to retry image upload when it fails
        findViewById<Button>(R.id.retry_upload_button).setOnClickListener {
            upload_error.visibility = GONE
            upload()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //delete the temporary image
        image.delete()
    }

    private fun saveImage(imageUri: Uri) {
        try {
            val stream = applicationContext.contentResolver
                .openAssetFileDescriptor(imageUri, "r")!!
                .createInputStream()
            val bm = BitmapFactory.decodeStream(stream)
            val bos = ByteArrayOutputStream()
            bm.compress(Bitmap.CompressFormat.PNG, 0, bos)
            image = File.createTempFile("temp_compressed_img", ".png", cacheDir)

            val fos = FileOutputStream(image)
            fos.write(bos.toByteArray())
            fos.flush()
            fos.close()
        } catch (error: IOException) {
            error.printStackTrace()
            throw error
        }
    }

    private fun setDescription(): Boolean {
        val textField = findViewById<TextInputEditText>(R.id.new_post_description_input_field)
        val content = textField.text.toString()
        if (content.length > maxLength) {
            // error, too many characters
            textField.error = getString(R.string.description_max_characters).format(maxLength)

            return false
        }
        // store the description
        description = content
        return true
    }

    private fun upload(){
        val imagePart = ProgressRequestBody(image)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", image.name, imagePart)
            .build()

        val sub = imagePart.progressSubject
            .subscribeOn(Schedulers.io())
            .subscribe { percentage ->
                uploadProgressBar.progress = percentage.toInt()
            }

        var postSub : Disposable?= null
        val inter = pixelfedAPI.mediaUpload("Bearer $accessToken", requestBody.parts[0])

        postSub = inter
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ attachment ->
                listOfIds = listOf(attachment.id!!)
            },{e->
                upload_error.visibility = VISIBLE
                e.printStackTrace()
                postSub?.dispose()
                sub.dispose()
            }, {
                uploadProgressBar.visibility = GONE
                upload_completed_textview.visibility = VISIBLE
                enableButton(true)
                postSub?.dispose()
                sub.dispose()
            })
    }

    private fun post() {
        enableButton(false)
        pixelfedAPI.postStatus(
            authorization = "Bearer $accessToken",
            statusText = description,
            media_ids = listOfIds
        ).enqueue(object: Callback<Status> {
            override fun onFailure(call: Call<Status>, t: Throwable) {
                enableButton(true)
                Toast.makeText(applicationContext,getString(R.string.upload_post_failed),
                    Toast.LENGTH_SHORT).show()
                Log.e(TAG, t.message + call.request())
            }

            override fun onResponse(call: Call<Status>, response: Response<Status>) {
                if (response.code() == 200) {
                    Toast.makeText(applicationContext,getString(R.string.upload_post_success),
                        Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@PostCreationActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(applicationContext,getString(R.string.upload_post_error),
                        Toast.LENGTH_SHORT).show()
                    Log.e(TAG, call.request().toString() + response.raw().toString())
                    enableButton(true)
                }
            }
        })
    }
    private fun enableButton(enable: Boolean = true){
        post_creation_send_button.isEnabled = enable
        if(enable){
            posting_progress_bar.visibility = GONE
            post_creation_send_button.visibility = VISIBLE
        } else{
            posting_progress_bar.visibility = VISIBLE
            post_creation_send_button.visibility = GONE
        }

    }

}

class ProgressRequestBody(private val mFile: File) : RequestBody() {

    private val getProgressSubject: PublishSubject<Float> = PublishSubject.create()

    val progressSubject: Observable<Float>
        get() {
            return getProgressSubject
        }


    override fun contentType(): MediaType? {
        return "image/png".toMediaTypeOrNull()
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return mFile.length()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val fileLength = contentLength()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val `in` = FileInputStream(mFile)
        var uploaded: Long = 0

        `in`.use {
            var read: Int
            var lastProgressPercentUpdate = 0.0f
            read = it.read(buffer)
            while (read != -1) {

                uploaded += read.toLong()
                sink.write(buffer, 0, read)
                read = it.read(buffer)

                val progress = (uploaded.toFloat() / fileLength.toFloat()) * 100f
                //prevent publishing too many updates, which slows upload, by checking if the upload has progressed by at least 1 percent
                if (progress - lastProgressPercentUpdate > 1 || progress == 100f) {
                    // publish progress
                    getProgressSubject.onNext(progress)
                    lastProgressPercentUpdate = progress
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 2048
    }
}
