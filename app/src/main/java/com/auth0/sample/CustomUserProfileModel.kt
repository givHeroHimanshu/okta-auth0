package com.auth0.sample

class CustomUserProfileModel : ArrayList<CustomUserProfileModel.CustomUserProfileModelItem>(){
    data class CustomUserProfileModelItem(
        val appMetadata: Map<String, Any>?,
        val authenticationmethod: String,
        val authnmethodsreferences: String,
        val companyname: String,
        val createdAt: String,
        val email: String,
        val employeeId: String,
        val familyName: String,
        val givenName: String,
        val identities: List<Identity>,
        val identityprovider: String,
        val issuer: String,
        val lastIp: String,
        val lastLogin: String,
        val loginsCount: Int,
        val name: String,
        val nameIdAttributes: NameIdAttributes,
        val nickname: String,
        val objectidentifier: String,
        val picture: String,
        val sessionIndex: String,
        val tenantid: String,
        val updatedAt: String,
        val userId: String,
        val userMetadata: Map<String, Any>?
    ) {
    
        data class Identity(
            val connection: String,
            val isSocial: Boolean,
            val provider: String,
            val userId: String
        )
    
        data class NameIdAttributes(
            val format: String,
            val value: String
        )
    }
}