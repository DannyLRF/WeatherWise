package com.example.weatherwise.ui.dashboard

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.weatherwise.GeoResponse
import com.example.weatherwise.R
import com.example.weatherwise.RetrofitClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * ViewModel for managing city-related data in the weather app.
 * It handles loading weather info for the current location, default cities, and search queries.
 */
class CityViewModel(application: Application, private val userId: String) : AndroidViewModel(application) {

    // OpenWeatherMap API key
    private val apiKey = "79a25643aff43a2dbd3f03165be96f1a"

    // The list of currently added cities (displayed on screen)
    private val cityDao = WeatherDatabase.getDatabase(application).cityDao()
    val cityList = cityDao.getCities(userId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // The text currently entered in the search bar
    var searchText by mutableStateOf("")

    init {
        val context = getApplication<Application>().applicationContext

        viewModelScope.launch {
            val location = getDeviceLocation(context)
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude

                val name = fetchCityName(lat, lon)
                val weather = RetrofitClient.instance.getCurrentWeather(lat, lon, apiKey)
                val iconRes = getIconRes(weather.weather[0].main)

                val currentLocationCity = CityEntity(
                    userId = userId,
                    name = name,
                    description = "My Location",
                    temperature = weather.main.temp.toInt(),
                    iconResId = iconRes,
                    lat = lat,
                    lon = lon
                )

                // 插入当前城市（若不存在）
                val exists = cityDao.getCurrentLocationCity(userId) != null
                if (!exists) {
                    cityDao.insert(currentLocationCity)
                }
            } else {
                // fallback
                val unknownCity = CityEntity(
                    userId = userId,
                    name = "My Location",
                    description = "Unavailable",
                    temperature = 0,
                    iconResId = R.drawable.unknown,
                    lat = 0.0,
                    lon = 0.0
                )
                cityDao.insert(unknownCity)
            }
        }
    }

    // List of search results from the Geo API
    var searchResults by mutableStateOf<List<GeoResponseItem>>(emptyList())
        private set

    fun removeCity(city: CityEntity) {
        viewModelScope.launch {
            cityDao.delete(city)
        }
    }

    /**
     * Search for cities using the user's query via the Geo API.
     */
    fun searchCity(query: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.geoApi.getCoordinatesByCity(query, 5, apiKey)
                searchResults = response
            } catch (e: Exception) {
                e.printStackTrace()
                searchResults = emptyList()
            }
        }
    }

    /**
     * Fetch weather info for a city and add it to the list.
     */
    fun fetchCityInfoAndAdd(cityName: String) {
        viewModelScope.launch {
            val city = getCityInfoFromApi(cityName)
            if (city != null) addCity(city)
        }
    }

    /**
     * Clear the current search result list.
     */
    fun clearSearchResults() {
        searchResults = emptyList()
    }

    /**
     * Update the search input value.
     */
    fun onSearchTextChange(newText: String) {
        searchText = newText
    }

    /**
     * Add a new city to the city list if it's not already present.
     */
    fun addCity(city: CityInfo) {
        viewModelScope.launch {
            val entity = CityEntity(
                userId = userId,
                name = city.name,
                description = city.description,
                temperature = city.temperature,
                iconResId = city.iconResId,
                lat = city.lat,
                lon = city.lon
            )
            cityDao.insert(entity)
        }
    }

    /**
     * Get weather and coordinate information for a city by name.
     * Returns a CityInfo object or null if failed.
     */
    private suspend fun getCityInfoFromApi(cityName: String): CityInfo? {
        return try {
            val geoResponse = RetrofitClient.geoApi.getCoordinatesByCity(cityName, 1, apiKey)
            if (geoResponse.isEmpty()) return null

            val lat = geoResponse[0].lat
            val lon = geoResponse[0].lon
            val stateOrCountry = geoResponse[0].state ?: geoResponse[0].country

            val weather = RetrofitClient.instance.getCurrentWeather(lat, lon, apiKey)
            val description = weather.weather[0].main
            val iconResId = getIconRes(description)

            CityInfo(
                name = cityName,
                description = stateOrCountry,
                temperature = weather.main.temp.toInt(),
                iconResId = iconResId,
                lat = lat,
                lon = lon
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Use reverse geocoding API to get the city name from latitude and longitude.
     */
    private suspend fun fetchCityName(lat: Double, lon: Double): String {
        val result = RetrofitClient.instance.reverseGeocoding(lat, lon, 1, apiKey)
        return result.firstOrNull()?.name ?: "Unknown"
    }

    /**
     * Maps weather condition keywords (e.g., "Clear", "Rain") to icon resource IDs.
     */
    private fun getIconRes(description: String): Int {
        val iconMap = mapOf(
            "Clear" to R.drawable.sun_icon,
            "Clouds" to R.drawable.cloudy_icon,
            "Rain" to R.drawable.rain_icon,
            "Snow" to R.drawable.snow_icon,
            "Thunderstorm" to R.drawable.thunder_icon,
            "Drizzle" to R.drawable.rain_icon,
            "Mist" to R.drawable.cloudy_icon,
            "Night" to R.drawable.night_icon
        )
        return iconMap[description] ?: R.drawable.unknown
    }

    /**
     * Get the device’s current location using Google Play Services.
     * Fallbacks to last known location, then active location request.
     */
    @SuppressLint("MissingPermission") // Caller is expected to handle permission check
    private suspend fun getDeviceLocation(context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            val fused = LocationServices.getFusedLocationProviderClient(context)

            // Try to get last known location first
            fused.lastLocation.addOnSuccessListener { last ->
                if (last != null) {
                    cont.resume(last)
                } else {
                    // If unavailable, request a new location fix
                    fused.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { loc -> cont.resume(loc) }
                        .addOnFailureListener { cont.resume(null) }
                }
            }.addOnFailureListener { cont.resume(null) }
        }
}

class CityViewModelFactory(
    private val application: Application,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CityViewModel(application, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}