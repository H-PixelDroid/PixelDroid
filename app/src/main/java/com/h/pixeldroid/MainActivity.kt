package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayoutMediator
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.fragments.NewPostFragment
import com.h.pixeldroid.fragments.SearchDiscoverFragment
import com.h.pixeldroid.fragments.feeds.PostsFeedFragment
import com.h.pixeldroid.fragments.feeds.NotificationsFragment
import com.h.pixeldroid.fragments.feeds.PublicTimelineFragment
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.utils.DBUtils
import com.h.pixeldroid.utils.ImageConverter
import com.h.pixeldroid.utils.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.view.drawer_account_name
import kotlinx.android.synthetic.main.nav_header.view.drawer_avatar


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var preferences: SharedPreferences
    private val searchDiscoverFragment: SearchDiscoverFragment = SearchDiscoverFragment()
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferences = getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.pref", Context.MODE_PRIVATE
        )
        db = DBUtils.initDB(applicationContext)

        //Check if we have logged in and gotten an access token
        if((Utils.hasInternet(applicationContext) && !preferences.contains("accessToken"))
            || !preferences.contains("user_id")) {
            launchActivity(LoginActivity())
        } else {
            setupDrawer()
            val tabs = arrayOf(
                PostsFeedFragment(),
                searchDiscoverFragment,
                NewPostFragment(),
                NotificationsFragment(),
                PublicTimelineFragment()
            )
            setupTabs(tabs)
        }
    }

    private fun setupDrawer() {
        nav_view.setNavigationItemSelectedListener(this)
        if (Utils.hasInternet(applicationContext)) {
            val accessToken = preferences.getString("accessToken", "")
            val pixelfedAPI = PixelfedAPI.create("${preferences.getString("domain", "")}")
            pixelfedAPI.verifyCredentials("Bearer $accessToken")
                .enqueue(object : Callback<Account> {
                    override fun onResponse(call: Call<Account>, response: Response<Account>) {
                        if (response.body() != null && response.isSuccessful) {
                            val account = response.body() as Account
                            DBUtils.addUser(db, account)
                            fillDrawerAccountInfo(account)
                        }
                    }

                    override fun onFailure(call: Call<Account>, t: Throwable) {
                        Log.e("DRAWER ACCOUNT:", t.toString())
                    }
                })
        } else {
            val userId = preferences.getString("user_id", null).orEmpty()
            if (userId.isNotEmpty()) {
                val user: UserDatabaseEntity = db.userDao().getUserWithId(userId)
                val account = Account(
                    id = user.user_id,
                    username = user.username,
                    display_name = user.display_name,
                    avatar_static = user.avatar_static
                )
                fillDrawerAccountInfo(account)
            } else {
                launchActivity(LoginActivity())
            }
        }
    }

    private fun fillDrawerAccountInfo(account: Account) {
        val drawerAvatar = nav_view.getHeaderView(0).drawer_avatar
        val drawerAccountName = nav_view.getHeaderView(0).drawer_account_name
        ImageConverter.setRoundImageFromURL(
            View(applicationContext),
            account.avatar_static,
            drawerAvatar
        )
        drawerAvatar.setOnClickListener { launchActivity(ProfileActivity()) }
        // Set account name
        drawerAccountName.apply {
            text = account.display_name
            setOnClickListener { launchActivity(ProfileActivity()) }
        }
    }

    private fun setupTabs(tab_array: Array<Fragment>){
        view_pager.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment {
                return tab_array[position]
            }

            override fun getItemCount(): Int {
                return 5
            }
        }
        TabLayoutMediator(tabs, view_pager) { tab, position ->
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

        drawer_layout.closeDrawer(GravityCompat.START)

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
        if(drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

}