package com.marvelous.betterrank.customView

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.marvelous.betterrank.R
import com.marvelous.betterrank.databinding.TakePhotoBinding
import com.marvelous.betterrank.fileManager.ImageHelper
import com.marvelous.betterrank.permissionManager.PermissionManager
import com.marvelous.betterrank.ui.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class TakePhoto : BaseActivity() {

    val binding: TakePhotoBinding by lazy { DataBindingUtil.setContentView(this, R.layout.take_photo) }
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraSelector: CameraSelector
    private lateinit var imageHelper: ImageHelper
    private var currentRotation: Int = Surface.ROTATION_0
    private lateinit var orientationEventListener: OrientationEventListener
    private val cameraSelect:String by lazy { intent.getStringExtra("cameraSelect")?:"front" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        onClickView()
    }

    override fun initView() {

        //getPermission
        if (PermissionManager.isPermissionGranted(this, Manifest.permission.CAMERA)) {
            startCamera()
        } else {
            PermissionManager.requestPermission(this, Manifest.permission.CAMERA)
        }

        //CameraX
        cameraExecutor = Executors.newSingleThreadExecutor()

        orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                currentRotation = when {
                    orientation in 45..134 -> Surface.ROTATION_270
                    orientation in 135..224 -> Surface.ROTATION_180
                    orientation in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
            }
        }
        orientationEventListener.enable()

        //ImageHelper
        imageHelper = ImageHelper(this)
    }

    override fun onClickView() {

        binding.imgTakePhoto.setOnClickListener {
            if (PermissionManager.isPermissionGranted(this, Manifest.permission.CAMERA)) {
                takePhoto()
            } else {
                PermissionManager.requestPermission(this, Manifest.permission.CAMERA)
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.handlePermissionResult(
            requestCode,
            grantResults,
            onGranted = {
                startCamera()
            },
            onDenied = {
                showMessage(getString(R.string.camera_permission_is_needed))
            }
        )
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .build()
                .also { it.surfaceProvider = binding.viewFinder.surfaceProvider }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .build()

            cameraSelector = if (cameraSelect=="front")
                CameraSelector.DEFAULT_FRONT_CAMERA
            else
                CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.targetRotation = currentRotation

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val photoFile = createFile(cacheDir, "$name.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        showMessage("Error: ${exception.message}")
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        val overlay = View(this@TakePhoto).apply {
                            setBackgroundColor(Color.WHITE)
                            alpha = 0f
                        }
                        binding.mainLayout.addView(overlay)

                        val imageCompressed=imageHelper.compressImageFromPath(photoFile.absolutePath, isFrontCamera = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
                        val resultIntent = Intent().apply {
                            putExtra("photoPath",imageCompressed)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)

                        overlay.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .withEndAction {
                                overlay.animate()
                                    .alpha(0f)
                                    .setDuration(300)
                                    .withEndAction {
                                        binding.mainLayout.removeView(overlay)
                                        finish()
                                    }
                            }
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        orientationEventListener.disable()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        private fun createFile(baseFolder: File, fileName: String): File {
            return File(baseFolder, fileName)
        }
    }
}
