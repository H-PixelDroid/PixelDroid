package com.h.pixeldroid

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.db.UserDatabaseEntity
import com.h.pixeldroid.fragments.CameraFragment
import com.h.pixeldroid.fragments.SearchDiscoverFragment
import com.h.pixeldroid.fragments.feeds.NotificationsFragment
import com.h.pixeldroid.fragments.feeds.OfflineFeedFragment
import com.h.pixeldroid.fragments.feeds.PostsFeedFragment
import com.h.pixeldroid.fragments.feeds.PublicTimelineFragment
import com.h.pixeldroid.objects.Account
import com.h.pixeldroid.utils.DBUtils
import com.h.pixeldroid.utils.Utils.Companion.hasInternet
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.materialdrawer.iconics.iconicsIcon
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.*
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val searchDiscoverFragment: SearchDiscoverFragment = SearchDiscoverFragment()
    private lateinit var db: AppDatabase
    private lateinit var header: AccountHeaderView
    private var user: UserDatabaseEntity? = null

    companion object {
        const val ADD_ACCOUNT_IDENTIFIER: Long = -13
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DBUtils.initDB(applicationContext)

        //get the currently active user
        user = db.userDao().getActiveUser()

        //Check if we have logged in and gotten an access token
        if (user == null) {
            launchActivity(LoginActivity(), firstTime = true)
        } else {
            setupDrawer()
            val tabs = arrayOf(
                if (hasInternet(applicationContext)) PostsFeedFragment()
                else OfflineFeedFragment(),
                searchDiscoverFragment,
                CameraFragment(),
                NotificationsFragment(),
                PublicTimelineFragment()
            )
            setupTabs(tabs)
        }
    }

    private fun setupDrawer() {
        header = AccountHeaderView(this).apply {
            headerBackgroundScaleType = ImageView.ScaleType.CENTER_CROP
            currentHiddenInList = true
            onAccountHeaderListener = { _: View?, profile: IProfile, current: Boolean ->
                clickProfile(profile, current)
            }
            addProfile(ProfileSettingDrawerItem().apply {
                identifier = ADD_ACCOUNT_IDENTIFIER
                nameRes = R.string.add_account_name
                descriptionRes = R.string.add_account_description
                iconicsIcon = GoogleMaterial.Icon.gmd_add
            }, 0)
            attachToSliderView(drawer)
            dividerBelowHeader = false
            closeDrawerOnProfileListClick = true
        }

        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView, uri: Uri, placeholder: Drawable, tag: String?) {
                    Glide.with(imageView.context)
                        .load(uri)
                        .placeholder(placeholder)
                        .into(imageView)
            }

            override fun cancel(imageView: ImageView) {
                Glide.with(imageView.context).clear(imageView)
            }

            override fun placeholder(ctx: Context, tag: String?): Drawable {
                if (tag == DrawerImageLoader.Tags.PROFILE.name || tag == DrawerImageLoader.Tags.PROFILE_DRAWER_ITEM.name) {
                    return ctx.getDrawable(R.drawable.ic_default_user)!!
                }

                return super.placeholder(ctx, tag)
            }
        })

        fillDrawerAccountInfo(user!!.user_id)

        //after setting with the values in the db, we make sure to update the database and apply
        //with the received one. This happens asynchronously.
        getUpdatedAccount()

        drawer.itemAdapter.add(
            primaryDrawerItem {
                nameRes = R.string.menu_account
                iconicsIcon = GoogleMaterial.Icon.gmd_person
            },
            primaryDrawerItem {
                nameRes = R.string.menu_settings
                iconicsIcon = GoogleMaterial.Icon.gmd_settings
            },
            primaryDrawerItem {
                nameRes = R.string.logout
                iconicsIcon = GoogleMaterial.Icon.gmd_close
            })
        drawer.onDrawerItemClickListener = { v, drawerItem, position ->
            when (position){
                1 -> launchActivity(ProfileActivity())
                2 -> launchActivity(SettingsActivity())
                3 -> logOut()
            }
            false
        }
    }

    private fun logOut(){
        db.userDao().deleteActiveUsers()

        val remainingUsers = db.userDao().getAll()
        if (remainingUsers.isEmpty()){
            //no more users, start first-time login flow
            launchActivity(LoginActivity(), firstTime = true)
        } else {
            val newActive = remainingUsers.first()
            db.userDao().activateUser(newActive.user_id)
            //relaunch the app
            launchActivity(MainActivity(), firstTime = true)
        }



    }
    private fun getUpdatedAccount(){
        if (hasInternet(applicationContext)) {
            val domain = user?.instance_uri.orEmpty()
            val accessToken = user?.accessToken.orEmpty()
            val pixelfedAPI = PixelfedAPI.create(domain)
            pixelfedAPI.verifyCredentials("Bearer $accessToken")
                .enqueue(object : Callback<Account> {
                    override fun onResponse(call: Call<Account>, response: Response<Account>) {
                        if (response.body() != null && response.isSuccessful) {
                            val account = response.body() as Account
                            DBUtils.addUser(db, account, domain, accessToken = accessToken)
                            fillDrawerAccountInfo(account.id)
                        }
                    }

                    override fun onFailure(call: Call<Account>, t: Throwable) {
                        Log.e("DRAWER ACCOUNT:", t.toString())
                    }
                })
        }
    }

    //called when switching profiles, or when clicking on current profile
    private fun clickProfile(profile: IProfile, current: Boolean): Boolean {
        if(current){
            launchActivity(ProfileActivity())
            return false
        }
        //Clicked on add new account
        if(profile.identifier == ADD_ACCOUNT_IDENTIFIER){
            launchActivity(LoginActivity())
            return false
        }

        db.userDao().deActivateActiveUser()
        db.userDao().activateUser(profile.identifier.toString())

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        return false
    }

    private inline fun primaryDrawerItem(block: PrimaryDrawerItem.() -> Unit): PrimaryDrawerItem {
        return PrimaryDrawerItem()
            .apply {
                isSelectable = false
                isIconTinted = true
            }
            .apply(block)
    }

    private fun fillDrawerAccountInfo(account: String) {
        val users = db.userDao().getAll().toMutableList()
        users.sortWith(Comparator { l, r ->
            when {
                l.isActive && !r.isActive -> -1
                r.isActive && !l.isActive -> 1
                else -> 0
            }
        })
        val profiles: MutableList<IProfile> = users.map { user ->
            ProfileDrawerItem().apply {
                isSelected = user.isActive
                nameText = user.display_name
                iconUrl = user.avatar_static
                isNameShown = true
                identifier = user.user_id.toLong()
                descriptionText = "${user.username}@${user.instance_uri.removePrefix("https://")}"
            }
        }.toMutableList()

        // reuse the already existing "add account" item
        for (profile in header.profiles.orEmpty()) {
            if (profile.identifier == ADD_ACCOUNT_IDENTIFIER) {
                profiles.add(profile)
                break
            }
        }
        header.clear()
        header.profiles = profiles
        header.setActiveProfile(account.toLong())
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
    Launches the given activity and put it as the current one
     Setting argument firstTime to true means the task history will be reset (as if the app were launched anew into
     this activity)
     */
    private fun launchActivity(activity: AppCompatActivity, firstTime: Boolean = false) {
        val intent = Intent(this, activity::class.java)

        if(firstTime){
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
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