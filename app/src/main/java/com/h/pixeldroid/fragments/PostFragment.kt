package com.h.pixeldroid.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.h.pixeldroid.R
import com.h.pixeldroid.objects.Status
import com.h.pixeldroid.objects.Status.Companion.POST_TAG
import com.h.pixeldroid.utils.ImageConverter
import kotlinx.android.synthetic.main.post_fragment.view.*



class PostFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val status = arguments?.getSerializable(POST_TAG) as Status?
        val root = inflater.inflate(R.layout.post_fragment, container, false)
        status?.setupPost(root)
        //Setup post and profile images
        ImageConverter.setImageViewFromURL(
            this,
            status?.getPostUrl(),
            root.postPicture
        )
        ImageConverter.setImageViewFromURL(
            this,
            status?.getProfilePicUrl(),
            root.profilePic
        )
        return root
    }
}
