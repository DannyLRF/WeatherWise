package com.example.weatherwise.ui.dashboard


import retrofit2.http.GET
import retrofit2.http.Query

// Response model
data class GeoResponseItem(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String?
)

interface GeocodingApi {

    @GET("geo/1.0/direct")
    suspend fun getCoordinatesByCity(
        @Query("q") cityName: String,
        @Query("limit") limit: Int = 1,
        @Query("appid") apiKey: String
    ): List<GeoResponseItem>
}
