package com.marvelous.betterrank.model


import com.google.gson.annotations.SerializedName

internal data class ArrangementDetail(
    @SerializedName("name")
    var name: String,
    @SerializedName("value")
    var value: String
)