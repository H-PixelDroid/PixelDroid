package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.h.pixeldroid.objects.Post

class PostActivity : AppCompatActivity() {

    //Class used to pass arguments to the activity
    class Arguments(val post : Post) {

        fun startActivity(context: Context) {
            val intent = Intent(context, PostActivity::class.java)
            intent.putExtra("postTag", post)
            context.startActivity(intent)
        }

    } // Arguments class

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

    }


}
