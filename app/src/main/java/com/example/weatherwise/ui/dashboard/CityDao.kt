package com.example.weatherwise.ui.dashboard

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface CityDao {
    @Query("SELECT * FROM cities WHERE userId = :userId")
    fun getCities(userId: String): Flow<List<CityEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(city: CityEntity)

    @Delete
    suspend fun delete(city: CityEntity)

    @Query("SELECT * FROM cities WHERE userId = :userId AND description = 'My Location' LIMIT 1")
    suspend fun getCurrentLocationCity(userId: String): CityEntity?
}