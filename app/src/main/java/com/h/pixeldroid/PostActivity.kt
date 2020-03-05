package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.h.pixeldroid.objects.Post

/**
 * @brief Shows a post using data retrieved from status
 * @param Profile, must be passed via the intent
 */
class PostActivity : AppCompatActivity() {

    //Class used to pass arguments to the activity
    class Arguments(val post : Post) {

        companion object {
            fun createFromIntent(intent: Intent): Arguments {
                return Arguments(
                    post = intent.getSerializableExtra("postTag") as Post
                )
            }
        }
        fun startActivity(context: Context) {
            val intent = Intent(context, PostActivity::class.java)
            intent.putExtra("postTag", post)
            context.startActivity(intent)
        }

    } // Arguments class

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        val post : Post? = intent.getSerializableExtra("posTag") as Post?

        //Set post fields
        Log.e("LOG: ", post?.getUsername().toString())
        findViewById<TextView>(R.id.username).text = post?.getUsername()
        findViewById<TextView>(R.id.description).text = post?.getDescription()


    }


}
