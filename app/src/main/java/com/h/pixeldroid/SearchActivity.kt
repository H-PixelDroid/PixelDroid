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
import com.h.pixeldroid.fragments.feeds.PostsFeedFragment
import com.h.pixeldroid.fragments.feeds.SearchAccountListFragment
import com.h.pixeldroid.fragments.feeds.SearchFeedFragment
import com.h.pixeldroid.objects.Status

class SearchActivity : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val query = intent.getSerializableExtra("searchFeed") as String

        val searchFeedFragment = SearchFeedFragment()
        val searchAccountListFragment = SearchAccountListFragment()
        val arguments = Bundle()
        arguments.putSerializable("searchFeed", query)
        searchFeedFragment.arguments = arguments
        searchAccountListFragment.arguments = arguments

        val tabs = arrayOf(
            searchFeedFragment,
            searchAccountListFragment,
            Fragment()
        )

        setupTabs(tabs)

    }

    private fun setupTabs(tabs: Array<Fragment>){
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

