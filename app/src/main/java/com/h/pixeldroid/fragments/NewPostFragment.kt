package com.h.pixeldroid.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.h.pixeldroid.PostCreationActivity
import com.h.pixeldroid.R

/**
 * This fragment is the entry point to create a post.
 * You can either upload an existing picture or take a new one.
 * once the URI of the picture to be posted is set, it will send
 * it to the post creation activity where you can modify it,
 * add a description and more.
 */

class NewPostFragment : Fragment() {

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_post, container, false)

        val uploadPictureButton: Button = view.findViewById(R.id.uploadPictureButton)
        uploadPictureButton.setOnClickListener{
            uploadPicture()
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null
            && requestCode == PICK_IMAGE_REQUEST  && data.data != null)
            startActivity(Intent(activity, PostCreationActivity::class.java)
                .putExtra("picture_uri", data.data)
            )
    }

    private fun uploadPicture() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 200)
            return
        }

        Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(this, "Select a Picture"), PICK_IMAGE_REQUEST
            )
        }
    }
}
