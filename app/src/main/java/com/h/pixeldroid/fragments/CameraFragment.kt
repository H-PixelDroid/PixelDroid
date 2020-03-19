package com.h.pixeldroid.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Button
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.h.pixeldroid.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer


class CameraFragment : Fragment() {

    private lateinit var previewsize: Size
    private lateinit var jpegSizes: Array<Size>


    private lateinit var    textureView: TextureView
    private lateinit var surfaceTexture: SurfaceTexture

    private lateinit var        manager: CameraManager
    private lateinit var   cameraDevice: CameraDevice
    private lateinit var requestBuilder: CaptureRequest.Builder
    private lateinit var captureSession: CameraCaptureSession

    private var uploadedPictureView: ImageView? = null


    val PICK_IMAGE_REQUEST = 1
    val TAG = "Camera Fragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)
        uploadedPictureView = view.findViewById(R.id.uploadedPictureView)


        val uploadPictureButton: Button = view.findViewById(R.id.uploadPictureButton)
        uploadPictureButton.setOnClickListener{
            uploadPicture()
        }

        val takePictureButton: Button = view.findViewById(R.id.takePictureButton)
        uploadPictureButton.setOnClickListener{
            takePicture()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textureView = view.findViewById(R.id.textureView)!!
        textureView.surfaceTextureListener = this.textureListener
    }

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            surfaceTexture = surface
            openCamera()
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

    private fun openCamera() {

        val context = requireContext().applicationContext
        manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED)
        {

            ActivityCompat.requestPermissions(requireActivity(), arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 200)
            return
        }

        try {
            manager.openCamera(manager.cameraIdList[0], previewStateCallback, null)
        } catch (e : CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val previewStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera

            try {
                cameraDevice.createCaptureSession(
                    listOf(Surface(surfaceTexture)),
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
            captureSession = session

            try {
                requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                requestBuilder.addTarget(Surface(textureView.surfaceTexture))
                captureSession.setRepeatingRequest(requestBuilder.build(), null, null)

            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

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
        val outputSurfaces: MutableList<Surface> = ArrayList(2)
        outputSurfaces.add(reader.surface)
        outputSurfaces.add(Surface(textureView.surfaceTexture))

        val requestBuilder =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        requestBuilder.addTarget(reader.surface)
        requestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        
    }

    private val captureStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {

            try {
                session.capture(requestBuilder.build(), captureCallback, null)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

        }

        override fun onConfigured(session: CameraCaptureSession) {
            TODO("Not yet implemented")
        }

    }

    private val captureCallback = object : CaptureCallback() {
        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
            super.onCaptureStarted(session, request, timestamp, frameNumber)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            //createCameraPreview()
        }


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
