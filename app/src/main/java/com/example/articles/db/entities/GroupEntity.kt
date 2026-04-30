package com.example.articles.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val idGroup: UUID = UUID.randomUUID(),
    val name: String
)