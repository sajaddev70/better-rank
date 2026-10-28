package com.marvelous.betterrank.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.marvelous.betterrank.R
import com.marvelous.betterrank.adapter.AirbnbPhotoAdapter
import com.marvelous.betterrank.adapter.SectionPhotoAdapter
import com.marvelous.betterrank.databinding.ActivityPhotoSectionBinding
import com.marvelous.betterrank.dialog.PhotoHomeResultDialog
import com.marvelous.betterrank.fileManager.FilePickerHelper
import com.marvelous.betterrank.fileManager.ImageHelper
import com.marvelous.betterrank.model.ApprovedModel
import com.marvelous.betterrank.model.PhotoModel
import com.marvelous.betterrank.viewModel.HomeViewModel
import com.marvelous.betterrank.viewModel.PhotographyHomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class PhotoSectionActivity : BaseActivity() {

    private val viewModel: PhotographyHomeViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    private var homeId: String = ""
    private lateinit var binding: ActivityPhotoSectionBinding
    private lateinit var imageHelper: ImageHelper
    private lateinit var filePickerHelper: FilePickerHelper

    private var currentSection = ""
    private var pendingUri: Uri? = null
    private var isReplaceMode = false
    private var selectedExistingPhoto: PhotoModel? = null
    private var pendingAirbnbUrl: String? = null
    private var selectedImageUrl: String? = null
    private var timeoutJob: Job? = null

    private lateinit var airbnbAdapter: AirbnbPhotoAdapter
    private lateinit var existingAdapter: SectionPhotoAdapter

    private var analyzingView: View? = null
    private var countdownTimer: CountDownTimer? = null
    private var ellipsisAnimator: ValueAnimator? = null
    private var pendingPhoto: PhotoModel? = null

    private var timeoutHandler: Handler? = null
    private val timeoutRunnable = Runnable {
        if (analyzingView != null) {
            hideAnalyzingDialog()
            showMessage("Analysis timed out. Please try again.")
//            Log.e("PHOTO_ANALYSIS", "AI analysis timed out after 60 seconds")
        }
    }

    private var cameraUri: Uri = Uri.EMPTY
    private var fullscreenLoading: View? = null

    private fun showInitialState(hasPhoto: Boolean) {
        if (hasPhoto) {
            binding.layoutHasPhotos.isVisible = true
            binding.layoutEmpty.isVisible = false
            binding.cardGuide.isVisible = false
            binding.btnAdd.isVisible = true
            binding.or.isVisible = true
            binding.btnReplace.isVisible = true
            binding.btnTakePhoto.isVisible = false
            binding.btnGallery.isVisible = false
            binding.btnCheckQuality.isVisible = false
        } else {
            binding.layoutHasPhotos.isVisible = false
            binding.layoutEmpty.isVisible = true
            binding.cardGuide.isVisible = true
            binding.btnTakePhoto.isVisible = true
            binding.btnGallery.isVisible = true
            binding.btnCheckQuality.isVisible = true
            binding.btnCheckQuality.isEnabled = false
            binding.btnCheckQuality.alpha = 0.3f
            binding.btnAdd.isVisible = false
            binding.btnReplace.isVisible = false
            binding.or.isVisible = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPhotoSectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Block back while AI analysis is running
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (analyzingView != null) return
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })

        // Read arguments FIRST
        currentSection = intent.getStringExtra("SECTION") ?: return finish()
        homeId = intent.getStringExtra("HOME_ID") ?: return finish()
        val hasPhoto = intent.getBooleanExtra("HAS_PHOTO", false)

        // Set title + guide text
        binding.tvTitle.text = getTitleForSection()
        binding.tvGuide.text = viewModel.getGuideText(currentSection)

        // FIRST DRAW ONLY → avoid flicker
        showInitialState(hasPhoto)

        // Small fade-in
        binding.rootContainer.alpha = 0f
        binding.rootContainer.post {
            binding.rootContainer.animate().alpha(1f).setDuration(80).start()
        }

        // Init helpers AFTER UI state
        imageHelper = ImageHelper(this)
        filePickerHelper = FilePickerHelper(
            this, activityResultRegistry,
            onFilePicked = { _, _, _, path -> path?.let { handleNewPhoto(it) } },
            onError = { showMessage(it) }
        ).also { it.register() }

        setupViews()
        observe()

        // Load Airbnb only once
        homeViewModel.resultGetHome.observe(this) { result ->
            val images = result?.airBnbResponseModel?.images.orEmpty()

            airbnbAdapter.submitList(images)
            binding.pagerAirbnb.adapter = airbnbAdapter
            binding.dotsAirbnb.attachTo(binding.pagerAirbnb)

            binding.pagerAirbnb.isVisible = images.isNotEmpty()
            binding.dotsAirbnb.isVisible = images.size >1
        }
        homeViewModel.resultGetHome.value ?: homeViewModel.getHomes()
    }


    override fun initView() {
        TODO("Not yet implemented")
    }

    override fun onClickView() {
        TODO("Not yet implemented")
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }

        airbnbAdapter = AirbnbPhotoAdapter(
            onPhotoSelected = { url ->
                pendingUri = null
                pendingAirbnbUrl = url
                if (!isReplaceMode) {
                    selectedExistingPhoto = null
                }
                showSelectedPreview(url)
            },

            onPhotoCleared = {
                pendingAirbnbUrl = null
                pendingUri = null
                selectedExistingPhoto = null
                clearPreview()  // Hides the preview + disables button
            }
        )



        existingAdapter = SectionPhotoAdapter(
            onPhotoClick = { photo ->
                selectedExistingPhoto = photo
                isReplaceMode = true
                binding.btnReplace.isEnabled = true
                binding.btnReplace.alpha = 1f
                showSelectedPreview(photo.url ?: "")
            },
            onSelectionChanged = { hasSelection ->
                binding.btnReplace.isEnabled = hasSelection
                binding.btnReplace.alpha = if (hasSelection) 1f else 0.3f
            }
        )

        // ============= VIEWPAGER SETUP =============
        binding.pagerAirbnb.adapter = airbnbAdapter
        binding.pagerExisting.adapter = existingAdapter

        // Disable overscroll glow
        binding.pagerAirbnb.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER
        binding.pagerExisting.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER

        // ============= DOTS INDICATORS =============
        binding.dotsAirbnb.attachTo(binding.pagerAirbnb)
        binding.dotsExisting.attachTo(binding.pagerExisting)

        // Default hidden until adapter loads data
        binding.dotsAirbnb.isVisible = false
        binding.dotsExisting.isVisible = false

        // ============= BUTTONS =============
        binding.btnTakePhoto.setOnClickListener { takePhoto() }
        binding.btnGallery.setOnClickListener { filePickerHelper.pickFile("image/*") }
        binding.btnAdd.setOnClickListener { showAddScreen() }
        binding.btnReplace.setOnClickListener {
            if (selectedExistingPhoto != null) {
                isReplaceMode = true   // ensure locked
                showAddScreen()
            }
        }

        binding.btnCheckQuality.setOnClickListener { checkQuality() }
    }

    private fun showAddScreen() {
        clearPreview()
        binding.layoutEmpty.isVisible = true
        binding.layoutHasPhotos.isVisible = false
        binding.btnAdd.isVisible = false
        binding.or.isVisible = false
        binding.btnReplace.isVisible = false
        binding.tvGuide.visibility = View.VISIBLE
        binding.btnTakePhoto.visibility = View.VISIBLE
        binding.btnGallery.visibility = View.VISIBLE
        binding.btnCheckQuality.visibility = View.VISIBLE
        binding.btnCheckQuality.isEnabled = false
        binding.btnCheckQuality.alpha = 0.3f
    }

    private fun handleNewPhoto(path: String) {
        val compressedPath = imageHelper.compressImageFromPath(path) ?: return
        val uri = imageHelper.getUriFromFilePath(compressedPath)
        pendingUri = uri
        showSelectedPreview(uri.toString())
    }

    private fun checkQuality() {
        if (selectedImageUrl == null || analyzingView != null) return

        analyzingView =
            layoutInflater.inflate(R.layout.dialog_analyzing, binding.dialogOverlayContainer, false)
        binding.dialogOverlayContainer.apply {
            visibility = View.VISIBLE
            isClickable = true
            isFocusable = true
        }
        binding.dialogOverlayContainer.addView(analyzingView)

        val tvAnalyzing = analyzingView!!.findViewById<TextView>(R.id.tv_analyzing_text)
        val tvCountdown = analyzingView!!.findViewById<TextView>(R.id.tv_countdown)

        startEllipsisAnimation(tvAnalyzing)
        startCountdown(tvCountdown)

        timeoutHandler = Handler(Looper.getMainLooper())
        timeoutHandler?.postDelayed(timeoutRunnable, 30_000)

        if (selectedImageUrl!!.startsWith("http")) {
            viewModel.downloadAndUploadImage(selectedImageUrl!!, currentSection)
        } else {
            pendingUri?.let { viewModel.uploadHomePhoto(currentSection, it) }
        }
    }

    private fun hideAnalyzingDialog() {
        analyzingView?.let {
            binding.dialogOverlayContainer.removeView(it)
            analyzingView = null
        }
        binding.dialogOverlayContainer.visibility = View.GONE
        ellipsisAnimator?.cancel()
        countdownTimer?.cancel()
        ellipsisAnimator = null
        countdownTimer = null
    }

    private fun startEllipsisAnimation(textView: TextView) {
        val dotsView = analyzingView!!.findViewById<TextView>(R.id.tv_dots)

        ellipsisAnimator?.cancel()
        ellipsisAnimator = ValueAnimator.ofInt(0, 3).apply {
            duration = 1500
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val count = it.animatedValue as Int
                dotsView.text = ".".repeat(count)
            }
            start()
        }
    }


    private fun startCountdown(textView: TextView) {
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(30_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                textView.text = "${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                textView.text = "0"
            }
        }.start()
    }

    private fun observe() {

        // Observe ONLY the current section’s photos
        viewModel.getLiveDataForItem(currentSection).observe(this) { userPhotos ->
            if (userPhotos == null) return@observe  // Wait for Firestore REAL data
            existingAdapter.submitList(userPhotos)

            // 🔥 MUST re-attach because adapter data changed
            binding.pagerExisting.adapter = existingAdapter
            binding.dotsExisting.attachTo(binding.pagerExisting)

            // 🔥 Visibility rules
            binding.pagerExisting.isVisible = userPhotos.isNotEmpty()
            binding.dotsExisting.isVisible = userPhotos.size >1
            binding.layoutHasPhotos.isVisible = userPhotos.isNotEmpty()

        }

        // AI Analysis Observer (unchanged)
        viewModel.aiApprovedHomePhotoResult.observe(this) { approved ->
            timeoutHandler?.removeCallbacks(timeoutRunnable)
            timeoutHandler = null
            hideAnalyzingDialog()

            val tempPhoto = viewModel.pendingPhotoResult.value ?: run {
                showMessage("Analysis failed.")
                return@observe
            }

//            logApprovalResponse(approved)

            PhotoHomeResultDialog(
                photo = tempPhoto,
                approved = approved,
                onUseThisPhoto = {finishSavingPhoto(tempPhoto)},
//                onUseThisPhoto = {
//                    viewModel.addPhotoPermanently(
//                        section = currentSection,
//                        photo = tempPhoto,
//                        replacePhoto = selectedExistingPhoto
//                    ) { success ->
//                        viewModel.clearPendingPhoto()
//                        if (success) {
//                            showMessage("Photo Saved")
//                            setResult(RESULT_OK)
//                        } else showMessage("Failed to save photo. Please try again.")
//                    }
//                },
                onSkip = {
                    viewModel.clearPendingPhoto()
                    viewModel.discardPendingPhoto(currentSection)
                }
            ).show(supportFragmentManager, "result")
        }
    }
    private fun finishSavingPhoto(tempPhoto: PhotoModel) {
        viewModel.addPhotoPermanently(
            section = currentSection,
            photo = tempPhoto,
            replacePhoto = selectedExistingPhoto
        ) { success ->
            // Find the dialog that is currently shown
            val dialog = supportFragmentManager.
            findFragmentByTag("result") as? PhotoHomeResultDialog
            if (success) {
                dialog?.hideLoading()       // 1️⃣ remove overlay first
                showMessage("Photo Saved")  // 2️⃣ optional toast/snackbar
                viewModel.clearPendingPhoto() // 3️⃣ clear temp state
                dialog?.dismiss()           // 4️⃣ close dialog
                setResult(RESULT_OK)        // 5️⃣ return success
                finish()            // ⬅️ CLOSE ACTIVITY
            } else {
                dialog?.hideLoading()
                showMessage("Failed to save photo.")
            }
        }
    }


    private fun getTitleForSection(): String = when (currentSection) {
        "cover_photo" -> "Cover Photo"
        "living_room" -> getString(R.string.living_room)
        "bedroom" -> getString(R.string.bedrooms)
        "kitchen" -> getString(R.string.kitchen)
        "bathroom" -> getString(R.string.bathroom)
        "yard" -> getString(R.string.yard_and_balcony)
        "building_door" -> "Building Door"
        "window_views" -> "Window Views"
        "personality" -> "Personality of the House"
        else -> currentSection.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    private fun takePhoto() {
        val photoFile = File.createTempFile("photo_", ".jpg", externalCacheDir)
        cameraUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            photoFile
        )
        cameraLauncher.launch(cameraUri)
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                pendingUri = cameraUri
                pendingAirbnbUrl = null
                showSelectedPreview(cameraUri.toString())
            }
        }

    private fun showSelectedPreview(url: String) {
        selectedImageUrl = url
        binding.imgSelectedPreview.loadImage(url)
        binding.imgSelectedPreview.isVisible = true
        binding.btnCheckQuality.isEnabled = true
        binding.btnCheckQuality.alpha = 1f
    }

    private fun clearPreview() {
        selectedImageUrl = null
        binding.imgSelectedPreview.isVisible = false
        binding.btnCheckQuality.isEnabled = false
        binding.btnCheckQuality.alpha = 0.3f
    }


