package com.marvelous.betterrank.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.marvelous.betterrank.R
import com.marvelous.betterrank.adapter.ImageAdapter
import com.marvelous.betterrank.customView.AirbnbPdfGenerator
import com.marvelous.betterrank.databinding.HomeBinding
import com.marvelous.betterrank.dialog.LogoutDialog
import com.marvelous.betterrank.dialog.SelectImageDialog
import com.marvelous.betterrank.ui.CityGuide
import com.marvelous.betterrank.ui.HomeGuide
import com.marvelous.betterrank.ui.HomeRegister
import com.marvelous.betterrank.ui.Login
import com.marvelous.betterrank.ui.PdfLanguageSelection
import com.marvelous.betterrank.ui.PhotographyGuide
import com.marvelous.betterrank.ui.Reviews
import com.marvelous.betterrank.viewModel.HomeViewModel
import com.marvelous.betterrank.viewModel.PhotographyGuideViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import android.view.LayoutInflater
import com.marvelous.betterrank.ui.AIAnalysisActivity
import com.marvelous.betterrank.util.Prefs

@AndroidEntryPoint
class Home:BaseFragment(), LogoutDialog.OnLogoutDialogDialogListener {

    lateinit var binding: HomeBinding
    val viewModel:HomeViewModel by viewModels()
    private lateinit var imageAdapter: ImageAdapter
    private var loadingBox: View? = null
    private var pendingCityGuide = false
    private var pendingHomeGuide = false
    private var aiLoadingBox: View? = null
    private var tvAiCountdown: TextView? = null
    private var dummyTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding= DataBindingUtil.inflate(inflater, R.layout.home,container,false)

        initView()
        onClick()

        //Api
        viewModel.getUser()
        viewModel.getHomes()

        return binding.root
    }

    override fun initView() {

        //ViewModel
        binding.viewModel=viewModel
        binding.lifecycleOwner=this

        //TextView
//        binding.txtWellcomeGuide.text= HtmlCompat.fromHtml(
//            getString(R.string.wellcome_guide),
//            HtmlCompat.FROM_HTML_MODE_COMPACT
//        )
    }

    override fun onClick() {

        viewModel.showMessage.observe(viewLifecycleOwner){
            showMessage(it)
        }

        viewModel.resultGetHome.observe(viewLifecycleOwner) { home ->
            home?.id?.let { id ->
                Prefs.saveHomeId(requireContext(), id)
            }

            home?.airBnbResponseModel?.images?.let { images ->
                imageAdapter = ImageAdapter(images)
                binding.pager.adapter = imageAdapter
                binding.dotsIndicator.attachTo(binding.pager)
            }
        }


        viewModel.resultDeleteUser.observe(viewLifecycleOwner){
            if (it){
                viewModel.appRepository.logout()
                startActivity(Intent(requireContext(),Login::class.java))
                requireActivity().finish()
            }

        }

        binding.btnLogout.setOnClickListener {
            val logoutDialog= LogoutDialog()
            logoutDialog.show(requireActivity().supportFragmentManager, LogoutDialog::class.simpleName)
            logoutDialog.onLogoutDialogDialogListener=this
        }

//        binding.btnCityGuide.setOnClickListener {
//            val intent=Intent(requireContext(),CityGuide::class.java)
//            intent.putExtra("homeId", viewModel.resultGetHome.value?.id)
//            startActivity(intent)
//        }
//
//        binding.btnHomeGuide.setOnClickListener {
//            val intent=Intent(requireContext(),HomeGuide::class.java)
//            intent.putExtra("homeId", viewModel.resultGetHome.value?.id)
//            startActivity(intent)
//        }

        binding.btnReload.setOnClickListener {
            launcher.launch(Intent(requireContext(),HomeRegister::class.java).apply {
                putExtra(HomeRegister.ACTION,HomeRegister.HOME)
            })
        }

        binding.btnPdf.setOnClickListener {
            val homeId = viewModel.resultGetHome.value?.id ?: return@setOnClickListener
            startActivity(Intent(requireContext(), PdfLanguageSelection::class.java).apply {
                putExtra("homeId", homeId)
            })
        }


//        binding.btnPdf.setOnClickListener {
//
//            viewModel.resultGetHome.value?.let { result ->
//                val cityGuide = result.cityGuide
//                val homeGuide = result.homeGuide
//                val mainImageUrl = result.airBnbResponseModel?.image ?: ""
//                val shortDescription = result.airBnbResponseModel?.detailsText ?: ""
//
//                if (cityGuide != null && homeGuide != null) {
//                    AirbnbPdfGenerator.generatePdf(
//                        activity = requireActivity(),
//                        cityGuide = cityGuide,
//                        homeGuide = homeGuide,
//                        shortDescription = shortDescription,
//                        mainImageUrl = mainImageUrl,
//                        appName = getString(R.string.app_name),
//                        outputFileName = "AirbnbGuide.pdf",
//                        onPdfReady = { pdfFile -> sharePdf(requireContext(),pdfFile) },
//                        onError = { errorMessage -> showMessage(errorMessage) }
//                    )
//                } else {
//                    showMessage("City guide or Home guide data is missing")
//                }
//            }
//
//        }

        binding.btnReviews.setOnClickListener {
            val intent=Intent(requireActivity(),Reviews::class.java)
            val reviews=viewModel.resultGetHome.value?.airBnbResponseModel
            val homeId = viewModel.resultGetHome.value?.id.orEmpty()
            intent.putExtra("airBnbResponse", reviews)
            intent.putExtra("homeId", homeId)
            startActivity(intent)
        }

        binding.btnMaintenanceTasks.setOnClickListener {
            val homeId = viewModel.resultGetHome.value?.id ?: return@setOnClickListener
            val intent=Intent(requireActivity(), PhotographyGuide::class.java).apply {
                putExtra("HOME_ID", homeId)}
            startActivity(intent)
        }
        binding.btnCityGuide.setOnClickListener {
            if (viewModel.bothGuidesReady.value == true) {
                startCityGuide()
            } else {
                pendingCityGuide = true
                showLoadingBox()
                observeUntilReady { startCityGuide() }
            }

        }

        binding.btnHomeGuide.setOnClickListener {
            if (viewModel.bothGuidesReady.value == true) {
                startHomeGuide()
            } else {
                pendingHomeGuide = true
                showLoadingBox()
                observeUntilReady { startHomeGuide() }
            }
        }
        binding.btnAiCityGuide.setOnClickListener {
            val homeId = viewModel.resultGetHome.value?.id ?: return@setOnClickListener

//            Log.d("HOME", "Start → showing saved AI analysis")

            showAILoading("Loading your AI analysis...")

            dummyTimer?.cancel()
            dummyTimer = object : CountDownTimer(15000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    tvAiCountdown?.text = "${millisUntilFinished / 1000}"
                }

                override fun onFinish() {
                    hideAILoading()
                    viewModel.loadAIAnalysis(homeId)  // ← LOAD SAVED
                    startActivity(Intent(requireContext(), AIAnalysisActivity::class.java).apply {
                        putExtra("homeId", homeId)
                    })
                }
            }.start()
        }
