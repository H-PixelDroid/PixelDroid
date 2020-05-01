package com.h.pixeldroid.fragments
// Your IDE likely can auto-import these classes, but there are several
// different implementations so we list them here to disambiguate.

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Size
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.h.pixeldroid.PostCreationActivity
import com.h.pixeldroid.R
import java.io.File
import java.util.concurrent.Executors

// This is an arbitrary number we are using to keep track of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts.
private const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest.
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class CameraActivity : Fragment() {

    private val PICK_IMAGE_REQUEST = 1
    private val CAPTURE_IMAGE_REQUEST = 2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        super.onCreate(savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_camera, container, false)

        viewFinder = view.findViewById(R.id.view_finder)
        setupUploadImage()

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        return view
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView

    private fun startCamera() {
        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(640, 480))
        }.build()


        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                // We don't set a resolution for image capture; instead, we
                // select a capture mode which will infer the appropriate
                // resolution based on aspect ration and requested mode
                setCaptureMode(CaptureMode.MIN_LATENCY)
            }.build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)

        setupImageCapture(imageCapture)
        setupFlipCameras()

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(requireContext(),
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
               requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupImageCapture(imageCapture: ImageCapture) {
        val takePictureButton = requireView().findViewById<Button>(R.id.takePictureButton)
        takePictureButton.setOnClickListener {

            val file = File.createTempFile(
                "${System.currentTimeMillis()}.jpg", null, context?.cacheDir
            )

            imageCapture.takePicture(file, executor, object : OnImageSavedListener {
                override fun onError(
                    imageCaptureError: ImageCaptureError,
                    message: String,
                    exc: Throwable?
                ) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }

                override fun onImageSaved(file: File) {
                    startPostCreation(file.toUri())
                }
            })
        }

    }

    private fun setupFlipCameras() {

    }

    private fun setupUploadImage() {
        val uploadPictureButton: Button = requireView().findViewById(R.id.uploadPictureButton)
        uploadPictureButton.setOnClickListener{
            Intent().apply {
                type = "image/*"
                action = Intent.ACTION_GET_CONTENT
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                startActivityForResult(
                    Intent.createChooser(this, "Select a Picture"), PICK_IMAGE_REQUEST
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null
            && (requestCode == PICK_IMAGE_REQUEST || requestCode == CAPTURE_IMAGE_REQUEST)
            && data.data != null) {

            startPostCreation(data.data!!)
        }
    }

    private fun startPostCreation(uri: Uri) {
        startActivity(
            Intent(activity, PostCreationActivity::class.java)
                .putExtra("picture_uri", uri)
        )

    }
}