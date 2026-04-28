package com.glassfiles.data.ai.providers

import android.content.Context
import com.glassfiles.data.ai.SystemPrompts
import com.glassfiles.data.ai.models.AiProviderId

object OpenRouterProvider : OpenAiCompatProvider(
    id = AiProviderId.OPENROUTER,
    systemPrompt = SystemPrompts.DEFAULT,
) {
    override fun baseUrl(context: Context): String = "https://openrouter.ai/api/v1"

    /** OpenRouter recommends sending a referrer + app title for analytics / rate-limits. */
    override fun extraHeaders(context: Context): Map<String, String> = mapOf(
        "HTTP-Referer" to "https://glassfiles.ru",
        "X-Title" to "GlassFiles",
    )
}
