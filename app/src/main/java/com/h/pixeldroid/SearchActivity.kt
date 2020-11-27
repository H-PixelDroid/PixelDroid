package com.h.pixeldroid

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.h.pixeldroid.fragments.feeds.uncachedFeeds.search.SearchAccountFragment
import com.h.pixeldroid.fragments.feeds.uncachedFeeds.search.SearchHashtagFragment
import com.h.pixeldroid.fragments.feeds.uncachedFeeds.search.SearchPostsFragment
import com.h.pixeldroid.objects.Results

class SearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun createSearchTabs(query: String): Array<Fragment>{

        val searchFeedFragment = SearchPostsFragment()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_SEARCH) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                search(query)
            }
        }
    }

    private fun search(query: String){
        Log.e("search", "")
    }
}

