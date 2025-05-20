package com.example.weatherwise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class WeatherViewModel : ViewModel() {

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

    fun loadAllData(context: android.content.Context, apiKey: String) {
        viewModelScope.launch {
            val location = getLastKnownLocation(context)
            location?.let {
                _userLat.value = it.latitude
                _userLon.value = it.longitude

                _cityName.value = fetchCityName(it.latitude, it.longitude, apiKey)
                _weatherData.value = fetchCurrentWeather(it.latitude, it.longitude, apiKey)
                _hourlyList.value = fetchHourlyWeather(it.latitude, it.longitude, apiKey)
                _dailyList.value = fetchNextFiveDaysWeather(it.latitude, it.longitude, apiKey)
            }
        }
    }
}

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

private suspend fun fetchCityName(lat: Double, lon: Double, apiKey: String): String {
    val response = withContext(Dispatchers.IO) {
        RetrofitClient.instance.reverseGeocoding(lat, lon, 1, apiKey)
    }
    return response.firstOrNull()?.name ?: "Unknown"
}

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