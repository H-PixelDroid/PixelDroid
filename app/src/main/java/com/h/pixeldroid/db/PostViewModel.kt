package com.h.pixeldroid.db

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import java.util.*
import kotlin.collections.ArrayList

class PostViewModel(application: Application) : AndroidViewModel(application) {

    // The ViewModel maintains a reference to the repository to get data.
    private val repository: PostRepository = PostRepository(application)
    // LiveData gives us updated posts when they change.
    val allPosts: LiveData<List<PostEntity>>?
    val MAX_NUMBER_OF_POSTS = 100

    init {
        // Constructs the PostRepository
        allPosts = repository.allPosts
    }

    fun insertPosts(posts: ArrayList<PostEntity>) {
        posts.forEach() {
            if (!IsInsertable()) {
                repository.deleteOldestPost()
            }

            repository.addDateToPost(it.uid, Calendar.getInstance().time)
            repository.insertPosts(posts)
        }
    }

    fun getByID(id: Int) = repository.getByID(id)

    fun getAllByDate() = repository.getAllByDate()

    fun getOldestPost() = repository.getOldestPost()

    fun getPostsCount() = repository.getPostsCount()

    fun addDateToPost(postId: Int, date: Date) { repository.addDateToPost(postId, date) }

    fun getAll() = repository.getAll()

    /**
     * Checks if we can add one post into the database
     * or if it is full
     */
    private fun IsInsertable(): Boolean {
        val nbPosts = repository.getPostsCount()?.value ?: 0

        return nbPosts + 1 <= MAX_NUMBER_OF_POSTS
    }
}
