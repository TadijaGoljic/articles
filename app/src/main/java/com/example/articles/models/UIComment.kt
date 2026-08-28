package com.example.articles.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class UIComment(
    val id: UUID = UUID.randomUUID(),
    val groupId: UUID = UUID.randomUUID(),
    val articleId: UUID? = null,
    val authorUid: String = "",
    val authorEmail: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) : Parcelable
