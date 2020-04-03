package com.h.pixeldroid

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Attachment
import com.h.pixeldroid.objects.Status
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.BufferedSink
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Multipart
import java.io.File

class PostCreationActivity : AppCompatActivity() {

    private val TAG = "Post Creation Activity"

    private lateinit var accessToken: String
    private lateinit var pixelfedAPI: PixelfedAPI
    private lateinit var preferences: SharedPreferences

    private lateinit var picture: Uri
    private var description: String = "Default description."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_creation)

        picture = intent.getParcelableExtra("picture_uri") as Uri
        val pictureFrame = findViewById<ImageView>(R.id.post_creation_picture_frame)
        pictureFrame.setImageURI(picture)

        preferences = getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )
        pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
        accessToken = preferences.getString("accessToken", "")!!

        // check if the picture is alright
        // TODO

        // edit the picture
        // TODO

        // ask for a description
        // TODO

        // upload it to PixelFed
        findViewById<Button>(R.id.post_creation_upload_media_button).setOnClickListener {
            upload(File(picture.path!!))
        }
    }

    private fun post(id: String) {
        Log.e(TAG, description)
        Log.e(TAG, id)
        if (id.isEmpty()) return
        pixelfedAPI.status(
            authorization = "Bearer $accessToken",
            statusText = description,
            media_ids = listOf(id)
        ).enqueue(object: Callback<Status> {
            override fun onFailure(call: Call<Status>, t: Throwable) {
                Toast.makeText(applicationContext,"Post upload failed",Toast.LENGTH_SHORT).show()
                Log.e(TAG, t.message + call.request())
            }

            override fun onResponse(call: Call<Status>, response: Response<Status>) {
                if (response.code() == 200) {
                    Toast.makeText(applicationContext,"Post upload success",Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext,"Post upload failed : not 200",Toast.LENGTH_SHORT).show()
                    Log.e(TAG, call.request().toString() + response.raw().toString())
                }
            }
        })
    }

    private fun upload(picture: File): String {
        var uploadId = ""
        val rBody: RequestBody = picture.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", picture.name, rBody)
        pixelfedAPI.mediaUpload("Bearer $accessToken", part).enqueue(object:
            Callback<Attachment> {
            override fun onFailure(call: Call<Attachment>, t: Throwable) {
                Log.e(TAG, t.toString() + call.request())
                Toast.makeText(applicationContext,"Picture upload error!",Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<Attachment>, response: Response<Attachment>) {
                if (response.code() == 200) {
                    val body = response.body()!!
                    if (body.type.name == "image") {
                        Toast.makeText(applicationContext, "File uploaded successfully!", Toast.LENGTH_SHORT).show()
                        post(body.id)
                    } else
                        Toast.makeText(applicationContext, "Upload error: wrong picture format.", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Server responded: $response" + call.request() + call.request().body)
                    Toast.makeText(applicationContext,"Upload error: bad request format",Toast.LENGTH_SHORT).show()
                }
            }
        })
        return uploadId
    }

}
