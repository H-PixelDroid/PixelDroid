package com.h.pixeldroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class CameraActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        val textureView = findViewById<TextureView>(R.id.textureView2)
        textureView.surfaceTextureListener

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val cameraCallback = object : CameraDevice.StateCallback() {
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onOpened(camera: CameraDevice) {

                val sessionCallback = object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        TODO("Not yet implemented")
                    }

                    override fun onConfigured(session: CameraCaptureSession) {


                        val requestBuilder = camera.createCaptureRequest(TEMPLATE_PREVIEW)

                        requestBuilder.addTarget(Surface(textureView.surfaceTexture))
                        session.setRepeatingRequest(requestBuilder.build(), null, null)
                    }

                }

                val outputConfigs =
                    listOf<OutputConfiguration>(OutputConfiguration(Surface(textureView.surfaceTexture)))
                camera.createCaptureSession(SessionConfiguration(
                    SESSION_REGULAR,
                    outputConfigs,
                    AsyncTask.THREAD_POOL_EXECUTOR,
                    sessionCallback))


            }

            override fun onDisconnected(camera: CameraDevice) {
                TODO("Not yet implemented")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                TODO("Not yet implemented")
            }

        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(applicationContext,"Hello non-existant camera", Toast.LENGTH_SHORT).show()

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
