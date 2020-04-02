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
import android.util.Size
import android.view.*
import android.view.OrientationEventListener.*
import android.widget.Button
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.h.pixeldroid.PostCreationActivity
import com.h.pixeldroid.R
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class CameraFragment : Fragment() {

    private lateinit var manager: CameraManager
    private lateinit var   cameraDevice: CameraDevice
    private enum class Cameras {
        FRONT, BACK
    }

    /* Entities necessary to the stream : */
    private lateinit var streamRequestBuilder: CaptureRequest.Builder
    private lateinit var          textureView: TextureView

    /* Entities necessary to the photo capture */
    private lateinit var jpegSizes: Array<Size>
    private val PICK_IMAGE_REQUEST = 1
    private val TAG = "Camera Fragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        /* Views */
        val view = inflater.inflate(R.layout.fragment_camera, container, false)
        manager = requireContext().applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Get the textureView which displays the camera's stream and set up its listener :
        // it allows us to be sure the textureView is correctly set up before performing any action
        textureView = view.findViewById(R.id.textureView)!!
        textureView.surfaceTextureListener = this.textureListener


        /* Buttons */

        val uploadPictureButton: Button = view.findViewById(R.id.uploadPictureButton)
        uploadPictureButton.setOnClickListener{
            uploadPicture()
        }

        val takePictureButton: Button = view.findViewById(R.id.takePictureButton)
        takePictureButton.setOnClickListener{
            takePicture()
        }

        return view // and start the stream building flow
    }

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            openCamera(Cameras.FRONT)
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


    private fun openCamera(cameraId: Cameras) {

        /* Ask for Camera and Write permissions and require them if not granted */
        if (ActivityCompat.checkSelfPermission(requireContext().applicationContext, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED)
        {

            ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 200)
            openCamera(cameraId)
        }

        /* Open the camera and enter its state callback */
        try {
            manager.openCamera(manager.cameraIdList[cameraId.ordinal], previewStateCallback, null)
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
                    listOf(Surface(textureView.surfaceTexture)),
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
                streamRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                streamRequestBuilder.addTarget(Surface(textureView.surfaceTexture))
                session.setRepeatingRequest(streamRequestBuilder.build(), null, null)

            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    /* END of stream building flow */
    /* START of picture capture flow */

    private fun takePicture() {
        val characteristics = manager.getCameraCharacteristics(cameraDevice.id)

        jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.
        getOutputSizes(ImageFormat.JPEG)

        var width = 640
        var height = 480
        if (jpegSizes.isNotEmpty()) {
            width = jpegSizes[0].width
            height = jpegSizes[0].height
        }
        val reader: ImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)

        val requestBuilder =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        requestBuilder.addTarget(reader.surface)
        requestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

        requestBuilder.set(
            CaptureRequest.JPEG_ORIENTATION,
            getJpegOrientation(
                manager.getCameraCharacteristics(cameraDevice.id),
                activity?.windowManager?.defaultDisplay?.rotation!!
            )
        )

        reader.setOnImageAvailableListener(readerListener, null)

        val captureListener = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)
            }
        }

        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                try {
                    session.capture(requestBuilder.build(), captureListener, null)
                } catch (e :CameraAccessException) {
                    e.printStackTrace()
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
            }
        }

        val outputSurfaces = ArrayList<Surface>(1)
        outputSurfaces.add(reader.surface)

        cameraDevice.createCaptureSession(outputSurfaces, stateCallback , null);
    }

    private val readerListener = OnImageAvailableListener { reader ->
        lateinit var image : Image
        try {
            image = reader.acquireLatestImage()
            val buffer = image.planes[0].buffer;
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes);

            val filename = "temp_capture.png"
            val file = File.createTempFile(filename, null, context?.cacheDir)
            file.writeBytes(bytes)
            startPostCreation(file.toUri())
        } catch (e: FileNotFoundException) {
            e.printStackTrace();
        } catch (e: IOException) {
            e.printStackTrace();
        } finally {
            image.close()
        }
    }

    private fun startPostCreation(pictureUri: Uri?) = startActivity(
        Intent(activity, PostCreationActivity::class.java)
            .putExtra("picture_uri", pictureUri))

    private fun getJpegOrientation(c: CameraCharacteristics, deviceOrientation: Int): Int {
        var deviceOrientation = deviceOrientation
        if (deviceOrientation == ORIENTATION_UNKNOWN) return 0
        val sensorOrientation = c[CameraCharacteristics.SENSOR_ORIENTATION]!!

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90

        // Reverse device orientation for front-facing cameras
        val facingFront =
            c[CameraCharacteristics.LENS_FACING] === CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) deviceOrientation = -deviceOrientation

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360
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
            startPostCreation(data.data)
        }
    }

}
