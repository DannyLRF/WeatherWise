package com.example.weatherwise.ui.dashboard

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: String,     // UID after login
    val name: String,
    val description: String,
    val temperature: Int,
    val iconResId: Int,
    val lat: Double,
    val lon: Double
)