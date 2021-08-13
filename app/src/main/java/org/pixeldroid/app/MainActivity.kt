package org.pixeldroid.app

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.materialdrawer.iconics.iconicsIcon
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.*
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import org.ligi.tracedroid.sending.sendTraceDroidStackTracesIfExist
import org.pixeldroid.app.databinding.ActivityMainBinding
import org.pixeldroid.app.postCreation.camera.CameraFragment
import org.pixeldroid.app.posts.NestedScrollableHost
import org.pixeldroid.app.posts.feeds.cachedFeeds.CachedFeedFragment
import org.pixeldroid.app.posts.feeds.cachedFeeds.notifications.NotificationsFragment
import org.pixeldroid.app.posts.feeds.cachedFeeds.postFeeds.PostFeedFragment
import org.pixeldroid.app.profile.ProfileActivity
import org.pixeldroid.app.searchDiscover.SearchDiscoverFragment
import org.pixeldroid.app.settings.SettingsActivity
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.db.addUser
import org.pixeldroid.app.utils.db.entities.HomeStatusDatabaseEntity
import org.pixeldroid.app.utils.db.entities.PublicFeedStatusDatabaseEntity
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.hasInternet
import retrofit2.HttpException
import java.io.IOException


class MainActivity : BaseActivity() {

    private lateinit var header: AccountHeaderView
    private var user: UserDatabaseEntity? = null

    companion object {
        const val ADD_ACCOUNT_IDENTIFIER: Long = -13
    }

    private lateinit var binding: ActivityMainBinding

    @ExperimentalPagingApi
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get the currently active user
        user = db.userDao().getActiveUser()

        //Check if we have logged in and gotten an access token
        if (user == null) {
            launchActivity(LoginActivity(), firstTime = true)
            finish()
        } else {
            sendTraceDroidStackTracesIfExist("contact@pixeldroid.org", this)

            setupDrawer()
            val tabs: List<() -> Fragment> = listOf(
                {
                    PostFeedFragment<HomeStatusDatabaseEntity>()
                        .apply {
                            arguments = Bundle().apply { putBoolean("home", true) }
                        }
                },
                { SearchDiscoverFragment() },
                { CameraFragment() },
                { NotificationsFragment() },
                {
                    PostFeedFragment<PublicFeedStatusDatabaseEntity>()
                        .apply {
                            arguments = Bundle().apply { putBoolean("home", false) }
                        }
                }
            )
            setupTabs(tabs)
        }
    }

    private fun setupDrawer() {
        binding.mainDrawerButton.setOnClickListener{
            binding.drawerLayout.open()
        }

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
            attachToSliderView(binding.drawer)
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
                    return ContextCompat.getDrawable(ctx, R.drawable.ic_default_user)!!
                }

                return super.placeholder(ctx, tag)
            }
        })

        fillDrawerAccountInfo(user!!.user_id)

        //after setting with the values in the db, we make sure to update the database and apply
        //with the received one. This happens asynchronously.
        getUpdatedAccount()

        binding.drawer.itemAdapter.add(
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
        binding.drawer.onDrawerItemClickListener = { v, drawerItem, position ->
            when (position){
                1 -> launchActivity(ProfileActivity())
                2 -> launchActivity(SettingsActivity())
                3 -> logOut()
            }
            false
        }
    }

    private fun logOut(){
        db.runInTransaction {
            db.userDao().deleteActiveUsers()

            val remainingUsers = db.userDao().getAll()
            if (remainingUsers.isEmpty()){
                //no more users, start first-time login flow
                launchActivity(LoginActivity(), firstTime = true)
            } else {
                val newActive = remainingUsers.first()
                db.userDao().activateUser(newActive.user_id)
                apiHolder.setToCurrentUser()
                //relaunch the app
                launchActivity(MainActivity(), firstTime = true)
            }
        }
    }
    private fun getUpdatedAccount() {
        if (hasInternet(applicationContext)) {

            lifecycleScope.launchWhenCreated {
                try {
                    val domain = user?.instance_uri.orEmpty()
                    val accessToken = user?.accessToken.orEmpty()
                    val refreshToken = user?.refreshToken
                    val clientId = user?.clientId.orEmpty()
                    val clientSecret = user?.clientSecret.orEmpty()
                    val api = apiHolder.api ?: apiHolder.setToCurrentUser()

                    val account = api.verifyCredentials()
                    addUser(db, account, domain, accessToken = accessToken, refreshToken = refreshToken, clientId = clientId, clientSecret = clientSecret)
                    fillDrawerAccountInfo(account.id!!)
                } catch (exception: IOException) {
                    Log.e("ACCOUNT UPDATE:", exception.toString())
                } catch (exception: HttpException) {
                    Log.e("ACCOUNT UPDATE:", exception.toString())
                }
            }
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

        db.userDao().deActivateActiveUsers()
        db.userDao().activateUser(profile.identifier.toString())
        apiHolder.setToCurrentUser()
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
        users.sortWith { l, r ->
            when {
                l.isActive && !r.isActive -> -1
                r.isActive && !l.isActive -> 1
                else -> 0
            }
        }
        val profiles: MutableList<IProfile> = users.map { user ->
            ProfileDrawerItem().apply {
                isSelected = user.isActive
                nameText = user.display_name
                iconUrl = user.avatar_static
                isNameShown = true
                identifier = user.user_id.toLong()
                descriptionText = "@${user.username}@${user.instance_uri.removePrefix("https://")}"
            }
        }.toMutableList()

        // reuse the already existing "add account" item
        header.profiles.orEmpty()
            .filter { it.identifier == ADD_ACCOUNT_IDENTIFIER }
            .take(1)
            .forEach { profiles.add(it) }

        header.clear()
        header.profiles = profiles
        header.setActiveProfile(account.toLong())
    }

    /**
     * Use reflection to make it a bit harder to swipe between tabs
     */
    private fun ViewPager2.reduceDragSensitivity() {
        val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
        recyclerViewField.isAccessible = true
        val recyclerView = recyclerViewField.get(this) as RecyclerView

        val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
        touchSlopField.isAccessible = true
        val touchSlop = touchSlopField.get(recyclerView) as Int
        touchSlopField.set(recyclerView, touchSlop*NestedScrollableHost.touchSlopModifier)
    }

    private fun setupTabs(tab_array: List<() -> Fragment>){
        binding.viewPager.reduceDragSensitivity()
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment {
                return tab_array[position]()
            }

            override fun getItemCount(): Int {
                return tab_array.size
            }
        }
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?){}

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {
                tab?.position?.let { position ->
                    val page =
                        //No clue why this works but it does. F to pay respects
                        supportFragmentManager.findFragmentByTag("f$position")
                    (page as? CachedFeedFragment<*>)?.onTabReClicked()
                }
            }
        })

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.icon = ContextCompat.getDrawable(applicationContext,
                    when(position){
                        0 -> R.drawable.ic_home_white_24dp
                        1 -> R.drawable.ic_search_white_24dp
                        2 -> R.drawable.ic_photo_camera_white_24dp
                        3 -> R.drawable.ic_heart
                        4 -> R.drawable.ic_filter_black_24dp
                        else -> throw IllegalArgumentException()
                    })
        }.attach()
    }

    /**
     * Launches the given activity and put it as the current one
     * @param firstTime to true means the task history will be reset (as if the app were
     * launched anew into this activity)
     */
    private fun launchActivity(activity: AppCompatActivity, firstTime: Boolean = false) {
        val intent = Intent(this, activity::class.java)

        if(firstTime){
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    /**
     * Closes the drawer if it is open, when we press the back button
     */
    override fun onBackPressed() {
        if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)){
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

}