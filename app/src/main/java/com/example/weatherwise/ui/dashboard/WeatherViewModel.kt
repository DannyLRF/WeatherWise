package com.example.weatherwise

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location            // ← 正確的 Location
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices // ← Google Play Services
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume                    // ← resume 擴充函式
import java.time.LocalDate

/**
 * ViewModel responsible for loading and managing weather-related data.
 *
 * This ViewModel handles:
 * - Retrieving the user's current location
 * - Fetching current weather data, hourly forecast, and daily forecast from the OpenWeatherMap API
 * - Managing loading, error, and display states for the UI
 *
 * Exposes state holders using `mutableStateOf` to enable Compose reactivity.
 */
class WeatherViewModel : ViewModel() {

    /* ---------- UI 狀態 ---------- */

    private val _cityName = mutableStateOf("Loading...")
    val cityName: State<String> = _cityName

    private val _weatherData = mutableStateOf<WeatherData?>(null)
    val weatherData: State<WeatherData?> = _weatherData

    private val _hourlyList = mutableStateOf<List<HourlyWeather>>(emptyList())
    val hourlyList: State<List<HourlyWeather>> = _hourlyList

    private val _dailyList = mutableStateOf<List<DailyWeather>>(emptyList())
    val dailyList: State<List<DailyWeather>> = _dailyList

    private val _userLat = mutableStateOf(0.0)
    val userLat: State<Double> = _userLat

    private val _userLon = mutableStateOf(0.0)
    val userLon: State<Double> = _userLon

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage


    /**
     * Loads weather data based on the user's current device location.
     *
     * This includes:
     * - Getting the current location using [getDeviceLocation]
     * - Fetching city name, current weather, hourly forecast, and daily forecast
     * - Updating state variables to be observed by the UI
     *
     * @param context Android Context required for location services
     * @param apiKey OpenWeatherMap API key
     */
    fun loadAllData(context: Context, apiKey: String) {
        viewModelScope.launch {

            if (_userLat.value != null && _weatherData.value != null) {
                Log.d("WeatherViewModel", "Loaded，skip loadAllData")
                return@launch
            }
            // Reset
            _cityName.value = "Loading..."
            _weatherData.value = null
            _hourlyList.value = emptyList()
            _dailyList.value = emptyList()
            _errorMessage.value = null

            Log.d("WeatherViewModel", "Attempting to load all data.")
            val location = getDeviceLocation(context)

            if (location == null) {
                _errorMessage.value = "無法取得目前位置，請確認已開啟 GPS 並允許定位權限"
                return@launch
            }

            _userLat.value = location.latitude
            _userLon.value = location.longitude
            Log.d("WeatherViewModel", "Location: ${location.latitude}, ${location.longitude}")

            try {
                _cityName.value  = fetchCityName(location.latitude, location.longitude, apiKey)
                _weatherData.value = fetchCurrentWeather(location.latitude, location.longitude, apiKey)
                _hourlyList.value = fetchHourlyWeather(location.latitude, location.longitude, apiKey)
                _dailyList.value  = fetchNextFiveDaysWeather(location.latitude, location.longitude, apiKey)
                Log.i("WeatherViewModel", "Weather data fetched for ${_cityName.value}")
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Fetch error: ${e.message}", e)
                _cityName.value = "Error"
                _errorMessage.value = "無法取得天氣資料，請檢查網路 (${e.javaClass.simpleName})"
            }
        }
    }

    /**
     * Loads weather data for a specified latitude and longitude.
     * Useful for city selection pages or bookmarked locations.
     *
     * @param lat Latitude of the target location
     * @param lon Longitude of the target location
     * @param context Android Context for potential fallback/error handling
     * @param apiKey OpenWeatherMap API key
     */
    fun loadAllDataByCoords(lat: Double, lon: Double, context: Context, apiKey: String) {
        Log.d("WeatherViewModel", "loadAllDataByCoords called with lat=$lat, lon=$lon")

        viewModelScope.launch {
            // Reset
            _cityName.value = "Loading..."
            _weatherData.value = null
            _hourlyList.value = emptyList()
            _dailyList.value = emptyList()
            _errorMessage.value = null

            _userLat.value = lat
            _userLon.value = lon

            try {
                _cityName.value  = fetchCityName(lat, lon, apiKey)
                _weatherData.value = fetchCurrentWeather(lat, lon, apiKey)
                _hourlyList.value = fetchHourlyWeather(lat, lon, apiKey)
                _dailyList.value  = fetchNextFiveDaysWeather(lat, lon, apiKey)
                Log.i("WeatherViewModel", "Weather data fetched for ${_cityName.value}")
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Fetch error: ${e.message}", e)
                _cityName.value = "Error"
                _errorMessage.value = "Failed to load weather: (${e.javaClass.simpleName})"
            }
        }
    }

}

/* ---------- 位置 ---------- */

