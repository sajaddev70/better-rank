package com.marvelous.betterrank.model

data class UserModel(
    var uid: String? = null,
    var name: String? = null,
    var email: String? = null,
    var photoUrl: String? = null,
    var createdAt: Long = System.currentTimeMillis()
)