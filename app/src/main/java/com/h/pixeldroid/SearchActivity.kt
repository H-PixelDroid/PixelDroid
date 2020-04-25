package com.h.pixeldroid

import android.app.SearchManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.h.pixeldroid.fragments.feeds.search.SearchAccountFragment
import com.h.pixeldroid.fragments.feeds.search.SearchHashtagFragment
import com.h.pixeldroid.fragments.feeds.search.SearchPostsFragment
import com.h.pixeldroid.objects.Results

class SearchActivity : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        var rawQuery = intent.getSerializableExtra("searchFeed") as String
        rawQuery = rawQuery.trim()

        val searchType = if (rawQuery.startsWith("#")){
            Results.SearchType.hashtags
        } else if(rawQuery.startsWith("@")){
            Results.SearchType.accounts
        } else Results.SearchType.statuses

        if(searchType != Results.SearchType.statuses) rawQuery = rawQuery.drop(1)

        val query = rawQuery
        val searchFeedFragment =
            SearchPostsFragment()
        val searchAccountListFragment =
            SearchAccountFragment()
        val searchHashtagFragment: Fragment = SearchHashtagFragment()
        val arguments = Bundle()
        arguments.putSerializable("searchFeed", query)
        searchFeedFragment.arguments = arguments
        searchAccountListFragment.arguments = arguments
        searchHashtagFragment.arguments = arguments

        val tabs = arrayOf(
            searchFeedFragment,
            searchAccountListFragment,
            searchHashtagFragment
        )

        setupTabs(tabs, searchType)
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
                0 -> tab.text = "POSTS"
                1 -> tab.text = "ACCOUNTS"
                2 -> tab.text = "HASHTAGS"
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

