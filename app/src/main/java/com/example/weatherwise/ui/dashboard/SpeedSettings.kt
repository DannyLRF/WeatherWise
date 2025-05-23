package com.example.weatherwise

enum class WindSpeedUnit {
    METER_PER_SECOND, KILOMETER_PER_HOUR, MILES_PER_HOUR
}

object WindSpeedSettings {
    var currentUnit: WindSpeedUnit = WindSpeedUnit.METER_PER_SECOND

    fun convertSpeed(speedInMps: Double): Double {
        return when (currentUnit) {
            WindSpeedUnit.METER_PER_SECOND -> speedInMps
            WindSpeedUnit.KILOMETER_PER_HOUR -> speedInMps * 3.6
            WindSpeedUnit.MILES_PER_HOUR -> speedInMps * 2.237
        }
    }

    fun getUnitLabel(): String {
        return when (currentUnit) {
            WindSpeedUnit.METER_PER_SECOND -> "m/s"
            WindSpeedUnit.KILOMETER_PER_HOUR -> "km/h"
            WindSpeedUnit.MILES_PER_HOUR -> "mph"
        }
    }

    fun formatSpeed(speedInMps: Double): String {
        val converted = convertSpeed(speedInMps)
        return String.format("%.2f %s", converted, getUnitLabel())
    }
}