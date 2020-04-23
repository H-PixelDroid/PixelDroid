package com.h.pixeldroid

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.h.pixeldroid.utils.ImageConverter
import kotlinx.android.synthetic.main.post_fragment.view.*
import java.io.Serializable

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val IMG_URL = "imgurl"
private const val RQST_BLDR = "rqstbldr"

/**
 * A simple [Fragment] subclass.
 * Use the [ImageFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ImageFragment : Fragment() {
    private lateinit var imgUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imgUrl = it.getString(IMG_URL)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Load the image into to view
        val imageView : ImageView = view.findViewById(R.id.imageImageView)!!
        val picRequest = Glide.with(this)
            .asDrawable().fitCenter()
            .placeholder(ColorDrawable(Color.GRAY))

        picRequest.load(imgUrl).into(imageView)
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
        fun newInstance(imageUrl: String) =
            ImageFragment().apply {
                arguments = Bundle().apply {
                    putString(IMG_URL, imageUrl)
                }
            }
    }
}
