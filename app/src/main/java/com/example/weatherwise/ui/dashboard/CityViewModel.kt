package com.example.weatherwise.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.weatherwise.R


class CityViewModel : ViewModel() {
    var cityList by mutableStateOf(
        listOf(
            CityInfo("My location", "Clayton | Sunny", 25, R.drawable.sun_icon),
            CityInfo("Changsha", "Overcast", 28, R.drawable.cloudy_icon),
            CityInfo("London", "Rainy", 10, R.drawable.rain_icon),
            CityInfo("Amsterdam", "Overcast", 12, R.drawable.cloudy_icon),
            CityInfo("Alaska", "Snow", -1, R.drawable.snow_icon),
            CityInfo("New York", "Cloudy Night", 12, R.drawable.night_icon)
        )
    )
        private set

    var searchText by mutableStateOf("")

    fun onSearchTextChange(newText: String) {
        searchText = newText
    }

    fun addCity(city: CityInfo) {
        // 避免重复添加
        if (cityList.none { it.name.equals(city.name, ignoreCase = true) }) {
            cityList = cityList + city
        }
    }
}
