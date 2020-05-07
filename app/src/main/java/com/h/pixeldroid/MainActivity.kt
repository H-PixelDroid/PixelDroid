package com.h.pixeldroid

import android.Manifest
import com.h.pixeldroid.fragments.CameraFragment
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.fragments.SearchDiscoverFragment
import com.h.pixeldroid.fragments.feeds.PostsFeedFragment
import com.h.pixeldroid.fragments.feeds.NotificationsFragment
import com.h.pixeldroid.fragments.feeds.PublicTimelineFragment
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.utils.ImageConverter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var preferences: SharedPreferences
    private val searchDiscoverFragment: SearchDiscoverFragment = SearchDiscoverFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        preferences = getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )

        //Check if we have logged in and gotten an access token
        if(!preferences.contains("accessToken")){
            launchActivity(LoginActivity())
        } else {
            setupDrawer()

            val tabs = arrayOf(
                PostsFeedFragment(),
                searchDiscoverFragment,
                Fragment(),
                NotificationsFragment(),
                PublicTimelineFragment()
            )

            setupTabs(tabs)
        }
    }

    private fun setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Setup views
        val accessToken = preferences.getString("accessToken", "")
        val pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")

        val drawerHeader = navigationView.getHeaderView(0)
        val accountName = drawerHeader.findViewById<TextView>(R.id.drawer_account_name)
        val avatar = drawerHeader.findViewById<ImageView>(R.id.drawer_avatar)

        pixelfedAPI.verifyCredentials("Bearer $accessToken")
            .enqueue(object : Callback<Account> {
                override fun onResponse(call: Call<Account>, response: Response<Account>) {
                    if (response.code() == 200) {
                        val account = response.body()!!

                        // Set profile picture
                        ImageConverter.setRoundImageFromURL(
                            View(applicationContext), account.avatar_static, avatar)
                        avatar.setOnClickListener{ launchActivity(ProfileActivity()) }

                        // Set account name
                        accountName.text = account.display_name
                        accountName.setOnClickListener{ launchActivity(ProfileActivity()) }
                    }
                }

                override fun onFailure(call: Call<Account>, t: Throwable) {
                    Log.e("DRAWER ACCOUNT:", t.toString())
                }
            })
    }

    private fun setupTabs(tabs: Array<Fragment>){

        viewPager = findViewById(R.id.view_pager)
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment {
                return tabs[position]
            }

            override fun getItemCount(): Int {
                return 5
            }
        }
        tabLayout = findViewById(R.id.tabs)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when(position){
                0 -> tab.icon = getDrawable(R.drawable.ic_home_white_24dp)
                1 -> tab.icon = getDrawable(R.drawable.ic_search_white_24dp)
                2 -> tab.icon = getDrawable(R.drawable.ic_photo_camera_white_24dp)
                3 -> tab.icon = getDrawable(R.drawable.ic_heart)
                4 -> tab.icon = getDrawable(R.drawable.ic_filter_black_24dp)
            }
        }.attach()
    }

    /**
    When clicking in the drawer menu, go to the corresponding activity
     */
    override fun onNavigationItemSelected(@NonNull item: MenuItem): Boolean {
        when (item.itemId){
            R.id.nav_account -> launchActivity(ProfileActivity())
            R.id.nav_settings -> launchActivity(SettingsActivity())
            R.id.nav_logout -> launchActivity(LoginActivity())
        }

        drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    /**
    Launches the given activity and put it as the current one
     */
    private fun launchActivity(activity: AppCompatActivity) {
        val intent = Intent(this, activity::class.java)
        startActivity(intent)
    }

    /**
    Closes the drawer if it is open, when we press the back button
     */
    override fun onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}