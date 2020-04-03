package com.h.pixeldroid.db

import android.app.Application
import android.util.Log
import androidx.annotation.NonNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.h.pixeldroid.objects.Status
import java.util.*


class PostViewModel(application: Application) : AndroidViewModel(application), ViewModelProvider.Factory {

    private val repository: PostRepository = PostRepository(application)
    val allPosts: List<PostEntity>?
    val MAX_NUMBER_OF_POSTS = 100
    private lateinit var myApplication: Application

    init {
        // Constructs the PostRepository
        allPosts = repository.allPosts
        myApplication = application
    }

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PostViewModel(myApplication) as T
    }

    fun insertAllPosts(posts: ArrayList<PostEntity>) {
        posts.forEach() {
            insertPost(it)
        }
    }

    fun insertPost(post: PostEntity) {
        if (!isInsertable()) {
            repository.deleteOldestPost()
        }

        repository.addDateToPost(post.uid, Calendar.getInstance().time)
        repository.insertPost(post)
    }

    fun insertStatusAsPost(status: Status) {
        val postToAdd = PostEntity(uid = 0, status = status, date = Calendar.getInstance().time)
        insertPost(postToAdd)
    }

    fun getById(id: Long): PostEntity? {
        Log.i("test1", id.toString())
        val post = repository.getById(id)

        Log.i("test1", post?.uid.toString())
        Log.i("test1", post?.status?.id!!)

        return post
    }

    fun getAllByDate() = repository.getAllByDate()

    fun getOldestPost() = repository.getOldestPost()

    fun getPostCount() = repository.getPostCount()!!

    fun addDateToPost(postId: Long, date: Date) { repository.addDateToPost(postId, date) }

    fun getAll(): List<PostEntity>? {
        val posts = repository.getAll()
        Log.d("test1", posts?.size.toString())
        Log.d("test1", posts?.get(0)?.uid.toString())
        Log.d("test1", posts?.get(0)?.status?.id!!)

        return posts
    }

    fun deleteAll() = repository.deleteAll()

    /**
     * Checks if we can add one post into the database
     * or if it is full
     */
    private fun isInsertable(): Boolean {
        val nbPosts = repository.getPostCount()!!

        return nbPosts + 1 <= MAX_NUMBER_OF_POSTS
    }
}