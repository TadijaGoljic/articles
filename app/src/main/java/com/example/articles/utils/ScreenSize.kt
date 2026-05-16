package com.example.articles.utils

import android.content.res.Resources

enum class ScreenSize {
    NORMAL, LARGE, VERY_LARGE;

    companion object {
        private const val MIN_LARGE_DP = 600
        private const val MIN_VERY_LARGE_DP = 1000

        fun getScreenSize(): ScreenSize {
            val metrics = Resources.getSystem().displayMetrics
            val screenWidthDp = metrics.widthPixels / metrics.density
            return when {
                screenWidthDp < MIN_LARGE_DP -> NORMAL
                screenWidthDp < MIN_VERY_LARGE_DP -> LARGE
                else -> VERY_LARGE
            }
        }
    }
}