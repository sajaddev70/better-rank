package com.marvelous.betterrank.model

data class ApprovedModel(
    val lighting:String="No response from the AI. Please try again.",
    val lightingPercent:Float=0f,
    val angle:String="No response from the AI. Please try again.",
    val anglePercent:Float=0f,
    val focus:String="No response from the AI. Please try again.",
    val focusPercent:Float=0f,
    val done:Boolean=false
)
