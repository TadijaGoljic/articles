package com.example.articles.models

import android.os.Parcelable
import androidx.annotation.StringRes
import com.example.articles.R
import com.example.articles.models.Location
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.UUID

@Parcelize
data class UIArticle(
    val id: UUID = UUID.randomUUID(),
    val name: String? = "",
    val description: String? = "",
    val dateCreated: Date = Date(),
    val location: Location? = null,
    val quantity: Double? = null,
    val unit: ArticleUnit = ArticleUnit.default,
    val photoUri: String? = null,
    val deleted: Boolean = false,
    val groupId: UUID? = null
) : Parcelable


enum class ArticleUnit(
    @StringRes val label: Int,
    @StringRes val abbreviation: Int
) {
    PIECES(
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
    ),
    LITERS(
        label = R.string.unit_liters,
        abbreviation = R.string.unit_liters_abbreviation
    );
    companion object {
        val default = PIECES
    }
}