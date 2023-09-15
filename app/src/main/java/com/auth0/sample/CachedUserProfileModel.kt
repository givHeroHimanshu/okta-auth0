package com.auth0.sample

data class CachedUserProfileModel(
    val userName: String,
    val userNickName: String,
    val userImageUrl: String,
    val userEmail: String,
    val userAuth0UserId: String,
    val userCompany: String,
    val userDepartment: String,
    val userEmployeeId: String,
    val userLocation: String,
    val userCity: String,
    val userCountry: String,
)