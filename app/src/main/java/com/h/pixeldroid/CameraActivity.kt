package com.h.pixeldroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getMainExecutor

class CameraActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        val textureView = TextureView(this)
        var surfaceTextureListener = textureView.surfaceTextureListener
        setContentView(textureView)
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager



        val idList = manager.cameraIdList
        val characteristic = manager.getCameraCharacteristics(idList[0])

        val callback = object : CameraDevice.StateCallback() {
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onOpened(camera: CameraDevice) {
                val config = null
                val sessionCallback = object : CameraCaptureSession.StateCallback() {
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        TODO("Not yet implemented")
                    }

                    override fun onConfigured(session: CameraCaptureSession) {
                        TODO("Not yet implemented")
                    }

                }

                camera.createCaptureSession(SessionConfiguration(
                    SESSION_REGULAR,
                    null,
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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        manager.openCamera(idList[0], callback, null)

    }
}
