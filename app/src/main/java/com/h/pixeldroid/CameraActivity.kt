package com.h.pixeldroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class CameraActivity : AppCompatActivity() {

    private lateinit var    textureView: TextureView
    private lateinit var surfaceTexture: SurfaceTexture

    private lateinit var   cameraDevice: CameraDevice
    private lateinit var requestBuilder: CaptureRequest.Builder
    private lateinit var captureSession: CameraCaptureSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        textureView = findViewById(R.id.textureView2)!!
        textureView.surfaceTextureListener = this.textureListener

    }

    private var textureListener = object : SurfaceTextureListener {
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

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                200
            )
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
            Toast.makeText(applicationContext,"Capture Session failed",Toast.LENGTH_SHORT).show()
        }

        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session

            try {
                requestBuilder = cameraDevice.createCaptureRequest(TEMPLATE_PREVIEW)
                requestBuilder.addTarget(Surface(textureView.surfaceTexture))
                captureSession.setRepeatingRequest(requestBuilder.build(), null, null)

            } catch (e : CameraAccessException) {
                e.printStackTrace()
            }
        }

    }

}
