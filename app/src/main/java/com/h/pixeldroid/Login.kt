package com.h.pixeldroid

import com.h.pixeldroid.objects.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.Call
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.h.pixeldroid.api.PixelfedAPI

class Login : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

    }

    fun connexion(view: View) {
        val api = PixelfedAPI.create("https://pixelfed.de")
        val app: Application? = api.registerApplication("PixelDroid", "urn:ietf:wg:oauth:2.0:oob", "read write follow").execute().body()


    }

    fun redirect() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com")))
    }
}
