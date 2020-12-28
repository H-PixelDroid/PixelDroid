package com.h.pixeldroid

/*
@RunWith(AndroidJUnit4::class)
class LoginActivityOfflineTest {

    private lateinit var context: Context

    companion object {
        fun switchAirplaneMode() {
            val device = UiDevice.getInstance(getInstrumentation())
            device.openQuickSettings()
            device.findObject(UiSelector().textContains("airplane")).click()
            device.pressHome()
        }
    }

    private lateinit var db: AppDatabase

    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(100)

    @Before
    fun before() {
        switchAirplaneMode()
        context = ApplicationProvider.getApplicationContext<Context>()
        db = initDB(context)
        db.clearAllTables()
        ActivityScenario.launch(LoginActivity::class.java)
    }

    @Test
    fun emptyDBandOfflineModeDisplayCorrectMessage() {
        onView(withId(R.id.login_activity_connection_required)).check(matches(isDisplayed()))
    }

    @Test
    fun retryButtonReloadsLoginActivity() {
        onView(withId(R.id.login_activity_connection_required_button)).perform(click())
        onView(withId(R.id.login_activity_connection_required)).check(matches(isDisplayed()))
    }

    @After
    fun after() {
        switchAirplaneMode()
        db.close()
        clearData()
    }
}

 */