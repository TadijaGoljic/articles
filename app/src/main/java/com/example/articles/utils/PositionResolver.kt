package com.example.articles.utils

import kotlin.math.abs

object PositionResolver {
    private const val GAP = 1000.0
    private const val EPSILON = 1e-3

    fun resolve(previousPosition: Double?, nextPosition: Double?): Double? = when {
        previousPosition != null && nextPosition != null -> {
            if (abs(previousPosition - nextPosition) < EPSILON) null
            else (previousPosition + nextPosition) / 2
        }
        previousPosition != null -> previousPosition + GAP
        nextPosition != null -> nextPosition - GAP
        else -> GAP
    }
}
