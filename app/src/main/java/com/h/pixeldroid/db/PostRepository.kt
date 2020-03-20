package com.h.pixeldroid.db

import android.app.Application
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

class PostRepository(application: Application) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private var postDao : PostDao?

    init {
        val db = AppDatabase.getDatabase(application)
        postDao = db.postDao()
    }

    val allPosts: LiveData<List<PostEntity>>? = postDao?.getAllByDate()

    fun getAll() = postDao?.getAll()

    fun getByID(id: Int) = postDao?.getById(id)

    fun getAllByDate() = postDao?.getAllByDate()

    fun getOldestPost() = postDao?.getOldestPost()

    fun getPostsCount() = postDao?.getPostsCount()

    fun deleteOldestPost() = postDao?.deleteOldestPost()

    fun addDateToPost(postId: Int, date: Date) {
        launch  { addDateToPostBG(postId, date) }
    }

    fun insertPosts(posts: ArrayList<PostEntity>) {
        launch  { insertPostsBG(posts) }
    }

    private suspend fun addDateToPostBG(postId: Int, date: Date){
        withContext(Dispatchers.IO){
            postDao?.addDateToPost(postId, date)
        }
    }

    private suspend fun insertPostsBG(posts: ArrayList<PostEntity>){
        withContext(Dispatchers.IO){
            postDao?.insertAll(posts)
        }
    }
}