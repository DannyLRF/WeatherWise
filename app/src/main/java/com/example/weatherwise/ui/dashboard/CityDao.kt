package com.example.weatherwise.ui.dashboard

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing city data in the local Room database.
 */
@Dao
interface CityDao {

    /**
     * Retrieves a list of saved cities associated with a specific user.
     *
     * @param userId The unique ID of the user.
     * @return A [Flow] emitting the list of [CityEntity] objects for the given user.
     */
    @Query("SELECT * FROM cities WHERE userId = :userId")
    fun getCities(userId: String): Flow<List<CityEntity>>

    /**
     * Inserts a city entry into the database.
     * If the city already exists (based on primary key), it will be ignored.
     *
     * @param city The [CityEntity] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(city: CityEntity)

    /**
     * Deletes a city entry from the database.
     *
     * @param city The [CityEntity] to delete.
     */
    @Delete
    suspend fun delete(city: CityEntity)

    /**
     * Retrieves the special "My Location" city entry for the specified user.
     * This is typically used to distinguish the user's current location from manually added cities.
     *
     * @param userId The unique ID of the user.
     * @return The [CityEntity] representing the user's current location, or null if not found.
     */
    @Query("SELECT * FROM cities WHERE userId = :userId AND description = 'My Location' LIMIT 1")
    suspend fun getCurrentLocationCity(userId: String): CityEntity?
}