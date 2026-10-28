package com.marvelous.betterrank.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import com.marvelous.betterrank.R
import com.marvelous.betterrank.adapter.HomeGuidePagerAdapter
import com.marvelous.betterrank.customView.AirbnbPdfGenerator
import com.marvelous.betterrank.databinding.HomeGuideBinding
import com.marvelous.betterrank.viewModel.HomeGuideViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class HomeGuide : BaseActivity() {

    val viewModel: HomeGuideViewModel by viewModels()
    val binding: HomeGuideBinding by lazy { DataBindingUtil.setContentView(this, R.layout.home_guide) }
    private val homeId: String by lazy { intent.getStringExtra("homeId").toString() }
    private lateinit var adapter: HomeGuidePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val homeId = intent.getStringExtra("homeId") ?: run {
            showMessage("No home ID provided")
            finish()
            return
        }
        initView()
        onClickView()
        viewModel.getHome(homeId)
    }

    override fun initView() {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.baseActionBar.txtTitle.text = getString(R.string.create_home_guide)
        binding.baseActionBar.imgStart.visibility = View.VISIBLE

        adapter = HomeGuidePagerAdapter(this)
        binding.pager.adapter = adapter

        viewModel.currentPosition.observe(this) {
            binding.pager.currentItem = it
        }

        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.currentPosition.value = position
            }
        })
    }

    override fun onClickView() {
        viewModel.showMessage.observe(this) { showMessage(it) }
        binding.baseActionBar.imgStart.setOnClickListener { finish() }

        // Listen for Done → Go Home + Trigger PDF
        viewModel.onCompleteTriggered.observe(this) {
            val guide = viewModel.resultUpdateHomeGuide.value ?: return@observe
            val homeId = viewModel.resultGetHome.value?.id ?: return@observe

            viewModel.updateHomeGuide(homeId, guide)  // ✅ you need this
            finish()
        }

    }

//    private fun showHomeGuideDialog() {
//        val result = viewModel.resultGetHome.value ?: return
//        val cityGuide = result.cityGuide
//        val homeGuide = result.homeGuide
//        val mainImageUrl = result.airBnbResponseModel?.image ?: ""
//        val shortDescription = result.airBnbResponseModel?.detailsText ?: ""
//
//        if (cityGuide == null || homeGuide == null) {
//            showMessage("City guide or Home guide data is missing")
//            return
//        }
//
//        HomeGuideDialog(
//            onHome = { finish() },
//            onPreview = {
//                AirbnbPdfGenerator.generateHtml(
//                    cityGuide = cityGuide,
//                    homeGuide = homeGuide,
//                    shortDescription = shortDescription,
//                    mainImageUrl = mainImageUrl,
//                    appName = getString(R.string.app_name),
//                    onHtmlContentReady = { html ->
//                        val intent = Intent(this@HomeGuide, Web::class.java).apply {
//                            putExtra("htmlContent", html)
//                        }
//                        startActivity(intent)
//                    }
//                )
//            },
//            onShare = {
//                AirbnbPdfGenerator.generatePdf(
//                    activity = this@HomeGuide,
//                    cityGuide = cityGuide,
//                    homeGuide = homeGuide,
//                    shortDescription = shortDescription,
//                    mainImageUrl = mainImageUrl,
//                    appName = getString(R.string.app_name),
//                    outputFileName = "AirbnbGuide.pdf",
//                    onPdfReady = { pdfFile -> sharePdf(pdfFile) },
//                    onError = { showMessage(it) }
//                )
//            }
//        ).show(supportFragmentManager, HomeGuideDialog::class.java.name)
//    }

    private fun sharePdf(pdfFile: File) {
        try {
            val pdfUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share PDF"))
        } catch (e: Exception) {
            showMessage("Failed to share PDF")
            e.printStackTrace()
        }
    }

}