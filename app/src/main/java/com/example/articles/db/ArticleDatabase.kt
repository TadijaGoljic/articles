package com.example.articles.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.articles.db.dao.ArticleDao
import com.example.articles.db.dao.GroupDao
import com.example.articles.db.entities.ArticleEntity
import com.example.articles.db.entities.GroupEntity

@TypeConverters(Converters::class)
@Database(
    entities = [
        ArticleEntity::class,
        GroupEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ArticleDatabase : RoomDatabase() {

    abstract fun articleDao(): ArticleDao
    abstract fun groupDao(): GroupDao

    companion object {
        const val DATABASE_NAME = "articles_db"
    }

}