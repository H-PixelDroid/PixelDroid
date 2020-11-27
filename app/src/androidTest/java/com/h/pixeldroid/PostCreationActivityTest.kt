package com.h.pixeldroid

/*
@RunWith(AndroidJUnit4::class)
class PostCreationActivityTest {

    private var testScenario: ActivityScenario<PostCreationActivity>? = null
    private lateinit var mockServer: MockServer
    private lateinit var db: AppDatabase
    private lateinit var context: Context


    @get:Rule
    val globalTimeout: Timeout = Timeout.seconds(30)

    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun File.writeBitmap(bitmap: Bitmap) {
        outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
            out.flush()
        }
    }

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        mockServer = MockServer()
        mockServer.start()
        val baseUrl = mockServer.getUrl()
        db = initDB(context)
        db.clearAllTables()
        db.instanceDao().insertInstance(
            InstanceDatabaseEntity(
                uri = baseUrl.toString(),
                title = "PixelTest"
            )
        )

        db.userDao().insertUser(
            UserDatabaseEntity(
                user_id = "123",
                instance_uri = baseUrl.toString(),
                username = "Testi",
                display_name = "Testi Testo",
                avatar_static = "some_avatar_url",
                isActive = true,
                accessToken = "token"
            )
        )
        db.close()

        var uri1: String = ""
        var uri2: String = ""
        val scenario = ActivityScenario.launch(AboutActivity::class.java)
        scenario.onActivity {
            val image1 = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
            image1.eraseColor(Color.GREEN)
            val image2 = Bitmap.createBitmap(270, 270, Bitmap.Config.ARGB_8888)
            image2.eraseColor(Color.RED)

            val file1 = File.createTempFile("temp_img1", ".png")
            val file2 = File.createTempFile("temp_img2", ".png")
            file1.writeBitmap(image1)
            uri1 = file1.toUri().toString()
            file2.writeBitmap(image2)
            uri2 = file2.toUri().toString()
            Log.d("test", uri1+"\n"+uri2)
        }
        val intent = Intent(context, PostCreationActivity::class.java).putExtra("pictures_uri", arrayListOf(uri1, uri2))
        testScenario = ActivityScenario.launch(intent)
    }

    @After
    fun after() {
        clearData()
        mockServer.stop()
    }

    @Test
    fun createPost() {
        onView(withId(R.id.post_creation_send_button)).perform(click())
        // should send on main activity
        onView(withId(R.id.main_activity_main_linear_layout)).check(matches(isDisplayed()))
    }

    @Test
    fun errorShown() {
        testScenario!!.onActivity { a -> a.upload_error.visibility = VISIBLE }
        onView(withId(R.id.retry_upload_button)).perform(click())
        // should send on main activity
        onView(withId(R.id.retry_upload_button)).check(matches(not(isDisplayed())))
    }

    @Test
    fun editImage() {
        Thread.sleep(1000)

        onView(withId(R.id.image_grid)).perform(
            RecyclerViewActions.actionOnItemAtPosition<PostCreationActivity.PostCreationAdapter.ViewHolder>(
                0,
                CustomMatchers.clickChildViewWithId(R.id.galleryImage)
            )
        )
        Thread.sleep(1000)

        onView(withId(R.id.recycler_view))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<ThumbnailAdapter.MyViewHolder>(
                    2,
                    CustomMatchers.clickChildViewWithId(R.id.thumbnail)
                )
            )
        Thread.sleep(1000)
        onView(withId(R.id.action_upload)).perform(click())
        Thread.sleep(1000)
        onView(withId(R.id.image_grid)).check(matches(isDisplayed()))
    }

    @Test
    fun cancelEdit() {
        onView(withId(R.id.image_grid)).perform(
            RecyclerViewActions.actionOnItemAtPosition<PostCreationActivity.PostCreationAdapter.ViewHolder>(
                0,
                CustomMatchers.clickChildViewWithId(R.id.galleryImage)
            )
        )
        Thread.sleep(1000)
    }
}*/