package com.example.articles.nominatim

import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimAPI {

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("format") format: String = "jsonv2"
    ): NominatimResponse

}