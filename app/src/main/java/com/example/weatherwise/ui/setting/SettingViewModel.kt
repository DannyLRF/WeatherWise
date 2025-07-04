package com.example.weatherwise.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherwise.Settings
import com.example.weatherwise.TemperatureSettings
import com.example.weatherwise.TemperatureUnit
import com.example.weatherwise.WindSpeedSettings
import com.example.weatherwise.WindSpeedUnit

import com.example.weatherwise.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    private val _settings = MutableStateFlow(
        Settings(
            id = 0,
            unit = "Celsius",
            windSpeed = "KM/h",
            pressure = "mbar",
            weatherNotifications = true,
            weatherWarnings = false
        )
    )
    val settings: StateFlow<Settings> = _settings

    init {
        viewModelScope.launch {
            repository.settings.collect { saved ->
                if (saved != null) _settings.value = saved
            }
        }
    }



    init {
        // Tell temperature setting change the unit
        viewModelScope.launch {
            settings.collect { currentSettings ->
                TemperatureSettings.currentUnit = when (currentSettings.unit) {
                    "Celsius" -> TemperatureUnit.CELSIUS
                    "Fahrenheit" -> TemperatureUnit.FAHRENHEIT
                    else -> TemperatureUnit.CELSIUS
                }

                WindSpeedSettings.currentUnit = when (currentSettings.windSpeed) {
                    "M/s" -> WindSpeedUnit.METER_PER_SECOND
                    "KM/h" -> WindSpeedUnit.KILOMETER_PER_HOUR
                    "MPH" -> WindSpeedUnit.MILES_PER_HOUR
                    else -> WindSpeedUnit.METER_PER_SECOND
                }
            }


        }
    }


    fun updateUnit(unit: String) = save(_settings.value.copy(unit = unit))
    fun updateWindSpeed(speed: String) = save(_settings.value.copy(windSpeed = speed))
    fun updatePressure(pressure: String) = save(_settings.value.copy(pressure = pressure))
    fun setWeatherNotifications(enabled: Boolean) = save(_settings.value.copy(weatherNotifications = enabled))
    fun setWeatherWarnings(enabled: Boolean) = save(_settings.value.copy(weatherWarnings = enabled))

    private fun save(newSettings: Settings) {
        _settings.value = newSettings
        viewModelScope.launch {
            repository.save(newSettings)
        }
    }

    fun logout() {
        //
    }
}