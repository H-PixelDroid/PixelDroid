package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.android.material.textfield.TextInputEditText
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Attachment
import com.h.pixeldroid.objects.Status
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class PostCreationActivity : AppCompatActivity() {

    private val TAG = "Post Creation Activity"

    private lateinit var accessToken: String
    private lateinit var pixelfedAPI: PixelfedAPI
    private lateinit var preferences: SharedPreferences
    private lateinit var pictureFrame: ImageView
    private lateinit var image: File

    private var description: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_creation)
        val imageUri: Uri = intent.getParcelableExtra<Uri>("picture_uri")!!
        saveImage(imageUri)

        pictureFrame = findViewById<ImageView>(R.id.post_creation_picture_frame)
        pictureFrame.setImageURI(image.toUri())

        preferences = getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )
        pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
        accessToken = preferences.getString("accessToken", "")!!

        // check if the picture is alright
        // TODO

        // edit the picture
        // TODO

        // get the description and send the post to PixelFed
        findViewById<Button>(R.id.post_creation_send_button).setOnClickListener {
            if (setDescription()) upload()
        }
    }

    private fun saveImage(imageUri: Uri) {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "PixelDroid_$timeStamp.png"
        try {
            val stream = applicationContext.contentResolver
                .openAssetFileDescriptor(imageUri, "r")!!
                .createInputStream()
            val bm = BitmapFactory.decodeStream(stream)
            val bos = ByteArrayOutputStream()
            bm.compress(Bitmap.CompressFormat.PNG, 0, bos)
            image = File(
                applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                fileName)
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
        val maxLength = preferences.getInt("max_toot_chars", 500)
        if (content.length > maxLength) {
            // error, too much characters
            textField.error = getString(R.string.description_max_characters) + maxLength +
                    getString(R.string.description_max_characters_end)
            return false
        }
        // store the description
        description = content
        return true
    }

    private fun upload() {
        val rBody: RequestBody = image.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", image.name, rBody)
        pixelfedAPI.mediaUpload("Bearer $accessToken", part).enqueue(object:
            Callback<Attachment> {
            override fun onFailure(call: Call<Attachment>, t: Throwable) {
                Log.e(TAG, t.toString() + call.request())
                Toast.makeText(applicationContext,getString(R.string.upload_picture_failed),
                    Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<Attachment>, response: Response<Attachment>) {
                if (response.code() == 200) {
                    val body = response.body()!!
                    if (body.type.name == "image") {
                        post(body.id)
                    } else
                        Toast.makeText(applicationContext, getString(R.string.picture_format_error),
                            Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Server responded: $response" + call.request() + call.request().body)
                    Toast.makeText(applicationContext,getString(R.string.request_format_error),
                        Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun post(id: String) {
        if (id.isEmpty()) return
        pixelfedAPI.postStatus(
            authorization = "Bearer $accessToken",
            statusText = description,
            media_ids = listOf(id)
        ).enqueue(object: Callback<Status> {
            override fun onFailure(call: Call<Status>, t: Throwable) {
                Toast.makeText(applicationContext,getString(R.string.upload_post_failed),
                    Toast.LENGTH_SHORT).show()
                Log.e(TAG, t.message + call.request())
            }

            override fun onResponse(call: Call<Status>, response: Response<Status>) {
                if (response.code() == 200) {
                    Toast.makeText(applicationContext,getString(R.string.upload_post_success),
                        Toast.LENGTH_SHORT).show()
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                } else {
                    Toast.makeText(applicationContext,getString(R.string.upload_post_error),
                        Toast.LENGTH_SHORT).show()
                    Log.e(TAG, call.request().toString() + response.raw().toString())
                }
            }
        })
    }

}
