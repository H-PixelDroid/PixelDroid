package com.h.pixeldroid.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.SparseIntArray
import android.view.*
import android.view.OrientationEventListener.ORIENTATION_UNKNOWN
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.h.pixeldroid.R
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

// A fragment that displays a stream coming from either the front or back cameras of the smartphone,
// and can capture an image from this stream.
class CameraFragment : Fragment() {

    private lateinit var        manager: CameraManager
    private lateinit var   cameraDevice: CameraDevice

    private val buttonClick = AlphaAnimation(1f, 0.8f) // Triggered when a photo is taken
    private var currentCameraType = CameraCharacteristics.LENS_FACING_FRONT

    private lateinit var  switchCameraButton: Button
    private lateinit var   takePictureButton: Button
    private lateinit var uploadPictureButton: Button

    /* Entities necessary to the stream : */
    private lateinit var textureView: TextureView
    private lateinit var surface    : SurfaceTexture

    /* Entities necessary to the photo capture */
    private lateinit var pictureRequestBuilder: CaptureRequest.Builder
    private lateinit var imageView: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private val TAG = "Camera Fragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the textureView which displays the camera's stream and set up its listener :
        // it allows us to be sure the textureView is correctly set up before performing any action
        textureView = view.findViewById(R.id.textureView)!!
        textureView.surfaceTextureListener = this.textureListener

        imageView = requireView().findViewById(R.id.imageView)


        /* Buttons */

        uploadPictureButton = view.findViewById(R.id.uploadPictureButton)
        uploadPictureButton.setOnClickListener{
            uploadPicture()
        }

        takePictureButton = view.findViewById(R.id.takePictureButton)
        takePictureButton.setOnClickListener{
            takePicture()
        }

