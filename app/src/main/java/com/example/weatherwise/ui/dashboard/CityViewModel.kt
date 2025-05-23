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
 * ViewModel responsible for managing the list of cities and weather data
 * in the WeatherWise app.
 *
 * This ViewModel:
 * - Loads the current device location and stores it as "My Location" in the database (if not already present)
 * - Retrieves saved cities from the local Room database (via [CityDao])
 * - Handles city search using the OpenWeatherMap Geocoding API
 * - Fetches weather information for a searched city and stores it locally
 * - Manages UI-related states such as search text and search results
 *
 * @param application Application context, used for accessing system services (e.g., location)
 * @param userId Unique user ID to scope database entries to the current user
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
        // On ViewModel creation, attempt to fetch device's current location and weather.
        // Insert the result into the local database if not already saved.
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
     * Searches for cities matching the user input using the OpenWeatherMap Geocoding API.
     *
     * On success, updates the [searchResults] list.
     * On failure (e.g., network error), logs the exception and clears the results.
     *
     * @param query User input string to search for
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
     * Adds a new city to the local Room database for the current user.
     *
     * This function should be called after fetching weather and location data via [CityInfo].
     *
     * @param city The city data to insert into the database
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
     * Fetches weather and location information for the given city name using both the geocoding
     * and current weather endpoints.
     *
     * @param cityName The name of the city to fetch data for
     * @return A [CityInfo] object with populated fields, or `null` if the city was not found or an error occurred
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
     * Performs reverse geocoding to obtain a human-readable city name from geographic coordinates.
     *
     * @param lat Latitude of the location
     * @param lon Longitude of the location
     * @return The name of the city, or "Unknown" if not found
     */
    private suspend fun fetchCityName(lat: Double, lon: Double): String {
        val result = RetrofitClient.instance.reverseGeocoding(lat, lon, 1, apiKey)
        return result.firstOrNull()?.name ?: "Unknown"
    }

    /**
     * Maps a weather condition string (e.g., "Clear", "Rain") to the appropriate weather icon resource ID.
     *
     * @param description Weather condition from the API response
     * @return Drawable resource ID corresponding to the weather condition, or a fallback icon if unknown
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
     * Retrieves the device's current location using Google's FusedLocationProviderClient.
     *
     * If the last known location is unavailable, it requests a new high-accuracy location fix.
     *
     * @param context The application context
     * @return [Location] object or `null` if location access fails
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

/**
 * Factory class for constructing a [CityViewModel] with custom parameters.
 *
 * @property application The application context.
 * @property userId The ID of the current user.
 */
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