package com.example.articles.ui.models

import androidx.annotation.StringRes
import com.example.articles.R
import com.example.articles.viewmodels.LocationData

data class UIArticle(
    val id: Int = 0,
    val name: String = "",
    val dateCreated: Long = 0L,
    val location: LocationData? = null,
    val expanded: Boolean = false,
    val quantity: Double? = null,
    val unit: ArticleUnit? = null
)


enum class ArticleUnit(
    @StringRes val label: Int,
    @StringRes val abbreviation: Int
) {
    QUANTITY(
        label = R.string.unit_quantity,
        abbreviation = R.string.unit_quantity_abbreviation
    ),
    KILOGRAMS(
        label = R.string.unit_kilograms,
        abbreviation = R.string.unit_kilograms_abbreviation
    ),
    GRAMS(
        label = R.string.unit_grams,
        abbreviation = R.string.unit_grams_abbreviation
    );
    companion object {
        val default = QUANTITY
    }
}