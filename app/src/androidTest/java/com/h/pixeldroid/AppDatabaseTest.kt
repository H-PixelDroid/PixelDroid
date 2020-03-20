package com.h.pixeldroid

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.PostEntity
import com.h.pixeldroid.db.PostViewModel
import org.junit.*
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private var postTest = PostEntity(1, "test", date= Calendar.getInstance().time)
    private lateinit var postViewModel: PostViewModel

    @Rule @JvmField
    var activityTestRule = ActivityTestRule(MainActivity::class.java)

    @Before
    fun setup() {
        val preferences = InstrumentationRegistry.getInstrumentation()
            .targetContext.getSharedPreferences("com.h.pixeldroid.pref", Context.MODE_PRIVATE)
        preferences.edit().putString("accessToken", "azerty").apply()
        preferences.edit().putString("domain", "http://localhost").apply()
        ActivityScenario.launch(MainActivity::class.java)

        AppDatabase.TEST_MODE = true
        postViewModel =  ViewModelProvider(activityTestRule.activity).get(PostViewModel::class.java)
    }

    @After
    fun tearDown() {

    }

    /*
    @Test
    fun testInsertPostItem() {
        postViewModel.insertPosts(arrayListOf(postTest))
        Assert.assertEquals(1, postViewModel.getPostsCount())
        //Assert.assertEquals(postTest.domain, postViewModel.getById(postTest.uid)?.value?.domain)
    }

    @Test
    fun testDeleteAll(){
        postViewModel.deleteAll()
        Assert.assertEquals(0, postViewModel.getPostsCount())
    }

    @Test
    fun testUtilsInsertAll() {
        val postTest2 = PostEntity(2, "test", date = Calendar.getInstance().time)
        postViewModel.insertPosts(arrayListOf(postTest, postTest2))

        Assert.assertEquals(2, postViewModel.getPostsCount())
        Assert.assertEquals(postTest.domain, postViewModel.getById(postTest.uid)?.value?.domain)
        Assert.assertEquals(postTest2.domain, postViewModel.getById(postTest2.uid)?.value?.domain)
    }

    @Test
    fun testUtilsLRU() {
        for(i in 1..postViewModel.MAX_NUMBER_OF_POSTS) {
            postViewModel.insertPosts(arrayListOf(PostEntity(i, i.toString(), date = Calendar.getInstance().time)))
            Thread.sleep(10)
        }

        Assert.assertEquals(postViewModel.MAX_NUMBER_OF_POSTS, postViewModel.getPostsCount())
        Assert.assertEquals("1", postViewModel.getOldestPost()?.value?.domain)

        postViewModel.insertPosts(arrayListOf(PostEntity(0, "0", date = Calendar.getInstance().time)))
        Assert.assertEquals(postViewModel.MAX_NUMBER_OF_POSTS, postViewModel.getPostsCount())

        val eldestPost = postViewModel.getById(1)
        Assert.assertEquals(null, eldestPost)
    }
    */
}