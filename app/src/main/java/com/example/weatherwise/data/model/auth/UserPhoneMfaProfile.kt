package com.example.weatherwise.data.model.auth

data class UserPhoneMfaProfile(
    val userId: String = "",
    val isPhoneMfaEnabled: Boolean = false,
    val phoneNumber: String? = null
)