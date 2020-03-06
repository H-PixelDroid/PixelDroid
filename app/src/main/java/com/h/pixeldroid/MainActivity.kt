package com.h.pixeldroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.h.pixeldroid.PostActivity.Companion.POST_TAG
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.models.Post
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private var statuses: ArrayList<Status>? = null
    private val BASE_URL = "https://pixelfed.de/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadData()
    }

    private fun loadData() {

        val pixelfedAPI= PixelfedAPI.create(BASE_URL)
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
            intent.putExtra(POST_TAG,
                Post(statuses?.get(0))
            )
            startActivity(intent)
        }))
    }
}
