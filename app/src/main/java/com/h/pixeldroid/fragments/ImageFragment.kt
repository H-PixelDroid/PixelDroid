package com.h.pixeldroid.fragments

import android.Manifest
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import com.bumptech.glide.Glide
import com.h.pixeldroid.R
import com.h.pixeldroid.utils.ImageUtils
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener

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
        val view = inflater.inflate(R.layout.fragment_image, container, false)

        view.findViewById<ImageView>(R.id.imageImageView).setOnLongClickListener {
            PopupMenu(view.context, it).apply {
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.image_popup_menu_save_to_gallery -> {
                            Dexter.withContext(view.context)
                                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .withListener(object: BasePermissionListener() {
                                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                        Toast.makeText(view.context, "You need to grant write permission to download pictures!", Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                        ImageUtils.downloadImage(requireActivity(), imgUrl)
                                    }
                                }).check()
                            true
                        }
                        R.id.image_popup_menu_share_picture ->  {
                            Dexter.withContext(view.context)
                                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .withListener(object: BasePermissionListener() {
                                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                                        Toast.makeText(view.context, "You need to grant write permission to share pictures!", Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                                        ImageUtils.downloadImage(requireActivity(), imgUrl, share = true)
                                    }
                                }).check()
                            true
                        }
                        else -> false
                    }
                }
                inflate(R.menu.image_popup_menu)
                show()
            }
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
