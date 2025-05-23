package com.example.weatherwise.ui.dashboard


import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Data class representing a single item returned by the geocoding API.
 *
 * @property name Name of the city
 * @property lat Latitude of the location
 * @property lon Longitude of the location
 * @property country Country code (e.g., "US", "AU")
 * @property state Optional state or region (available for some countries)
 */
data class GeoResponseItem(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String?
)


/**
 * Retrofit interface for accessing the OpenWeatherMap Geocoding API.
 * This endpoint allows retrieving geographical coordinates by city name.
 *
 * Example request:
 * `https://api.openweathermap.org/geo/1.0/direct?q=Melbourne&limit=1&appid=YOUR_API_KEY`
 */
interface GeocodingApi {

    /**
     * Fetches the geographic coordinates for the given city name.
     *
     * @param cityName The name of the city to look up (e.g., "Melbourne")
     * @param limit Maximum number of results to return (default is 1)
     * @param apiKey API key provided by OpenWeatherMap
     * @return A list of [GeoResponseItem] objects with location details
     */
    @GET("geo/1.0/direct")
    suspend fun getCoordinatesByCity(
        @Query("q") cityName: String,
        @Query("limit") limit: Int = 1,
        @Query("appid") apiKey: String
    ): List<GeoResponseItem>
}
