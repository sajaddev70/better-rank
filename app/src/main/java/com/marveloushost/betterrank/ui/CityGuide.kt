package com.marveloushost.betterrank.ui


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.customView.AirbnbPdfGenerator
import com.marveloushost.betterrank.databinding.CityGuideBinding
import com.marveloushost.betterrank.dialog.CityGuideDialog
import com.marveloushost.betterrank.viewModel.CityGuideViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class CityGuide:BaseActivity() {

    val viewModel: CityGuideViewModel by viewModels()
    val binding: CityGuideBinding by lazy { DataBindingUtil.setContentView(this, R.layout.city_guide) }
    private val homeId:String by lazy { intent.getStringExtra("homeId").toString() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val homeId = intent.getStringExtra("homeId") ?: run {
            showMessage("No home ID provided")
            finish()
            return
        }
        initView()
        onClickView()
        //Api
        viewModel.getHome(homeId)
    }

    override fun initView() {

        //ViewModel
        binding.viewModel=viewModel
        binding.lifecycleOwner=this

        //ActionBar
        binding.baseActionBar.txtTitle.text=getString(R.string.create_city_guide)
        binding.baseActionBar.imgStart.visibility=View.VISIBLE

    }

    override fun onClickView() {
        viewModel.showMessage.observe(this) { showMessage(it) }
        binding.baseActionBar.imgStart.setOnClickListener { finish() }

        binding.btnApply.setOnClickListener {
            val homeId = intent.getStringExtra("homeId") ?: return@setOnClickListener
            val cityGuide = viewModel.resultUpdateCityGuide.value ?: return@setOnClickListener

            viewModel.updateCityGuide(homeId, cityGuide)
            finish()  // Go back to Home
        }
    }

//    private fun sharePdf(context: Context, pdfFile: File) {
//        try {
//            val pdfUri = FileProvider.getUriForFile(
//                context,
//                "${context.packageName}.provider",
//                pdfFile
//            )
//
//            val shareIntent = Intent(Intent.ACTION_SEND).apply {
//                type = "application/*"
//                putExtra(Intent.EXTRA_STREAM, pdfUri)
//                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            }
//
//            context.startActivity(
//                Intent.createChooser(shareIntent, "Share PDF via")
//            )
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }

}