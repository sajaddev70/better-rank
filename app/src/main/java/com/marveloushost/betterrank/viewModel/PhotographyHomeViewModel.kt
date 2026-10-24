package com.marveloushost.betterrank.viewModel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.model.ApprovedModel
import com.marveloushost.betterrank.model.PhotoModel
import com.marveloushost.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ActivityScoped
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
@HiltViewModel
class PhotographyHomeViewModel @Inject constructor(
    internal val savedStateHandle: SavedStateHandle,
    override val appRepository: AppRepository,
    application: Application
) : BaseViewModel(appRepository, application) {

    private val homeId: String get() = (savedStateHandle["HOME_ID"] ?: "").toString()

    private val pendingPhotos = mutableMapOf<String, PhotoModel>()

    private val _pendingPhotoResult = MutableLiveData<PhotoModel?>()
    val pendingPhotoResult: LiveData<PhotoModel?> = _pendingPhotoResult

    // === ALL 9 PHOTO SECTIONS – NOW LISTS ===
    private val _coverPhoto = MutableLiveData<List<PhotoModel>>()
    val coverPhoto: LiveData<List<PhotoModel>> = _coverPhoto

    private val _livingRoom = MutableLiveData<List<PhotoModel>>()
    val livingRoom: LiveData<List<PhotoModel>> = _livingRoom

    private val _bedroom = MutableLiveData<List<PhotoModel>>()
    val bedroom: LiveData<List<PhotoModel>> = _bedroom

    private val _kitchen = MutableLiveData<List<PhotoModel>>()
    val kitchen: LiveData<List<PhotoModel>> = _kitchen

    private val _bathroom = MutableLiveData<List<PhotoModel>>()
    val bathroom: LiveData<List<PhotoModel>> = _bathroom

    private val _yard = MutableLiveData<List<PhotoModel>>()
    val yard: LiveData<List<PhotoModel>> = _yard

    private val _buildingDoor = MutableLiveData<List<PhotoModel>>()
    val buildingDoor: LiveData<List<PhotoModel>> = _buildingDoor

    private val _windowViews = MutableLiveData<List<PhotoModel>>()
    val windowViews: LiveData<List<PhotoModel>> = _windowViews

    private val _personality = MutableLiveData<List<PhotoModel>>()
    val personality: LiveData<List<PhotoModel>> = _personality

    private val _aiApprovedHomePhotoResult = MutableLiveData<ApprovedModel>()
    val aiApprovedHomePhotoResult: LiveData<ApprovedModel> = _aiApprovedHomePhotoResult

    private val _hasApprovedPhotos = MutableLiveData<Boolean>(false)
    val hasApprovedPhotos: LiveData<Boolean> = _hasApprovedPhotos

    // === Helper: Get LiveData by Name ===
    fun getLiveDataForItem(name: String): LiveData<List<PhotoModel>> = when (name) {
        PhotoName.CoverPhoto.value -> coverPhoto
        PhotoName.LivingRoom.value -> livingRoom
        PhotoName.Bedroom.value -> bedroom
        PhotoName.Kitchen.value -> kitchen
        PhotoName.Bathroom.value -> bathroom
        PhotoName.Yard.value -> yard
        PhotoName.BuildingDoor.value -> buildingDoor
        PhotoName.WindowViews.value -> windowViews
        PhotoName.Personality.value -> personality
        else -> MutableLiveData(emptyList())
    }

    fun getPhotosForItem(name: String): List<PhotoModel> = getLiveDataForItem(name).value.orEmpty()

    // === Upload + AI Approval ===
    fun uploadHomePhoto(name: String, selectedImageUri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: run {
                showMessage.value = "Please log in again"
                return
            }

        isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val url = appRepository.uploadHomePhoto(
                    uid = currentUser.uid,
                    name = name,
                    selectedImageUri = selectedImageUri
                )
                aiApprovedHomePhoto(name, url)
            } catch (e: Exception) {
//                Log.e("BetterRankException", "Upload failed: ${e.message}", e)
                showMessage.value = "Upload failed. Please try again."
                isLoading.postValue(false)
            }
        }
    }

    private fun aiApprovedHomePhoto(name: String, url: String) {
        viewModelScope.launch {
            try {
                val result = appRepository.aiApprovedHomePhoto(name, url)
                addPhotoToList(name, url, result)
                _aiApprovedHomePhotoResult.postValue(result)
            } catch (e: Exception) {
//                Log.e("BetterRankException", "AI approval failed: ${e.message}", e)
                showMessage.value = "AI analysis failed."
            } finally {
                isLoading.postValue(false)
            }
        }
    }

    // Update addPhotoToList to store the temp photo and expose it
    private fun addPhotoToList(name: String, url: String, approvedModel: ApprovedModel) {
        val tempPhoto = PhotoModel(
            name = name,
            title = getTitleForName(name),
            url = url,
            approved = approvedModel.done
        )

        // Store temporarily
        pendingPhotos[name] = tempPhoto

        // Expose it so UI can show in result dialog
        _pendingPhotoResult.postValue(tempPhoto)

        // Send only the approval result
        _aiApprovedHomePhotoResult.postValue(approvedModel)
    }
    fun clearPendingPhoto() {
        _pendingPhotoResult.postValue(null)
    }

    fun addPhotoPermanently(
        section: String,
        photo: PhotoModel,
        replacePhoto: PhotoModel? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
//        Log.d("PHOTO_FLOW", "addPhotoPermanently: Adding to $section")

        val liveData = when (section) {
            PhotoName.CoverPhoto.value -> _coverPhoto
            PhotoName.LivingRoom.value -> _livingRoom
            PhotoName.Bedroom.value -> _bedroom
            PhotoName.Kitchen.value -> _kitchen
            PhotoName.Bathroom.value -> _bathroom
            PhotoName.Yard.value -> _yard
            PhotoName.BuildingDoor.value -> _buildingDoor
            PhotoName.WindowViews.value -> _windowViews
            PhotoName.Personality.value -> _personality
            else -> return
        }

        val currentList = liveData.value?.toMutableList() ?: mutableListOf()

        // REPLACE LOGIC
        replacePhoto?.let { old ->
            currentList.removeAll { it.url == old.url }
        }

        val approvedPhoto = photo.copy(approved = true)
        currentList.add(approvedPhoto)

//        Log.d("PHOTO_FLOW", "SUCCESS: List now has ${currentList.size} photos")

        liveData.postValue(currentList.toList()) // toList() to make immutable
        pendingPhotos.remove(section)
        _pendingPhotoResult.postValue(null)
        updateHasApprovedPhotos()

        // SAVE TO FIRESTORE — CORRECT PATH
        viewModelScope.launch {
            try {
                // 🔥 delete old one from Firestore if replacing
                replacePhoto?.url?.let { oldUrl ->
                    deleteApprovedPhotoFromFirestore(homeId, section, oldUrl)
                }

                // then add new one
                appRepository.saveApprovedPhotoToFirestore(
                    homeId = homeId,
                    section = section,
                    photoUrl = photo.url!!,
                    title = photo.title
                )

                withContext(Dispatchers.Main) { onComplete?.invoke(true) }

            } catch (e: Exception) {
                onComplete?.invoke(false)
            }
        }

    }

    fun discardPendingPhoto(section: String) {
        pendingPhotos.remove(section)
        _pendingPhotoResult.postValue(null)
    }


    // === Get Approved List (All Photos) ===
    fun getPhotoHomesApproved(): List<PhotoModel> {
        return listOf(
            coverPhoto, livingRoom, bedroom, kitchen, bathroom,
            yard, buildingDoor, windowViews, personality
        ).flatMap { it.value.orEmpty() }.filter { it.approved }
    }

    private fun updateHasApprovedPhotos() {
        val total = listOf(
            _coverPhoto, _livingRoom, _bedroom, _kitchen, _bathroom,
            _yard, _buildingDoor, _windowViews, _personality
        ).sumOf { it.value?.size ?: 0 }

        _hasApprovedPhotos.postValue(total > 0)
//        Log.d("BTN_DEBUG", "Approved total = $total")
    }

    private suspend fun deleteApprovedPhotoFromFirestore(
        homeId: String,
        section: String,
        photoUrl: String
    ) {
        val firestore = FirebaseFirestore.getInstance()

        // homes/{homeId}/photos/{section}/images/{doc}
        val imagesRef = firestore
            .collection("homes")
            .document(homeId)
            .collection("photos")
            .document(section)
            .collection("images")

        // Find the document with matching URL
        val snapshot = imagesRef
            .whereEqualTo("url", photoUrl)
            .get()
            .await()

        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
    }



    private fun getTitleForName(name: String): String = when (name) {
        PhotoName.CoverPhoto.value -> "Cover Photo"
        PhotoName.LivingRoom.value -> application.getString(R.string.living_room)
        PhotoName.Bedroom.value -> application.getString(R.string.bedrooms)
        PhotoName.Kitchen.value -> application.getString(R.string.kitchen)
        PhotoName.Bathroom.value -> application.getString(R.string.bathroom)
        PhotoName.Yard.value -> application.getString(R.string.yard_and_balcony)
        PhotoName.BuildingDoor.value -> "Building door"
        PhotoName.WindowViews.value -> "Window views"
        PhotoName.Personality.value -> "Personality of the house"
        else -> ""
    }

    // === Download & Upload from URL ===
    fun downloadAndUploadImage(url: String, name: String) {
        isLoading.value = true
        viewModelScope.launch {
            var tempFile: File? = null
            try {
                tempFile = withContext(Dispatchers.IO) {
                    val file = File.createTempFile("temp_image", ".jpg", application.cacheDir)
                    URL(url).openConnection().apply {
                        connectTimeout = 10_000
                        readTimeout = 15_000
                    }.getInputStream().use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    file
                }

                val localUri = FileProvider.getUriForFile(
                    application,
                    "${application.packageName}.fileprovider",
                    tempFile
                )
                uploadHomePhoto(name, localUri)
            } catch (e: Exception) {
//                Log.e("VM", "Download/Upload failed: $url", e)
                showMessage.value = when (e) {
                    is java.net.UnknownHostException -> "No internet connection"
                    is java.net.SocketTimeoutException -> "Download timed out"
                    else -> "Failed to process image"
                }
            } finally {
                tempFile?.delete()
                isLoading.value = false
            }
        }
    }
    fun getGuideText(item: String): String = when (item) {
        PhotoName.CoverPhoto.value -> "Choose a wide, welcoming shot that shows the main living area at its best. Make it bright, include windows or a cozy detail that makes guests want to walk in."
        PhotoName.LivingRoom.value -> "Capture the entire room from a corner to make the space feel open. Arrange cushions neatly, add a soft throw or book, and let one warm detail draw attention."
        PhotoName.Bedroom.value -> "Make the bed perfectly with smooth sheets and layered pillows. Include a nightstand with a straightforward touch, like a lamp or book, and frame the bed as the focal point."
        PhotoName.Kitchen.value -> "Show the kitchen as ready to cook in. Clear every surface, open a few cabinets slightly, and highlight clean counters with one welcoming touch, like fruit or a mug."
        PhotoName.Bathroom.value -> "Show the bathroom to be clean and bright. Close the toilet lid, hang fresh towels neatly, and include the mirror or sink to make the space feel fresh and well cared for."
        PhotoName.Yard.value -> "Show the view and how guests can enjoy it. Include seating, plants, or a coffee cup to suggest relaxation, and frame part of the sky or greenery for depth."
        PhotoName.BuildingDoor.value -> "Photograph the door straight on so guests can easily recognize it. Keep the entry clear, show any visible number or feature, and include a bit of the surrounding area for context."
        PhotoName.WindowViews.value -> "Take the photo from inside, framing both the window and the view. Open curtains fully, focus on what’s outside, and keep window edges clean and clear."
        PhotoName.Personality.value -> "Focus on small details that show character: art, textures, colors, or decor that guests will remember. Capture them naturally, not staged, to show the home’s unique personality."
        else -> ""
    }

    fun getApprovedCount(section: String): Int {
        return getPhotosForItem(section).count { it.approved }
    }
    private fun loadSavedPhotosFromFirestore() {
        if (homeId.isEmpty()) {
//            Log.e("FIRESTORE", "homeId is empty!")
            return
        }

//        Log.d("FIRESTORE", "Loading photos for homeId: $homeId")

        // 1) ALWAYS initialize all sections with emptyList()
        val sectionTargets = mapOf(
            PhotoName.CoverPhoto.value to _coverPhoto,
            PhotoName.LivingRoom.value to _livingRoom,
            PhotoName.Bedroom.value to _bedroom,
            PhotoName.Kitchen.value to _kitchen,
            PhotoName.Bathroom.value to _bathroom,
            PhotoName.Yard.value to _yard,
            PhotoName.BuildingDoor.value to _buildingDoor,
            PhotoName.WindowViews.value to _windowViews,
            PhotoName.Personality.value to _personality
        )

        // initialize every section as empty immediately → prevents null
        sectionTargets.values.forEach { it.postValue(emptyList()) }

        viewModelScope.launch {
            try {
                // 2) Load all sections from Firestore
                val photosRef = FirebaseFirestore.getInstance()
                    .collection("homes")
                    .document(homeId)
                    .collection("photos")

                val sectionDocs = photosRef.get().await()

//                Log.d("FIRESTORE", "Found ${sectionDocs.documents.size} sections")

                for (sectionDoc in sectionDocs.documents) {
                    val section = sectionDoc.id
                    val targetLiveData = sectionTargets[section] ?: continue

                    val imagesSnap = sectionDoc.reference
                        .collection("images")
                        .get()
                        .await()

                    val photos = imagesSnap.documents.mapNotNull { imgDoc ->
                        val url = imgDoc.getString("url") ?: return@mapNotNull null
                        val title = imgDoc.getString("title")
                            ?: section.replace("_", " ").replaceFirstChar { it.uppercase() }

                        PhotoModel(
                            name = section,
                            title = title,
                            url = url,
                            approved = true
                        )
                    }

//                    Log.d("FIRESTORE", "Loaded ${photos.size} photos for $section")

                    // 3) Overwrite the initialized empty list ONLY if Firestore has data
                    targetLiveData.postValue(photos)
                    delay(100)
                    updateHasApprovedPhotos()
                }

                updateHasApprovedPhotos()
//                Log.d("FIRESTORE", "All photos loaded ✔️")

            } catch (e: Exception) {
//                Log.e("FIRESTORE", "Failed to load photos", e)
            }
        }
    }
    fun reloadPhotosFromFirestore() {
        loadSavedPhotosFromFirestore()
    }

    init {
        loadSavedPhotosFromFirestore()
    }




}