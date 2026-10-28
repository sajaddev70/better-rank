package com.marvelous.betterrank.retrofit


import com.marvelous.betterrank.hilt.modual.HiltNetworkModule.Companion.BRIGHT_DATA_DATA_SET_ID
import com.marvelous.betterrank.hilt.modual.HiltNetworkModule.Companion.BRIGHT_DATA_URL
import com.marvelous.betterrank.model.AirBnbRequestModel
import com.marvelous.betterrank.model.AirBnbResponseModel
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface RequestApi {

    @GET("generate_204")
    suspend fun pingGoogle(): Response<Unit>

    @POST
    suspend fun getAirbnbInfo(@Url queueUrl: String=BRIGHT_DATA_URL,
                              @Query("dataset_id") datasetId: String=BRIGHT_DATA_DATA_SET_ID,
                              @Body input:AirBnbRequestModel): Response<AirBnbResponseModel>

}