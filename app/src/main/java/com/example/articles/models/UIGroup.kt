package com.example.articles.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class UIGroup(
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    val articles: List<UIArticle> = emptyList(),
    val expanded: Boolean = false,
): Parcelable
