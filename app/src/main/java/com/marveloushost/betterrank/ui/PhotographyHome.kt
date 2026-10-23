package com.marveloushost.betterrank.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.customView.TakePhoto
import com.marveloushost.betterrank.databinding.PhotographyHomeBinding
import com.marveloushost.betterrank.dialog.MultiImagePickerDialog
import com.marveloushost.betterrank.dialog.PhotoHomeDownloadDialog
import com.marveloushost.betterrank.dialog.PhotoHomeResultDialog
import com.marveloushost.betterrank.fileManager.DownloadHelper
import com.marveloushost.betterrank.fileManager.FilePickerHelper
import com.marveloushost.betterrank.fileManager.ImageHelper
import com.marveloushost.betterrank.model.ApprovedModel
import com.marveloushost.betterrank.model.PhotoModel
import com.marveloushost.betterrank.permissionManager.PermissionManager
import com.marveloushost.betterrank.viewModel.HomeViewModel
import com.marveloushost.betterrank.viewModel.PhotographyHomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PhotographyHome : BaseActivity() {
    private var homeId: String = ""

    private val binding: PhotographyHomeBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.photography_home)
    }
    private val viewModel: PhotographyHomeViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var imageHelper: ImageHelper
    private lateinit var filePickerHelper: FilePickerHelper
    private var currentSection: String = ""
    private var currentCameraUri: Uri? = null
    // MOVE THIS TO CLASS LEVEL — NOT INSIDE onCreate()
    private val photoSectionLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.reloadPhotosFromFirestore()
