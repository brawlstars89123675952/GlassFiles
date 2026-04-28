package com.glassfiles.data.ai.models

/**
 * Chat-style message that flows between the UI and a provider.
 *
 * For backwards compatibility with the older `ChatMessage` type, [imageBase64]
 * and [fileContent] are kept as flat optional fields; richer multimodal payloads
 * (e.g. multiple attachments) can be modelled later via a dedicated parts list.
 *
 * [role] follows OpenAI conventions: `"user"`, `"assistant"`, `"system"`.
 */
data class AiMessage(
    val role: String,
    val content: String,
    val imageBase64: String? = null,
    val fileContent: String? = null,
)
