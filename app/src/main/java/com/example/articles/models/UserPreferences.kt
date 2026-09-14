package com.example.articles.models

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
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

    var syncDataEnabled: Boolean
        get() = sharedPreferences.getBoolean(DATA_SYNCHRONIZATION_ENABLED_KEY, true)
        set(value) = sharedPreferences.edit { putBoolean(DATA_SYNCHRONIZATION_ENABLED_KEY, value) }

    var confirmNonDestructiveAgentActions: Boolean
        get() = sharedPreferences.getBoolean(CONFIRM_NON_DESTRUCTIVE_AGENT_ACTIONS_KEY, true)
        set(value) = sharedPreferences.edit { putBoolean(CONFIRM_NON_DESTRUCTIVE_AGENT_ACTIONS_KEY, value) }

    companion object {
        const val USER_PREFERENCES_KEY = "user_preferences_articles"
        const val LAST_SYNC_TIMESTAMP_KEY = "last_sync_timestamp"
        const val DATA_SYNCHRONIZATION_ENABLED_KEY = "data_synchronization_enabled"
        const val CONFIRM_NON_DESTRUCTIVE_AGENT_ACTIONS_KEY = "confirm_non_destructive_agent_actions"
    }

}