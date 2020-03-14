package com.h.pixeldroid.fragments

import android.Manifest
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import com.h.pixeldroid.R

class CameraFragment : Fragment() {

    private lateinit var    textureView: TextureView
    private lateinit var surfaceTexture: SurfaceTexture

    private lateinit var        manager: CameraManager
    private lateinit var   cameraDevice: CameraDevice
    private lateinit var requestBuilder: CaptureRequest.Builder
    private lateinit var captureSession: CameraCaptureSession


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_camera, container, false)

        // Soon : buttons and stuff

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textureView = view.findViewById(R.id.textureView)!!
        textureView.surfaceTextureListener = this.textureListener
    }

    private var textureListener = object : TextureView.SurfaceTextureListener {
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
            manager.openCamera(manager.cameraIdList[0], cameraCallback, null)
        } catch (e : CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val cameraCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera

            try {
                cameraDevice.createCaptureSession(
                    listOf(Surface(surfaceTexture)),
                    sessionCallback,
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

    private val sessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {
        }

        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session

            try {
                requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                requestBuilder.addTarget(Surface(textureView.surfaceTexture))
                captureSession.setRepeatingRequest(requestBuilder.build(), null, null)

            } catch (e : CameraAccessException) {
                e.printStackTrace()
            }
        }

    }
}
