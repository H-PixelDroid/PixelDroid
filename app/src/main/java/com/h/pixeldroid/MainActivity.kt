package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.h.pixeldroid.fragments.HomeFragment
import com.h.pixeldroid.fragments.MyProfileFragment


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferences = getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )

        //Check if we have logged in and gotten an access token
        if(!preferences.contains("accessToken")){
            launchActivity(LoginActivity())
        } else {
            // Setup the drawer
            drawerLayout = findViewById(R.id.drawer_layout)
            val navigationView: NavigationView = findViewById(R.id.nav_view)
            navigationView.setNavigationItemSelectedListener(this)

            val tabs =
                arrayOf(HomeFragment(), Fragment(), Fragment(), Fragment(), MyProfileFragment())

            setupTabs(tabs)
        }
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
                3 -> tab.icon = getDrawable(R.drawable.ic_star_white_24dp)
                4 -> tab.icon = getDrawable(R.drawable.ic_person_white_24dp)
            }
        }.attach()
    }

    /**
    When clicking in the drawer menu, go to the corresponding activity
     */
    override fun onNavigationItemSelected(@NonNull item: MenuItem): Boolean {
        when (item.itemId){
            R.id.nav_settings -> launchActivity(SettingsActivity())
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