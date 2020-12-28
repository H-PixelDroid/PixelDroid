package com.h.pixeldroid.postCreation

import android.app.Activity
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
import androidx.core.net.toFile
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputLayout
import com.h.pixeldroid.utils.BaseActivity
import com.h.pixeldroid.MainActivity
import com.h.pixeldroid.R
import com.h.pixeldroid.utils.api.PixelfedAPI
import com.h.pixeldroid.postCreation.camera.CameraActivity
import com.h.pixeldroid.utils.db.entities.UserDatabaseEntity
import com.h.pixeldroid.utils.api.objects.Attachment
import com.h.pixeldroid.utils.api.objects.Instance
import com.h.pixeldroid.utils.api.objects.Status
import com.h.pixeldroid.postCreation.photoEdit.PhotoEditActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_post_creation.*
import kotlinx.android.synthetic.main.image_album_creation.view.*
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "Post Creation Activity"
private const val MORE_PICTURES_REQUEST_CODE = 0xffff


class PostCreationActivity : BaseActivity() {

    private lateinit var recycler : RecyclerView
    private lateinit var adapter : PostCreationAdapter

    private lateinit var accessToken: String
    private lateinit var pixelfedAPI: PixelfedAPI

    private var muListOfIds: MutableList<String> = mutableListOf()
    private var progressList: ArrayList<Int> = arrayListOf()


    private var positionResult = 0
    private var user: UserDatabaseEntity? = null

    private var posts: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_creation)

        // get image URIs
        if(intent.clipData != null) {
            val count = intent.clipData!!.itemCount
            for (i in 0 until count) {
                val imageUri: String = intent.clipData!!.getItemAt(i).uri.toString()
                posts.add(imageUri)
            }
        }

        user = db.userDao().getActiveUser()

        val instances = db.instanceDao().getAll()

        val textField = findViewById<TextInputLayout>(R.id.postTextInputLayout)

        textField.counterMaxLength = if (user != null){
            val thisInstances =
                instances.filter { instanceDatabaseEntity ->
                    instanceDatabaseEntity.uri.contains(user!!.instance_uri)
                }
            thisInstances.first().max_toot_chars
        } else {
            Instance.DEFAULT_MAX_TOOT_CHARS
        }

        accessToken = user?.accessToken.orEmpty()
        pixelfedAPI = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)

        // check if the pictures are alright
        // TODO

        //upload the picture and display progress while doing so
        muListOfIds = posts.map { "" }.toMutableList()
        progressList = posts.map { 0 } as ArrayList<Int>
        upload()

        adapter = PostCreationAdapter(posts)
        recycler = findViewById(R.id.image_grid)
        recycler.layoutManager = GridLayoutManager(this, 3)
        recycler.adapter = adapter

        // get the description and send the post
        findViewById<Button>(R.id.post_creation_send_button).setOnClickListener {
            if (validateDescription() && muListOfIds.isNotEmpty()) post()
        }

        // Button to retry image upload when it fails
        findViewById<Button>(R.id.retry_upload_button).setOnClickListener {
            upload_error.visibility = View.GONE
            muListOfIds = posts.map { "" }.toMutableList()
            progressList = posts.map { 0 } as ArrayList<Int>
            upload()
        }
    }

    private fun validateDescription(): Boolean {
        val textField = findViewById<TextInputLayout>(R.id.postTextInputLayout)
        val content = textField.editText?.length() ?: 0
        if (content > textField.counterMaxLength) {
            // error, too many characters
            textField.error = getString(R.string.description_max_characters).format(textField.counterMaxLength)
            return false
        }
        return true
    }

    /**
     * Uploads the images that are in the [posts] array.
     * Keeps track of them in the [progressList] (for the upload progress), and the [muListOfIds]
     * (for the list of ids of the uploads).
     * @param newImagesStartingIndex is the index in the [posts] array we want to start uploading at.
     * Indices before this are already uploading, or done uploading, from before.
     * @param editedImage contains the index of the image that was edited. If set, other images are
     * not uploaded again: they should already be uploading, or be done uploading, from before.
     */
    private fun upload(newImagesStartingIndex: Int = 0, editedImage: Int? = null) {
        enableButton(false)
        uploadProgressBar.visibility = View.VISIBLE
        upload_completed_textview.visibility = View.INVISIBLE

        val range: IntRange = if(editedImage == null){
            newImagesStartingIndex until posts.size
        } else IntRange(editedImage, editedImage)

        for (index in range) {
            val imageUri = Uri.parse(posts[index])
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
        val description = new_post_description_input_field.text.toString()
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
        } else {
            posting_progress_bar.visibility = View.VISIBLE
            post_creation_send_button.visibility = View.GONE
        }

    }

    fun onClick(position: Int) {
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
                muListOfIds[positionResult] = ""
                progressList[positionResult] = 0
                upload(editedImage = positionResult)
            } else if(resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(applicationContext, "Editing canceled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Error while editing", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == MORE_PICTURES_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data?.clipData != null) {

                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri: String = data.clipData!!.getItemAt(i).uri.toString()
                    posts.add(imageUri)
                    progressList.add(0)
                    muListOfIds.add("")
                }
                adapter.notifyDataSetChanged()
                upload(newImagesStartingIndex = posts.size - count)
            } else if(resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(applicationContext, "Adding images canceled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Error while adding images", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class PostCreationAdapter(private val posts: ArrayList<String>): RecyclerView.Adapter<PostCreationAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                if(viewType == 0) LayoutInflater.from(parent.context)
                .inflate(R.layout.image_album_creation, parent, false)
                else LayoutInflater.from(parent.context)
                    .inflate(R.layout.add_more_album_creation, parent, false)
            return ViewHolder(view)
        }

        override fun getItemViewType(position: Int): Int {
            if(position == posts.size) return 1
            return 0
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if(position != posts.size) {
                holder.bindImage()
            } else{
                holder.bindPlusButton()
            }
        }

        override fun getItemCount(): Int = posts.size + 1

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bindImage() {
                val image = Uri.parse(
                    posts[adapterPosition]
                )
                // load image
                Glide.with(itemView.context)
                    .load(image)
                    .centerCrop()
                    .into(itemView.galleryImage)
                // adding click or tap handler for the image layout
                itemView.setOnClickListener {
                    this@PostCreationActivity.onClick(adapterPosition)
                }

            }

            fun bindPlusButton() {
                itemView.setOnClickListener {
                    val intent = Intent(itemView.context, CameraActivity::class.java)
                    this@PostCreationActivity.startActivityForResult(intent, MORE_PICTURES_REQUEST_CODE)
                }
            }
        }
    }
}