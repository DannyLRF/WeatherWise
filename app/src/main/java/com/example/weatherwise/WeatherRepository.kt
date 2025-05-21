package com.example.weatherwise

class WeatherRepository(private val api: WeatherApi) {
    suspend fun getWeather(city: String, units: String): WeatherInfo {
        val response = api.getWeatherByCity(city, YOUR_API_KEY, units)
        return WeatherInfo(
            city = response.name,
            description = response.weather.firstOrNull()?.description ?: "",
            temperature = response.main.temp,
            icon = response.weather.firstOrNull()?.icon ?: "01d"
        )
    }
}