//            refreshAllApprovedTexts()


        }
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            // Permission granted → open camera
            val uri = createImageUri()
            cameraLauncher.launch(uri)
        } else {
            showMessage("Camera permission is required to take photos")
        }
    }
    private val launcher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra("photoPath")?.let { path ->
                setImagePath(path)
            }
        }
    }
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentCameraUri != null) {
            viewModel.uploadHomePhoto(currentSection, currentCameraUri!!)
            currentCameraUri = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeId = intent.getStringExtra("HOME_ID") ?: run {
            showMessage("Home not found")
            finish()
            return
        }
        viewModel.savedStateHandle["HOME_ID"] = homeId
        initView()
        onClickView()
        observeViewModel()
        homeViewModel.getHomes()

    }

    override fun initView() {
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        binding.baseActionBar.apply {
            imgStart.apply {
                setImageResource(R.drawable.ic_arrow_left)
                visibility = View.VISIBLE
            }
            txtTitle.text = getString(R.string.photography_home)
        }

        imageHelper = ImageHelper(this)
        filePickerHelper = FilePickerHelper(
            context = this,
            activityResultRegistry = activityResultRegistry,
            onFilePicked = { _, _, _, realPath ->
                setImagePath(imageHelper.compressImageFromPath(realPath))
            },
            onError = { showMessage(it) }
        ).also { it.register() }
    }

    override fun onClickView() {

        viewModel.showMessage.observe(this) { showMessage(it) }
        binding.baseActionBar.imgStart.setOnClickListener { finish() }

        // === ALL 9 BOXES – Set currentSection + Open Dialog ===
        binding.liCoverPhoto.setOnClickListener {
            currentSection = PhotoName.CoverPhoto.value
            val hasPhoto = viewModel.getApprovedCount(currentSection) > 0
//            Log.d("PHOTO_DEBUG", "Opening $currentSection → hasPhoto = $hasPhoto")
            photoSectionLauncher.launch(Intent(this, PhotoSectionActivity::class.java).apply {
                putExtra("SECTION", currentSection)
                putExtra("HOME_ID", homeId)
                putExtra("HAS_PHOTO", hasPhoto)
            })
        }
        binding.liLivingRoom.setOnClickListener {
            currentSection = PhotoName.LivingRoom.value
            val hasPhoto = viewModel.getApprovedCount(currentSection) > 0
            photoSectionLauncher.launch(Intent(this, PhotoSectionActivity::class.java).apply {
                putExtra("SECTION", currentSection)
                putExtra("HOME_ID", homeId)
                putExtra("HAS_PHOTO", hasPhoto)
            })
        }
        binding.liBedrooms.setOnClickListener {
            currentSection = PhotoName.Bedroom.value
            val hasPhoto = viewModel.getApprovedCount(currentSection) > 0
            photoSectionLauncher.launch(Intent(this, PhotoSectionActivity::class.java).apply {
                putExtra("SECTION", currentSection)
                putExtra("HOME_ID", homeId)
                putExtra("HAS_PHOTO", hasPhoto)
            })
        }
        binding.liKitchen.setOnClickListener {
            currentSection = PhotoName.Kitchen.value
            val hasPhoto = viewModel.getApprovedCount(currentSection) > 0
            photoSectionLauncher.launch(Intent(this, PhotoSectionActivity::class.java).apply {
                putExtra("SECTION", currentSection)
                putExtra("HOME_ID", homeId)
                putExtra("HAS_PHOTO", hasPhoto)
            })
        }
        binding.liBathroom.setOnClickListener {
            currentSection = PhotoName.Bathroom.value
            val hasPhoto = viewModel.getApprovedCount(currentSection) > 0
            photoSectionLauncher.launch(Intent(this, PhotoSectionActivity::class.java).apply {
                putExtra("SECTION", currentSection)
                putExtra("HOME_ID", homeId)
                putExtra("HAS_PHOTO", hasPhoto)
            })
        }
        binding.liYard.setOnClickListener {
            currentSection = PhotoName.Yard.value
            val hasPhoto = viewModel.getApprovedCount(currentSection) > 0
            photoSectionLauncher.launch(Intent(this, PhotoSectionActivity::class.java).apply {
                putExtra("SECTION", currentSection)
                putExtra("HOME_ID", homeId)
                putExtra("HAS_PHOTO", hasPhoto)
            })
        }
        binding.liBuildingDoor.setOnClickListener {
            currentSection = PhotoName.BuildingDoor.value
            val hasPhoto = viewModel.getApprovedCount(currentSection) > 0
            photoSectionLauncher.launch(Intent(this, PhotoSectionActivity::class.java).apply {
                putExtra("SECTION", currentSection)
                putExtra("HOME_ID", homeId)
                putExtra("HAS_PHOTO", hasPhoto)
            })
        }
        binding.liWindowViews.setOnClickListener {
            currentSection = PhotoName.WindowViews.value
            val hasPhoto = viewModel.getApprovedCount(currentSection) > 0
            photoSectionLauncher.launch(Intent(this, PhotoSectionActivity::class.java).apply {
                putExtra("SECTION", currentSection)
                putExtra("HOME_ID", homeId)
                putExtra("HAS_PHOTO", hasPhoto)
            })
        }
        binding.liPersonality.setOnClickListener {
            currentSection = PhotoName.Personality.value
            val hasPhoto = viewModel.getApprovedCount(currentSection) > 0
            photoSectionLauncher.launch(Intent(this, PhotoSectionActivity::class.java).apply {
                putExtra("SECTION", currentSection)
                putExtra("HOME_ID", homeId)
                putExtra("HAS_PHOTO", hasPhoto)
            })
        }

        // Download Button
        binding.btnDownload.setOnClickListener {
            if (Build.VERSION.SDK_INT <= 28) {
                if (PermissionManager.isPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    showPhotoHomeDownloadDia()
                } else {
                    PermissionManager.requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            } else {
                showPhotoHomeDownloadDia()
            }
        }
//        // AI Approval Result
//        viewModel.aiApprovedHomePhotoResult.observe(this) { approvedModel ->
//            approvedModel?.let { showPhotoHomeResultDialog(it) }
//        }
    }
    private fun createImageUri(): Uri {
        val file = File.createTempFile("camera_", ".jpg", externalCacheDir)
        return FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        ).also { currentCameraUri = it }
    }

    private fun openPhotoPickerDialog(itemName: String) {
        val airbnbImages = homeViewModel.resultGetHome.value?.airBnbResponseModel?.images.orEmpty()

        MultiImagePickerDialog(
            images = airbnbImages,
            guideText = viewModel.getGuideText(itemName),
            onImagesSelected = { urls ->
                urls.forEach { viewModel.downloadAndUploadImage(it, itemName) }
            },
            onTakePhoto = {
                // Request camera permission safely
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        val uri = createImageUri()
                        cameraLauncher.launch(uri)
                    } else {
                        requestCameraPermission.launch(Manifest.permission.CAMERA)
                    }
                } else {
                    // Below Android 6 → permission granted at install
                    val uri = createImageUri()
                    cameraLauncher.launch(uri)
                }
            },
            onPickFromGallery = {
                filePickerHelper.pickFile("image/*")
            }
        ).apply {
            setTitle("Add from current Airbnb photos")
        }.show(supportFragmentManager, "PhotoPickerDialog")
    }

    // Fixed: parameter name is 'list', not 'photos'
    private fun showPhotoHomeDownloadDia() {
        PhotoHomeDownloadDialog(
            approvedPhotos = viewModel.getPhotoHomesApproved(),  // ← Correct parameter
            onDownload = { startDownload(it) }
        ).show(supportFragmentManager, "DownloadDialog")
    }

    private fun startDownload(photo: PhotoModel) {
        photo.url?.let {
            DownloadHelper.downloadFile(
                context = this,
                url = it,
                fileName = "${photo.title}.jpg",
                startDownloading = { showMessage("Download started...") }
            )
        }
    }


    // Fixed: use currentSection instead of selectedItem
    private fun setImagePath(path: String?) {
        path?.let {
            val uri = imageHelper.getUriFromFilePath(it)
            viewModel.uploadHomePhoto(currentSection, uri)  // ← Fixed
        }
    }
    private fun observeViewModel() {
        // Observe ALL 9 sections
        viewModel.coverPhoto.observe(this) { updateSectionUI(PhotoName.CoverPhoto.value, it) }
        viewModel.livingRoom.observe(this) { updateSectionUI(PhotoName.LivingRoom.value, it) }
        viewModel.bedroom.observe(this) { updateSectionUI(PhotoName.Bedroom.value, it) }
        viewModel.kitchen.observe(this) { updateSectionUI(PhotoName.Kitchen.value, it) }
        viewModel.bathroom.observe(this) { updateSectionUI(PhotoName.Bathroom.value, it) }
        viewModel.yard.observe(this) { updateSectionUI(PhotoName.Yard.value, it) }
        viewModel.buildingDoor.observe(this) { updateSectionUI(PhotoName.BuildingDoor.value, it) }
        viewModel.windowViews.observe(this) { updateSectionUI(PhotoName.WindowViews.value, it) }
        viewModel.personality.observe(this) { updateSectionUI(PhotoName.Personality.value, it) }
    }

    private fun updateSectionUI(section: String, photos: List<PhotoModel>?) {
        val count = photos?.count { it.approved } ?: 0
        val text = if (count > 0) "Approved ($count)" else "Not Done"

        when (section) {
            PhotoName.CoverPhoto.value -> binding.tvApprovedCoverPhoto.text = text
            PhotoName.LivingRoom.value -> binding.tvApprovedLivingRoom.text = text
            PhotoName.Bedroom.value -> binding.tvApprovedBedrooms.text = text
            PhotoName.Kitchen.value -> binding.tvApprovedKitchen.text = text
            PhotoName.Bathroom.value -> binding.tvApprovedBathroom.text = text
            PhotoName.Yard.value -> binding.tvApprovedYard.text = text
            PhotoName.BuildingDoor.value -> binding.tvApprovedBuildingDoor.text = text
            PhotoName.WindowViews.value -> binding.tvApprovedWindowViews.text = text
            PhotoName.Personality.value -> binding.tvApprovedPersonality.text = text
        }
    }

//    private fun refreshAllApprovedTexts() {
//        viewModel.coverPhoto.value?.let { updateSectionUI(PhotoName.CoverPhoto.value, it) }
//        viewModel.livingRoom.value?.let { updateSectionUI(PhotoName.LivingRoom.value, it) }
//        viewModel.bedroom.value?.let { updateSectionUI(PhotoName.Bedroom.value, it) }
//        viewModel.kitchen.value?.let { updateSectionUI(PhotoName.Kitchen.value, it) }
//        viewModel.bathroom.value?.let { updateSectionUI(PhotoName.Bathroom.value, it) }
//        viewModel.yard.value?.let { updateSectionUI(PhotoName.Yard.value, it) }
//        viewModel.buildingDoor.value?.let { updateSectionUI(PhotoName.BuildingDoor.value, it) }
//        viewModel.windowViews.value?.let { updateSectionUI(PhotoName.WindowViews.value, it) }
//        viewModel.personality.value?.let { updateSectionUI(PhotoName.Personality.value, it) }
//    }
}