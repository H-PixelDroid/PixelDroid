package com.h.pixeldroid.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.h.pixeldroid.databinding.PostFragmentBinding
import com.h.pixeldroid.utils.api.objects.Status
import com.h.pixeldroid.utils.api.objects.Status.Companion.DOMAIN_TAG
import com.h.pixeldroid.utils.api.objects.Status.Companion.POST_TAG
import com.h.pixeldroid.utils.BaseFragment
import com.h.pixeldroid.utils.bindingLifecycleAware
import com.h.pixeldroid.utils.displayDimensionsInPx


class PostFragment : BaseFragment() {

    private lateinit var statusDomain: String
    private var currentStatus: Status? = null

    var binding: PostFragmentBinding by bindingLifecycleAware()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentStatus = arguments?.getSerializable(POST_TAG) as Status?
        statusDomain = arguments?.getString(DOMAIN_TAG)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PostFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val api = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)

        val holder = StatusViewHolder(binding)



        holder.bind(currentStatus, api, db, lifecycleScope, requireContext().displayDimensionsInPx())
    }

}
