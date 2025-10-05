package com.example.articles.db

import androidx.room.TypeConverter
import com.example.articles.viewmodels.LocationData

class Converters {

    @TypeConverter
    fun fromStringToLocationData(string: String): LocationData {
        return LocationData(
            latitude = string.split(",")[0].toDouble(),
            longitude = string.split(",")[1].toDouble()
        )
    }

    @TypeConverter
    fun fromLocationDataToString(locationData: LocationData): String {
        return locationData.latitude.toString() + "," + locationData.longitude.toString()
    }
}