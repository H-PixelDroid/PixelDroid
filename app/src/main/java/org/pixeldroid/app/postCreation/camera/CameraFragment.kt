package org.pixeldroid.app.postCreation.camera

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.pixeldroid.app.databinding.FragmentCameraBinding
import org.pixeldroid.app.postCreation.PostCreationActivity
import org.pixeldroid.app.utils.BaseFragment
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

private const val ANIMATION_FAST_MILLIS = 50L
private const val ANIMATION_SLOW_MILLIS = 100L

/**
 * Camera fragment
 */
class CameraFragment : BaseFragment() {

    private lateinit var container: ConstraintLayout

    private val cameraLifecycleOwner = CameraLifecycleOwner()

    private lateinit var binding: FragmentCameraBinding

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    private var inActivity by Delegates.notNull<Boolean>()
    private var addToStory by Delegates.notNull<Boolean>()

    private var filePermissionDialogLaunched: Boolean = false

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    override fun onDestroyView() {
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        inActivity = arguments?.getBoolean(CAMERA_ACTIVITY) ?: false
        addToStory = arguments?.getBoolean(CAMERA_ACTIVITY_STORY) ?: false

        binding = FragmentCameraBinding.inflate(layoutInflater)

        return binding.root
    }

    private fun setGalleryThumbnail(uri: Uri) {
        val thumbnail = binding.photoViewButton

        // Run the operations in the view's thread
        thumbnail.post {

            // Remove thumbnail padding
            thumbnail.setPadding(10)

            // Load thumbnail into circular button using Glide
            if(activity?.isDestroyed == false) Glide.with(thumbnail)
                .load(uri)
                .apply(RequestOptions.circleCropTransform())
                .into(thumbnail)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bindCameraUseCases()
        }
        else {
            // Ask for Camera permission.
            bindCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupUploadImage()
        setupFlipCameras()
        setupImageCapture()

        // Wait for the views to be properly laid out
        binding.viewFinder.post {

            // Keep track of the display in which this view is attached
            displayId = binding.viewFinder.display?.displayId ?: -1
        }
    }

    /** Declare and bind preview and capture use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics()

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)

        val rotation = binding.viewFinder.display?.rotation ?: 0

        // Bind the CameraProvider to the LifeCycleOwner
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({

            // CameraProvider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()


            // Preview
            preview = Preview.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .build()

            // ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()

            // Must unbind the use-cases before rebinding them
            cameraProvider.unbindAll()

            try {
                // A variable number of use-cases can be passed here -
                // camera provides access to CameraControl & CameraInfo
                camera = cameraProvider.bindToLifecycle(
                    cameraLifecycleOwner, cameraSelector, preview, imageCapture
                )

                // Attach the viewfinder's surface provider to preview use case
                preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onPause() {
        super.onPause()
        cameraLifecycleOwner.pause()
    }

    override fun onResume() {
        super.onResume()

        // Update gallery thumbnail
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
                else Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            updateGalleryThumbnail()
        }
        //TODO check if we can get rid of this filePermissionDialogLaunched check (& the variable)
        else if (!filePermissionDialogLaunched) {
            // Ask for external storage permission.
            updateGalleryThumbnailPermissionLauncher.launch(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        cameraLifecycleOwner.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraLifecycleOwner.destroy()
    }

    override fun onStop() {
        super.onStop()
        cameraLifecycleOwner.stop()
    }

    override fun onStart() {
        super.onStart()
        cameraLifecycleOwner.start()
    }


    /**
     *  setTargetAspectRatio requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private val updateGalleryThumbnailPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                updateGalleryThumbnail()
            } else {
                //TODO should we show the user some message like we did until 75ae26fa4755530794267041de1038f3302ec306 ?
                filePermissionDialogLaunched = true
            }
        }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateGalleryThumbnail() {

        // In the background, load latest photo taken (if any) for gallery thumbnail
        lifecycleScope.launch(Dispatchers.IO) {
            // Find the last picture
            val projection = arrayOf(
                MediaStore.Images.ImageColumns._ID,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.ImageColumns.DATE_TAKEN
                else MediaStore.Images.ImageColumns.DATE_MODIFIED,
            )
            val cursor = requireContext().contentResolver
                .query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                    null,
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.ImageColumns.DATE_TAKEN
                    else MediaStore.Images.ImageColumns.DATE_MODIFIED) + " DESC"
                )
            if (cursor != null && cursor.moveToFirst()) {
                val url = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(0)
                )
                setGalleryThumbnail(url)
                cursor.close()
            }
        }
    }


    private val uploadImageResultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data: Intent? = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val images: ArrayList<String> = ArrayList()
            val clipData = data.clipData
            if (clipData != null) {
                val count = clipData.itemCount
                for (i in 0 until count) {
                    val imageUri: String = clipData.getItemAt(i).uri.toString()
                    images.add(imageUri)
                }
                startAlbumCreation(images)
            } else if (data.data != null) {
                images.add(data.data!!.toString())
                startAlbumCreation(images)
            }
        }
    }

    private fun setupUploadImage() {
        val videoEnabled: Boolean = db.instanceDao().getInstance(db.userDao().getActiveUser()!!.instance_uri).videoEnabled
        var mimeTypes: Array<String> = arrayOf("image/*")
        if(videoEnabled) mimeTypes += "video/*"

        // Listener for button used to view the most recent photo
        binding.photoViewButton.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                action = Intent.ACTION_GET_CONTENT
                addCategory(Intent.CATEGORY_OPENABLE)
                // Don't allow multiple for story
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !addToStory)
                uploadImageResultContract.launch(
                    Intent.createChooser(this, null)
                )
            }
        }
    }


    private val bindCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            binding.cameraPermissionErrorCard.isVisible = false
            bindCameraUseCases()
        } else {
            binding.cameraPermissionErrorCard.isVisible = true
        }
    }

    private fun setupFlipCameras() {
        // Listener for button used to switch cameras
        binding.cameraSwitchButton.setOnClickListener {
            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            // Re-bind use cases to update selected camera, being careful about permissions.
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bindCameraUseCases()
            }
            else {
                // Ask for Camera permission.
                bindCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setupImageCapture() {
        // Listener for button used to capture photo
        binding.cameraCaptureButton.setOnClickListener {

            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                takePhoto()
            }
            else {
                // Ask for Camera permission.
                // Use the same permission launcher as bind camera
                // (taking a photo after the permission prompt is going to be useless anyways)
                bindCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        imageCapture?.let { imageCapture ->

            // Create output file to hold the image. CameraX saves a JPEG image to this file,
            // so it makes no sense to use another extension here
            val photoFile = File.createTempFile(
                "cachedPhoto", ".jpg", context?.cacheDir
            )

            // Setup image capture metadata
            val metadata = Metadata().apply {
                // Mirror image when using the front camera
                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata)
                .build()

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(
                outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        val uri: ArrayList<String> = ArrayList()
                        uri.add(savedUri.toString())
                        startAlbumCreation(uri)
                    }
                })

            // Display flash animation to indicate that photo was captured
            container.postDelayed({
                container.foreground = ColorDrawable(Color.WHITE)
                container.postDelayed(
                    { container.foreground = null }, ANIMATION_FAST_MILLIS
                )
            }, ANIMATION_SLOW_MILLIS)

        }
    }

    private fun startAlbumCreation(uris: ArrayList<String>) {

        val intent = Intent(requireActivity(), PostCreationActivity::class.java)
            .apply {
                uris.forEach{
                    //Why are we using ClipData here? Because the FLAG_GRANT_READ_URI_PERMISSION
                    //needs to be applied to the URIs, and this flag only applies to the
                    //Intent's data and any URIs specified in its ClipData.
                    if(clipData == null){
                        clipData = ClipData("", emptyArray(), ClipData.Item(it.toUri()))
                    } else {
                        clipData!!.addItem(ClipData.Item(it.toUri()))
                    }
                }
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        if(inActivity && !addToStory){
            requireActivity().setResult(Activity.RESULT_OK, intent)
            requireActivity().finish()
        } else {
            if(addToStory){
                intent.putExtra(CAMERA_ACTIVITY_STORY, addToStory)
            }
            startActivity(intent)
        }
    }

    companion object {
        const val CAMERA_ACTIVITY = "CameraActivity"
        const val CAMERA_ACTIVITY_STORY = "CameraActivityStory"

        private const val TAG = "CameraFragment"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

    }
}