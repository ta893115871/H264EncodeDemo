package com.bj.gxz.h264encodedemo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE = 0X1
    }

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var encodeThread: H264EncodeThread? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()

        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)
            encodeThread = H264EncodeThread(mediaProjection)
            encodeThread?.startEncode()
        }
    }

    fun start(view: View) {
        val intent: Intent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, REQUEST_CODE)
    }

    fun stop(view: View) {
        encodeThread?.stopEncode()
    }

    override fun onDestroy() {
        super.onDestroy()
        encodeThread?.stopEncode()
    }

    fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 1
            )
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}