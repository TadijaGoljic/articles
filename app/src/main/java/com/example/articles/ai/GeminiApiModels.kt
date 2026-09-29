package com.example.articles.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): GeminiGenerateContentResponse
}

@Serializable
data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val tools: List<GeminiTool>? = null
)

@Serializable
data class GeminiTool(
    @SerialName("google_search") val googleSearch: GeminiGoogleSearch = GeminiGoogleSearch()
)

@Serializable
class GeminiGoogleSearch

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

@Serializable
data class GeminiGenerationConfig(
    val responseSchema: JsonElement,
    val responseMimeType: String = "application/json"
)

@Serializable
data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val groundingMetadata: GeminiGroundingMetadata? = null
)

@Serializable
data class GeminiGroundingMetadata(
    val groundingChunks: List<GeminiGroundingChunk>? = null
)

@Serializable
data class GeminiGroundingChunk(
    val web: GeminiWebSource? = null
)

@Serializable
data class GeminiWebSource(
    val uri: String? = null,
    val title: String? = null
)
