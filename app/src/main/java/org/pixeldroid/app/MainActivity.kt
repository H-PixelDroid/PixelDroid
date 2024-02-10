package org.pixeldroid.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.materialdrawer.iconics.iconicsIcon
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IProfile
import com.mikepenz.materialdrawer.model.interfaces.descriptionRes
import com.mikepenz.materialdrawer.model.interfaces.descriptionText
import com.mikepenz.materialdrawer.model.interfaces.iconUrl
import com.mikepenz.materialdrawer.model.interfaces.nameRes
import com.mikepenz.materialdrawer.model.interfaces.nameText
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import kotlinx.coroutines.launch
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
import org.pixeldroid.app.utils.api.objects.Notification
import org.pixeldroid.app.utils.db.entities.HomeStatusDatabaseEntity
import org.pixeldroid.app.utils.db.entities.PublicFeedStatusDatabaseEntity
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity
import org.pixeldroid.app.utils.db.updateUserInfoDb
import org.pixeldroid.app.utils.hasInternet
import org.pixeldroid.app.utils.notificationsWorker.NotificationsWorker.Companion.INSTANCE_NOTIFICATION_TAG
import org.pixeldroid.app.utils.notificationsWorker.NotificationsWorker.Companion.SHOW_NOTIFICATION_TAG
import org.pixeldroid.app.utils.notificationsWorker.NotificationsWorker.Companion.USER_NOTIFICATION_TAG
import org.pixeldroid.app.utils.notificationsWorker.enablePullNotifications
import org.pixeldroid.app.utils.notificationsWorker.removeNotificationChannelsFromAccount
import java.time.Instant


class MainActivity : BaseActivity() {

    private lateinit var header: AccountHeaderView
    private var user: UserDatabaseEntity? = null

    private val model: MainActivityViewModel by viewModels()

    companion object {
        const val ADD_ACCOUNT_IDENTIFIER: Long = -13
    }

    private lateinit var binding: ActivityMainBinding

    @OptIn(ExperimentalPagingApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setOnExitAnimationListener {
            it.remove()
        }

        // Workaround for dynamic colors not applying due to splash screen?
        DynamicColors.applyToActivityIfAvailable(this)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get the currently active user
        user = db.userDao().getActiveUser()

        if (notificationFromOtherUser()) return

        //Check if we have logged in and gotten an access token
        if (user == null) {
            finish()
            launchActivity(LoginActivity(), firstTime = true)
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

            val showNotification: Boolean = intent.getBooleanExtra(SHOW_NOTIFICATION_TAG, false)

            if(showNotification){
                binding.viewPager.currentItem = 3
            }
            if (ActivityCompat.checkSelfPermission(applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) enablePullNotifications(this)
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) enablePullNotifications(this)
    }

    // Checks if the activity was launched from a notification from another account than the
    // current active one, and if so switches to that account
    private fun notificationFromOtherUser(): Boolean {
        val userOfNotification: String? = intent.extras?.getString(USER_NOTIFICATION_TAG)
        val instanceOfNotification: String? = intent.extras?.getString(INSTANCE_NOTIFICATION_TAG)
        if (userOfNotification != null && instanceOfNotification != null
            && (userOfNotification != user?.user_id
                    || instanceOfNotification != user?.instance_uri)
        ) {

            switchUser(userOfNotification, instanceOfNotification)

            val newIntent = Intent(this, MainActivity::class.java)
            newIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            if (intent.getBooleanExtra(SHOW_NOTIFICATION_TAG, false)) {
                newIntent.putExtra(SHOW_NOTIFICATION_TAG, true)
            }

            finish()
            startActivity(newIntent)
            return true
        }
        return false
    }

    private fun setupDrawer() {
       binding.mainDrawerButton.setOnClickListener{
            binding.drawerLayout.openDrawer(binding.drawer)
        }

        header = AccountHeaderView(this).apply {
            attachToSliderView(binding.drawer)
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
            dividerBelowHeader = false
            closeDrawerOnProfileListClick = true
        }

        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView, uri: Uri, placeholder: Drawable, tag: String?) {
                    Glide.with(this@MainActivity)
                        .load(uri)
                        .placeholder(placeholder)
                        .circleCrop()
                        .into(imageView)
            }

            override fun cancel(imageView: ImageView) {
                Glide.with(this@MainActivity).clear(imageView)
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
            },
        )
        binding.drawer.onDrawerItemClickListener = { v, drawerItem, position ->
            when (position){
                1 -> launchActivity(ProfileActivity())
                2 -> launchActivity(SettingsActivity())
                3 -> logOut()
            }
            false
        }