//        binding.btnAiCityGuide.setOnClickListener {
//            viewModel.startAIAnalysis()
//        }

    }


    private val launcher= registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
        if(result.resultCode== RESULT_OK){
            viewModel.getHomes()
        }
    }

    override fun onLogout() {
        viewModel.appRepository.logout()
        startActivity(Intent(requireContext(),Login::class.java))
        requireActivity().finish()
    }

    override fun onDelete() {

        viewModel.deleteUserData()
    }

    private fun sharePdf(context: Context, pdfFile: File) {
        try {
            val pdfUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(shareIntent, "Share PDF via")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun showLoadingBox() {
        if (loadingBox == null) {
            loadingBox = LayoutInflater.from(requireContext()).inflate(R.layout.loadingbox1, binding.root as ViewGroup, false)
            (binding.root as ViewGroup).addView(loadingBox)
        }
        loadingBox?.findViewById<TextView>(R.id.tv_loading_message)
        loadingBox?.visibility = View.VISIBLE
    }


    private fun hideLoadingBox() {
        loadingBox?.visibility = View.GONE
    }

    private fun observeUntilReady(onReady: () -> Unit) {
        viewModel.bothGuidesReady.removeObservers(viewLifecycleOwner)
        viewModel.bothGuidesReady.observe(viewLifecycleOwner) { ready ->
            if (ready) {
                hideLoadingBox()
                onReady()
                pendingCityGuide = false
                pendingHomeGuide = false

                val homeId = viewModel.resultGetHome.value?.id ?: return@observe
                viewModel.setActivity(requireActivity())
                viewModel.generatePdf(homeId)
                viewModel.bothGuidesReady.removeObservers(viewLifecycleOwner)
            }
        }
    }
    private fun startCityGuide() {
        val intent = Intent(requireContext(), CityGuide::class.java).apply {
            putExtra("homeId", viewModel.resultGetHome.value?.id)
        }
        startActivity(intent)
    }

    private fun startHomeGuide() {
        val intent = Intent(requireContext(), HomeGuide::class.java).apply {
            putExtra("homeId", viewModel.resultGetHome.value?.id)
        }
        startActivity(intent)
    }
    private fun showAILoading(message: String) {
        if (aiLoadingBox == null) {
            aiLoadingBox = LayoutInflater.from(requireContext())
                .inflate(R.layout.loadingbox2, binding.root as ViewGroup, false)
            (binding.root as ViewGroup).addView(aiLoadingBox)
            tvAiCountdown = aiLoadingBox?.findViewById(R.id.tv_countdown)
        }
        aiLoadingBox?.findViewById<TextView>(R.id.tv_loading_message)?.text = message
        aiLoadingBox?.visibility = View.VISIBLE
        tvAiCountdown?.visibility = View.VISIBLE
    }

    private fun hideAILoading() {
        aiLoadingBox?.visibility = View.GONE
        tvAiCountdown?.visibility = View.GONE
        dummyTimer?.cancel()
    }




}