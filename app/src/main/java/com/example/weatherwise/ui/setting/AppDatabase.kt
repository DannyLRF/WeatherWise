package com.example.weatherwise

import android.content.Context
import androidx.compose.ui.text.font.FontVariation
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.weatherwise.Settings
import com.example.weatherwise.SettingsDao

@Database(entities = [Settings::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_settings_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}