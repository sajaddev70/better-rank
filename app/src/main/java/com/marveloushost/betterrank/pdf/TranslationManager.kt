package com.marveloushost.betterrank.translation

import android.icu.util.TimeUnit
import android.util.Log
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.marveloushost.betterrank.hilt.modual.HiltNetworkModule
import com.marveloushost.betterrank.model.CityGuideModel
import com.marveloushost.betterrank.model.HomeGuideModel
import com.marveloushost.betterrank.model.HomeModel
import com.marveloushost.betterrank.pdf.MutableSingleLiveEvent
import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletionCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TranslationManager {
    private val _showMessage = MutableSingleLiveEvent<String>()
    val showMessage: LiveData<String> = _showMessage

    private val openAIClient = OpenAIOkHttpClient.builder()
        .apiKey(HiltNetworkModule.GPT_API_KEY)
        .build()

    suspend fun translateHomeGuide(
        home: HomeModel,
        targetLang: String
    ): HomeModel = withContext(Dispatchers.IO) {
//        Log.d("TRANSLATE", "START → targetLang: \"$targetLang\"")

        val cityGuide = home.cityGuide ?: CityGuideModel()
        val homeGuide = home.homeGuide ?: HomeGuideModel()

        val textToTranslate = buildString {
            // City Guide
            appendLine(cityGuide.bestPublicTransportation)
            appendLine(cityGuide.transportationGuidance)
            appendLine(cityGuide.transportationCosts)
            appendLine(cityGuide.typicalStoreHours)
            appendLine(cityGuide.usefulCityApps)
            appendLine(cityGuide.dosAndDonts)
            appendLine(cityGuide.mustTryFoods)
            appendLine(cityGuide.recommendedRestaurants)
            appendLine(cityGuide.popularShopping)
            appendLine(cityGuide.topPlacesToVisit)
            appendLine(cityGuide.importantTips)
            appendLine(cityGuide.safetyEmergencyTips)

            // Home Guide
            appendLine(homeGuide.welcomeNote)
            appendLine(homeGuide.hostIntro)
            appendLine(homeGuide.briefDescription)
            appendLine(homeGuide.propertyAddress)
            appendLine(homeGuide.propertyAddressLocal)
            appendLine(homeGuide.howToReachAirport)
            appendLine(homeGuide.howToReachTrain)
            appendLine(homeGuide.howToReachCar)
            appendLine(homeGuide.howToFind)
            appendLine(homeGuide.howToGetIn)
            appendLine(homeGuide.entranceCode)
            appendLine(homeGuide.checkInWindow)
            appendLine(homeGuide.additionalAccess)
            appendLine(homeGuide.floor)
            appendLine(homeGuide.stairsElevator)
            appendLine(homeGuide.parking)
            appendLine(homeGuide.parkingComments)
            appendLine(homeGuide.wifiPassword)
            appendLine(homeGuide.propertyRules)
            appendLine(homeGuide.smokingRules)
            appendLine(homeGuide.petsPolicy)
            appendLine(homeGuide.quietHours)
            appendLine(homeGuide.lockoutPolicy)
            appendLine(homeGuide.checkOutWindow)
            appendLine(homeGuide.departureChecklist)
            appendLine(homeGuide.whereToLeaveKeys)
            appendLine(homeGuide.facilities)
            appendLine(homeGuide.amenities)
            appendLine(homeGuide.firstAid)
            appendLine(homeGuide.wasteSorting)
            appendLine(homeGuide.extraLinens)
            appendLine(homeGuide.towels)
            appendLine(homeGuide.cleaningTools)
            appendLine(homeGuide.hairDryer)
            appendLine(homeGuide.acFans)
            appendLine(homeGuide.washerDryer)
            appendLine(homeGuide.kettleCoffee)
            appendLine(homeGuide.bbqFireplace)
            appendLine(homeGuide.hotWater)
            appendLine(homeGuide.fireEscape)
            appendLine(homeGuide.nearestHospital)
            appendLine(homeGuide.pharmacy)
            appendLine(homeGuide.groceries)
            appendLine(homeGuide.atms)
            appendLine(homeGuide.simCards)
            appendLine(homeGuide.parks)
            appendLine(homeGuide.metro)
            appendLine(homeGuide.foodDelivery)
            appendLine(homeGuide.breakfast)
            appendLine(homeGuide.lunchDinner)
            appendLine(homeGuide.lateNight)
            appendLine(homeGuide.topAttractions)
        }.trim()

        if (textToTranslate.isBlank()) {
//            Log.w("TRANSLATE", "Text is blank → returning original")
            return@withContext home
        }

//        Log.d("TRANSLATE", "Text to translate: ${textToTranslate.lines().size} lines")

        val systemPrompt = """
You are a professional translator. Translate the following text to **$targetLang** only.
- Keep meaning, tone, and structure.
- Do not add, remove, or explain.
- Output only the translated text, one line per original line.
- Do not include any JSON, markdown, or formatting.
""".trimIndent()

        val userPrompt = textToTranslate

        val params = ChatCompletionCreateParams.builder()
            .model(ChatModel.GPT_5_MINI)
            .addSystemMessage(systemPrompt)
            .addUserMessage(userPrompt)
            .build()
//        Log.d("TRANSLATE", "GPT Request → model: gpt-5-mini")

        return@withContext try {
            val response = openAIClient.chat().completions().create(params)
            val startTime = System.currentTimeMillis()
            val endTime = System.currentTimeMillis()

//            Log.d("TRANSLATE", "GPT SUCCESS → ${endTime - startTime}ms")
//            Log.d("TRANSLATE", "Response: ${response.choices().firstOrNull()?.message()?.content()}")

            val translatedText = response.choices().firstOrNull()?.message()?.content()?.toString()
            val lines = translatedText?.lines()?.filter { it.isNotBlank() }
            var index = 0

            val translatedCity = cityGuide.copy(
                bestPublicTransportation = lines?.getOrNull(index++) ?: cityGuide.bestPublicTransportation.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                transportationGuidance = lines?.getOrNull(index++) ?: cityGuide.transportationGuidance.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                transportationCosts = lines?.getOrNull(index++) ?: cityGuide.transportationCosts.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                typicalStoreHours = lines?.getOrNull(index++) ?: cityGuide.typicalStoreHours.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                usefulCityApps = lines?.getOrNull(index++) ?: cityGuide.usefulCityApps.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                dosAndDonts = lines?.getOrNull(index++) ?: cityGuide.dosAndDonts.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                mustTryFoods = lines?.getOrNull(index++) ?: cityGuide.mustTryFoods.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                recommendedRestaurants = lines?.getOrNull(index++) ?: cityGuide.recommendedRestaurants.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                popularShopping = lines?.getOrNull(index++) ?: cityGuide.popularShopping.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                topPlacesToVisit = lines?.getOrNull(index++) ?: cityGuide.topPlacesToVisit.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                importantTips = lines?.getOrNull(index++) ?: cityGuide.importantTips.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                safetyEmergencyTips = lines?.getOrNull(index++) ?: cityGuide.safetyEmergencyTips.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                }
            )

            val translatedHome = homeGuide.copy(
                welcomeNote = lines?.getOrNull(index++) ?: homeGuide.welcomeNote.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                hostIntro = lines?.getOrNull(index++) ?: homeGuide.hostIntro.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                briefDescription = lines?.getOrNull(index++) ?: homeGuide.briefDescription.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                propertyAddress = lines?.getOrNull(index++) ?: homeGuide.propertyAddress.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                propertyAddressLocal = lines?.getOrNull(index++) ?: homeGuide.propertyAddressLocal.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                howToReachAirport = lines?.getOrNull(index++) ?: homeGuide.howToReachAirport.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                howToReachTrain = lines?.getOrNull(index++) ?: homeGuide.howToReachTrain.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                howToReachCar = lines?.getOrNull(index++) ?: homeGuide.howToReachCar.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                howToFind = lines?.getOrNull(index++) ?: homeGuide.howToFind.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                howToGetIn = lines?.getOrNull(index++) ?: homeGuide.howToGetIn.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                entranceCode = lines?.getOrNull(index++) ?: homeGuide.entranceCode.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                checkInWindow = lines?.getOrNull(index++) ?: homeGuide.checkInWindow.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                additionalAccess = lines?.getOrNull(index++) ?: homeGuide.additionalAccess.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                floor = lines?.getOrNull(index++) ?: homeGuide.floor.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                stairsElevator = lines?.getOrNull(index++) ?: homeGuide.stairsElevator.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                parking = lines?.getOrNull(index++) ?: homeGuide.parking.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                parkingComments = lines?.getOrNull(index++) ?: homeGuide.parkingComments.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                wifiPassword = lines?.getOrNull(index++) ?: homeGuide.wifiPassword.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                propertyRules = lines?.getOrNull(index++) ?: homeGuide.propertyRules.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                smokingRules = lines?.getOrNull(index++) ?: homeGuide.smokingRules.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                petsPolicy = lines?.getOrNull(index++) ?: homeGuide.petsPolicy.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                quietHours = lines?.getOrNull(index++) ?: homeGuide.quietHours.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                lockoutPolicy = lines?.getOrNull(index++) ?: homeGuide.lockoutPolicy.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                checkOutWindow = lines?.getOrNull(index++) ?: homeGuide.checkOutWindow.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                departureChecklist = lines?.getOrNull(index++) ?: homeGuide.departureChecklist.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                whereToLeaveKeys = lines?.getOrNull(index++) ?: homeGuide.whereToLeaveKeys.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                facilities = lines?.getOrNull(index++) ?: homeGuide.facilities.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                amenities = lines?.getOrNull(index++) ?: homeGuide.amenities.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                firstAid = lines?.getOrNull(index++) ?: homeGuide.firstAid.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                wasteSorting = lines?.getOrNull(index++) ?: homeGuide.wasteSorting.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                extraLinens = lines?.getOrNull(index++) ?: homeGuide.extraLinens.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                towels = lines?.getOrNull(index++) ?: homeGuide.towels.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                cleaningTools = lines?.getOrNull(index++) ?: homeGuide.cleaningTools.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                hairDryer = lines?.getOrNull(index++) ?: homeGuide.hairDryer.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                acFans = lines?.getOrNull(index++) ?: homeGuide.acFans.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                washerDryer = lines?.getOrNull(index++) ?: homeGuide.washerDryer.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                kettleCoffee = lines?.getOrNull(index++) ?: homeGuide.kettleCoffee.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                bbqFireplace = lines?.getOrNull(index++) ?: homeGuide.bbqFireplace.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                hotWater = lines?.getOrNull(index++) ?: homeGuide.hotWater.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                fireEscape = lines?.getOrNull(index++) ?: homeGuide.fireEscape.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                nearestHospital = lines?.getOrNull(index++) ?: homeGuide.nearestHospital.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                pharmacy = lines?.getOrNull(index++) ?: homeGuide.pharmacy.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                groceries = lines?.getOrNull(index++) ?: homeGuide.groceries.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                atms = lines?.getOrNull(index++) ?: homeGuide.atms.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                simCards = lines?.getOrNull(index++) ?: homeGuide.simCards.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                parks = lines?.getOrNull(index++) ?: homeGuide.parks.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                metro = lines?.getOrNull(index++) ?: homeGuide.metro.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                foodDelivery = lines?.getOrNull(index++) ?: homeGuide.foodDelivery.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                breakfast = lines?.getOrNull(index++) ?: homeGuide.breakfast.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                lunchDinner = lines?.getOrNull(index++) ?: homeGuide.lunchDinner.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                lateNight = lines?.getOrNull(index++) ?: homeGuide.lateNight.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
                topAttractions = lines?.getOrNull(index++) ?: homeGuide.topAttractions.also {
//                    Log.w("TRANSLATE", "Line $index missing → using original")
                },
            )
//            Log.d("TRANSLATE", "Translation complete → returning updated model")
            home.copy(cityGuide = translatedCity, homeGuide = translatedHome)
        } catch (e: Exception) {
//            Log.e("TRANSLATE", "FAILED → ${e.javaClass.simpleName}: ${e.message}", e)
            home
        }
    }
}