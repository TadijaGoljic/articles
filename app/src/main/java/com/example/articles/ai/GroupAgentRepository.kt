package com.example.articles.ai

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.example.articles.ArticlesApplication
import com.example.articles.BuildConfig
import com.example.articles.R
import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.models.UILocation
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
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupAgentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val okHttpClient: OkHttpClient
) {

    private val actionItemSchema = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("action") {
                put("type", "STRING")
                putJsonArray("enum") {
                    add(ACTION_CREATE_GROUP)
                    add(ACTION_RENAME_GROUP)
                    add(ACTION_DELETE_GROUP)
                    add(ACTION_ADD_ARTICLE)
                    add(ACTION_RENAME_ARTICLE)
                    add(ACTION_REMOVE_ARTICLE)
                    add(ACTION_UNKNOWN)
                }
            }
            putJsonObject("groupName") { put("type", "STRING") }
            putJsonObject("articleName") { put("type", "STRING") }
            putJsonObject("newName") { put("type", "STRING") }
        }
        putJsonArray("required") {
            add("action")
            add("groupName")
            add("articleName")
            add("newName")
        }
    }

    private val responseSchema = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("actions") {
                put("type", "ARRAY")
                put("items", actionItemSchema)
            }
        }
        putJsonArray("required") {
            add("actions")
        }
    }

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(
                okHttpClient.newBuilder()
                    .readTimeout(GEMINI_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .callTimeout(GEMINI_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun handlePrompt(
        promptText: String,
        groups: List<UIGroup>,
        location: UILocation?
    ): List<GroupAgentCommand> {
        val commands = try {
            requestCommands(promptText, groups)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(ArticlesApplication.LOG_TAG, "parseCommand failed: ${e.message}", e)
            return listOf(GroupAgentCommand.Unresolved(errorMessageFor(e)))
        }
        val isFullyUnresolved = commands.all { it is GroupAgentCommand.Unresolved }
        if (!isFullyUnresolved) return commands
        return listOf(answerQuestion(promptText, groups, location))
    }

    private suspend fun requestCommands(promptText: String, groups: List<UIGroup>): List<GroupAgentCommand> {
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
        return parseResponse(rawJson, groups)
    }

    private fun errorMessageFor(error: Throwable): String = context.getString(
        when {
            error is HttpException && error.code() in RETRYABLE_HTTP_CODES -> R.string.agent_service_busy
            error is IOException -> R.string.agent_network_error
            else -> R.string.something_went_wrong
        }
    )

    private suspend fun answerQuestion(
        promptText: String,
        groups: List<UIGroup>,
        location: UILocation?
    ): GroupAgentCommand {
        var failure: Throwable? = null
        val result = runCatching {
            val response = service.generateContent(
                model = MODEL_NAME,
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = GeminiGenerateContentRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = buildQuestionPrompt(promptText, groups, location))))
                    ),
                    tools = listOf(GeminiTool())
                )
            )
            val candidate = response.candidates.firstOrNull()
            val answer = candidate?.content?.parts?.firstOrNull()?.text?.trim().orEmpty()
            val sources = candidate?.groundingMetadata?.groundingChunks
                ?.mapNotNull { it.web?.uri }
                ?.distinct()
                .orEmpty()
            if (answer.isBlank()) null else GroupAgentCommand.AnswerQuestion(answer = answer, sources = sources)
        }.getOrElse { e ->
            if (e is CancellationException) throw e
            Log.e(ArticlesApplication.LOG_TAG, "answerQuestion failed: ${e.message}", e)
            failure = e
            null
        }
        return result ?: GroupAgentCommand.Unresolved(
            failure?.let(::errorMessageFor) ?: context.getString(R.string.something_went_wrong)
        )
    }

    @VisibleForTesting
    internal fun buildQuestionPrompt(promptText: String, groups: List<UIGroup>, location: UILocation?): String {
        val locationContext = location?.let {
            "The user's approximate current location is latitude ${it.latitude}, longitude ${it.longitude}. " +
                    "Ground your answer in stores, prices, and availability relevant to that specific area — " +
                    "never substitute a different city or country."
        }
            ?: ("The user's current location is unknown. If the answer genuinely depends on location " +
                    "(e.g. where to buy something), say you need their city instead of guessing one.")
        val groupsDescription = groups.joinToString(separator = "\n") { group ->
            val articleLines = group.articles.joinToString(separator = ", ") { article ->
                val price = article.price?.let { " (%.2f)".format(Locale.ROOT, it) }.orEmpty()
                "${article.name}$price"
            }
            "- ${group.name}: ${articleLines.ifBlank { "(no articles)" }}"
        }.ifBlank { "(no groups yet)" }
        return """
            You are the shopping assistant inside a grocery shopping-list app, answering a general question the user asked (this request did not match any group/article management action, so treat it as a free-form question instead).

            The user's existing groups and the articles in each are listed below, with each article's known price in parentheses when set (prices have no currency attached — assume the user's local currency and do not invent a different one):
            $groupsDescription

            $locationContext

            If the request involves a budget (e.g. "I have X to spend, what should I buy") or asks you to prioritize a specific group, build your answer from the articles listed above — do not invent items that are not in the user's lists. Prefer articles from any group the user names as a priority, only spilling over into other groups' articles if budget remains. If an article has no known price, say you cannot include it in a budget calculation without a price instead of guessing one. If the requested budget cannot cover the priority group's articles, say so plainly rather than silently dropping items.

            Use Google Search to find current, factual information when the question needs outside facts (e.g. where to buy something, or current prices for items with no stored price). Keep the answer short and practical, in the same language the user asked in. If you cannot find reliable information, say so honestly instead of guessing a price, store, or address.

            User question: "$promptText"
        """.trimIndent()
    }

    private fun buildPrompt(promptText: String, groups: List<UIGroup>): String {
        val groupsDescription = groups.joinToString(separator = "\n") { group ->
            val articleNames = group.articles.joinToString(separator = ", ") { it.name }
            "- ${group.name}: ${articleNames.ifBlank { "(no articles)" }}"
        }
        return """
            You translate a user's natural-language request into a list of one or more shopping-list operations: creating a group, renaming a group, deleting a group, adding an article to a group, renaming an article, or removing an article. Return every operation the request implies, in the order they should be performed, inside the "actions" array. A request can imply several operations at once (e.g. "create groups A and B", "remove A and create C", "create group fridge and put bread and milk in it").

            The user's existing groups and the articles currently in each are:
            $groupsDescription

            Rules:
            - "action" must be one of: "$ACTION_CREATE_GROUP", "$ACTION_RENAME_GROUP", "$ACTION_DELETE_GROUP", "$ACTION_ADD_ARTICLE", "$ACTION_RENAME_ARTICLE", "$ACTION_REMOVE_ARTICLE", "$ACTION_UNKNOWN".
            - For "$ACTION_CREATE_GROUP": "groupName" is the name of the new group to create; "articleName" and "newName" must be "".
            - For "$ACTION_RENAME_GROUP": "groupName" must be the group name (copied exactly, character for character) that the user wants to rename; "newName" is the requested new name; "articleName" must be "".
            - For "$ACTION_DELETE_GROUP": "groupName" must be the group name (copied exactly) that the user wants to delete; "articleName" and "newName" must be "".
            - For "$ACTION_ADD_ARTICLE": "groupName" must be the group name (copied exactly) to add the article into; "articleName" is the name of the new article; "newName" must be "".
            - For "$ACTION_RENAME_ARTICLE": "articleName" must be the article name (copied exactly) the user wants to rename; "newName" is the requested new article name; "groupName" is the group that article belongs to if the user mentioned it, otherwise "".
            - For "$ACTION_REMOVE_ARTICLE": "articleName" must be the article name (copied exactly) the user wants to remove; "groupName" is the group that article belongs to if the user mentioned it, otherwise ""; "newName" must be "".
            - A "groupName" or "articleName" does not have to already exist in the list above if an earlier action in the same "actions" array creates it first (e.g. an "$ACTION_ADD_ARTICLE" action can target the "groupName" that an earlier "$ACTION_CREATE_GROUP" action in the same list is about to create).
            - If one part of the request does not clearly match one of these operations, or refers to a group or article that is not clearly and unambiguously identifiable (either already listed above, or created earlier in the same "actions" array), emit an entry with "action": "$ACTION_UNKNOWN" and all three name fields as "" for that part instead of guessing, but still return the other, understood parts.
            - Never invent a group or article name that was not mentioned by the user.

            User request: "$promptText"
        """.trimIndent()
    }

    @VisibleForTesting
    internal fun parseResponse(rawJson: String, groups: List<UIGroup>): List<GroupAgentCommand> {
        val json = runCatching { JSONObject(rawJson) }.getOrNull()
        val actionsArray = json?.optJSONArray("actions")
        if (actionsArray == null || actionsArray.length() == 0) {
            return listOf(GroupAgentCommand.Unresolved(context.getString(R.string.agent_could_not_understand)))
        }

        var workingGroups = groups
        val commands = mutableListOf<GroupAgentCommand>()

        for (i in 0 until actionsArray.length()) {
            val item = actionsArray.optJSONObject(i) ?: continue

            val action = item.optString("action", ACTION_UNKNOWN)
            val groupName = item.optString("groupName").trim()
            val articleName = item.optString("articleName").trim()
            val newName = item.optString("newName").trim()

            fun findGroup(name: String) = workingGroups.firstOrNull { it.name.equals(name, ignoreCase = true) }

            fun findArticleMatches(groupNameHint: String, name: String): List<Pair<UIGroup, UIArticle>> {
                val candidateGroups = if (groupNameHint.isNotBlank()) {
                    workingGroups.filter { it.name.equals(groupNameHint, ignoreCase = true) }
                } else {
                    workingGroups
                }
                return candidateGroups
                    .flatMap { group -> group.articles.map { article -> group to article } }
                    .filter { (_, article) -> article.name.equals(name, ignoreCase = true) }
            }

            val command = when (action) {
                ACTION_CREATE_GROUP -> {
                    if (groupName.isBlank()) {
                        GroupAgentCommand.Unresolved(context.getString(R.string.agent_could_not_understand))
                    } else if (findGroup(groupName) != null) {
                        GroupAgentCommand.Unresolved(context.getString(R.string.group_already_exists))
                    } else {
                        val newGroup = UIGroup(name = groupName)
                        workingGroups = workingGroups + newGroup
                        GroupAgentCommand.CreateGroup(group = newGroup)
                    }
                }

                ACTION_RENAME_GROUP -> {
                    val group = findGroup(groupName)
                    if (group == null) {
                        GroupAgentCommand.Unresolved(context.getString(R.string.agent_group_not_found, groupName))
                    } else if (newName.isBlank()) {
                        GroupAgentCommand.Unresolved(context.getString(R.string.agent_could_not_understand))
                    } else {
                        workingGroups = workingGroups.map { if (it.id == group.id) it.copy(name = newName) else it }
                        GroupAgentCommand.RenameGroup(group = group, newName = newName)
                    }
                }

                ACTION_DELETE_GROUP -> {
                    val group = findGroup(groupName)
                    if (group == null) {
                        GroupAgentCommand.Unresolved(context.getString(R.string.agent_group_not_found, groupName))
                    } else {
                        workingGroups = workingGroups.filter { it.id != group.id }
                        GroupAgentCommand.DeleteGroup(group = group)
                    }
                }

                ACTION_ADD_ARTICLE -> {
                    val group = findGroup(groupName)
                    if (group == null) {
                        GroupAgentCommand.Unresolved(context.getString(R.string.agent_group_not_found, groupName))
                    } else if (articleName.isBlank()) {
                        GroupAgentCommand.Unresolved(context.getString(R.string.agent_could_not_understand))
                    } else {
                        val newArticle = UIArticle(name = articleName, groupId = group.id)
                        workingGroups = workingGroups.map {
                            if (it.id == group.id) it.copy(articles = it.articles + newArticle) else it
                        }
                        GroupAgentCommand.AddArticle(group = group, article = newArticle)
                    }
                }

                ACTION_RENAME_ARTICLE -> {
                    val matches = findArticleMatches(groupName, articleName)
                    when {
                        matches.isEmpty() ->
                            GroupAgentCommand.Unresolved(context.getString(R.string.agent_article_not_found, articleName))
                        matches.size > 1 ->
                            GroupAgentCommand.Unresolved(context.getString(R.string.agent_article_ambiguous, articleName))
                        newName.isBlank() ->
                            GroupAgentCommand.Unresolved(context.getString(R.string.agent_could_not_understand))
                        else -> {
                            val (group, article) = matches.single()
                            workingGroups = workingGroups.map { g ->
                                if (g.id != group.id) g
                                else g.copy(articles = g.articles.map { a -> if (a.id == article.id) a.copy(name = newName) else a })
                            }
                            GroupAgentCommand.RenameArticle(article = article, groupName = group.name, newName = newName)
                        }
                    }
                }

                ACTION_REMOVE_ARTICLE -> {
                    val matches = findArticleMatches(groupName, articleName)
                    when {
                        matches.isEmpty() ->
                            GroupAgentCommand.Unresolved(context.getString(R.string.agent_article_not_found, articleName))
                        matches.size > 1 ->
                            GroupAgentCommand.Unresolved(context.getString(R.string.agent_article_ambiguous, articleName))
                        else -> {
                            val (group, article) = matches.single()
                            workingGroups = workingGroups.map { g ->
                                if (g.id != group.id) g
                                else g.copy(articles = g.articles.filter { it.id != article.id })
                            }
                            GroupAgentCommand.RemoveArticle(article = article, groupName = group.name)
                        }
                    }
                }

                else -> GroupAgentCommand.Unresolved(context.getString(R.string.agent_could_not_understand))
            }

            commands.add(command)
        }

        return commands.ifEmpty {
            listOf(GroupAgentCommand.Unresolved(context.getString(R.string.agent_could_not_understand)))
        }
    }

    private companion object {
        const val MODEL_NAME = "gemini-3.6-flash"
        const val GEMINI_READ_TIMEOUT_SECONDS = 60L
        const val GEMINI_CALL_TIMEOUT_SECONDS = 90L
        val RETRYABLE_HTTP_CODES = setOf(429, 500, 502, 503, 504)
        const val ACTION_CREATE_GROUP = "create_group"
        const val ACTION_RENAME_GROUP = "rename_group"
        const val ACTION_DELETE_GROUP = "delete_group"
        const val ACTION_ADD_ARTICLE = "add_article"
        const val ACTION_RENAME_ARTICLE = "rename_article"
        const val ACTION_REMOVE_ARTICLE = "remove_article"
        const val ACTION_UNKNOWN = "unknown"
    }

}