//    private fun logApprovalResponse(approved: ApprovedModel) {
//        val threshold = 50f
////        Log.d("AI_RESPONSE", "════════════════════════════════")
////        Log.d("AI_RESPONSE", "Photo Analysis Result:")
////        Log.d("AI_RESPONSE", " • Overall Approved: ${approved.done}")
////        Log.d("AI_RESPONSE", " • Threshold: $threshold%")
////        Log.d("AI_RESPONSE", "")
//        Log.d(
//            "AI_RESPONSE",
//            " Lighting: ${approved.lightingPercent}% → ${if (approved.lightingPercent < threshold) "FAILED" else "PASS"}"
//        )
//        if (approved.lightingPercent < threshold) Log.d(
//            "AI_RESPONSE",
//            " → \"${approved.lighting}\""
//        )
//        Log.d(
//            "AI_RESPONSE",
//            " Angle: ${approved.anglePercent}% → ${if (approved.anglePercent < threshold) "FAILED" else "PASS"}"
//        )
//        if (approved.anglePercent < threshold) Log.d("AI_RESPONSE", " → \"${approved.angle}\"")
//        Log.d(
//            "AI_RESPONSE",
//            " Focus: ${approved.focusPercent}% → ${if (approved.focusPercent < threshold) "FAILED" else "PASS"}"
//        )
//        if (approved.focusPercent < threshold) Log.d("AI_RESPONSE", " → \"${approved.focus}\"")
//        Log.d("AI_RESPONSE", "════════════════════════════════")
//    }

    override fun onDestroy() {
        timeoutHandler?.removeCallbacks(timeoutRunnable)
        timeoutHandler = null
        viewModel.clearPendingPhoto()
        super.onDestroy()
    }

    private fun showFullscreenLoading() {
        if (fullscreenLoading != null) return
        fullscreenLoading = layoutInflater.inflate(R.layout.loading_fullscreen, null)
        (window.decorView as ViewGroup).addView(fullscreenLoading)
    }

    private fun hideFullscreenLoading() {
        fullscreenLoading?.let {
            (window.decorView as ViewGroup).removeView(it)
            fullscreenLoading = null
        }
    }
}

// Extension function for RoundedImageView (must be outside the class)
fun com.makeramen.roundedimageview.RoundedImageView.loadImage(url: String?) {
    if (url.isNullOrEmpty()) return
    Glide.with(this)
        .load(url)
        .into(this)
}