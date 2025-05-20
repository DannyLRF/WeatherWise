package com.example.weatherwise

enum class TemperatureUnit {
    CELSIUS, FAHRENHEIT
}

object TemperatureSettings {
    var currentUnit: TemperatureUnit = TemperatureUnit.CELSIUS

    fun convertTemp(tempInCelsius: Double): Double {
        return when (currentUnit) {
            TemperatureUnit.CELSIUS -> tempInCelsius
            TemperatureUnit.FAHRENHEIT -> tempInCelsius * 9 / 5 + 32
        }
    }

    fun getUnitSymbol(): String {
        return when (currentUnit) {
            TemperatureUnit.CELSIUS -> "°C"
            TemperatureUnit.FAHRENHEIT -> "°F"
        }
    }
}
