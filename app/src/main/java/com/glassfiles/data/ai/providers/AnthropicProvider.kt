package com.glassfiles.data.ai.providers

import android.content.Context
import com.glassfiles.data.ai.CapabilityClassifier
import com.glassfiles.data.ai.SystemPrompts
import com.glassfiles.data.ai.models.AiMessage
import com.glassfiles.data.ai.models.AiModel
import com.glassfiles.data.ai.models.AiProviderId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Anthropic Claude provider.
 *
 * Uses `/v1/models` for listing and `/v1/messages` with SSE for streaming.
 * Anthropic-specific headers: `anthropic-version`, `x-api-key` (no `Authorization`).
 *
 * Anthropic does NOT support image generation or video generation.
 */
object AnthropicProvider : AiProvider {
    override val id: AiProviderId = AiProviderId.ANTHROPIC

    private const val BASE = "https://api.anthropic.com/v1"
    private const val ANTHROPIC_VERSION = "2023-06-01"

    private fun authHeaders(apiKey: String): Map<String, String> = mapOf(
        "x-api-key" to apiKey,
        "anthropic-version" to ANTHROPIC_VERSION,
    )

    override suspend fun listModels(context: Context, apiKey: String): List<AiModel> = withContext(Dispatchers.IO) {
        val conn = Http.open("$BASE/models", "GET", authHeaders(apiKey))
        Http.ensureOk(conn, id.displayName)
        val raw = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val data = JSONObject(raw).optJSONArray("data") ?: return@withContext emptyList()
        (0 until data.length()).mapNotNull { i ->
            val obj = data.optJSONObject(i) ?: return@mapNotNull null
            val rawId = obj.optString("id", "")
            if (rawId.isBlank()) return@mapNotNull null
            AiModel(
                providerId = id,
                id = rawId,
                displayName = obj.optString("display_name", rawId),
                capabilities = CapabilityClassifier.classify(id, rawId),
            )
        }
    }

    override suspend fun chat(
        context: Context,
        modelId: String,
        messages: List<AiMessage>,
        apiKey: String,
        onChunk: (String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        val msgs = JSONArray()
        messages.forEach { msg ->
            val role = if (msg.role == "system") "user" else msg.role  // Anthropic doesn't accept "system" inside messages
            val content = JSONArray()
            if (msg.imageBase64 != null) {
                content.put(
                    JSONObject()
                        .put("type", "image")
                        .put(
                            "source",
                            JSONObject()
                                .put("type", "base64")
                                .put("media_type", "image/jpeg")
                                .put("data", msg.imageBase64),
                        ),
                )
            }
            val textBlock = if (msg.fileContent != null) {
                if (msg.content.isNotBlank()) "${msg.content}\n\n--- File content ---\n${msg.fileContent}" else msg.fileContent
            } else msg.content
            if (textBlock.isNotBlank()) content.put(JSONObject().put("type", "text").put("text", textBlock))

            msgs.put(JSONObject().put("role", role).put("content", content))
        }

        val body = JSONObject()
            .put("model", modelId)
            .put("messages", msgs)
            .put("system", SystemPrompts.DEFAULT)
            .put("max_tokens", 4096)
            .put("stream", true)
            .toString()

        val conn = Http.postJson("$BASE/messages", body, authHeaders(apiKey))
        Http.ensureOk(conn, id.displayName)

        val sb = StringBuilder()
        Http.iterateSse(conn) { data ->
            try {
                val event = JSONObject(data)
                if (event.optString("type") == "content_block_delta") {
                    val delta = event.optJSONObject("delta") ?: return@iterateSse
                    val text = delta.optString("text", "")
                    if (text.isNotEmpty()) {
                        sb.append(text)
                        onChunk(text)
                    }
                }
            } catch (_: Exception) { /* keep-alive frame */ }
        }
        conn.disconnect()
        sb.toString()
    }
}
