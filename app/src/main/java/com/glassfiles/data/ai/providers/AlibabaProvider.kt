package com.glassfiles.data.ai.providers

import android.content.Context
import com.glassfiles.data.ai.AiKeyStore
import com.glassfiles.data.ai.SystemPrompts
import com.glassfiles.data.ai.models.AiProviderId

object AlibabaProvider : OpenAiCompatProvider(
    id = AiProviderId.ALIBABA,
    systemPrompt = SystemPrompts.DEFAULT,
) {
    override fun baseUrl(context: Context): String {
        val region = AiKeyStore.getQwenRegion(context)
        return when (region) {
            "cn" -> "https://dashscope.aliyuncs.com/compatible-mode/v1"
            "us" -> "https://dashscope-us.aliyuncs.com/compatible-mode/v1"
            "hk" -> "https://cn-hongkong.dashscope.aliyuncs.com/compatible-mode/v1"
            else -> "https://dashscope-intl.aliyuncs.com/compatible-mode/v1"
        }
    }
}
