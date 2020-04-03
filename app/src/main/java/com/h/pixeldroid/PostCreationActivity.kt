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
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
            upload(picture, description)
        }
    }

    private fun upload(picture: Uri, description: String) {
        val file = File(picture.path!!)
        val rBody: RequestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, rBody)
        pixelfedAPI.mediaUpload("Bearer $accessToken", part).enqueue(object:
            Callback<Attachment> {
            override fun onFailure(call: Call<Attachment>, t: Throwable) {
                Log.e(TAG, t.toString() + call.request())
                Toast.makeText(applicationContext,"Picture upload error!",Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<Attachment>, response: Response<Attachment>) {
                if (response.code() == 200) {
                    Toast.makeText(applicationContext, "File uploaded successfully: $response", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Server responded: $response" + call.request() + call.request().body)
                    Toast.makeText(applicationContext,"Picture upload error!",Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

}
