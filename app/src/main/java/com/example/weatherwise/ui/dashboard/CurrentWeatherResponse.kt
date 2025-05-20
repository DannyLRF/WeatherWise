package com.example.weatherwise

data class CurrentWeatherResponse(
    val main: Main,
    val weather: List<Weather>
) {
    data class Main(
        val temp: Double,
        val temp_min: Double,
        val temp_max: Double
    )

    data class Weather(
        val main: String,
        val icon: String
    )
}