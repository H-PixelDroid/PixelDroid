package com.h.pixeldroid.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import com.h.pixeldroid.PhotoEditActivity
import com.h.pixeldroid.R

const val PICK_IMAGE_REQUEST = 1
const val TAG = "Camera Fragment"

class CameraFragment : Fragment() {

    private var uploadedPictureView: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)
        val uploadPictureButton: Button = view.findViewById(R.id.upload_picture_button)
        val editPictureButton: Button = view.findViewById(R.id.edit_picture_button)
        uploadedPictureView = view.findViewById(R.id.uploaded_picture_view)
        uploadPictureButton.setOnClickListener{
            uploadPicture()
        }

        editPictureButton.setOnClickListener{
            val intent = Intent (activity, PhotoEditActivity::class.java)
            activity!!.startActivity(intent)
        }

        // Inflate the layout for this fragment
        return view
    }

    private fun uploadPicture() {
        Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(this, "Select a Picture"), PICK_IMAGE_REQUEST
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if(data == null || data.data == null){
                Log.w(TAG, "No picture uploaded")
                return
            }
            uploadedPictureView?.setImageURI(data.data)
        }
    }
}
