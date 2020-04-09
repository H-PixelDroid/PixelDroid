package com.h.pixeldroid.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.h.pixeldroid.PostCreationActivity
import com.h.pixeldroid.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * This fragment is the entry point to create a post.
 * You can either upload an existing picture or take a new one.
 * once the URI of the picture to be posted is set, it will send
 * it to the post creation activity where you can modify it,
 * add a description and more.
 */

const val PICK_IMAGE_REQUEST = 1
//const val IMAGE_CAPTURE_REQUEST = 2
const val TAG = "New Post Fragment"

class NewPostFragment : Fragment() {

    private var pictureUri: Uri? = null
//    lateinit var currentPhotoPath: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_post, container, false)

        val uploadPictureButton: Button = view.findViewById(R.id.uploadPictureButton)
        uploadPictureButton.setOnClickListener{
            uploadPicture()
        }

//        if (requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
//            val takePictureButton: Button = view.findViewById(R.id.takePictureButton)
//            takePictureButton.setOnClickListener{
//                openCamera()
//            }
//        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK
            && data != null)
            if (requestCode == PICK_IMAGE_REQUEST)
                if (data.data != null)
                    pictureUri = data.data
                else Log.w(TAG, "Upload of picture failed.")
//            else if (requestCode == IMAGE_CAPTURE_REQUEST) {
//                try {
//                    Log.e(TAG, "file: " + data.data.toString())
//                    pictureUri = currentPhotoPath.toUri()
//                }
//                catch (err: IOException) {
//                    Log.e(TAG, "Saving new picture failed: ${err.message}")
//                }
//            }
//        else Log.e(TAG, "something went wrong")
        if (pictureUri != null) {
            startActivity(Intent(activity, PostCreationActivity::class.java)
                    .putExtra("picture_uri", pictureUri)
            )
        }
    }

//    @Throws(IOException::class)
//    private fun createImageFile(): File {
//        // Create an image file name
//        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        return File.createTempFile(
//            "JPEG_${timeStamp}_", /* prefix */
//            ".jpg", /* suffix */
//            storageDir /* directory */
//        ).apply {
//            // Save a file: path for use with ACTION_VIEW intents
//            currentPhotoPath = absolutePath
//        }
//
//    }


//    @Throws(IOException::class)
//    private fun createImageFile(): File {
//        // Create an image file name
//        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//        val storageDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
//        return File.createTempFile(
//            "JPEG_${timeStamp}_", /* prefix */
//            ".jpg", /* suffix */
//            storageDir /* directory */
//        ).apply {
//            // Save a file: path for use with ACTION_VIEW intents
//            currentPhotoPath = absolutePath
//        }
//    }



    private fun uploadPicture() {
        val context = requireContext().applicationContext
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
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

//    private fun openCamera() {
////        val image = createImageFile()
////        Intent().apply {
////            action = MediaStore.ACTION_IMAGE_CAPTURE
////            putExtra(MediaStore.EXTRA_OUTPUT, image.toUri())
////            startActivityForResult(this, IMAGE_CAPTURE_REQUEST)
////        }
//        dispatchTakePictureIntent()
//    }

//    private fun dispatchTakePictureIntent() {
//        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
//            // Ensure that there's a camera activity to handle the intent
//            takePictureIntent.resolveActivity(requireContext().packageManager)?.also {
//                // Create the File where the photo should go
//                val photoFile: File? = try {
//                    createImageFile()
//                } catch (ex: IOException) {
//                    // Error occurred while creating the File
//                    null
//                }
//                // Continue only if the File was successfully created
//                photoFile?.also {
//                    val photoURI: Uri = currentPhotoPath.toUri()
//                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//                    startActivityForResult(takePictureIntent, IMAGE_CAPTURE_REQUEST)
//                }
//            }
//        }
//    }


}
