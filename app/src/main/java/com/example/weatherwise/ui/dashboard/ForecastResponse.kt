package com.example.weatherwise

data class ForecastResponse(
    val list: List<ForecastItem>
)

data class ForecastItem(
    val dt_txt: String,
    val main: ForecastMain,
    val weather: List<ForecastWeather>,
    val wind: ForecastWind
)

data class ForecastMain(
    val temp: Double,
    val humidity: Int
)

data class ForecastWeather(
    val main: String,
    val description: String
)

data class ForecastWind(
    val speed: Double
)