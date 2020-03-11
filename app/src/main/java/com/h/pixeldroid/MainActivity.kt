package com.h.pixeldroid

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.h.pixeldroid.fragments.HomeFragment
import com.h.pixeldroid.motions.OnSwipeListener

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainLinearLayout : LinearLayout = findViewById(R.id.main_linear_layout)
        val homepageButton : ImageButton = findViewById(R.id.activity_main_home_btn)
        val accountButton : ImageButton = findViewById(R.id.activity_main_account_btn)

        homepageButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        accountButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Setup the drawer
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val onSwipeListener = object: OnSwipeListener(this) {
            override fun onSwipeRight() = drawerLayout.openDrawer(GravityCompat.START)
        }
        mainLinearLayout.setOnTouchListener(onSwipeListener)

        launchFragment(HomeFragment())
    }

    /*
    Launches the given fragment and put it as the current "activity"
     */
    private fun launchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }

    /*
    When clicking in the drawer menu, go to the corresponding activity
     */
    override fun onNavigationItemSelected(@NonNull item: MenuItem): Boolean {
        when (item.itemId){
            R.id.nav_settings -> launchActivity(SettingsActivity())
            R.id.nav_account -> launchActivity(ProfileActivity())
        }

        drawerLayout.closeDrawer(GravityCompat.START)

        return true
    }

    /*
    Launches the given activity and put it as the current one
     */
    private fun launchActivity(activity: AppCompatActivity) {
        val intent = Intent(this, activity::class.java)
        startActivity(intent)
    }

    /*
    Closes the drawer if we are clicking behind it
     */
    override fun onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

}
