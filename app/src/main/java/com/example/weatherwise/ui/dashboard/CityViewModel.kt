package com.example.weatherwise.ui.dashboard

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.weatherwise.R
import com.example.weatherwise.RetrofitClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class CityViewModel(application: Application) : AndroidViewModel(application) {
    private val apiKey = "79a25643aff43a2dbd3f03165be96f1a"

    var cityList by mutableStateOf(listOf<CityInfo>())
        private set

    var searchText by mutableStateOf("")

    init {
        val context = getApplication<Application>().applicationContext
        val defaultCities = listOf("")

        viewModelScope.launch {
            val cityInfos = mutableListOf<CityInfo>()

            // 1. My location
            val location = getDeviceLocation(context)
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude

                val name = fetchCityName(lat, lon)
                val weather = RetrofitClient.instance.getCurrentWeather(lat, lon, apiKey)
                val iconRes = getIconRes(weather.weather[0].main)

                cityInfos.add(
                    CityInfo(
                        name = name,
                        description = "My Location",
                        temperature = weather.main.temp.toInt(),
                        iconResId = iconRes
                    )
                )
            } else {
                cityInfos.add(
                    CityInfo("My location", "Unavailable", 0, R.drawable.unknown)
                )
            }

            // 2. Default cities
            defaultCities.mapNotNullTo(cityInfos) { getCityInfoFromApi(it) }

            cityList = cityInfos
        }
    }

    fun onSearchTextChange(newText: String) {
        searchText = newText
    }

    fun addCity(city: CityInfo) {
        if (cityList.none { it.name.equals(city.name, ignoreCase = true) }) {
            cityList = cityList + city
        }
    }

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

    private suspend fun fetchCityName(lat: Double, lon: Double): String {
        val result = RetrofitClient.instance.reverseGeocoding(lat, lon, 1, apiKey)
        return result.firstOrNull()?.name ?: "Unknown"
    }

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

    @SuppressLint("MissingPermission")
    private suspend fun getDeviceLocation(context: Context): Location? =
        suspendCancellableCoroutine { cont ->
            val fused = LocationServices.getFusedLocationProviderClient(context)

            fused.lastLocation.addOnSuccessListener { last ->
                if (last != null) {
                    cont.resume(last)
                } else {
                    fused.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { loc -> cont.resume(loc) }
                        .addOnFailureListener { cont.resume(null) }
                }
            }.addOnFailureListener { cont.resume(null) }
        }
}
