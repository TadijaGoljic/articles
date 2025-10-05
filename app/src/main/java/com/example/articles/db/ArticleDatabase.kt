package com.example.articles.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@TypeConverters(Converters::class)
@Database(
    entities = [ArticleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ArticleDatabase : RoomDatabase() {

    abstract fun articleDao(): ArticleDao

    companion object {

        const val DATABASE_NAME = "articles_db"
        private var instance: ArticleDatabase? = null

        fun getDatabase(context: Context): ArticleDatabase {
            return instance ?: synchronized(this) {
                val dbInstance = Room.databaseBuilder(
                    context = context.applicationContext,
                    klass = ArticleDatabase::class.java,
                    name = DATABASE_NAME
                ).build()
                instance = dbInstance
                return@synchronized dbInstance
            }
        }

    }

}