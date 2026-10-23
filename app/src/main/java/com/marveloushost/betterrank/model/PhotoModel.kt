package com.marveloushost.betterrank.model

data class PhotoModel (
    var title:String="",
    var name:String="",
    var path:String?=null,
    var url:String?=null,
    var approved:Boolean=false,
    var isSelected: Boolean = false
)