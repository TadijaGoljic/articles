package com.example.articles.models

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.core.content.edit
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {

    private val sharedPreferences =
        context.getSharedPreferences(USER_PREFERENCES_KEY, Context.MODE_PRIVATE)

    var lastSyncTimestamp: Long
        get() = sharedPreferences.getLong(LAST_SYNC_TIMESTAMP_KEY, 0)
        set(value) = sharedPreferences.edit { putLong(LAST_SYNC_TIMESTAMP_KEY, value) }

    companion object {
        const val USER_PREFERENCES_KEY = "user_preferences_articles"
        const val LAST_SYNC_TIMESTAMP_KEY = "last_sync_timestamp"
    }

}