package com.glassfiles.data.ai.providers

import android.content.Context
import com.glassfiles.data.ai.SystemPrompts
import com.glassfiles.data.ai.models.AiProviderId

object XaiProvider : OpenAiCompatProvider(
    id = AiProviderId.XAI,
    systemPrompt = SystemPrompts.DEFAULT,
) {
    override fun baseUrl(context: Context): String = "https://api.x.ai/v1"
}
