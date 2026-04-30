package com.example.articles.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Location(
    val latitude: Double,
    val longitude: Double
) : Parcelable {
    override fun toString(): String {
        return "${"%.6f".format(latitude)},${"%.6f".format(longitude)}"
    }
}
