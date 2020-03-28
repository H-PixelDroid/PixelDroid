package com.h.pixeldroid.db

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.CoroutineContext

class PostRepository(application: Application) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private var postDao : PostDao?

    init {
        val db = AppDatabase.getDatabase(application)
        postDao = db.postDao()
    }

    val allPosts = postDao?.getAllByDate()

    fun getAll() =  postDao?.getAll()

    fun getById(id: Long) = postDao?.getById(id)

    fun getAllByDate() = postDao?.getAllByDate()

    fun getOldestPost() = postDao?.getOldestPost()

    fun getPostCount() = postDao?.getPostCount()

    fun deleteOldestPost() = postDao?.deleteOldestPost()

    fun deleteAll() = postDao?.deleteAll()

    fun addDateToPost(postId: Long, date: Date) {
        launch  { addDateToPostBG(postId, date) }
    }

    fun insertPost(post: PostEntity) {
        var uid: Long = 0
        launch  { uid = insertPostBG(post) }

        Log.d("test1", "uid = " + post.uid.toString() + " return = " + uid.toString())
    }

    private suspend fun addDateToPostBG(postId: Long, date: Date){
        withContext(Dispatchers.IO){
            postDao?.addDateToPost(postId, date)
        }
    }

    private suspend fun insertPostBG(post: PostEntity): Long {
        var uid: Long = 0
        withContext(Dispatchers.IO){
            uid = postDao?.insertPost(post)!!
        }

        return uid
    }
}