        switchCameraButton = view.findViewById(R.id.switch_return_button)
        switchCameraButton.setOnClickListener{
            if (takePictureButton.visibility == View.VISIBLE) {
                flipCameras()
            } else {
                openCamera(currentCameraType) // return to capture
                takePictureButton.visibility = View.VISIBLE
                switchCameraButton.background = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_camera)
                imageView.visibility = View.INVISIBLE
            }
        }

    }

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            this@CameraFragment.surface = surface
            openCamera(currentCameraType)
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {}

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }


    private fun openCamera(requiredType: Int) {

        manager = requireContext().applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        /* Ask for Camera and Write permissions and require them if not granted */
        if (ActivityCompat.checkSelfPermission(requireContext().applicationContext, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED)
        {

            ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 200)
            //openCamera(requiredType)
            return
        }


        /* Open the camera and enter its state callback */
        try {
            for(cameraID in manager.cameraIdList) {

                val characteristics: CameraCharacteristics =
                    manager.getCameraCharacteristics(cameraID)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (facing != null && facing == requiredType)
                    manager.openCamera(manager.cameraIdList[facing], previewStateCallback, null)

            }

        } catch (e : CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val previewStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera

            try {
                /* Now that the camera is available, start a stream on the textureView */
                cameraDevice.createCaptureSession(
                    listOf(Surface(surface)),
                    previewSessionCallback,
                    null
                )
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }


        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice.close()
        }

    }

    private val previewSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {
        }

        override fun onConfigured(session: CameraCaptureSession) {

            try {
                /* Build the stream with a flow of requests */
                val streamRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                streamRequestBuilder.addTarget(Surface(surface))
                session.setRepeatingRequest(streamRequestBuilder.build(), null, null)

            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    /* END of stream building flow */
    /* START of picture capture flow */

    private fun takePicture() {
        requireView().startAnimation(buttonClick)

        /* Get output dimensions of the current camera */
        val characteristics = manager.getCameraCharacteristics(cameraDevice.id)
        val jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.
        getOutputSizes(ImageFormat.JPEG)

        var width = 640
        var height = 480
        if (jpegSizes.isNotEmpty()) {
            width = jpegSizes[0].width
            height = jpegSizes[0].height
        }


        /* Build a reader which will contain our picture, and link it to the request builder */
        val reader: ImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)

        pictureRequestBuilder =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        pictureRequestBuilder.addTarget(reader.surface)
        //pictureRequestBuilder.addTarget(Surface(textureView.surfaceTexture))
        pictureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

        reader.setOnImageAvailableListener(readerListener, null)
        val outputSurfaces = ArrayList<Surface>(1)
        outputSurfaces.add(reader.surface)
        //outputSurfaces.add(Surface(textureView.surfaceTexture))

        // Handle rotation ?!
        pictureRequestBuilder.set(
            CaptureRequest.JPEG_ORIENTATION,
            getJpegOrientation(
                // manager.getCameraCharacteristics(cameraDevice.id),
                // activity?.windowManager?.defaultDisplay?.rotation!!
            )
        )

        cameraDevice.createCaptureSession(outputSurfaces, pictureStateCallback , null);
        flipButtons()
    }

    private val pictureStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            try {
                session.capture(pictureRequestBuilder.build(), pictureCaptureListener, null)
            } catch (e :CameraAccessException) {
                e.printStackTrace()
            }
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
        }
    }

    private val pictureCaptureListener = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
        }
    }

    private val readerListener = OnImageAvailableListener { reader ->
        lateinit var image : Image
        try {
            image = reader.acquireLatestImage()
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)

            val filename = "tmp_capture.png"
            val file = File.createTempFile(filename, null, context?.cacheDir)
            file.writeBytes(bytes)

            val uri = file.toUri() as Uri?

            imageView.setImageURI(uri)
            imageView.visibility = View.VISIBLE

            pictureHandle(uri)

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            image.close()
        }
    }

    private fun getJpegOrientation(c: CameraCharacteristics, deviceOrientation: Int): Int {
        var deviceOrientation = deviceOrientation
        if (deviceOrientation == ORIENTATION_UNKNOWN) return 0
        val sensorOrientation = c[CameraCharacteristics.SENSOR_ORIENTATION]!!

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90

        // Reverse device orientation for front-facing cameras
        val facingFront =
            c[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) deviceOrientation = - deviceOrientation

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        val value = (sensorOrientation + deviceOrientation + 360) % 360
        return value
    }

    private fun getJpegOrientation(): Int {
        val rotation = requireActivity().windowManager.defaultDisplay.rotation
        val characteristics = manager.getCameraCharacteristics(cameraDevice.id)
        val sensorOrientation =  characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

        val orientations = SparseIntArray(4)
        orientations.append(Surface.ROTATION_0, 90)
        orientations.append(Surface.ROTATION_90, 0)
        orientations.append(Surface.ROTATION_180, 270)
        orientations.append(Surface.ROTATION_270, 180)

        val surfaceRotation = orientations.get(rotation)

        val jpegOrientation = (surfaceRotation + sensorOrientation!! + 270) % 360

        val string = rotation.toString() + ", "+ sensorOrientation.toString() +", "+ jpegOrientation.toString()
        Toast.makeText(requireContext(), string, Toast.LENGTH_SHORT).show()
        return jpegOrientation
    }

    private fun flipButtons() {
        takePictureButton.visibility = View.INVISIBLE
        switchCameraButton.background = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_revert)
    }

    private fun flipCameras() {
        if (::cameraDevice.isInitialized) cameraDevice.close()

        if (currentCameraType == CameraCharacteristics.LENS_FACING_FRONT) {
            currentCameraType = CameraCharacteristics.LENS_FACING_BACK
            openCamera(currentCameraType)
        } else {
            currentCameraType = CameraCharacteristics.LENS_FACING_FRONT
            openCamera(currentCameraType)
        }
    }

    // Ulysse's part

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
            pictureHandle(data.data)
        }
    }

    private fun pictureHandle(uri: Uri?) {
    }
}
