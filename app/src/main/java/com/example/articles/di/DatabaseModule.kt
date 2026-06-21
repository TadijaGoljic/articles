package com.example.articles.di

import android.content.Context
import androidx.room.Room
import com.example.articles.db.ArticleDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, ArticleDatabase::class.java, ArticleDatabase.DATABASE_NAME)
            .addMigrations(ArticleDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    @Singleton
    fun providesArticleDao(database: ArticleDatabase) = database.articleDao()

    @Provides
    @Singleton
    fun providesGroupDao(database: ArticleDatabase) = database.groupDao()
}