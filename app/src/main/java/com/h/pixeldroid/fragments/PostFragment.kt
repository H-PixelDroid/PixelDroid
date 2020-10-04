package com.h.pixeldroid.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.h.pixeldroid.Pixeldroid
import com.h.pixeldroid.R
import com.h.pixeldroid.api.PixelfedAPI
import com.h.pixeldroid.db.AppDatabase
import com.h.pixeldroid.di.PixelfedAPIHolder
import com.h.pixeldroid.fragments.feeds.postFeeds.PostViewHolder
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.objects.Status.Companion.DOMAIN_TAG
import com.h.pixeldroid.objects.Status.Companion.POST_TAG
import javax.inject.Inject


class PostFragment : Fragment() {

    @Inject
    lateinit var db: AppDatabase

    @Inject
    lateinit var apiHolder: PixelfedAPIHolder

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val currentStatus = arguments?.getSerializable(POST_TAG) as Status?
        val statusDomain = arguments?.getString(DOMAIN_TAG)!!
        val root: View = inflater.inflate(R.layout.post_fragment, container, false)
        val picRequest = Glide.with(this)
            .asDrawable().fitCenter()
            .placeholder(ColorDrawable(Color.GRAY))

        (requireActivity().application as Pixeldroid).getAppComponent().inject(this)

        val user = db.userDao().getActiveUser()

        val accessToken = user?.accessToken.orEmpty()
        val api = apiHolder.api ?: apiHolder.setDomainToCurrentUser(db)

        currentStatus?.setupPost(root, picRequest, this, statusDomain, true)

        val holder = PostViewHolder(
            root,
            root.context
        )

        currentStatus?.activateButtons(holder, api, "Bearer $accessToken")

        return root

    }

}
