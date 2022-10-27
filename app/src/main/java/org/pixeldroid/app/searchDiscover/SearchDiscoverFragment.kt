package org.pixeldroid.app.searchDiscover

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import org.pixeldroid.app.databinding.FragmentSearchBinding
import org.pixeldroid.app.utils.api.PixelfedAPI
import org.pixeldroid.app.utils.BaseFragment
import org.pixeldroid.app.utils.bindingLifecycleAware

/**
 * This fragment lets you search and use Pixelfed's Discover feature
 */

class SearchDiscoverFragment : BaseFragment() {
    private lateinit var api: PixelfedAPI

    var binding: FragmentSearchBinding by bindingLifecycleAware()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        // Configure the search widget (see https://developer.android.com/guide/topics/search/search-dialog#ConfiguringWidget)
        val searchManager = requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        binding.search.apply {
            setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
            isSubmitButtonEnabled = true
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        api = apiHolder.api ?: apiHolder.setToCurrentUser()

        binding.discoverCardView.setOnClickListener { onClickDiscover() }
    }

    private fun onClickDiscover() {
        val intent = Intent(requireContext(), DiscoverActivity::class.java)
        ContextCompat.startActivity(binding.root.context, intent, null)
    }
}
