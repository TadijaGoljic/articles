package com.example.articles.ui.models

data class UIArticle(
    val id: Int = 0,
    val name: String = "",
    val dateCreated: Long = 0L,
    val location: String? = null
)
