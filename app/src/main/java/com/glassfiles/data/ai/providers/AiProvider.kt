package com.glassfiles.data.ai.providers

import android.content.Context
import com.glassfiles.data.ai.models.AiMessage
import com.glassfiles.data.ai.models.AiModel
import com.glassfiles.data.ai.models.AiProviderId

/**
 * One entry per provider (OpenAI, Anthropic, Google, ...).
 *
 * Implementations live in this package and are registered in [AiProviders].
 * The dispatcher ([com.glassfiles.data.ai.AiManager]) selects an implementation
 * by [AiProviderId] and forwards calls.
 */
interface AiProvider {
    val id: AiProviderId

    /**
     * Fetches the live model catalog from the provider's `/models`-style endpoint.
     *
     * Implementations should NOT cache — that's [com.glassfiles.data.ai.ModelRegistry]'s
     * job. Callers pass a non-blank API key. Errors should propagate as exceptions
     * with a message safe to show in a Toast.
     */
    suspend fun listModels(context: Context, apiKey: String): List<AiModel>

    /**
     * Streams a chat completion. [onChunk] is called on the IO dispatcher with
     * each delta. Returns the full assembled response string.
     *
     * Throws on HTTP error / auth failure.
     */
    suspend fun chat(
        context: Context,
        modelId: String,
        messages: List<AiMessage>,
        apiKey: String,
        onChunk: (String) -> Unit,
    ): String

    /**
     * Optional: generates one or more images. Returns local file paths in
     * [Context.cacheDir] / `ai_images/`. Implementations that don't support image
     * generation throw [UnsupportedOperationException] (default below).
     */
    suspend fun generateImage(
        context: Context,
        modelId: String,
        prompt: String,
        apiKey: String,
        size: String = "1024x1024",
        n: Int = 1,
    ): List<String> = throw UnsupportedOperationException("${id.displayName} doesn't support image generation in this build")

    /**
     * Optional: generates a video. Returns the local file path of the produced
     * mp4. Implementations may poll a job-status endpoint internally.
     */
    suspend fun generateVideo(
        context: Context,
        modelId: String,
        prompt: String,
        apiKey: String,
        durationSec: Int = 5,
        aspectRatio: String = "16:9",
        onProgress: (String) -> Unit = {},
    ): String = throw UnsupportedOperationException("${id.displayName} doesn't support video generation in this build")
}
