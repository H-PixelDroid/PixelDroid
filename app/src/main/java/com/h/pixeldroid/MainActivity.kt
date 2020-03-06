package com.h.pixeldroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.view.MenuItem
import androidx.annotation.NonNull
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.h.pixeldroid.settings.ui.*
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.h.pixeldroid.PostActivity.Companion.POST_TAG
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.objects.Post
import com.h.pixeldroid.objects.Status
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout

    private var statuses: ArrayList<Status>? = null
    private val BASE_URL = "https://pixelfed.de/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val homepage_button : ImageButton = findViewById(R.id.activity_main_home_btn)
        val search_button : ImageButton = findViewById(R.id.activity_main_search_btn)
        val camera_button : ImageButton = findViewById(R.id.activity_main_camera_btn)
        val favorite_button : ImageButton = findViewById(R.id.activity_main_favorite_btn)
        val account_button : ImageButton = findViewById(R.id.activity_main_account_btn)

        homepage_button.setOnClickListener(
            View.OnClickListener {
                startActivity(Intent(this, MainActivity::class.java))
            }
        )

        account_button.setOnClickListener((View.OnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }))

        loadData()

        // setup the top toolbar
        val toolbar : Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Setup the drawer
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // On startup ONLY, start at the account settings
        if(savedInstanceState == null) {
            launchFragment(AccountFragment())
            navigationView.setCheckedItem(R.id.nav_account)
        }

        val button = findViewById<Button>(R.id.button_start_login)
        button.setOnClickListener((View.OnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent) }))
    }

    private fun loadData() {

        val pixelfedAPI = PixelfedAPI.create(BASE_URL)

        pixelfedAPI.timelinePublic(null, null, null, null, null)
            .enqueue(object : Callback<List<Status>> {
                override fun onResponse(call: Call<List<Status>>, response: Response<List<Status>>) {
                    if (response.code() == 200) {
                        statuses = response.body() as ArrayList<Status>?
                    }
                }

                override fun onFailure(call: Call<List<Status>>, t: Throwable) {
                    Log.e("Ouch, not OK", t.toString())
                }
            })

        val postButton = findViewById<Button>(R.id.postButton)
        postButton.setOnClickListener((View.OnClickListener {
            val intent = Intent(this, PostActivity::class.java)
            intent.putExtra(POST_TAG ,Post(statuses?.get(0)))
            startActivity(intent)
        }))

    }

    /*
    When clicking in the drawer menu, go to the corresponding activity
     */
    override fun onNavigationItemSelected(@NonNull item: MenuItem): Boolean {
        when (item.itemId){
            R.id.nav_account -> launchFragment(AccountFragment())
            R.id.nav_accessibility -> launchFragment(AccessibilityFragment())
            R.id.nav_settings -> launchFragment(SettingsFragment())
        }

        drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    /*
    Launches the given fragment and put it as the current "activity"
     */
    private fun launchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }

    /*
    Makes it possible to drag the settings menu from the left
     */
    override fun onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }

    }

}
