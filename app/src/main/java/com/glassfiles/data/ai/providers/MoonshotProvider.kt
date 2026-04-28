package com.glassfiles.data.ai.providers

import android.content.Context
import com.glassfiles.data.ai.SystemPrompts
import com.glassfiles.data.ai.models.AiProviderId

object MoonshotProvider : OpenAiCompatProvider(
    id = AiProviderId.MOONSHOT,
    systemPrompt = SystemPrompts.DEFAULT,
) {
    override fun baseUrl(context: Context): String = "https://api.moonshot.ai/v1"
}
