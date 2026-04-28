package com.glassfiles.data.ai.models

/**
 * Stable identifier for an AI provider. Used as a key everywhere (preferences,
 * registry cache, chat sessions, etc.) — never expose the enum's `name` to UI;
 * use [displayName] instead.
 */
enum class AiProviderId(val displayName: String, val keyPrefsKey: String) {
    OPENAI("OpenAI", "ai_key_openai"),
    ANTHROPIC("Anthropic", "ai_key_anthropic"),
    GOOGLE("Google", "ai_key_google"),
    XAI("xAI (Grok)", "ai_key_xai"),
    MOONSHOT("Moonshot (Kimi)", "ai_key_moonshot"),
    ALIBABA("Alibaba (Qwen)", "ai_key_alibaba"),
    OPENROUTER("OpenRouter", "ai_key_openrouter"),
}
