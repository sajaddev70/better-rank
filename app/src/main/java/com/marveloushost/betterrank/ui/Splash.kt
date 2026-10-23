package com.marveloushost.betterrank.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.SplashBinding
import com.marveloushost.betterrank.hilt.modual.HiltSharedPrefModule
import com.marveloushost.betterrank.viewModel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Splash : BaseActivity() {
    private var permissionRequestedOnce = false
    private var splashStarted = false


    private val viewModel: SplashViewModel by viewModels()
    private val binding: SplashBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.splash)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()

    }
    override fun onStart() {
        super.onStart()
        handlePermissions()
    }

    private fun handlePermissions() {
        val missing = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            continueSplashFlow()
            return
        }

        // First time → request
        if (!permissionRequestedOnce) {
            permissionRequestedOnce = true
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 999)
            return
        }

        // After denial → show explanation UI
        showPermissionExplanation()
    }

    private fun requiredPermissions(): List<String> {
        val list = mutableListOf<String>()

        list.add(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= 34) {
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT == 33) {
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        return list
    }



    private fun checkPermissions() {
        val needed = mutableListOf<String>()

        needed.add(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= 34) {
            needed.add(Manifest.permission.READ_MEDIA_IMAGES)
            needed.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT == 33) {
            needed.add(Manifest.permission.READ_MEDIA_IMAGES)
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val toAsk = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toAsk.isEmpty()) {
            continueSplashFlow()
        } else {
            ActivityCompat.requestPermissions(this, toAsk.toTypedArray(), 999)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 999) {
            val allGranted = grantResults.all {
                it == PackageManager.PERMISSION_GRANTED
            }

            if (allGranted) {
                continueSplashFlow()
            } else {
                showPermissionExplanation()
            }
        }
    }
    private fun showPermissionExplanation() {
        binding.permissionContainer.visibility = View.VISIBLE
    }


    private fun continueSplashFlow() {
        if (splashStarted) return
        splashStarted = true

        binding.permissionContainer.visibility = View.GONE

        initView()
        onClickView()
        viewModel.pingGoogle()
    }


    override fun initView() {

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.pingGoogleResult.observe(this) { result ->
            if (result == 1) {
                if (
                    viewModel.appRepository.getDataSharedPref(HiltSharedPrefModule.UID).isNotBlank() &&
                    viewModel.appRepository.getDataSharedPref(HiltSharedPrefModule.HOME).isBlank()
                ) {
                    viewModel.getHomes(
                        viewModel.appRepository.getDataSharedPref(HiltSharedPrefModule.UID)
                    )
                } else {
                    viewModel.isExistingHome.value = true
                }
            } else {
                showMessage(getString(R.string.check_your_network_connection))
            }
        }

        viewModel.isExistingHome.observe(this) { exists ->
            Handler(Looper.getMainLooper()).postDelayed({

                val intent = when {
                    viewModel.appRepository.getDataSharedPref(HiltSharedPrefModule.INTRO_STATUS)
                        .isBlank() -> Intent(this, Intro::class.java)

                    viewModel.appRepository.getDataSharedPref(HiltSharedPrefModule.UID)
                        .isBlank() -> Intent(this, Login::class.java)

                    exists -> Intent(this, Main::class.java)

                    else -> Intent(this, HomeRegister::class.java)
                }

                startActivity(intent)
                finish()

            }, 2000)
        }

        binding.btnTryAgain.setOnClickListener {
            binding.btnTryAgain.visibility = View.GONE
            viewModel.pingGoogle()
        }

        binding.btnGrantPermission.setOnClickListener {
            val missing = requiredPermissions().filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missing.isEmpty()) {
                continueSplashFlow()
                return@setOnClickListener
            }

            val canAskAgain = missing.any {
                ActivityCompat.shouldShowRequestPermissionRationale(this, it)
            }

            if (canAskAgain) {
                ActivityCompat.requestPermissions(this, missing.toTypedArray(), 999)
            } else {
                openAppSettings()
            }
        }


    }
    private fun openAppSettings() {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }


    override fun onClickView() {
        viewModel.showMessage.observe(this) { msg ->
            showMessage(msg)
        }
    }
}
