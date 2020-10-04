package com.h.pixeldroid.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.h.pixeldroid.R
import kotlinx.android.synthetic.main.fragment_image.*

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val IMG_URL = "imgurl"
private const val IMG_DESCRIPTION = "imgdescription"
private const val RQST_BLDR = "rqstbldr"

/**
 * A simple [Fragment] subclass.
 * Use the [ImageFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ImageFragment : Fragment() {
    private lateinit var imgUrl: String
    private lateinit var imgDescription: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imgUrl         = it.getString(IMG_URL)!!
            imgDescription = it.getString(IMG_DESCRIPTION)!!.ifEmpty { getString(R.string.no_description) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_image, container, false)

        view.findViewById<ImageView>(R.id.imageImageView).setOnLongClickListener {
            Snackbar.make(it, imgDescription, Snackbar.LENGTH_SHORT).show()
            true
        }

        // Inflate the layout for this fragment
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Load the image into to view
        Glide.with(this)
            .asDrawable().fitCenter()
            .placeholder(ColorDrawable(Color.GRAY))
            .load(imgUrl)
            .into(view.findViewById(R.id.imageImageView)!!)
        imageImageView.contentDescription = imgDescription
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param imageUrl the url of the image we want to create a fragment for
         * @return A new instance of fragment ImageFragment.
         */
        @JvmStatic
        fun newInstance(imageUrl: String, imageDescription: String) =
            ImageFragment().apply {
                arguments = Bundle().apply {
                    putString(IMG_URL, imageUrl)
                    putString(IMG_DESCRIPTION, imageDescription)
                }
            }
    }
}
