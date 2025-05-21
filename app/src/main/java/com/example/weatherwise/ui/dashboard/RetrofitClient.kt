package com.example.weatherwise

import com.example.weatherwise.ui.dashboard.GeocodingApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Base URL for OpenWeatherMap API
    private const val BASE_URL = "https://api.openweathermap.org/"

    // Lazily initialized Retrofit instance for weather-related API calls
    val instance: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    // Lazily initialized Retrofit instance for geocoding-related API calls
    val geoApi: GeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingApi::class.java)
    }
}
