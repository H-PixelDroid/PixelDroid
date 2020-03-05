package com.h.pixeldroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.NonNull
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.h.pixeldroid.settings.ui.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
