package com.h.pixeldroid.utils

import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.PostEntity
import java.util.Calendar

class DatabaseUtils {
    companion object {
        /**
         * Inserts one post into the specified database,
         * after it has checked the LRU
         */
//        private fun insertPost(db: AppDatabase, post: PostEntity) {
//            if (!IsInsertable(db)) {
//                removeEldestPost(db)
//            }
//
//            db.postDao().addDateToPost(post.uid, Calendar.getInstance().time)
//            db.postDao().insertAll(post)
//        }
//
//        /**
//         * Inserts multiple posts into the specified database
//         */
//        fun insertAllPosts(db: AppDatabase, vararg posts: PostEntity) {
//            posts.forEach { insertPost(db, it) }
//        }
//
//        /**
//         * Checks if we can add one post into the database
//         * or if it is full
//         */
//        private fun IsInsertable(db: AppDatabase): Boolean {
//            return db.postDao().getPostsCount() + 1 <= db.MAX_NUMBER_OF_POSTS
//        }
//
//        /**
//         * Removes the eldest post from the database
//         */
//        private fun removeEldestPost(db: AppDatabase) {
//            db.postDao().deleteOldestPost()
//        }
    }

}