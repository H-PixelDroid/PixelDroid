package com.h.pixeldroid.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Status

import com.h.pixeldroid.objects.Status.Companion.POST_TAG
import kotlinx.android.synthetic.main.post_fragment.view.*


class PostFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val status = arguments?.getSerializable(POST_TAG) as Status?
        val root = inflater.inflate(R.layout.post_fragment, container, false)
        val picRequest = Glide.with(this)
            .asDrawable().fitCenter()
            .placeholder(ColorDrawable(Color.GRAY))

        status?.setupPost(root, picRequest, root.postPicture, root.profilePic)

        return root
    }
}
