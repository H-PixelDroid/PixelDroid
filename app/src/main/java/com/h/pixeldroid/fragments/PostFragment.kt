package com.h.pixeldroid.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.h.pixeldroid.R
import com.h.pixeldroid.models.Post
import com.h.pixeldroid.models.Post.Companion.POST_TAG


class PostFragment : Fragment() {

    companion object {
        fun newInstance(post : Post) : PostFragment {
            val postFragment = PostFragment()
            val arguments = Bundle()
            arguments.putSerializable(POST_TAG, post)
            postFragment.arguments = arguments
            return postFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val post = arguments?.getSerializable(POST_TAG) as Post?
        val root = inflater.inflate(R.layout.post_fragment, container, false)
        post?.setupPost(this, root)
        return root
    }

}
