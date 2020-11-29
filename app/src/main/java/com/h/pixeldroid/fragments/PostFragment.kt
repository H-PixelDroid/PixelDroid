package com.h.pixeldroid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.objects.Status.Companion.DOMAIN_TAG
import com.h.pixeldroid.objects.Status.Companion.POST_TAG


class PostFragment : BaseFragment() {

    private lateinit var statusDomain: String
    private var currentStatus: Status? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentStatus = arguments?.getSerializable(POST_TAG) as Status?
        statusDomain = arguments?.getString(DOMAIN_TAG)!!

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root: View = inflater.inflate(R.layout.post_fragment, container, false)

        val user = db.userDao().getActiveUser()!!

        val api = apiHolder.api ?: apiHolder.setDomain(user)

        val holder = StatusViewHolder(root)

        holder.bind(currentStatus, api, db, lifecycleScope)

        return root

    }

}
