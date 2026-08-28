package com.example.articles.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UILocation(
    val latitude: Double,
    val longitude: Double
) : Parcelable