/**
 * Attempts to retrieve the user's current location using FusedLocationProviderClient.
 * Falls back to requesting a new high-accuracy location if last known is null.
 *
 * @param context Android context
 * @return A [Location] object, or null if location could not be retrieved
 */
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
                    /* cancellationToken = */ null
                ).addOnSuccessListener { loc -> cont.resume(loc) }
                    .addOnFailureListener  { _  -> cont.resume(null) }
            }
        }.addOnFailureListener { _ ->
            cont.resume(null)
        }
    }

/**
 * Fetches current weather information from OpenWeatherMap.
 *
 * @param lat Latitude
 * @param lon Longitude
 * @param apiKey API key for OpenWeatherMap
 * @return [WeatherData] object containing temperature, description, and icon
 */
private suspend fun fetchCurrentWeather(lat: Double, lon: Double, apiKey: String): WeatherData {
    val response = withContext(Dispatchers.IO) {
        RetrofitClient.instance.getCurrentWeather(lat, lon, apiKey)
    }
    val main = response.main
    val weather = response.weather.first()

    return WeatherData(
        temperature = main.temp,
        description = weather.main,
        icon = weather.icon,
        minTemp = main.temp_min,
        maxTemp = main.temp_max
    )
}

/**
 * Uses reverse geocoding to fetch the name of a city from its coordinates.
 *
 * @param lat Latitude
 * @param lon Longitude
 * @param apiKey API key for OpenWeatherMap
 * @return The name of the city, or "Unknown" if not found
 */
private suspend fun fetchCityName(lat: Double, lon: Double, apiKey: String): String {
    val response = withContext(Dispatchers.IO) {
        RetrofitClient.instance.reverseGeocoding(lat, lon, 1, apiKey)
    }
    return response.firstOrNull()?.name ?: "Unknown"
}

/**
 * Fetches hourly forecast data for the next several hours (typically 3-hour intervals).
 *
 * @param lat Latitude
 * @param lon Longitude
 * @param apiKey API key for OpenWeatherMap
 * @return A list of [HourlyWeather] entries (up to 8 time slots)
 */
private suspend fun fetchHourlyWeather(lat: Double, lon: Double, apiKey: String): List<HourlyWeather> {
    val response = withContext(Dispatchers.IO) {
        RetrofitClient.instance.getForecast(lat, lon, apiKey)
    }

    return response.list.take(8).map { item ->
        HourlyWeather(
            time = item.dt_txt.substring(11, 16),
            temperature = item.main.temp,
            icon = item.weather.first().main,
            description = item.weather.first().description
        )
    }
}

/**
 * Processes OpenWeatherMap 3-hour interval forecast to extract daily summaries for the next 5 days.
 *
 * Includes:
 * - Date parsing and grouping by day
 * - Average/min/max temperatures
 * - Noon-hour condition (or fallback to first available entry)
 * - Extracted values: humidity, wind, icon, etc.
 *
 * @param lat Latitude
 * @param lon Longitude
 * @param apiKey API key for OpenWeatherMap
 * @return A list of [DailyWeather] representing a 5-day forecast
 */
private suspend fun fetchNextFiveDaysWeather(lat: Double, lon: Double, apiKey: String): List<DailyWeather> {
    val forecast = withContext(Dispatchers.IO) {
        RetrofitClient.instance.getForecast(lat, lon, apiKey)
    }

    // 1. 将 list 归类到 date（yyyy-MM-dd）
    val dailyMap = mutableMapOf<String, MutableList<ForecastItem>>()
    forecast.list.forEach { item ->
        val date = item.dt_txt.substring(0, 10)
        dailyMap.getOrPut(date) { mutableListOf() }.add(item)
    }

    // 2. 提取未来五天
    val today = LocalDate.now()
    val dailyList = mutableListOf<DailyWeather>()

    for (i in 0..4) {
        val date = today.plusDays(i.toLong()).toString()
        val entries = dailyMap[date] ?: continue

        val temps = entries.map { it.main.temp }
        val minTemp = temps.minOrNull() ?: continue
        val maxTemp = temps.maxOrNull() ?: continue

        val noon = entries.find { it.dt_txt.contains("12:00:00") } ?: entries[0]
        val currentTemp = noon.main.temp
        val weather = noon.weather.first()
        val windSpeed = noon.wind.speed
        val humidity = noon.main.humidity // OpenWeather forecast 没有 humidity，需替代或额外模型扩展

        val dayLabel = when (i) {
            0 -> "Today"
            else -> today.plusDays(i.toLong()).dayOfWeek.name.take(3).replaceFirstChar { it.uppercase() }
        }

        dailyList.add(
            DailyWeather(
                date = date,
                dayLabel = dayLabel,
                currentTemp = currentTemp,
                maxTemp = maxTemp,
                minTemp = minTemp,
                description = weather.main,
                icon = weather.main,
                windSpeed = windSpeed,
                humidity = humidity // 如需要准确湿度，可改 forecast 数据模型结构
            )
        )
    }
    return dailyList
}