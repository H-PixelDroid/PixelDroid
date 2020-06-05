package com.h.pixeldroid

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.interfaces.PostCreationListener
import com.h.pixeldroid.objects.Attachment
import com.h.pixeldroid.objects.Instance
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.utils.DBUtils
import com.h.pixeldroid.utils.ProgressRequestBody
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_post_creation.*
import kotlinx.android.synthetic.main.image_album_creation.view.*
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PostCreationActivity : AppCompatActivity(), PostCreationListener {

    private val TAG = "Post Creation Activity"

    private lateinit var recycler : RecyclerView
    private lateinit var adapter : PostCreationAdapter

    private lateinit var accessToken: String
    private lateinit var pixelfedAPI: PixelfedAPI

    private var muListOfIds: MutableList<String> = mutableListOf()
    private var progressList: ArrayList<Int> = arrayListOf()


    private var positionResult = 0
    private var user: UserDatabaseEntity? = null

    private var posts: ArrayList<String> = ArrayList()

    private var maxLength: Int = Instance.DEFAULT_MAX_TOOT_CHARS

    private var description: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_creation)

        // load images
        posts = intent.getStringArrayListExtra("pictures_uri")!!

        progressList = posts.map { 0 } as ArrayList<Int>
        muListOfIds = posts.map { "" }.toMutableList()

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

        // check if the pictures are alright
        // TODO

        //upload the picture and display progress while doing so
        upload()

        adapter = PostCreationAdapter(posts)
        adapter.listener = this
        recycler = findViewById(R.id.image_grid)
        recycler.layoutManager = GridLayoutManager(this, if (posts.size > 2) 2 else 1)
        recycler.adapter = adapter

        // get the description and send the post
        findViewById<Button>(R.id.post_creation_send_button).setOnClickListener {
            if (setDescription() && muListOfIds.isNotEmpty()) post()
        }

        // Button to retry image upload when it fails
        findViewById<Button>(R.id.retry_upload_button).setOnClickListener {
            upload_error.visibility = View.GONE
            muListOfIds = posts.map { "" }.toMutableList()
            progressList = posts.map { 0 } as ArrayList<Int>
            upload()
        }
    }

    private fun setDescription(): Boolean {
        val textField = findViewById<TextInputEditText>(R.id.new_post_description_input_field)
        val content = textField.text.toString()
        if (content.length > maxLength) {
            // error, too much characters
            textField.error = "Description must contain $maxLength characters at most."
            return false
        }
        // store the description
        description = content
        return true
    }

    private fun upload() {
        for ((index, post) in posts.withIndex()) {
            val imageUri = Uri.parse(post)
            val imageInputStream = contentResolver.openInputStream(imageUri)!!

            val size =
                if (imageUri.toString().startsWith("content")) {
                    contentResolver.query(imageUri, null, null, null, null)
                        ?.use { cursor ->
                        /* Get the column indexes of the data in the Cursor,
                         * move to the first row in the Cursor, get the data,
                         * and display it.
                         */
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            cursor.moveToFirst()
                            cursor.getLong(sizeIndex)
                        } ?: 0
                } else {
                    imageUri.toFile().length()
                }

            val imagePart = ProgressRequestBody(imageInputStream, size)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", System.currentTimeMillis().toString(), imagePart)
                .build()

            val sub = imagePart.progressSubject
                .subscribeOn(Schedulers.io())
                .subscribe { percentage ->
                    progressList[index] = percentage.toInt()
                    uploadProgressBar.progress =
                        progressList.sum() / progressList.size
                }

            var postSub: Disposable? = null
            val inter = pixelfedAPI.mediaUpload("Bearer $accessToken", requestBody.parts[0])

            postSub = inter
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { attachment: Attachment ->
                        progressList[index] = 0
                        muListOfIds[index] = attachment.id!!
                    },
                    { e ->
                        upload_error.visibility = View.VISIBLE
                        e.printStackTrace()
                        postSub?.dispose()
                        sub.dispose()
                    },
                    {
                        progressList[index] = 100
                        if(progressList.all{it == 100}){
                            enableButton(true)
                            uploadProgressBar.visibility = View.GONE
                            upload_completed_textview.visibility = View.VISIBLE
                        }
                        postSub?.dispose()
                        sub.dispose()
                    }
                )
        }
    }

    private fun post() {
        enableButton(false)
        pixelfedAPI.postStatus(
            authorization = "Bearer $accessToken",
            statusText = description,
            media_ids = muListOfIds.toList()
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
            posting_progress_bar.visibility = View.GONE
            post_creation_send_button.visibility = View.VISIBLE
        } else{
            posting_progress_bar.visibility = View.VISIBLE
            post_creation_send_button.visibility = View.GONE
        }

    }

    override fun onClick(position: Int) {
        positionResult = position

        val intent = Intent(this, PhotoEditActivity::class.java)
            .putExtra("picture_uri", Uri.parse(posts[position]))
            .putExtra("no upload", false)
        startActivityForResult(intent, positionResult)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == positionResult) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                posts[positionResult] = data.getStringExtra("result")!!
                adapter.notifyItemChanged(positionResult)
                muListOfIds.clear()
                upload()
            }
            else if(resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(applicationContext, "Edition cancelled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Error while editing", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class PostCreationAdapter(private val posts: ArrayList<String>): RecyclerView.Adapter<PostCreationAdapter.ViewHolder>() {
        private var context: Context? = null
        var listener: PostCreationListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            context = parent.context
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.image_album_creation, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            Log.d("test", "binded")
            holder.bind()
        }

        override fun getItemCount(): Int = posts.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bind() {
                val image = Uri.parse(posts[adapterPosition])
                // load image
                Glide.with(context!!)
                    .load(image)
                    .centerCrop()
                    .into(itemView.galleryImage)

                // adding click or tap handler for the image layout
                itemView.galleryImage.setOnClickListener {
                    Log.d("test", "clicked")
                    listener?.onClick(adapterPosition)
                }
            }
        }
    }
}
