package org.pixeldroid.app.searchDiscover

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.ActivitySearchBinding
import org.pixeldroid.app.posts.feeds.uncachedFeeds.UncachedPostsFragment
import org.pixeldroid.app.posts.feeds.uncachedFeeds.search.SearchAccountFragment
import org.pixeldroid.app.posts.feeds.uncachedFeeds.search.SearchHashtagFragment
import org.pixeldroid.app.utils.BaseActivity
import org.pixeldroid.app.utils.api.objects.Results

class SearchActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivitySearchBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.topBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var query = ""
        if (Intent.ACTION_SEARCH == intent.action) {
            query = intent.getStringExtra(SearchManager.QUERY).orEmpty()
        }

        query = query.trim()
        supportActionBar?.title = query

        val searchType = when {
            query.startsWith("#") -> {
                Results.SearchType.hashtags
            }
            query.startsWith("@") -> {
                Results.SearchType.accounts
            }
            else -> Results.SearchType.statuses
        }

        if(searchType != Results.SearchType.statuses) query = query.drop(1)

        val tabs = createSearchTabs(query)

        setupTabs(tabs, searchType)
    }

    private fun createSearchTabs(query: String): Array<Fragment>{

        val searchFeedFragment = UncachedPostsFragment()
        val searchAccountListFragment =
            SearchAccountFragment()
        val searchHashtagFragment: Fragment = SearchHashtagFragment()
        val arguments = Bundle()
        arguments.putSerializable("searchFeed", query)
        searchFeedFragment.arguments = arguments
        searchAccountListFragment.arguments = arguments
        searchHashtagFragment.arguments = arguments
        return arrayOf(
            searchFeedFragment,
            searchAccountListFragment,
            searchHashtagFragment
        )
    }

    private fun setupTabs(
        tabs: Array<Fragment>,
        searchType: Results.SearchType
    ){
        val viewPager = findViewById<ViewPager2>(R.id.search_view_pager)
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int): Fragment {
                return tabs[position]
            }

            override fun getItemCount(): Int {
                return 3
            }
        }
        val tabLayout = findViewById<TabLayout>(R.id.search_tabs)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when(position){
                0 -> tab.text = getString(R.string.posts)
                1 -> tab.text = getString(R.string.accounts)
                2 -> tab.text = getString(R.string.hashtags)
            }
        }.attach()
        when(searchType){
            Results.SearchType.statuses ->  tabLayout.selectTab(tabLayout.getTabAt(0))
            Results.SearchType.accounts ->  tabLayout.selectTab(tabLayout.getTabAt(1))
            Results.SearchType.hashtags ->  tabLayout.selectTab(tabLayout.getTabAt(2))

        }
    }
}

