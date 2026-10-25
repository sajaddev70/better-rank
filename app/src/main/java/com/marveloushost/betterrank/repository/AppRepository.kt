package com.marveloushost.betterrank.repository

import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.marveloushost.betterrank.hilt.modual.HiltNetworkModule
import com.marveloushost.betterrank.model.AirBnbRequestModel
import com.marveloushost.betterrank.model.AirBnbResponseModel
import com.marveloushost.betterrank.model.ApprovedModel
import com.marveloushost.betterrank.model.CityGuideModel
import com.marveloushost.betterrank.model.HomeGuideModel
import com.marveloushost.betterrank.model.HomeModel
import com.marveloushost.betterrank.model.NoteModel
import com.marveloushost.betterrank.model.UserModel
import com.marveloushost.betterrank.retrofit.RequestApi
import com.marveloushost.betterrank.util.RemoteConfigManager
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Response
import kotlin.text.Regex


class AppRepository(
    private val sharedPreferences: SharedPreferences,
    private val requestApi: RequestApi) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private var openAIClient: OpenAIClient = OpenAIOkHttpClient.builder().apiKey(HiltNetworkModule.GPT_API_KEY).build()

    ///////// API

    /** Authenticate user with Google ID Token */
    suspend fun authenticateWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user ?: throw Exception("Login Error: User is null")
    }

    /** Check if user exists in FireStore */
    suspend fun getUser(uid: String): UserModel? {
        val doc = db.collection("users")
            .document(uid)
            .get()
            .await()

        return if (doc.exists()) {
            doc.toObject(UserModel::class.java)
        } else {
            null
        }
    }

    /** Create a new user in FireStore, optionally uploading a custom photo */
    suspend fun createUser(
        uid: String,
        name: String?,
        email: String?,
        photoUrl: String?,
        selectedImageUri: Uri?
    ): UserModel {

        val finalPhotoUrl: String? = if (selectedImageUri != null) {
            val profileRef = storage.child("users/$uid/profile.jpg")
            profileRef.putFile(selectedImageUri).await()

            profileRef.downloadUrl.await().toString()
        } else {
            photoUrl
        }

        return saveUserToFireStore(uid, name, email, finalPhotoUrl)
    }

    private suspend fun saveUserToFireStore(
        uid: String,
        name: String?,
        email: String?,
        photoUrl: String?
    ): UserModel {
        val userData = UserModel(
            uid = uid,
            name = name,
            email = email,
            photoUrl = photoUrl
        )
        db.collection("users").document(uid).set(userData).await()
        return userData
    }

    suspend fun deleteUserData(uid: String): Boolean {

        db.collection("users").document(uid).delete().await()

        val homesSnapshot = db.collection("homes")
            .whereEqualTo("userId", uid)
            .get()
            .await()

        val batch = db.batch()
        for (doc in homesSnapshot.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()

        return true
    }

    //////// Homes
    suspend fun getHomes(uid:String):List<HomeModel>{
        val snapshot=db.collection("homes")
            .whereEqualTo("userId", uid)
            .get()
            .await()
        return snapshot.documents
            .mapNotNull { doc ->
                val home = doc.toObject(HomeModel::class.java)
                home?.id = doc.id
                home
            }
            .sortedBy { it.createdAt }
    }

//    suspend fun getHome(homeId:String):HomeModel?{
//        val doc=db.collection("homes")
//            .document(homeId)
//            .get()
//            .await()
//        return if (doc.exists()) {
//            doc.toObject(HomeModel::class.java)
//        } else {
//            null
//        }
//    }

    suspend fun getHome(homeId: String): HomeModel? {
        if (homeId.isBlank()) {
//            Log.w("AppRepository", "getHome: homeId is empty")
            return null
        }

        return try {
            val doc = db.collection("homes")
                .document(homeId)
                .get()
                .await()

            if (doc.exists()) {
                doc.toObject(HomeModel::class.java)?.apply { id = doc.id }
            } else {
//                Log.w("AppRepository", "getHome: Document does not exist for ID: $homeId")
                null
            }
        } catch (e: Exception) {
//            Log.e("AppRepository", "getHome failed for ID: $homeId", e)
            null
        }
    }

    suspend fun createHome(homeModel: HomeModel):HomeModel
    {
        val docRef=db.collection("homes").add(homeModel).await()
        homeModel.id=docRef.id
        return homeModel
    }

    suspend fun updateCityGuide(homeId:String,cityGuideModel: CityGuideModel):CityGuideModel
    {
        db.collection("homes").document(homeId).update("cityGuide",cityGuideModel).await()
        return cityGuideModel
    }

    suspend fun updateHomeGuide(homeId:String,homeGuideModel: HomeGuideModel):HomeGuideModel
    {
        db.collection("homes").document(homeId).update("homeGuide",homeGuideModel).await()
        return homeGuideModel
    }

    suspend fun uploadHomePhoto(
        uid: String,
        name: String,
        selectedImageUri: Uri
    ): String {
        val profileRef = storage.child("users/$uid/$name.jpg")
        profileRef.putFile(selectedImageUri).await()
        return profileRef.downloadUrl.await().toString()
    }

    suspend fun saveApprovedPhotoToFirestore(
        homeId: String,
        section: String,
        photoUrl: String,
        title: String
    ) {
        val db = FirebaseFirestore.getInstance()
        val sectionRef = db.collection("homes")
            .document(homeId)
            .collection("photos")
            .document(section)

        // THIS LINE IS THE ONLY THING YOU NEED
        sectionRef.set(mapOf("section" to section), SetOptions.merge()).await()

        val photoData = hashMapOf(
            "url" to photoUrl,
            "title" to title,
            "approved" to true,
            "timestamp" to System.currentTimeMillis()
        )

        sectionRef
            .collection("images")  // ← your "images" subcollection
            .add(photoData)
            .await()
    }


    suspend fun pingGoogle(): Result<Response<Unit>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = requestApi.pingGoogle()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAirbnbInfo(url: String): Result<Response<AirBnbResponseModel>> = withContext(Dispatchers.IO) {

        val airBnbRequestModel=AirBnbRequestModel(input = listOf(AirBnbRequestModel.Input(url = url)))

        // The startup Remote Config fetch can fail or still be running, which sends the
        // request without an Authorization header (BrightData answers 401 "Credentials are missing")
        if (HiltNetworkModule.BRIGHT_DATA_TOKEN.isBlank()) {
            RemoteConfigManager.fetchAndActivate()
            HiltNetworkModule.loadKeysFromRemoteConfig()
        }

        return@withContext try {
            val response = requestApi.getAirbnbInfo(input=airBnbRequestModel)
            // ADD THIS LOG BLOCK
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val prettyJson = body.toJson(prettyPrint = true)
                    android.util.Log.d(
                        "BRIGHTDATA_RAW",
                        "SUCCESSFUL RESPONSE (pretty-printed):\n$prettyJson"
                    )
                } ?: run {
//                    android.util.Log.w("BRIGHTDATA_RAW", "Response successful but body is null")
                }
            } else {
                val errorBody = response.errorBody()?.string()
//                android.util.Log.e(
//                    "BRIGHTDATA_RAW",
//                    "API ERROR ${response.code()}: $errorBody"
//                )
            }
            // END OF LOG BLOCK
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //////// Notes
    suspend fun getNotes(uid:String):MutableList<NoteModel>{
        val snapshot=db.collection("notes")
            .whereEqualTo("userId", uid)
            .get()
            .await()
        return snapshot.documents
            .mapNotNull { doc ->
                val note = doc.toObject(NoteModel::class.java)
                note?.id = doc.id
                note
            }
            .sortedBy { it.createdAt }
            .toMutableList()
    }

    suspend fun getNote(id:String):NoteModel?{
        val doc=db.collection("notes")
            .document(id)
            .get()
            .await()
        return if (doc.exists()) {
            doc.toObject(NoteModel::class.java)
        } else {
            null
        }
    }

    suspend fun createNote(noteModel: NoteModel):NoteModel
    {
        val docRef=db.collection("notes").add(noteModel).await()
        noteModel.id=docRef.id
        return noteModel
    }

    suspend fun updateNote(noteModel: NoteModel):NoteModel{
        db.collection("notes").document(noteModel.id).set(noteModel).await()
        return noteModel
    }

    suspend fun deleteNote(id: String): Boolean {
        db.collection("notes")
            .document(id)
            .delete()
            .await()
        return true
    }

    suspend fun aiExtractCityGuide(airbnbJson: String): CityGuideModel = withContext(Dispatchers.IO) {
//        Log.d("GPT_CITY_START", "Starting CityGuide generation")
//        Log.d("GPT_CITY_INPUT", "Airbnb JSON length: ${airbnbJson.length}")
        val gson = Gson()
        val airbnbData = try {
            gson.fromJson(airbnbJson, AirBnbResponseModel::class.java)
        } catch (e: Exception) {
//            Log.e("GPT_CITY_PARSE", "Failed to parse Airbnb JSON", e)
            return@withContext CityGuideModel()
        }

        val cityName = airbnbData.city

        // === YOUR EXACT PROMPT ===
        val systemPrompt = """
You are a professional travel guide writer.
Your task is to write very short, clear, and practical travel advice (20–30 words maximum) for a guest visiting a city.
You will receive two inputs:
{city_name} – the name of the city.
{topic} – the specific travel topic (for example: “Best public transportation options”, “Recommended restaurants”, “Top places to visit”, etc.).
Your answer must:
Be short, direct, and informative (one or two short sentences, maximum 30 words).
Use simple and natural English.
Include up to 3–5 specific examples only when necessary (e.g., names of restaurants or attractions).
Avoid emojis, icons, and the “—” character.
Never exceed 40 words.
Example format:
Input:
City: Lisbon
Topic: Best public transportation options
Output:
Use trams, metro, or buses. Metro is fastest for long distances; trams are scenic for short trips. Avoid driving in the city center due to traffic and parking.
""".trimIndent()

        val topics = listOf(
            "Best public transportation options",
            "General information and guidance on using public transportation",
            "Approximate transportation costs (single ticket, daily pass, taxi, etc.)",
            "Typical store and restaurant opening hours",
            "Most useful mobile apps for getting around, food, and services",
            "Key do’s and don’ts for visitors(etiquette, behavior, customs)",
            "Top 5 places to visit",
            "Must-try local foods",
            "Recommended restaurants and cafes",
            "Popular shopping centers, markets, and streets for souvenirs in",
            "Important safety and cultural tips",
            "Safety and emergency tips"
        )

        val userPrompt = """
Input:
City: $cityName

Fill each topic below with a short answer (20–30 words max):
${topics.joinToString("\n") { "Topic: $it" }}
""".trimIndent()

//        Log.d("GPT_CITY_PROMPT", "System prompt sent (truncated):\n${systemPrompt.take(500)}...")
//        Log.d("GPT_CITY_PROMPT", "User prompt sent (truncated):\n${userPrompt.take(500)}...")

        val params = ChatCompletionCreateParams.builder()
            .model(ChatModel.GPT_5_MINI)
            .addSystemMessage(systemPrompt)
            .addUserMessage(userPrompt)
            .build()

        return@withContext try {
//            Log.d("GPT_CITY_CALL", "Calling GPT...")
            val response = openAIClient.chat().completions().create(params)
            var text = response.choices().firstOrNull()?.message()?.content().toString()

            // Remove Optional[...]
            if (text.startsWith("Optional[")) {
                text = text.substring(9, text.length - 1)
            }
//            Log.d("GPT_CITY_RAW", "GPT RESPONSE (RAW):\n$text")

            val cityGuide = CityGuideModel()

            // Split by "Topic:" and map to fields
            val topicMap = mapOf(
                "Best public transportation options" to { s: String -> cityGuide.bestPublicTransportation = s },
                "General information and guidance on using public transportation" to { s: String -> cityGuide.transportationGuidance = s },
                "Approximate transportation costs" to { s: String -> cityGuide.transportationCosts = s },
                "Typical store and restaurant opening hours" to { s: String -> cityGuide.typicalStoreHours = s },
                "Most useful mobile apps" to { s: String -> cityGuide.usefulCityApps = s },
                "Key do’s and don’ts" to { s: String -> cityGuide.dosAndDonts = s },
                "Top 5 places to visit" to { s: String -> cityGuide.topPlacesToVisit = s },
                "Must-try local foods" to { s: String -> cityGuide.mustTryFoods = s },
                "Recommended restaurants and cafes" to { s: String -> cityGuide.recommendedRestaurants = s },
                "Popular shopping centers" to { s: String -> cityGuide.popularShopping = s },
                "Important safety and cultural tips" to { s: String -> cityGuide.importantTips = s },
                "Safety and emergency tips" to { s: String -> cityGuide.safetyEmergencyTips = s }
            )

            var currentTopic = ""
            val lines = text.lines()
            for (line in lines) {
                if (line.startsWith("Topic:")) {
                    currentTopic = line.removePrefix("Topic:").trim()
//                    Log.d("GPT_CITY_TOPIC", "Found topic: $currentTopic")
                } else if (currentTopic.isNotEmpty() && line.isNotBlank()) {
                    val cleanAnswer = line.trim()
//                    Log.d("GPT_CITY_ANSWER", "→ $currentTopic: $cleanAnswer")
                    topicMap.entries.find { currentTopic.contains(it.key, ignoreCase = true) }?.value?.invoke(cleanAnswer)
                    currentTopic = "" // Reset after assigning
                }
            }

            // Keep blank
            cityGuide.personalRecommendations = ""
            cityGuide.uniqueExperiences = ""

//            Log.d("GPT_CITY_SUCCESS", "CityGuide fully built")

            return@withContext cityGuide
        } catch (e: Exception) {
//            Log.e("GPT_CITY_ERROR", "GPT CALL OR PARSING FAILED", e)
            CityGuideModel()
        }
    }


    suspend fun aiExtractHomeGuide(airbnbJson: String): HomeGuideModel = withContext(Dispatchers.IO) {
//        Log.d("GPT_HOME_START", "Starting HomeGuide generation")

        val gson = Gson()
        val airbnbData = try {
            gson.fromJson(airbnbJson, AirBnbResponseModel::class.java)
        } catch (e: Exception) {
//            Log.e("GPT_HOME_PARSE", "Failed to parse Airbnb JSON", e)
            return@withContext HomeGuideModel()
        }

        val cityName = airbnbData.city
        val address = airbnbData.location ?: ""
        val lat = airbnbData.lat
        val lng = airbnbData.long

        val systemPrompt = """
You are a local host writing a practical home guide. Use Airbnb JSON + Google Maps to fill ONLY these fields.
Answer in clear, human, step-by-step English (under 30 words per field).
Include landmarks, turns, walking tips, price and duration.
Output ONLY valid JSON. No explanations.

Fields:
{
  "propertyAddress": "",
  "propertyAddressLocal": "",
  "howToReachAirport": "From main airport: taxi, bus, metro, walk (if <15 min)",
  "howToReachTrain": "From main train station: taxi, bus, metro, walk",
  "howToReachCar": "Shortest driving route from city entrance",
  "howToFind": "Landmarks to spot the building",
  "howToGetIn": "How to enter (door, key, code)",
  "entranceCode": "Door code or key location",
  "checkInWindow": "Best check-in time",
  "additionalAccess": "Extra keys, garage, storage",
  "floor": "Floor number",
  "stairsElevator": "Stairs or elevator",
  "parking": "On Street / Lot / None",
  "parkingComments": "Parking tips",
  "wifiPassword": "Wi-Fi password",
  "propertyRules": "House rules",
  "smokingRules": "Smoking allowed?",
  "petsPolicy": "Pets allowed?",
  "quietHours": "Quiet hours",
  "lockoutPolicy": "Lockout policy",
  "checkOutWindow": "Best check-out time",
  "departureChecklist": "Before leaving",
  "whereToLeaveKeys": "Leave keys here",
  "facilities": "Main facilities",
  "amenities": "Key amenities",
  "firstAid": "First aid location",
  "wasteSorting": "Trash sorting",
  "extraLinens": "Extra sheets",
  "towels": "Towel policy",
  "cleaningTools": "Cleaning supplies",
  "hairDryer": "Hair dryer",
  "acFans": "AC or fans",
  "washerDryer": "Washer/dryer",
  "kettleCoffee": "Kettle or coffee maker",
  "bbqFireplace": "BBQ or fireplace",
  "hotWater": "Hot water",
  "fireEscape": "Fire escape route",
  "nearestHospital": "Nearest hospital",
  "pharmacy": "24h pharmacy",
  "groceries": "Supermarket",
  "atms": "ATM",
  "simCards": "SIM card store",
  "parks": "Nearby park",
  "metro": "Nearest metro",
  "foodDelivery": "Delivery apps",
  "breakfast": "Breakfast spots",
  "lunchDinner": "Lunch/dinner",
  "lateNight": "Late-night food",
  "topAttractions": "Top 3 attractions",
  "emergencyContact": "Emergency number"
}
""".trimIndent()

        val userPrompt = """
City: $cityName
Address: $address
Lat/Lng: $lat, $lng
Airbnb JSON: $airbnbJson

Fill JSON above. Use Google Maps for neighborhood. Give step-by-step directions.
""".trimIndent()

//        Log.d("GPT_HOME_PROMPT", "User prompt (truncated):\n${userPrompt.take(500)}...")

        val params = ChatCompletionCreateParams.builder()
            .model(ChatModel.GPT_5_MINI)
            .addSystemMessage(systemPrompt)
            .addUserMessage(userPrompt)
            .build()

        return@withContext try {
//            Log.d("GPT_HOME_CALL", "Calling GPT...")
            val response = openAIClient.chat().completions().create(params)
            var jsonText = response.choices().firstOrNull()?.message()?.content().toString()

            // Remove Optional[...]
            if (jsonText.startsWith("Optional[")) {
                jsonText = jsonText.substring(9, jsonText.length - 1)
            }
//            Log.d("GPT_HOME_RAW", "GPT RESPONSE (RAW JSON):\n$jsonText")

            val extracted = gson.fromJson(jsonText, HomeGuideModel::class.java) ?: HomeGuideModel()

//            Log.d("GPT_HOME_PARSED", "Extracted ${extracted::class.simpleName} with ${extracted.propertyAddress?.length ?: 0} chars in address")

            HomeGuideModel().apply {
                googleMapsLink = lat?.let { l -> lng?.let { g -> "https://www.google.com/maps/search/?api=1&query=$l,$g" } }
                briefDescription = airbnbData.description

                // Log each field as it's copied
                fun logField(field: String?, name: String) {
//                    Log.d("GPT_HOME_FIELD", "$name: ${field?.take(100) ?: "null"}")
                }

                // Copy AI-extracted fields
                city = cityName
                propertyAddress = extracted.propertyAddress.also { logField(it, "propertyAddress") }
                propertyAddressLocal = extracted.propertyAddressLocal.also { logField(it, "propertyAddressLocal") }
                howToReachAirport = extracted.howToReachAirport.also { logField(it, "howToReachAirport") }
                howToReachTrain = extracted.howToReachTrain.also { logField(it, "howToReachTrain") }
                howToReachCar = extracted.howToReachCar.also { logField(it, "howToReachCar") }
                howToFind = extracted.howToFind.also { logField(it, "howToFind") }
                howToGetIn = extracted.howToGetIn.also { logField(it, "howToGetIn") }
                entranceCode = extracted.entranceCode.also { logField(it, "entranceCode") }
                checkInWindow = extracted.checkInWindow.also { logField(it, "checkInWindow") }
                additionalAccess = extracted.additionalAccess.also { logField(it, "additionalAccess") }
                floor = extracted.floor.also { logField(it, "floor") }
                stairsElevator = extracted.stairsElevator.also { logField(it, "stairsElevator") }
                parking = extracted.parking.also { logField(it, "parking") }
                parkingComments = extracted.parkingComments.also { logField(it, "parkingComments") }
                wifiPassword = extracted.wifiPassword.also { logField(it, "wifiPassword") }
                propertyRules = extracted.propertyRules.also { logField(it, "propertyRules") }
                smokingRules = extracted.smokingRules.also { logField(it, "smokingRules") }
                petsPolicy = extracted.petsPolicy.also { logField(it, "petsPolicy") }
                quietHours = extracted.quietHours.also { logField(it, "quietHours") }
                lockoutPolicy = extracted.lockoutPolicy.also { logField(it, "lockoutPolicy") }
                checkOutWindow = extracted.checkOutWindow.also { logField(it, "checkOutWindow") }
                departureChecklist = extracted.departureChecklist.also { logField(it, "departureChecklist") }
                whereToLeaveKeys = extracted.whereToLeaveKeys.also { logField(it, "whereToLeaveKeys") }
                facilities = extracted.facilities.also { logField(it, "facilities") }
                amenities = extracted.amenities.also { logField(it, "amenities") }
                firstAid = extracted.firstAid.also { logField(it, "firstAid") }
                wasteSorting = extracted.wasteSorting.also { logField(it, "wasteSorting") }
                extraLinens = extracted.extraLinens.also { logField(it, "extraLinens") }
                towels = extracted.towels.also { logField(it, "towels") }
                cleaningTools = extracted.cleaningTools.also { logField(it, "cleaningTools") }
                hairDryer = extracted.hairDryer.also { logField(it, "hairDryer") }
                acFans = extracted.acFans.also { logField(it, "acFans") }
                washerDryer = extracted.washerDryer.also { logField(it, "washerDryer") }
                kettleCoffee = extracted.kettleCoffee.also { logField(it, "kettleCoffee") }
                bbqFireplace = extracted.bbqFireplace.also { logField(it, "bbqFireplace") }
                hotWater = extracted.hotWater.also { logField(it, "hotWater") }
                fireEscape = extracted.fireEscape.also { logField(it, "fireEscape") }
                nearestHospital = extracted.nearestHospital.also { logField(it, "nearestHospital") }
                pharmacy = extracted.pharmacy.also { logField(it, "pharmacy") }
                groceries = extracted.groceries.also { logField(it, "groceries") }
                atms = extracted.atms.also { logField(it, "atms") }
                simCards = extracted.simCards.also { logField(it, "simCards") }
                parks = extracted.parks.also { logField(it, "parks") }
                metro = extracted.metro.also { logField(it, "metro") }
                foodDelivery = extracted.foodDelivery.also { logField(it, "foodDelivery") }
                breakfast = extracted.breakfast.also { logField(it, "breakfast") }
                lunchDinner = extracted.lunchDinner.also { logField(it, "lunchDinner") }
                lateNight = extracted.lateNight.also { logField(it, "lateNight") }
                topAttractions = extracted.topAttractions.also { logField(it, "topAttractions") }
                emergencyContact = extracted.emergencyContact?.let { raw ->
                    raw.replace(Regex("^\\s*Emergency numbers?[:\\s-]*", RegexOption.IGNORE_CASE), "")
                        .trim()
                        .takeIf { it.isNotBlank() } ?: "112"
                } ?: "112"
                logField(emergencyContact, "emergencyContact")
            }
        } catch (e: Exception) {
//            Log.e("GPT_HOME_ERROR", "GPT CALL OR PARSING FAILED", e)
            HomeGuideModel().apply { briefDescription = airbnbData.description?.take(200) }
        }
    }

//    private suspend fun extractNeighborhoodFromMaps(mapsLink: String): NeighborhoodInfo {
//        val systemPrompt = """
//You are a local guide. From this Google Maps link, list nearby places (within 1km) in short phrases.
//Format: "Pharmacy: Name1, Name2"
//Only include: pharmacy, groceries, ATMs, SIM stores, parks, metro stops, food delivery, breakfast, lunch/dinner, late-night, top attractions.
//Max 2 examples per type.
//""".trimIndent()
//
//        val userPrompt = "Map: $mapsLink"
//
//        val params = ChatCompletionCreateParams.builder()
//            .model(ChatModel.GPT_5_MINI)
//            .addSystemMessage(systemPrompt)
//            .addUserMessage(userPrompt)
//            .build()
//
//        return try {
//            val response = openAIClient.chat().completions().create(params)
//            val text = response.choices().firstOrNull()?.message()?.content() ?: ""
//            parseNeighborhood(text.toString())
//        } catch (e: Exception) {
//            NeighborhoodInfo()
//        }
//    }
//
//    private fun parseNeighborhood(text: String): NeighborhoodInfo {
//        val info = NeighborhoodInfo()
//        text.lines().forEach { line ->
//            when {
//                line.startsWith("Pharmacy:") -> info.pharmacy = line.removePrefix("Pharmacy:").trim()
//                line.startsWith("Groceries:") -> info.groceries = line.removePrefix("Groceries:").trim()
//                line.startsWith("ATMs:") -> info.atms = line.removePrefix("ATMs:").trim()
//                line.startsWith("SIM:") -> info.simCards = line.removePrefix("SIM:").trim()
//                line.startsWith("Parks:") -> info.parks = line.removePrefix("Parks:").trim()
//                line.startsWith("Metro:") -> info.metro = line.removePrefix("Metro:").trim()
//                line.startsWith("Delivery:") -> info.foodDelivery = line.removePrefix("Delivery:").trim()
//                line.startsWith("Breakfast:") -> info.breakfast = line.removePrefix("Breakfast:").trim()
//                line.startsWith("Lunch/Dinner:") -> info.lunchDinner = line.removePrefix("Lunch/Dinner:").trim()
//                line.startsWith("Late-night:") -> info.lateNight = line.removePrefix("Late-night:").trim()
//                line.startsWith("Attractions:") -> info.topAttractions = line.removePrefix("Attractions:").trim()
//            }
//        }
//        return info
//    }
//    private suspend fun getTransportRoutes(from: String, to: String): String {
//        val systemPrompt = """
//You are a transit expert. Give 3 short options from "$from" to "$to":
//1. Taxi (cost, time)
//2. Bus/Metro (route, stops)
//3. Walk (if under 15 min)
//Max 25 words total.
//""".trimIndent()
//
//        val userPrompt = "From: $from\nTo: $to"
//
//        val params = ChatCompletionCreateParams.builder()
//            .model(ChatModel.GPT_5_MINI)
//            .addSystemMessage(systemPrompt)
//            .addUserMessage(userPrompt)
//            .build()
//
//        return try {
//            val response = openAIClient.chat().completions().create(params)
//            response.choices().firstOrNull()?.message()?.content()?.toString() ?: ""
//        } catch (e: Exception) {
//            ""
//        }
//    }
//    private suspend fun getCarRoute(cityName: String, address: String): String {
//        val systemPrompt = """
//Shortest driving route from main city entrance to "$address" in $cityName.
//Include highway, turns, landmarks. Max 20 words.
//""".trimIndent()
//
//        val userPrompt = "City: $cityName\nAddress: $address"
//
//        val params = ChatCompletionCreateParams.builder()
//            .model(ChatModel.GPT_5_MINI)
//            .addSystemMessage(systemPrompt)
//            .addUserMessage(userPrompt)
//            .build()
//
//        return try {
//            val response = openAIClient.chat().completions().create(params)
//            response.choices().firstOrNull()?.message()?.content()?.toString() ?: ""
//        } catch (e: Exception) {
//            ""
//        }
//    }
//    private suspend fun getEmergencyContact(cityName: String): String {
//        val systemPrompt = """
//Emergency numbers for $cityName (police, fire, ambulance).
//""".trimIndent()
//
//        val userPrompt = "City: $cityName"
//
//        val params = ChatCompletionCreateParams.builder()
//            .model(ChatModel.GPT_5_MINI)
//            .addSystemMessage(systemPrompt)
//            .addUserMessage(userPrompt)
//            .build()
//
//        return try {
//            val response = openAIClient.chat().completions().create(params)
//            response.choices().firstOrNull()?.message()?.content()?.toString() ?: "112"
//        } catch (e: Exception) {
//            "112"
//        }
//    }

    suspend fun aiApprovedHomePhoto(name:String,url:String):ApprovedModel= withContext(Dispatchers.IO){

        val percent=50
        val systemPrompt = """
        You are an Airbnb photo quality evaluator. Always respond in English.
        Analyze the provided image of a specific property area (e.g., kitchen, garden, bedroom, etc.).
        The image URL will be provided inside the user prompt.
        
        Your goal:
        Evaluate the photo based on three key aspects:
        1. **Lighting** — brightness, contrast, evenness, and use of natural light.
        2. **Angle** — camera position, framing, depth, and perspective.
        3. **Focus** — clarity, sharpness, and emphasis on important details.
        
        Output Rules:
        - Always respond **only** in valid JSON format.
        - JSON must include **all three sections** with their percentages:
        
        {
          "lighting": "",
          "lightingPercent": 0,
          "angle": "",
          "anglePercent": 0,
          "focus": "",
          "focusPercent": 0,
          "done": true or false
        }
        
        Evaluation Criteria:
        1. Evaluate each aspect independently, using consistent and objective rules.
        2. For each aspect:
           - Compute its actual quality percentage.
           - If the computed percentage ≥ $percent → 
               - set its Percent field **to 100**
           - If the computed percentage < $percent → 
               - provide a clear, concise tip (under 40 words) in the main field
               - set its Percent field to the computed percentage
        3. `"done": true` **only if all three aspects ≥ $percent**, otherwise `"done": false`.
        4. Be fair and not overly strict; minor imperfections should not excessively lower percentages.
        5. Always produce consistent, deterministic results for the same image.
        6. Do NOT include any text outside the JSON. No explanations or comments.
        
        When evaluating, consider:
        - Natural light should dominate over artificial lighting.
        - Shoot from corners or wide angles to make the space appear larger.
        - Include key details like decor, open space, and amenities.
        - Keep the area tidy and clean.
        - Avoid tilted or shaky shots.
        - Photos should look natural, authentic, and inviting.
        
        Always follow the JSON schema exactly.
        """.trimIndent()


        val userPrompt = """
        Analyze the following image of a $name: $url
        """.trimIndent()



        val params = ChatCompletionCreateParams.builder()
            .model(ChatModel.GPT_5_MINI)
            .addSystemMessage(systemPrompt)
            .addUserMessage(userPrompt)
            .build()

        val chatCompletion: ChatCompletion = openAIClient.chat().completions().create(params)
        var responseText = chatCompletion.choices().firstOrNull()?.message()?.content().toString()
        if (responseText.startsWith("Optional[")) {
            responseText = responseText.removePrefix("Optional[").removeSuffix("]")
        }

        val approvedModel: ApprovedModel = try {
            Gson().fromJson(responseText, ApprovedModel::class.java)
        } catch (e: JsonSyntaxException) {
            ApprovedModel()
        }

        return@withContext approvedModel.copy(
            lighting = approvedModel.lighting,
            lightingPercent = approvedModel.lightingPercent,
            angle = approvedModel.angle,
            anglePercent = approvedModel.anglePercent,
            focus = approvedModel.focus,
            focusPercent = approvedModel.focusPercent,
            done = approvedModel.done
        )

    }
    //////// SharedPref
    fun saveDataSharedPref(key: String, value: String)
    {
        sharedPreferences.edit {
            putString(key, value)
        }
    }

    fun getDataSharedPref(key: String): String {
        return sharedPreferences.getString(key, "").toString()
    }

    //LogOut
    fun logout(){
        sharedPreferences.edit { clear() }
    }

    /*suspend fun aiExtractCityGuide(
        airbnbJson: String
    ): CityGuideModel = withContext(Dispatchers.IO) {

        val systemPrompt = """
            You are a travel information extractor. Always respond in English.
            Your task is to extract information strictly from the provided Airbnb JSON.
            Do NOT add, modify, or invent information for fields that already have data in the JSON.
            Only for fields that are missing or null, generate up to 1–2 short realistic examples based on world knowledge.
            Keep each field concise (ideally under 30 words). Output must strictly follow the JSON schema below. Do NOT include explanations, comments, or extra text.

            Special rules:
            1. For the field "usefulLinks", generate exactly one Google Maps link using only the latitude and longitude from the JSON. Do NOT include any other links.
            2. For all other fields, if the JSON does not provide information, try to **infer or estimate** the value logically from the related fields in the JSON (e.g., city, address, description, amenities, host info).
               Do NOT invent unrelated information; only base your inference on the JSON content.
            3. If no inference is possible, generate up to 1–2 short realistic examples.

            Example of how output should look:

            {
              "cityOrAreaName": "Rome, Italy",
              "zipCode": "00184",
              "bestPublicTransportation": "Metro, bus lines 64, 70",
              "typicalStoreHours": "Most shops open 10 AM - 8 PM",
              "topPlacesToVisit": "Colosseum, Vatican Museums, Pantheon",
              "mustTryFoods": "Pizza al taglio, gelato, carbonara",
              "recommendedRestaurants": "Roscioli, Pizzarium, Da Enzo al 29",
              "importantTips": "Carry cash, beware pickpockets, tip optional",
              "emergencyContacts": "112 - police and ambulance",
              "personalRecommendations": "Walk Trastevere at night, enjoy sunset from Pincio terrace",
              "uniqueExperiences": "Attend a papal audience, take a Vespa tour",
              "usefulLinks": "https://www.google.com/maps/search/?api=1&query=41.9028,12.4964"
            }

            The JSON must contain the following fields exactly:

            {
              "cityOrAreaName": "",
              "zipCode": null,
              "bestPublicTransportation": "",
              "typicalStoreHours": "",
              "topPlacesToVisit": "",
              "mustTryFoods": "",
              "recommendedRestaurants": "",
              "importantTips": "",
              "emergencyContacts": "",
              "personalRecommendations": "",
              "uniqueExperiences": "",
              "usefulLinks": ""
            }
            """.trimIndent()

        val userPrompt = """
            Here is an Airbnb listing JSON. Extract travel info and fill every field in the JSON above strictly.
            Use the lat/lng values to generate exactly one Google Maps link for 'usefulLinks'.
            $airbnbJson
            """.trimIndent()

        val params = ChatCompletionCreateParams.builder()
            .model(ChatModel.GPT_5_MINI)
            .addSystemMessage(systemPrompt)
            .addUserMessage(userPrompt)
            .build()

        val chatCompletion: ChatCompletion = openAIClient.chat().completions().create(params)
        var responseText = chatCompletion.choices().first().message().content().toString()
        responseText = responseText.removePrefix("Optional[").removeSuffix("]")
        val cityGuide = runCatching {
            Gson().fromJson(responseText, CityGuideModel::class.java)
        }.getOrDefault(CityGuideModel())

        return@withContext cityGuide
    }*/

    //    //////// AI
//    suspend fun aiExtractCityGuide(
//        airbnbJson: String
//    ): CityGuideModel = withContext(Dispatchers.IO) {
//
////        val systemPrompt = """
////        You are a travel information extractor. Always respond in English.
////        Your task is to extract information strictly from the provided Airbnb JSON.
////        Do NOT add, modify, or invent information for fields that already have data in the JSON.
////        Only for fields that are missing or null, generate up to 1–2 short realistic examples based on world knowledge.
////        Keep each field concise (ideally under 30 words). Output must strictly follow the JSON schema below. Do NOT include explanations, comments, or extra text.
////
////        Special rules:
////        1. For the field "usefulLinks", generate exactly one Google Maps link using only the latitude and longitude from the JSON. Do NOT include any other links.
////        2. For all other fields, if the JSON does not provide information, try to **infer or estimate** the value logically from the related fields in the JSON (e.g., city, address, description, amenities, host info).
////           Do NOT invent unrelated information; only base your inference on the JSON content.
////        3. If no inference is possible, generate up to 1–2 short realistic examples.
////
////        Example of how output should look:
////
////        {
////          "cityOrAreaName": "Rome, Italy",
////          "zipCode": "00184",
////          "bestPublicTransportation": "Metro, bus lines 64, 70",
////          "typicalStoreHours": "Most shops open 10 AM - 8 PM",
////          "topPlacesToVisit": "Colosseum, Vatican Museums, Pantheon",
////          "mustTryFoods": "Pizza al taglio, gelato, carbonara",
////          "recommendedRestaurants": "Roscioli, Pizzarium, Da Enzo al 29",
////          "importantTips": "Carry cash, beware pickpockets, tip optional",
////          "emergencyContacts": "112 - police and ambulance",
////          "personalRecommendations": "Walk Trastevere at night, enjoy sunset from Pincio terrace",
////          "uniqueExperiences": "Attend a papal audience, take a Vespa tour",
////          "usefulLinks": "https://www.google.com/maps/search/?api=1&query=41.9028,12.4964"
////        }
////
////        The JSON must contain the following fields exactly:
////
////        {
////          "cityOrAreaName": "",
////          "zipCode": null,
////          "bestPublicTransportation": "",
////          "typicalStoreHours": "",
////          "topPlacesToVisit": "",
////          "mustTryFoods": "",
////          "recommendedRestaurants": "",
////          "importantTips": "",
////          "emergencyContacts": "",
////          "personalRecommendations": "",
////          "uniqueExperiences": "",
////          "usefulLinks": ""
////        }
////    """.trimIndent()
//        val systemPrompt = """
//You are a professional travel guide writer.
//Your task is to write very short, clear, and practical travel advice (20–30 words maximum) for a guest visiting a city.
//You will receive two inputs:
//{city_name} – the name of the city.
//{topic} – the specific travel topic (for example: “Best public transportation options”, “Recommended restaurants”, “Top places to visit”, etc.).
//Your answer must:
//Be short, direct, and informative (one or two short sentences, maximum 30 words).
//Use simple and natural English.
//Include up to 3–5 specific examples only when necessary (e.g., names of restaurants or attractions).
//Avoid emojis, icons, and the “—” character
//Never exceed 40 words.
//Do not fill or generate content for the fields `personalRecommendations` or `uniqueExperiences`.
//Example format:
//Input:
//City: Lisbon
//Topic: Best public transportation options
//Output:
//Use trams, metro, or buses. Metro is fastest for long distances; trams are scenic for short trips. Avoid driving in the city center due to traffic and parking.
//""".trimIndent()
//
//
////        val userPrompt = """
////        Here is an Airbnb listing JSON. Extract travel info and fill every field in the JSON above strictly.
////        Use the lat/lng values to generate exactly one Google Maps link for 'usefulLinks'.
////        $airbnbJson
////    """.trimIndent()
//
//        val userPrompt = """
//Here is an Airbnb listing JSON. Extract travel info and fill each field with short, practical advice (20–30 words max).
//$airbnbJson
//""".trimIndent()
//
//        val params = ChatCompletionCreateParams.builder()
//            .model(ChatModel.GPT_5_MINI)
//            .addSystemMessage(systemPrompt)
//            .addUserMessage(userPrompt)
//            .build()
//
//        val chatCompletion: ChatCompletion = openAIClient.chat().completions().create(params)
//        var responseText = chatCompletion.choices().firstOrNull()?.message()?.content().toString()
//        if (responseText.startsWith("Optional[")) {
//            responseText = responseText.removePrefix("Optional[").removeSuffix("]")
//        }
//
//        val cityGuide: CityGuideModel = try {
//            Gson().fromJson(responseText, CityGuideModel::class.java)
//        } catch (e: JsonSyntaxException) {
//            CityGuideModel()
//        }
//
//        return@withContext cityGuide.copy(
//            bestPublicTransportation = cityGuide.bestPublicTransportation ?: "",
//            transportationGuidance = cityGuide.transportationGuidance ?: "",
//            transportationCosts = cityGuide.transportationCosts ?: "",
//            typicalStoreHours = cityGuide.typicalStoreHours ?: "",
//            usefulCityApps = cityGuide.usefulCityApps ?: "",
//            dosAndDonts = cityGuide.dosAndDonts ?: "",
//            mustTryFoods = cityGuide.mustTryFoods ?: "",
//            recommendedRestaurants = cityGuide.recommendedRestaurants ?: "",
//            popularShopping = cityGuide.popularShopping ?: "",
//            topPlacesToVisit = cityGuide.topPlacesToVisit ?: "",
//            importantTips = cityGuide.importantTips ?: "",
//            personalRecommendations = cityGuide.personalRecommendations ?: "",
//            uniqueExperiences = cityGuide.uniqueExperiences ?: "",
//            safetyEmergencyTips = cityGuide.safetyEmergencyTips ?: ""
//        )
//    }

    suspend fun analyzeReviews(reviews: List<AirBnbResponseModel.ReviewDetail>): String = withContext(Dispatchers.IO) {
//        Log.d("REVANL", "analyzeReviews() started with ${reviews.size} reviews")
        val reviewText = reviews.joinToString("\n\n") { r ->
            buildString {
                append("Guest: ${r.guestName ?: "Unknown"}\n")
                append("Rating: ${r.rating ?: "N/A"}\n")
                append("Date: ${r.reviewDate ?: "N/A"}\n")
                append("Stayed: ${r.stayedFor ?: "N/A"}\n")
                append("On Airbnb: ${r.guestTimeOnAirbnb ?: "N/A"}\n")
                append("Review: ${r.reviewText}\n")
                if (!r.hostResponse.isNullOrBlank()) append("Host: $r.hostResponse")
            }
        }

        val systemPrompt = """
You are an Airbnb review analyst helping hosts improve their listings based on guest feedback.
You will receive several guest reviews for a single property (or room).
Your task:
1. Read all reviews carefully and thoroughly.
2. Identify issues or areas for improvement mentioned by guests.
3. Pay attention to review dates — give more weight to newer reviews.
4. If an earlier review mentioned a problem but a newer one says it’s fixed, mention it as: “If this issue still exists…”
5. If more than one guest mentioned the same issue, clearly state how many times it was reported.
6. List recommendations from most important to least important, and from easiest to fix to harder ones.
7. Write your recommendations in short, clear, and practical sentences that tell the host exactly:
   * What was the problem?
   * How to fix or improve it.
8. Focus on practical, actionable advice, not just summaries.

Output format:
* A short introductory sentence (optional).
* Then a numbered list of recommendations, written in simple and natural English.
* Be clear, polite, and helpful.
* Keep it concise and easy to read.
* Do not use emojis, icons, or the “—” character.

Example phrasing:
* “Several guests mentioned that the apartment felt a bit dark. Consider adding a floor lamp or brighter bulbs.”
* “If the elevator is still unavailable, please make sure to mention this clearly in your listing description.”
""".trimIndent()

        val userPrompt = "Here are the guest reviews:\n\n$reviewText"
//        Log.d("REVANL", "Sending to GPT...")
        val params = ChatCompletionCreateParams.builder()
            .model(ChatModel.GPT_5_MINI)
            .addSystemMessage(systemPrompt)
            .addUserMessage(userPrompt)
            .build()

        return@withContext try {
            val response = openAIClient.chat().completions().create(params)
            var text = response.choices().firstOrNull()?.message()?.content().toString()
//            Log.d("REVANL", "GPT SUCCESS: $text")
            if (text.startsWith("Optional[")) {
                text = text.substring(9, text.length - 1)
            }
            text.trim()
        } catch (e: Exception) {
//            Log.e("AI", "Analysis failed", e)
            "Analysis failed."
        }
    }

    suspend fun updateReviewAnalysis(homeId: String, analysis: String) {
//        Log.d("REVANL", "Firestore updateReviewAnalysis() called for $homeId")
        try {
            db.collection("homes")
                .document(homeId)
                .update("reviewAnalysis", analysis)
                .await()
//            Log.d("REVANL", "Firestore update SUCCESS")
        } catch (e: Exception) {
//            Log.e("REVANL", "Firestore update FAILED", e)
            throw e
        }
    }

    suspend fun generateAIAnalysisPrompt(homeId: String): String = withContext(Dispatchers.IO) {
//        Log.d("AI_REPO", "generateAIAnalysisPrompt() STARTED for homeId: $homeId")

        val home = getHome(homeId)
        if (home == null) {
//            Log.w("AI_REPO", "No home found for ID: $homeId")
            return@withContext "No home data"
        }
//        Log.d("AI_REPO", "Home loaded: ${home.id}")

        val airbnb = home.airBnbResponseModel
        if (airbnb == null) {
//            Log.w("AI_REPO", "No Airbnb data in home")
            return@withContext "No Airbnb data"
        }
//        Log.d("AI_REPO", "Airbnb data loaded. Name: ${airbnb.name}, Reviews: ${airbnb.reviews?.size ?: 0}")
        val systemPrompt = """
You are an expert Airbnb listing advisor and travel copywriter.
You will receive:
The current text and information from an Airbnb listing (title, description, amenities, etc.)
The photos used on the listing
The guest reviews for the same property
Your task:
Carefully review all information, photos, and reviews.
Evaluate if the text is:
Accurate and complete enough for guests
Straightforward, friendly, and easy to read
Attractive and persuasive (encourages guests to book)
Check if all essential details are mentioned (e.g., views from windows, lighting, location highlights, kitchen features, etc.).
If something important is missing, clearly say it should be added.
Analyze the photos:
Are they clear, bright, and attractive?
Do they show essential spaces, such as kitchen and bedroom windows?
Do they match what’s written in the description?
Analyze the reviews:
Identify what guests liked or found memorable.
Suggest ways to include those positive points naturally in the listing text (for example: “Guests love the morning sunlight in the living room.”).
Provide specific, practical, and short suggestions to make the listing more appealing and trustworthy.
Avoid long or vague explanations.
Be clear, direct, and easy to follow.
Use simple examples when helpful.
Output format:
A short introductory line (optional)
Then a numbered list of improvement suggestions.
Each suggestion should be concise, action-oriented, and valuable.
Do not use emojis, icons, or the “—” character.
""".trimIndent()

//        Log.d("AI_REPO", "System prompt ready (length: ${systemPrompt.length})")

        val userPrompt = """        
Listing Data:
Name: ${airbnb.name}
Title: ${airbnb.listingTitle}
Listing Name: ${airbnb.listingName}
Description: ${airbnb.description}
Sections: ${airbnb.descriptionBySections?.joinToString("\n") { it.title + ": " + it.value }}
Ratings: ${airbnb.ratings}
Details: ${airbnb.details?.joinToString(", ")}
Highlights: ${airbnb.highlights?.joinToString("\n") { it.name + ": " + it.value }}
Category Ratings: ${airbnb.categoryRating?.joinToString("\n") { it.name + ": " + it.value }}
Reviews: ${airbnb.reviews?.joinToString("\n\n")}
Photos: ${airbnb.images?.size ?: 0} images available.
""".trimIndent()
//        Log.d("AI_REPO", "User prompt built (length: ${userPrompt.length})")
        return@withContext try {
//            Log.d("AI_REPO", "Calling GPT...")
            val params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_5_MINI)
                .addSystemMessage(systemPrompt)
                .addUserMessage(userPrompt)
                .build()

            val response = openAIClient.chat().completions().create(params)
            var text = response.choices().firstOrNull()?.message()?.content().toString()

            if (text.startsWith("Optional[")) {
                text = text.substring(9, text.length - 1)
            }
//            Log.d("AI_REPO", "FINAL ANALYSIS: $text")
//            Log.d("AI_REPO", "generateAIAnalysisPrompt() SUCCESS")
            text.trim()


        } catch (e: Exception) {
//            Log.e("GPT", "Analysis failed", e)
            "Analysis failed."
        }
    }
    suspend fun updateAIListingAnalysis(homeId: String, analysis: String) {
        db.collection("homes")
            .document(homeId)
            .update("aiListingAnalysis", analysis)
            .await()
    }

}