package com.example.weatherwise.ui.dashboard

/**
 * Represents UI-level information about a city, typically used for display purposes
 * such as in weather cards or lists.
 *
 * This class is separate from [CityEntity] to decouple UI representation from database schema.
 *
 * @property name The name of the city (e.g., "Sydney").
 * @property description A short label or note (e.g., "My Location", "Saved City").
 * @property temperature Current temperature in Celsius.
 * @property iconResId Resource ID for the weather icon (used in the UI).
 * @property lat Latitude of the city. Defaults to 0.0 if not provided.
 * @property lon Longitude of the city. Defaults to 0.0 if not provided.
 */
data class CityInfo(
    val name: String,
    val description: String,
    val temperature: Int,
    val iconResId: Int,
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

