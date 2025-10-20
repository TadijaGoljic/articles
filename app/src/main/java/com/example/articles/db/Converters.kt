package com.example.articles.db

import androidx.room.TypeConverter
import com.example.articles.viewmodels.LocationData
import java.util.UUID

class Converters {

    @TypeConverter
    fun fromString(string: String): LocationData {
        return LocationData(
            latitude = string.split(",")[0].toDouble(),
            longitude = string.split(",")[1].toDouble()
        )
    }

    @TypeConverter
    fun toString(locationData: LocationData): String {
        return locationData.latitude.toString() + "," + locationData.longitude.toString()
    }

    @TypeConverter
    fun fromUUID(uuid: UUID): String {
        return uuid.toString()
    }

    @TypeConverter
    fun toUUID(string: String): UUID {
        return UUID.fromString(string)
    }

}