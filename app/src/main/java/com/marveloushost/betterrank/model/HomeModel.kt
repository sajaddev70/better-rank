package com.marveloushost.betterrank.model

import java.io.Serializable

data class HomeModel(
    var id: String = "",
    var userId: String="",
    var airBnbResponseModel: AirBnbResponseModel? = AirBnbResponseModel(),
    var createdAt: Long? = System.currentTimeMillis(),
    var cityGuide: CityGuideModel? = null,
    var homeGuide: HomeGuideModel? = null,
    var aiListingAnalysis: String? = null,      // ← For generateAIAnalysisPrompt
    var reviewAnalysis: String? = null,
) : Serializable