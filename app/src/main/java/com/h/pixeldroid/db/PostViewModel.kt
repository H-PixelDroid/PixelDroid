package com.h.pixeldroid.db

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.h.pixeldroid.objects.Status
import java.util.*
import kotlin.collections.ArrayList

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PostRepository = PostRepository(application)
    val allPosts: LiveData<List<PostEntity>>?
    val MAX_NUMBER_OF_POSTS = 100

    init {
        // Constructs the PostRepository
        allPosts = repository.allPosts
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

    fun getById(id: Long): PostEntity {
        Log.d("test1", id.toString())
        val post = repository.getById(id)?.value!!

        Log.d("test1", post.uid.toString())
        Log.d("test1", post.status.id)

        return post
    }

    fun getAllByDate() : List<PostEntity> {
        return repository.getAllByDate()?.value!!
    }

    fun getOldestPost(): PostEntity {
        return repository.getOldestPost()?.value!!
    }

    fun getPostCount(): Int {
        return repository.getPostCount()!!
    }

    fun addDateToPost(postId: Long, date: Date) { repository.addDateToPost(postId, date) }

    fun getAll(): List<PostEntity> {
        val posts = repository
            .getAll()
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