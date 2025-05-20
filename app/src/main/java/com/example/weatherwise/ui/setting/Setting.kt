package com.example.weatherwise

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 0,
    val unit: String,
    val windSpeed: String,
    val pressure: String,
    val weatherNotifications: Boolean,
    val weatherWarnings: Boolean
)

