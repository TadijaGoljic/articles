package com.example.articles.db

import androidx.room.TypeConverter
import com.example.articles.models.UILocation
import java.util.UUID

class Converters {

    @TypeConverter
    fun fromString(string: String): UILocation {
        return UILocation(
            latitude = string.split(",")[0].toDouble(),
            longitude = string.split(",")[1].toDouble()
        )
    }

    @TypeConverter
    fun toString(location: UILocation): String {
        return location.latitude.toString() + "," + location.longitude.toString()
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