        // Closes the drawer if it is open, when we press the back button
        onBackPressedDispatcher.addCallback(this) {
            // Handle the back button event
            if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)){
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
            else {
                this.isEnabled = false
                super.onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun logOut(){
        finish()

        removeNotificationChannelsFromAccount(applicationContext, user)

        db.runInTransaction {
            db.userDao().deleteActiveUsers()

            val remainingUsers = db.userDao().getAll()
            if (remainingUsers.isEmpty()){
                // No more users, start first-time login flow
                launchActivity(LoginActivity(), firstTime = true)
            } else {
                val newActive = remainingUsers.first()
                db.userDao().activateUser(newActive.user_id, newActive.instance_uri)
                apiHolder.setToCurrentUser()
                // Relaunch the app
                launchActivity(MainActivity(), firstTime = true)
            }
        }
    }

    private fun getUpdatedAccount() {
        if (hasInternet(applicationContext)) {

            lifecycleScope.launchWhenCreated {
                try {
                    val api = apiHolder.api ?: apiHolder.setToCurrentUser()

                    val account = api.verifyCredentials()
                    updateUserInfoDb(db, account)

                    //No need to update drawer account info here, the ViewModel listens to db updates
                } catch (exception: Exception) {
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

        switchUser(profile.identifier.toString(), profile.tag as String)

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        finish()
        startActivity(intent)

        return false
    }

    private fun switchUser(userId: String, instance_uri: String) {
        db.runInTransaction{
            db.userDao().deActivateActiveUsers()
            db.userDao().activateUser(userId, instance_uri)
            apiHolder.setToCurrentUser()
        }
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
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.users.collect { list ->
                    val users = list.toMutableList()
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
                            descriptionText = user.fullHandle
                            tag = user.instance_uri
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
            }
        }
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

        binding.viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val selected = when(position){
                    0 -> R.id.page_1
                    1 -> R.id.page_2
                    2 -> R.id.page_3
                    3 -> {
                        setNotificationBadge(false)
                        R.id.page_4
                    }
                    4 -> R.id.page_5
                    else -> null
                }
                if (selected != null) {
                    binding.tabs.selectedItemId = selected
                }
                super.onPageSelected(position)
            }
        })

        fun MenuItem.itemPos(): Int? {
            return when(itemId){
                R.id.page_1 -> 0
                R.id.page_2 -> 1
                R.id.page_3 -> 2
                R.id.page_4 -> 3
                R.id.page_5 -> 4
                else -> null
            }
        }

        binding.tabs.setOnItemSelectedListener {item ->
            item.itemPos()?.let {
                binding.viewPager.currentItem = it
                true
            } ?: false
        }

        binding.tabs.setOnItemReselectedListener { item ->
            item.itemPos()?.let { position ->
                val page =
                    //No clue why this works but it does. F to pay respects
                    supportFragmentManager.findFragmentByTag("f$position")
                (page as? CachedFeedFragment<*>)?.onTabReClicked()
            }
        }

        // Fetch one notification to show a badge if there are new notifications
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                user?.let {
                    val lastNotification = db.notificationDao().latestNotification(it.user_id, it.instance_uri)
                    try {
                        val notification: List<Notification>? = apiHolder.api?.notifications(
                            min_id = lastNotification?.id,
                            limit = "20"
                        )
                            val filtered = notification?.filter { notification ->
                            lastNotification == null || (notification.created_at
                                ?: Instant.MIN) > (lastNotification.created_at ?: Instant.MIN)
                        }
                        val numberOfNewNotifications = if((filtered?.size ?: 20) >= 20) null else filtered?.size
                        if(filtered?.isNotEmpty() == true ) setNotificationBadge(true, numberOfNewNotifications)
                    } catch (exception: Exception) {
                        return@repeatOnLifecycle
                    }
                }
            }
        }
    }

    private fun setNotificationBadge(show: Boolean, count: Int? = null){
        if(show){
            val badge = binding.tabs.getOrCreateBadge(R.id.page_4)
            if (count != null) badge.number = count
        }
        else binding.tabs.removeBadge(R.id.page_4)
    }

    fun BottomNavigationView.uncheckAllItems() {
        menu.setGroupCheckable(0, true, false)
        for (i in 0 until menu.size()) {
            menu.getItem(i).isChecked = false
        }
        menu.setGroupCheckable(0, true, true)
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
}