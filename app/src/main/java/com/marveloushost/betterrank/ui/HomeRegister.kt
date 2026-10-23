package com.marveloushost.betterrank.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.customView.TakePhoto
import com.marveloushost.betterrank.databinding.HomeRegisterBinding
import com.marveloushost.betterrank.dialog.CheckHomeInfoDialog
import com.marveloushost.betterrank.dialog.ProblemImageDialog
import com.marveloushost.betterrank.dialog.SelectImageDialog
import com.marveloushost.betterrank.fileManager.FilePickerHelper
import com.marveloushost.betterrank.fileManager.ImageHelper
import com.marveloushost.betterrank.hilt.modual.HiltSharedPrefModule
import com.marveloushost.betterrank.mlKit.MlKitHelper
import com.marveloushost.betterrank.model.AirBnbResponseModel
import com.marveloushost.betterrank.model.HomeModel
import com.marveloushost.betterrank.viewModel.HomeRegisterViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class HomeRegister:BaseActivity(), CheckHomeInfoDialog.OnCheckHomeInfoDialogListener,
    SelectImageDialog.OnSelectImageDialogListener {

    val viewModel: HomeRegisterViewModel by viewModels()
    val binding: HomeRegisterBinding by lazy { DataBindingUtil.setContentView(this, R.layout.home_register) }
    internal lateinit var filePickerHelper: FilePickerHelper
    private var imagePath:String? = null
    private var countDownTimer: CountDownTimer? = null
    private val action by lazy { intent.getStringExtra(ACTION)?:"" }

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
            getString(R.string.registering_home_information),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        //FilePicker
        filePickerHelper = FilePickerHelper(
            context = this,
            activityResultRegistry = this.activityResultRegistry,
            onFilePicked = { uri, name, stream ,realPath->
                imagePath= ImageHelper(this).compressImageFromPath(inputPath = realPath)
                imagePath?.let { extractUrlFromImage(it) }
            },
            onError = {
                showMessage(it)
            }
        )
        filePickerHelper.register()

    }

    override fun onClickView() {
        viewModel.showMessage.observe(this) { showMessage(it) }

        viewModel.getAirbnbInfo.observe(this) {
            binding.baseLoading.txtText.text = ""
            countDownTimer?.cancel()
            val dialog = CheckHomeInfoDialog(it)
            dialog.show(supportFragmentManager, CheckHomeInfoDialog::class.java.name)
            dialog.onCheckHomeInfoDialogListener = this
        }

        viewModel.createHome.observe(this) {
            if (it) {
                showMessage("Home added successfully")
                startActivity(Intent(this, Main::class.java))
                finishAffinity()
            }
        }

        // Camera Button
        binding.btnCamera.setOnClickListener {
            val intent = Intent(this@HomeRegister, TakePhoto::class.java).apply {
                putExtra("cameraSelect", "back")
            }
            launcher.launch(intent)
        }

        // Gallery Button
        binding.btnGallery.setOnClickListener {
            filePickerHelper.pickFile("image/*")
        }

        binding.btnCheck.setOnClickListener {
            val link = viewModel.withoutQuery(binding.etLink.text.toString())
            binding.etLink.setText(link)
            viewModel.validationAndGetAirbnbInfo(link)  // name removed
            binding.baseLoading.txtTitle.visibility = View.VISIBLE
            startCountDown(binding.baseLoading.txtText)
            hideKeyboard()
        }
    }

    override fun onAddHome(airBnbResponseModel: AirBnbResponseModel) {
        binding.baseLoading.txtTitle.visibility = View.GONE

        // Only create the home — nothing more.
        viewModel.validationAndCreateHome(airBnbResponseModel)
    }

    private fun startCountDown(
        textView: AppCompatTextView,
        totalTimeInMillis: Long = 60_000,
        intervalInMillis: Long = 1000
    ) {
        countDownTimer?.cancel()
        countDownTimer=object : CountDownTimer(totalTimeInMillis, intervalInMillis) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                textView.text = seconds.toString()
            }

            override fun onFinish() {
                textView.text = ""
            }
        }.start()
    }

    companion object {
        const val ACTION = "action"
        const val HOME = "home"
    }

    override fun onCamera() {
        val intent=Intent(this@HomeRegister, TakePhoto::class.java)
        intent.putExtra("cameraSelect","back")
        launcher.launch(intent)
    }

    override fun onGallery() {
        filePickerHelper.pickFile("image/*")
    }

    private var launcher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photoPath = result.data?.getStringExtra("photoPath")
            photoPath?.let { extractUrlFromImage(it) }
        }
    }

    private fun extractUrlFromImage(imagePath:String){
        val problemImageDialog = ProblemImageDialog()
        MlKitHelper.scanImageForUrls(this, File(imagePath).toUri(),
            onResult = {
                if (it.isEmpty())
                    problemImageDialog.show(supportFragmentManager,ProblemImageDialog::class.java.name)
                else{
                    binding.etLink.setText(it[0])
                }
            },
            onError = {
                problemImageDialog.show(supportFragmentManager,ProblemImageDialog::class.java.name)
            }
        )

    }


}