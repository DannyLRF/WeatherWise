package com.example.weatherwise.data

import android.content.Context
import com.example.weatherwise.AppDatabase
import com.example.weatherwise.Settings

class SettingsRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).settingsDao()

    val settings = dao.getSettings()

    suspend fun save(settings: Settings) {
        dao.saveSettings(settings)
    }
}