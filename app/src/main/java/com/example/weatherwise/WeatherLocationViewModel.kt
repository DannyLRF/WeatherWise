package com.example.weatherwise

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class WeatherLocationsViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    var weatherList by mutableStateOf<List<WeatherInfo>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val cityList = listOf("Clayton", "Changsha", "London", "Amsterdam", "Alaska", "New York")

    fun loadWeatherData(units: String = "metric") {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val results = cityList.map { city -> repository.getWeather(city, units) }
                weatherList = results
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unexpected error occurred"
            } finally {
                isLoading = false
            }
        }
    }
}
