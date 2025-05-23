package com.example.weatherwise.ui.dashboard

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a city entry stored in the local Room database.
 *
 * This entity holds weather-related information for a city associated with a specific user.
 *
 * @property id Auto-generated primary key for each city entry.
 * @property userId The ID of the user who saved this city (typically obtained after login).
 * @property name The display name of the city (e.g., "Melbourne").
 * @property description A short description (e.g., "My Location" or a user-defined label).
 * @property temperature The current temperature in Celsius.
 * @property iconResId Resource ID for the weather icon to be displayed.
 * @property lat Latitude of the city.
 * @property lon Longitude of the city.
 */
@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: String,        // UID after login
    val name: String,          // City name (e.g., "Melbourne")
    val description: String,   // e.g., "My Location", "Vacation Spot"
    val temperature: Int,      // Temperature in Celsius
    val iconResId: Int,        // Drawable resource ID for weather icon
    val lat: Double,           // Latitude
    val lon: Double            // Longitude
)