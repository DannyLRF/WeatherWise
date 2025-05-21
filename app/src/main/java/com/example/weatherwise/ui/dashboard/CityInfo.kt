package com.example.weatherwise.ui.dashboard

data class CityInfo(
    val name: String,
    val description: String,
    val temperature: Int,
    val iconResId: Int,
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

