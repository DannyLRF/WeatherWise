package com.example.weatherwise

data class WeatherInfo(
    val city: String,
    val description: String,
    val temperature: Double,
    val iconResId: Int
)