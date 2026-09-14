package com.example.articles.ai

import android.content.Context
import android.util.Log
import com.example.articles.ArticlesApplication
import com.example.articles.BuildConfig
import com.example.articles.R
import com.example.articles.models.UIGroup
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupAgentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val okHttpClient: OkHttpClient
) {

    private val responseSchema = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("action") {
                put("type", "STRING")
                putJsonArray("enum") {
                    add(ACTION_CREATE)
                    add(ACTION_RENAME)
                    add(ACTION_DELETE)
                    add(ACTION_UNKNOWN)
                }
            }
            putJsonObject("targetGroupName") { put("type", "STRING") }
            putJsonObject("newGroupName") { put("type", "STRING") }
        }
        putJsonArray("required") {
            add("action")
            add("targetGroupName")
            add("newGroupName")
        }
    }

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun parseCommand(promptText: String, groups: List<UIGroup>): GroupAgentCommand {
        val result = runCatching {
            val response = service.generateContent(
                model = MODEL_NAME,
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = GeminiGenerateContentRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = buildPrompt(promptText, groups))))
                    ),
                    generationConfig = GeminiGenerationConfig(responseSchema = responseSchema)
                )
            )
            val rawJson = response.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                .orEmpty()
            parseResponse(rawJson, groups)
        }
        return result.getOrElse { e ->
            Log.e(ArticlesApplication.LOG_TAG, "parseCommand failed: ${e.message}", e)
            GroupAgentCommand.Unresolved(context.getString(R.string.something_went_wrong))
        }
    }

    private fun buildPrompt(promptText: String, groups: List<UIGroup>): String {
        val groupNames = groups.joinToString(separator = "\n") { "- ${it.name}" }
        return """
            You translate a user's natural-language request into one of three shopping-list group operations: creating a group, renaming a group, or deleting a group. You must only ever return one of these operations, nothing else.

            The user's existing groups are:
            $groupNames

            Rules:
            - "action" must be one of: "$ACTION_CREATE", "$ACTION_RENAME", "$ACTION_DELETE", "$ACTION_UNKNOWN".
            - For "$ACTION_CREATE": "targetGroupName" is the name of the new group to create; "newGroupName" must be "".
            - For "$ACTION_RENAME": "targetGroupName" must be the existing group name (copied exactly, character for character, from the list above) that the user wants to rename; "newGroupName" is the requested new name.
            - For "$ACTION_DELETE": "targetGroupName" must be the existing group name (copied exactly from the list above) that the user wants to delete; "newGroupName" must be "".
            - If the request does not clearly match one of these three operations, or refers to a group that is not clearly and unambiguously one of the existing group names above, return "action": "$ACTION_UNKNOWN" with both name fields as "".
            - Never invent a group name that is not in the list above for "targetGroupName".

            User request: "$promptText"
        """.trimIndent()
    }

    private fun parseResponse(rawJson: String, groups: List<UIGroup>): GroupAgentCommand {
        val json = runCatching { JSONObject(rawJson) }.getOrNull()
            ?: return GroupAgentCommand.Unresolved(context.getString(R.string.agent_could_not_understand))

        val action = json.optString("action", ACTION_UNKNOWN)
        val targetGroupName = json.optString("targetGroupName").trim()
        val newGroupName = json.optString("newGroupName").trim()

        fun findGroup(name: String) = groups.firstOrNull { it.name.equals(name, ignoreCase = true) }

        return when (action) {
            ACTION_CREATE -> {
                if (targetGroupName.isBlank()) {
                    GroupAgentCommand.Unresolved(context.getString(R.string.agent_could_not_understand))
                } else {
                    GroupAgentCommand.CreateGroup(name = targetGroupName)
                }
            }

            ACTION_RENAME -> {
                val group = findGroup(targetGroupName)
                if (group == null) {
                    GroupAgentCommand.Unresolved(
                        context.getString(R.string.agent_group_not_found, targetGroupName)
                    )
                } else if (newGroupName.isBlank()) {
                    GroupAgentCommand.Unresolved(context.getString(R.string.agent_could_not_understand))
                } else {
                    GroupAgentCommand.RenameGroup(group = group, newName = newGroupName)
                }
            }

            ACTION_DELETE -> {
                val group = findGroup(targetGroupName)
                if (group == null) {
                    GroupAgentCommand.Unresolved(
                        context.getString(R.string.agent_group_not_found, targetGroupName)
                    )
                } else {
                    GroupAgentCommand.DeleteGroup(group = group)
                }
            }

            else -> GroupAgentCommand.Unresolved(context.getString(R.string.agent_could_not_understand))
        }
    }

    private companion object {
        const val MODEL_NAME = "gemini-3.6-flash"
        const val ACTION_CREATE = "create_group"
        const val ACTION_RENAME = "rename_group"
        const val ACTION_DELETE = "delete_group"
        const val ACTION_UNKNOWN = "unknown"
    }

}
