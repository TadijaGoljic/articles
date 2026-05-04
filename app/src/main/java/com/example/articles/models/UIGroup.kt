package com.example.articles.models

import java.util.UUID

data class UIGroup(
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    val articles: List<UIArticle> = emptyList(),
    val expanded: Boolean = false,
)
