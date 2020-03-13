package com.h.pixeldroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class CameraActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var  cameraDevice: CameraDevice
    private lateinit var requestBuilder: CaptureRequest.Builder
    private lateinit var captureSession: CameraCaptureSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        textureView = findViewById<TextureView>(R.id.textureView2)!!
        textureView.surfaceTextureListener = textureListener;

    }

    private var textureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            //open your camera here
            Toast.makeText(applicationContext,"Capture Session failed",Toast.LENGTH_SHORT).show();
            openCamera(surface)
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            // Transform you image captured size according to the surface width and height
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }


    fun openCamera(surfaceTexture: SurfaceTexture) {

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val cameraCallback = object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera

                val sessionCallback = object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(applicationContext,"Capture Session failed",Toast.LENGTH_SHORT).show();
                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session

                        requestBuilder = camera.createCaptureRequest(TEMPLATE_PREVIEW)
                        requestBuilder.addTarget(Surface(textureView.surfaceTexture))

                        captureSession.setRepeatingRequest(requestBuilder.build(), null, null)
                    }

                }

                cameraDevice.createCaptureSession(
                    listOf(Surface(surfaceTexture)),
                    sessionCallback,
                    null
                )


            }

            override fun onDisconnected(camera: CameraDevice) {
                Toast.makeText(applicationContext,"CameraDevice disconnected",Toast.LENGTH_SHORT).show();
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Toast.makeText(applicationContext,"CameraDevice error",Toast.LENGTH_SHORT).show();
            }

        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(applicationContext, "Hello non-existant camera", Toast.LENGTH_SHORT)
                .show()

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                200
            )

            return
        }
        manager.openCamera(manager.cameraIdList[0], cameraCallback, null)
    }
}
