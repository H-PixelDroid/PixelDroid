package com.h.pixeldroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.h.pixeldroid.PostActivity.Companion.POST_TAG
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Post
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private var statuses: ArrayList<Status>? = null
    private val BASE_URL = "https://pixelfed.de/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val homepage_button : ImageButton = findViewById(R.id.activity_main_home_btn)
        val search_button : ImageButton = findViewById(R.id.activity_main_search_btn)
        val camera_button : ImageButton = findViewById(R.id.activity_main_camera_btn)
        val favorite_button : ImageButton = findViewById(R.id.activity_main_favorite_btn)
        val account_button : ImageButton = findViewById(R.id.activity_main_account_btn)

        homepage_button.setOnClickListener(
            View.OnClickListener {
                startActivity(Intent(this, MainActivity::class.java))
            }
        )

        account_button.setOnClickListener((View.OnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }))

        loadData()
    }

    private fun loadData() {

//Define the Retrofit request//

        val pixelfedAPI = Retrofit.Builder()

//Set the API’s base URL//

            .baseUrl(BASE_URL)

//Specify the converter factory to use for serialization and deserialization//

            .addConverterFactory(GsonConverterFactory.create())

//Add a call adapter factory to support RxJava return types//

            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())

//Build the Retrofit instance//

            .build().create(PixelfedAPI::class.java)

        pixelfedAPI.timelinePublic(null, null, null, null, null)
            .enqueue(object : Callback<List<Status>> {
                override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
                    if (response.code() == 200) {
                        statuses = response.body() as ArrayList<Status>?
                    }
                }

                override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                    Log.e("Ouch, not OK", t.toString())
                }
            })

        val postButton = findViewById<Button>(R.id.postButton)
        postButton.setOnClickListener((View.OnClickListener {
            val intent = Intent(this, PostActivity::class.java)
            intent.putExtra(POST_TAG ,Post(statuses?.get(0)))
            startActivity(intent)
        }))

    }

}
