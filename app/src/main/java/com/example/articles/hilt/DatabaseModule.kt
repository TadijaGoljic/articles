package com.example.articles.hilt

import android.content.Context
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
        ArticleDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun providesWordDao(database: ArticleDatabase) = database.articleDao()
}