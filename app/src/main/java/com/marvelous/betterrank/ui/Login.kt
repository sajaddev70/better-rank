package com.marvelous.betterrank.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.marvelous.betterrank.R
import com.marvelous.betterrank.databinding.LoginBinding
import com.marvelous.betterrank.viewModel.LoginViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

@AndroidEntryPoint
class Login:BaseActivity() {

    val viewModel: LoginViewModel by viewModels()
    val binding: LoginBinding by lazy { DataBindingUtil.setContentView(this, R.layout.login) }

    // Whatever the user picks in the system dialog, carry on with the login
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            startGoogleSignIn()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        onClickView()
    }

    override fun initView() {

        //ViewModel
        binding.viewModel=viewModel
        binding.lifecycleOwner=this

        //TextView
        binding.txtTitle.text= HtmlCompat.fromHtml(
            getString(R.string.login_register),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

    }

    override fun onClickView() {

        viewModel.showMessage.observe(this) { value ->
            showMessage(value)
        }

        viewModel.isExistingUser.observe(this){
            if (it)
            {
                viewModel.userModel.uid?.let { uid -> viewModel.getHomes(uid) }
            }
            else
            {
                val intent=Intent(this@Login,Register::class.java)
                intent.putExtra(UID,viewModel.userModel.uid)
                intent.putExtra(EMAIL,viewModel.userModel.email)
                intent.putExtra(NAME,viewModel.userModel.name)
                intent.putExtra(PHOTO,viewModel.userModel.photoUrl)
                startActivity(intent)
                finish()
            }
        }

        viewModel.isExistingHome.observe(this){
            if (it)
            {
                startActivity(Intent(this@Login,Main::class.java))
                finish()
            }
            else
            {
                val intent=Intent(this@Login,HomeRegister::class.java)
                intent.putExtra(UID,viewModel.userModel.uid)
                startActivity(intent)
                finish()
            }
        }

        binding.btnGoogle.setOnClickListener {
            askNotificationPermissionThenLogin()
        }
    }

    /** Explain notifications once before login; never block the login if the user declines. */
    private fun askNotificationPermissionThenLogin() {
        val prefs = getPreferences(MODE_PRIVATE)
        val alreadyAsked = prefs.getBoolean(NOTIFICATION_PROMPT_SHOWN, false)
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED

        if (!needsPermission || alreadyAsked) {
            startGoogleSignIn()
            return
        }

        prefs.edit().putBoolean(NOTIFICATION_PROMPT_SHOWN, true).apply()
        AlertDialog.Builder(this)
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_message)
            .setCancelable(false)
            .setPositiveButton(R.string.notification_permission_ok) { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton(R.string.notification_permission_later) { _, _ ->
                startGoogleSignIn()
            }
            .show()
    }

    private fun googleIdOption(): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()
    }

    private fun buildRequest(): GetCredentialRequest {
        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption())
            .build()
    }

    private fun startGoogleSignIn() {

        viewModel.isLoading.value = true
        val credentialManager = CredentialManager.create(this)
        val request = buildRequest()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@Login, request)
                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                    val googleCred = try {
                        GoogleIdTokenCredential.createFrom(credential.data)
                    } catch (e: GoogleIdTokenParsingException) {
                        viewModel.isLoading.value = false
                        showMessage(getString(R.string.token_error)) // توکن خراب
                        return@launch
                    } catch (e: IllegalArgumentException) {
                        viewModel.isLoading.value = false
                        showMessage(getString(R.string.token_invalid)) // داده‌های ورودی نادرست
                        return@launch
                    }

                    try {
                        viewModel.loginWithGoogle(googleCred.idToken)
                    } catch (e: IOException) {
                        showMessage(getString(R.string.network_error)) // مشکل شبکه
                    } catch (e: HttpException) {
                        showMessage(getString(R.string.server_error)) // خطای سمت سرور
                    } catch (e: NullPointerException) {
                        showMessage(getString(R.string.token_missing)) // توکن نال بود
                    }

                } else {
                    viewModel.isLoading.value = false
                    showMessage(getString(R.string.login_error)) // Credential معتبر نبود
                }

            } catch (e: NoCredentialException) {
                viewModel.isLoading.value = false
                showMessage(getString(R.string.no_account)) // کاربری پیدا نشد

            } catch (e: GetCredentialCancellationException) {
                viewModel.isLoading.value = false
                showMessage(getString(R.string.login_cancelled)) // کاربر کنسل کرد

            } catch (e: GetCredentialInterruptedException) {
                viewModel.isLoading.value = false
                showMessage(getString(R.string.login_interrupted)) // فرایند قطع شد

            } catch (e: GetCredentialProviderConfigurationException) {
                viewModel.isLoading.value = false
                showMessage(getString(R.string.provider_config_error)) // مشکل تنظیمات Provider

            } catch (e: GetCredentialUnknownException) {
                viewModel.isLoading.value = false
                showMessage(getString(R.string.unknown_error)) // خطای ناشناخته

            } catch (e: IllegalArgumentException) {
                viewModel.isLoading.value = false
                showMessage(getString(R.string.invalid_request)) // ورودی نادرست به CredentialManager
            }
        }
    }

    companion object{
        const val UID="uid"
        const val EMAIL="email"
        const val NAME="name"
        const val PHOTO="photo"
        private const val NOTIFICATION_PROMPT_SHOWN="notification_prompt_shown"
    }
}