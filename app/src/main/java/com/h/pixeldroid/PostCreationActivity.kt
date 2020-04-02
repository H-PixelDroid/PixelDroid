package com.h.pixeldroid

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Attachment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class PostCreationActivity : AppCompatActivity() {

    private lateinit var accessToken: String
    private lateinit var pixelfedAPI: PixelfedAPI
    private lateinit var preferences: SharedPreferences

    private lateinit var picture: Uri
    private lateinit var description: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_creation)

        picture = intent.getParcelableExtra("picture_uri")
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
//        upload(picture, description)

    }

    private fun upload(picture: Uri, description: String) {
        pixelfedAPI.mediaUpload(accessToken, File(picture.path)).enqueue(object:
            Callback<Attachment> {
            override fun onFailure(call: Call<Attachment>, t: Throwable) {
                Log.e("UPLOAD ERROR", t.toString())
                Toast.makeText(applicationContext,"Picture upload error!",Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<Attachment>, response: Response<Attachment>) {
                TODO("Not yet implemented")
            }
        })
    }
}
